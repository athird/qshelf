package uk.ac.open.kmi.carre.qs.service.withings;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateFormatUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.WithingsApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import uk.ac.open.kmi.carre.qs.metrics.Activity;
import uk.ac.open.kmi.carre.qs.metrics.BloodPressure;
import uk.ac.open.kmi.carre.qs.metrics.Height;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.O2Saturation;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Sleep;
import uk.ac.open.kmi.carre.qs.metrics.SleepRecord;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class WithingsService extends Service {
	public static final String name = "Withings API";
	public static final String machineName = "withings";
	public static final int version = 1;

	public static final String baseURL = "http://wbsapi.withings.net";

	public static final String requestTokenURL = "https://oauth.withings.com/account/request_token";
	public static final String authURL = "https://oauth.withings.com/account/authorize";
	public static final String accessTokenURL = "https://oauth.withings.com/account/access_token";

	public static final String REQUEST_TOKEN_SESSION = "WITHINGS_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "WITHINGS_ACCESS_TOKEN_SESSION";

	public static final Verb HTTP_METHOD = Verb.GET;

	public static final int MANUAL_DEVTYPE = 0;
	public static final int SCALE_DEVTYPE = 1;
	public static final int BP_DEVTYPE = 4;
	public static final int TRACKER_DEVTYPE = 16;
	public static final int PULSE_DEVTYPE = BP_DEVTYPE | TRACKER_DEVTYPE;

	public static final int ACTUAL_MEASUREMENT = 1;
	public static final int GOAL_MEASUREMENT = 2;

	public static final int WEIGHT_MEASUREMENT = 1;
	public static final int HEIGHT_MEASUREMENT = 4;
	public static final int LEAN_MASS = 5;
	public static final int FAT_PERCENTAGE = 6;
	public static final int FAT_MASS = 8;
	public static final int DIASTOLIC = 9;
	public static final int SYSTOLIC = 10;
	public static final int PULSE_RATE = 11;
	public static final int SPO2 = 54;

	public static final int AWAKE = 0;
	public static final int LIGHT_SLEEP = 1;
	public static final int DEEP_SLEEP = 2;
	public static final int REM_SLEEP = 3;

	public static final long SECONDS_PER_DAY = 24 * 60 * 60;
	private static final long MILLISECONDS_PER_SECOND = 1000;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + "withings";

	public static final String PROVENANCE = RDF_SERVICE_NAME;

	private OAuthService service = null;
	private String userId = "";
	private RDFAbleToken accessToken = null;

	public WithingsService(String propertiesPath) {
		super(propertiesPath);
	}

	@Override
	public void handleNotification(HttpServletRequest request, HttpServletResponse response) {
		String startDateString = request.getParameter("startdate");
		String endDateString = request.getParameter("enddate");
		String useridentifier = request.getParameter("userid");
		long startTime = Long.parseLong(startDateString);
		long endTime = Long.parseLong(endDateString);
		Date startDate = new Date(startTime * MILLISECONDS_PER_SECOND);
		Date endDate = new Date(endTime * MILLISECONDS_PER_SECOND);

		RDFAbleToken userToken = getTokenForUser(useridentifier);
		if (userToken != null && !userToken.getUser().equals("")) {
			RDFAbleToken oldAccessToken = accessToken;
			accessToken = userToken;
			String oldUserId = userId;
			userId = useridentifier;
			if (service == null) {
				service = new ServiceBuilder()
				.provider(WithingsApi.class)
				.apiKey(oauth_token)
				.apiSecret(oauth_secret)
				.signatureType(SignatureType.QueryString)
				//.build();
				.callback("")
				.build();
			}

			List<Metric> newMetrics = new ArrayList<Metric>();
			newMetrics.addAll(getMetrics(startDate, endDate));

			String subscriptionId = userToken.getUser()
					.replaceAll("<" + CARREVocabulary.BASE_URL, "")
					.replaceAll(">", "");

			String rdf = "";
			for (Metric metric : newMetrics) {
				rdf += metric.getMeasuredByRDF(PROVENANCE);
				rdf += metric.toRDFString();
			}
			accessToken = oldAccessToken;
			userId = oldUserId;

			System.err.println(rdf);
			if (!rdf.equals("")) {
				CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
				boolean success = true;
				List<String> triples = Service.chunkRDF(rdf);
				String userId = subscriptionId.substring(CARREVocabulary.USER_URL.length());
				System.err.println("USERID is " + userId);
				for (String tripleSet : triples) {
					success &= connector.insertTriples(userId, tripleSet);
				}
				if (!success) {
					System.err.println("Failed to insert triples.");
				}
			}
		}
	}

	public RDFAbleToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?user ?oauth_token ?oauth_secret WHERE {\n" + 
				"GRAPH ?user {\n ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				" ?connection <" + 
				CARREVocabulary.USER_ID_PREDICATE + "> \"" + userId + "\"" + CARREVocabulary.STRING_TYPE +  " .\n" +
				" ?connection <" 
				+ CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n" +
				" ?connection <" + 
				CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret. }\n}\n";
		
		System.err.println(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			Literal secretLiteral = solution.getLiteral("oauth_secret");
			Resource userResource = solution.getResource("user");
			if (tokenLiteral == null || secretLiteral == null || userResource == null) {
				System.err.println("Token, secret literal or user id is null!");
				return null;
			}
			String oauth_token = tokenLiteral.getString();
			String oauth_secret = secretLiteral.getString();
			String user = userResource.getURI();
			System.err.println("token literal is " + oauth_token + 
					", secret literal is " + oauth_secret + 
					", user is " + user);
			RDFAbleToken token = new RDFAbleToken(oauth_token, oauth_secret);
			token.setUser(user);
			return token;
		}
		return null;
	}

	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		if (service == null) {
			service = new ServiceBuilder()
			.provider(WithingsApi.class)
			.apiKey(oauth_token)
			.apiSecret(oauth_secret)
			.signatureType(SignatureType.QueryString)
			//.build();
			.callback(callback)
			.build();
		}



		String oauthToken = request.getParameter("oauth_token");
		String oauthTokenSecret = request.getParameter("oauth_token_secret");
		if (oauthTokenSecret == null || oauthTokenSecret.equals("")) {
			oauthTokenSecret = request.getParameter("oauth_verifier");
		}

		userId = request.getParameter("userid");

		if (!((oauthToken != null && !oauthToken.equals("")) ||
				oauthTokenSecret != null && !oauthTokenSecret.equals(""))) {
			Token requestToken = service.getRequestToken();
			System.err.println("RequestToken:" + requestToken.getToken() 
					+ ", " + requestToken.getSecret());
			request.getSession().setAttribute(machineName + "reqtoken", requestToken.getToken());
			request.getSession().setAttribute(machineName + "reqsec", requestToken.getSecret());

			String authURL= service.getAuthorizationUrl(requestToken);

			System.err.println(authURL);
			return response.encodeRedirectURL(authURL);
		} else {
			System.err.println("oauthTokenSecret" + oauthTokenSecret);
			Verifier verifier = new Verifier(oauthTokenSecret);
			Token requestToken = new Token((String) request.getSession().getAttribute(machineName + "reqtoken"),
					(String) request.getSession().getAttribute(machineName + "reqsec"));

			Token tmpAccessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);
			accessToken = new RDFAbleToken(tmpAccessToken.getToken(), tmpAccessToken.getSecret());

			System.err.println("accessToken: " + accessToken.getToken());
			System.err.println("accessTokenSecret: " + accessToken.getSecret());
			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,04,19);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				System.err.println(metric.getMeasuredByRDF(PROVENANCE));
				System.err.println(metric.toRDFString());
			}
			return "";
		}
	}

	private List<Sleep> getSleepBetween(Date startDate, Date endDate) {
		List<Sleep> results = new ArrayList<Sleep>();
		Sleep sleep = new Sleep(machineName, startDate);
		Map<String,String> parameters = getDefaultParams("get");
		long dateEpoch = startDate.getTime() / MILLISECONDS_PER_SECOND;
		long endEpoch = endDate.getTime() / MILLISECONDS_PER_SECOND;
		parameters.put("startdate", "" + dateEpoch);
		parameters.put("enddate", "" + endEpoch);
		String sleepRecord = makeApiCall("/v2/sleep", parameters);
		sleep = parseSleep(sleep, sleepRecord);
		List<SleepRecord> dailyRecords = sleep.getSleepRecords();
		List<SleepRecord> currentDailyRecords = new ArrayList<SleepRecord>();
		String currentDate = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		for (SleepRecord record : dailyRecords) {
			String timestamp = DateFormatUtils.format(record.getStartDate(), "yyyy-MM-dd");
			if (timestamp.equals(currentDate)) {
				currentDailyRecords.add(record);
			} else {
				sleep.setSleepRecords(currentDailyRecords);
				results.add(sleep);
				sleep = new Sleep(machineName, record.getStartDate());
				currentDailyRecords = new ArrayList<SleepRecord>();
				currentDate = timestamp;
				currentDailyRecords.add(record);
			}
		}
		sleep.setSleepRecords(currentDailyRecords);
		sleep = computeSleepSummaries(sleep);
		results.add(sleep);
		return results;
	}

	private Sleep getSleep(Date date) {
		Sleep sleep = new Sleep(machineName, date);
		Map<String,String> parameters = getDefaultParams("get");
		long dateEpoch = date.getTime() / MILLISECONDS_PER_SECOND;
		long nextDay = dateEpoch + SECONDS_PER_DAY;
		parameters.put("startdate", "" + dateEpoch);
		parameters.put("enddate", "" + nextDay);
		String sleepRecord = makeApiCall("/v2/sleep", parameters);
		sleep = parseSleep(sleep, sleepRecord);
		return sleep;
	}


	private Sleep parseSleep(Sleep sleep, String sleepString) {
		JSONObject json = getBodyJson(sleepString);
		if (json == null || json.keySet() == null || json.keySet().size() == 0) {
			return sleep;
		}
		return parseSingleSleep(sleep, json);
	}


	private Sleep parseSingleSleep(Sleep sleep, JSONObject sleepJson) {
		JSONArray series = (JSONArray) sleepJson.get("series");
		for (int i = 0; i < series.size(); i++) {
			JSONObject sleepRecordJson = (JSONObject) series.get(i);
			SleepRecord sleepRecord = parseSleepRecord(sleep.getDate(), sleepRecordJson);
			sleep.addSleepRecord(sleepRecord);
		}
		return sleep;
	}

	private SleepRecord parseSleepRecord(Date date, JSONObject sleepJson) {
		long startTime = (Long) sleepJson.get("startdate");
		int sleepStatus = ((Long) sleepJson.get("state")).intValue();;
		long endTime = (Long) sleepJson.get("enddate");
		SleepRecord sleepRecord = new SleepRecord(machineName, date);
		switch(sleepStatus) {
		case AWAKE:
			sleepRecord.setSleepStatus(SleepRecord.AWAKE);
			break;
		case LIGHT_SLEEP:
			sleepRecord.setSleepStatus(SleepRecord.LIGHT_SLEEP);
		case DEEP_SLEEP:
			sleepRecord.setSleepStatus(SleepRecord.DEEP_SLEEP);
		case REM_SLEEP:
			sleepRecord.setSleepStatus(SleepRecord.REM_SLEEP);
		default:
			break;
		}

		Date startDate = new Date(startTime * MILLISECONDS_PER_SECOND);
		Date endDate = new Date(endTime * MILLISECONDS_PER_SECOND);
		sleepRecord.setStartDate(startDate);
		sleepRecord.setEndDate(endDate);
		return sleepRecord;
	}

	private Sleep computeSleepSummaries(Sleep sleep) {
		sleep.setTimesAwake(0);
		sleep.setTimesLightlyAsleep(0);
		sleep.setTimesDeeplyAsleep(0);
		sleep.setTimesRemAsleep(0);

		sleep.setAwakeDuration(0);
		sleep.setRemDuration(0);
		sleep.setDeepSleepDuration(0);
		sleep.setLightSleepDuration(0);

		String lastStatus = "";

		long totalSleepTime = 0;
		for (SleepRecord record : sleep.getSleepRecords()) {

			long interval = record.getEndDate().getTime() - record.getStartDate().getTime();

			if (record.getSleepStatus() == CARREVocabulary.AWAKE) {
				if (lastStatus != CARREVocabulary.AWAKE) {
					sleep.setTimesAwake(sleep.getTimesAwake() + 1);
				}
				sleep.setAwakeDuration(sleep.getAwakeDuration() + interval);

			} else if (record.getSleepStatus() == CARREVocabulary.LIGHT_SLEEP) {
				if (lastStatus != CARREVocabulary.LIGHT_SLEEP) {
					sleep.setTimesLightlyAsleep(sleep.getTimesLightlyAsleep() + 1);
				}
				sleep.setLightSleepDuration(sleep.getLightSleepDuration() + interval);
				totalSleepTime += interval;
			} else if (record.getSleepStatus() == CARREVocabulary.DEEP_SLEEP) {
				if (lastStatus != CARREVocabulary.DEEP_SLEEP) {
					sleep.setTimesDeeplyAsleep(sleep.getTimesDeeplyAsleep() + 1);
				}
				sleep.setDeepSleepDuration(sleep.getDeepSleepDuration() + interval);
				totalSleepTime += interval;
			} else if (record.getSleepStatus() == CARREVocabulary.REM_SLEEP) {
				if (lastStatus != CARREVocabulary.REM_SLEEP) {
					sleep.setTimesRemAsleep(sleep.getTimesRemAsleep() + 1);
				}
				sleep.setRemDuration(sleep.getRemDuration() + interval);
				totalSleepTime += interval;
			}
			lastStatus = record.getSleepStatus();
		}
		sleep.setSleepTime(totalSleepTime);
		return sleep;
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		Map<String, String> parameters = getDefaultParams("getmeas");
		System.err.println(DateFormatUtils.format(startDate, "yyyy-MM-dd"));
		System.err.println(DateFormatUtils.format(endDate, "yyyy-MM-dd"));
		parameters.put("startdate", "" + (startDate.getTime()/MILLISECONDS_PER_SECOND));
		parameters.put("enddate", "" + (endDate.getTime()/MILLISECONDS_PER_SECOND));
		parameters.put("category", "" + ACTUAL_MEASUREMENT);
		parameters.put("lastupdate", "" + (startDate.getTime()/MILLISECONDS_PER_SECOND));
		String metricRecord = makeApiCall("/measure", parameters);
		results = parseMetrics(results, metricRecord);

		Date backupStart = new Date(startDate.getTime());
		Date backupEnd = new Date(endDate.getTime());

		results.addAll(getActivityBetween(startDate, endDate));
		results.addAll(getSleepBetween(backupStart, backupEnd));
		return results;
	}

	private List<Metric> parseMetrics(List<Metric> results, String jsonString) {
		JSONObject json = getBodyJson(jsonString);
		if (json == null || json.keySet() == null || json.keySet().size() == 0) {
			return results;
		}
		JSONArray measureGroups = (JSONArray) json.get("measuregrps");
		for (int i = 0; i < measureGroups.size(); i++) {		
			JSONObject measureGroup = (JSONObject) measureGroups.get(i);
			String id = machineName + (Long) measureGroup.get("grpid");
			int provenance = ((Long) measureGroup.get("attrib")).intValue();
			long dateEpoch = (Long) measureGroup.get("date");
			Date date = new Date(dateEpoch * MILLISECONDS_PER_SECOND);
			long category = (Long) measureGroup.get("category");
			String actuality = "";
			if (category == ACTUAL_MEASUREMENT) {
				actuality = Metric.ACTUALITY_ACTUAL;
			} else if (category == GOAL_MEASUREMENT) {
				actuality = Metric.ACTUALITY_GOAL;
			} else {
				actuality = Metric.ACTUALITY_ACTUAL;
			}
			JSONArray measures = (JSONArray) measureGroup.get("measures");


			List<Metric> measureGroupResults = new ArrayList<Metric>();

			for (int j = 0; j < measures.size(); j++) {
				JSONObject measure = (JSONObject) measures.get(j);
				measureGroupResults = parseSingleMetric(measureGroupResults, date, id, measure);

			}
			for (Metric metric : measureGroupResults) {
				metric.setActuality(actuality);
				metric.setProvenance(provenance);
			}
			results.addAll(measureGroupResults);
		}
		return results;
	}

	private List<Metric> parseSingleMetric(List<Metric> results, Date date, String id, JSONObject measureJson) {
		int type = ((Long) measureJson.get("type")).intValue();
		long value = (Long) measureJson.get("value");
		int unit = ((Long) measureJson.get("unit")).intValue();
		double actualValue = value * Math.pow(10, unit);

		switch (type) {
		case HEIGHT_MEASUREMENT:
			Height height = new Height(id, date);
			height.setId(id);
			height.setHeight(actualValue);
			results.add(height);
			break;
		case DIASTOLIC:
			boolean foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof BloodPressure) {
					BloodPressure bp = (BloodPressure) metric;
					if (bp.getDiastolicBloodPressure() == Metric.NO_VALUE_PROVIDED 
							&& bp.getId().equals(id)) {
						bp.setDiastolicBloodPressure(new Double(actualValue).longValue());
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				BloodPressure bp = new BloodPressure(id, date);
				bp.setId(id);
				bp.setDiastolicBloodPressure(new Double(actualValue).longValue());
				results.add(bp);
			}
			break;
		case SYSTOLIC:
			foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof BloodPressure) {
					BloodPressure bp = (BloodPressure) metric;
					if (bp.getSystolicBloodPressure() == Metric.NO_VALUE_PROVIDED
							&& bp.getId().equals(id)) {
						bp.setSystolicBloodPressure(new Double(actualValue).longValue());
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				BloodPressure bp = new BloodPressure(id, date);
				bp.setId(id);
				bp.setSystolicBloodPressure(new Double(actualValue).longValue());
				results.add(bp);
			}
			break;
		case SPO2:
			O2Saturation o2sat = new O2Saturation(id, date);
			o2sat.setO2saturation(actualValue);
			results.add(o2sat);
			break;
		case PULSE_RATE:
			Pulse pulse = new Pulse(id, date);
			pulse.setPulse(new Double(actualValue).longValue());
			results.add(pulse);
			break;
		case WEIGHT_MEASUREMENT:
			foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof Weight) {
					Weight weight = (Weight) metric;
					if (weight.getWeight() == Metric.NO_VALUE_PROVIDED
							&& weight.getId().equals(id)) {
						weight.setWeight(actualValue);
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				Weight weight = new Weight(id, date);
				weight.setId(id);
				weight.setWeight(actualValue);
				results.add(weight);
			}
			break;
		case LEAN_MASS:
			foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof Weight) {
					Weight weight = (Weight) metric;
					if (weight.getLeanMass() == Metric.NO_VALUE_PROVIDED 
							&& weight.getId().equals(id)) {
						weight.setLeanMass(actualValue);
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				Weight weight = new Weight(id, date);
				weight.setId(id);
				weight.setLeanMass(actualValue);
				results.add(weight);
			}
			break;
		case FAT_PERCENTAGE:
			foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof Weight) {
					Weight weight = (Weight) metric;
					if (weight.getBodyFat() == Metric.NO_VALUE_PROVIDED
							&& weight.getId().equals(id)) {
						weight.setBodyFat(actualValue);
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				Weight weight = new Weight(id, date);
				weight.setId(id);
				weight.setBodyFat(actualValue);
				results.add(weight);
			}
			break;
		case FAT_MASS:
			foundExisting = false;
			for (Metric metric : results) {
				if (metric instanceof Weight) {
					Weight weight = (Weight) metric;
					if (weight.getFatMass() == Metric.NO_VALUE_PROVIDED
							&& weight.getId().equals(id)) {
						weight.setFatMass(actualValue);
						foundExisting = true;
					}
				}
			}
			if (!foundExisting) {
				Weight weight = new Weight(id, date);
				weight.setId(id);
				weight.setFatMass(actualValue);
				results.add(weight);
			}
			break;
		default:
			break;
		}
		return results;
	}

	private List<Activity> getActivityBetween(Date startDate, Date endDate) {
		List<Activity> results = new ArrayList<Activity>();
		String endDateString = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		String dateString = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		Date date = startDate;
		while(!dateString.equals(endDateString)) {
			Activity activity = getActivity(date);
			results.add(activity);
			date.setTime(date.getTime() + (SECONDS_PER_DAY * MILLISECONDS_PER_SECOND));
			dateString = DateFormatUtils.format(date, "yyyy-MM-dd");
		}
		return results;
	}

	private Activity getActivity(Date date) {
		Activity activity = new Activity(machineName, date);
		Map<String,String> parameters = getDefaultParams("getactivity");
		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd");
		parameters.put("date", dateString);
		System.err.println(dateString);
		String activityRecord = makeApiCall("/v2/measure", parameters);
		System.err.println(activityRecord);
		return parseActivity(activity, activityRecord);
	}

	private Activity parseActivity(Activity activity, String activityString) {
		JSONObject json = getBodyJson(activityString);
		if (json == null || json.keySet() == null || json.keySet().size() == 0) {
			return activity;
		}
		return parseSingleActivity(activity, json);
	}

	private Activity parseSingleActivity(Activity activity, JSONObject activityJson) {
		activity.setSteps(((Long) activityJson.get("steps")).intValue());
		try {
			activity.setDistance(((Double) activityJson.get("distance")).floatValue());
		} catch(ClassCastException e) {
			activity.setDistance(0);
		}
		try {
			activity.setCalories(((Double) activityJson.get("calories")).floatValue());
		} catch (ClassCastException e) {
			activity.setCalories(0);
		}
		try {
			activity.setElevation(((Double) activityJson.get("elevation")).floatValue());
		} catch (ClassCastException e) {
			activity.setElevation(0);
		}
		activity.setLightActivityDuration(((Long) activityJson.get("soft")).intValue());
		activity.setModerateActivityDuration(((Long) activityJson.get("moderate")).intValue());
		activity.setIntenseActivityDuration(((Long) activityJson.get("intense")).intValue());
		activity.setTimezone((String) activityJson.get("timezone"));
		return activity;
	}

	private String makeApiCall(String apiMethod, Map<String,String> parameters) {
		OAuthRequest serviceRequest = new OAuthRequest(HTTP_METHOD, 
				baseURL + apiMethod);

		System.err.println(baseURL + apiMethod);

		for (String paramName : parameters.keySet()) {
			System.err.println(paramName + "=" + parameters.get(paramName));
			serviceRequest.addQuerystringParameter(paramName, 
					parameters.get(paramName));
		}



		service.signRequest(accessToken, serviceRequest); 


		Response requestResponse = serviceRequest.send();
		String output = requestResponse.getBody();
		System.err.println(output);
		return output;
	}

	private JSONObject getBodyJson(String jsonString) {
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		long status = (Long) json.get("status");
		if (status != 0) {
			return null;
		}
		JSONObject body = (JSONObject) json.get("body");
		return body;
	}

	private Map<String,String> getDefaultParams(String action) {
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("action", action);
		parameters.put("userid", userId);
		return parameters;
	}

	@Override
	public String getProvenance() {
		return PROVENANCE;
	}

}
