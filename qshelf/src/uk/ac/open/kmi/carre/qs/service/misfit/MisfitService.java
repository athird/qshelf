package uk.ac.open.kmi.carre.qs.service.misfit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
import uk.ac.open.kmi.carre.qs.metrics.Body;
import uk.ac.open.kmi.carre.qs.metrics.Food;
import uk.ac.open.kmi.carre.qs.metrics.Glucose;
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

public class MisfitService extends Service {

	private static Logger logger = Logger.getLogger(MisfitService.class.getName());
	
	public static final int MAX_DAYS_RETRIEVABLE = 31;

	public MisfitService(String propertiesPath) {
		super(propertiesPath);
	}

	public static final String name = "Misfit API";
	public static final String machineName = "misfit";
	public static final int version = 1;

	public static final String baseURL = "https://api.misfitwearables.com";

	public static final String requestTokenURL = baseURL + "/auth/tokens/exchange";
	public static final String authURL = baseURL + "/auth/dialog/authorize";
	public static final String accessTokenURL = requestTokenURL;

	public static final String REQUEST_TOKEN_SESSION = "MISFIT_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "MISFIT_ACCESS_TOKEN_SESSION";

	private OAuthService service = null;
	private String userId = "";
	private RDFAbleToken accessToken = null;

	public static final int AWAKE = 1;
	public static final int ASLEEP = 2;
	public static final int DEEP_SLEEP = 3;

	public static final int MILLISECONDS_PER_SECOND = 1000;
	public static final int SECONDS_PER_MINUTE = 60;
	public static final int MINUTES_PER_HOUR = 60;
	public static final int HOURS_PER_DAY = 24;
	public static final long MILLISECONDS_PER_DAY = HOURS_PER_DAY 
			* MINUTES_PER_HOUR
			* SECONDS_PER_MINUTE
			* MILLISECONDS_PER_SECOND;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + "misfit";

	public static final String PROVENANCE = RDF_SERVICE_NAME;

	@Override
	public void handleNotification(String requestContent) {
		String json = requestContent;
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(json);
		if (jsonObject == null) {
			logger.finer("json wouldn't parse.");
			logger.finer(json);
		}
		String notificationType = (String) jsonObject.get("Type");
		if (notificationType.equals("SubscriptionConfirmation")) {
			String confirmURL = (String) jsonObject.get("SubscribeURL");
			logger.finer("Got subscription confirmation: " + confirmURL);
			HttpURLConnection conn;
			try {
				conn = (HttpURLConnection) (new URL(confirmURL)).openConnection();

				conn.setRequestMethod("GET");
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
				}

				BufferedReader br = new BufferedReader(new InputStreamReader(
						(conn.getInputStream())));

				String output;
				logger.finer("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					logger.finer(output);
				}

				conn.disconnect();

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (notificationType.equals("Notification")) {
			logger.finer("JSON string: " + jsonObject.toJSONString());

			JSONArray jsonArray = null; 
			try {
				jsonArray = (JSONArray) jsonObject.get("Message");
			} catch (ClassCastException e) {
				String messageString = (String) jsonObject.get("Message");
				logger.finer("Message is a quoted string.");
				logger.finer(messageString);
				if (messageString.startsWith("\"") && messageString.endsWith("\"") ) {
					logger.finer("Removing quoting on Message.");
					messageString = messageString.substring(1, messageString.length() - 2);
					jsonArray = (JSONArray) JSONValue.parse(messageString);
				} else {
					jsonArray = (JSONArray) JSONValue.parse(messageString);
				}
			}
			if (jsonArray == null) {
				String messageString = (String) jsonObject.get("Message");
				logger.finer("Initial message string: " + messageString);
				messageString = messageString.replaceAll("\\\"", "\"");
				logger.finer("New message string: " + messageString);
				jsonArray = (JSONArray) JSONValue.parse(messageString);
				
			}
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject notifyJson = (JSONObject) jsonArray.get(i);
				String type = (String) notifyJson.get("type");
				String dateString = (String) notifyJson.get("updatedAt");
				String startDateString = (String) notifyJson.get("dataFrom");
				String ownerId = (String) notifyJson.get("ownerId");
				String action = (String) notifyJson.get("action");
				String dataObjectId = (String) notifyJson.get("id");
				logger.finer(type + ", " +dateString + ", " +ownerId + 
						", " +action);
				if (dataObjectId != null) {
					logger.finer("dataObjectId " + dataObjectId);
				}
				if (startDateString != null) {
					logger.finer("notification start date " + startDateString);
				}
				handleNotification(type, dateString, startDateString, ownerId, 
						action, dataObjectId);
			}

		}
	}

	public void handleNotification(String notificationType, String dateString, String startDateString,
			String ownerId, String action, String dataObjectId) {
		RDFAbleToken token = getTokenForUser(ownerId);
		if (token != null ) {
			logger.finer("Token is not null.");
			RDFAbleToken oldAccessToken = accessToken;
			accessToken = token;
			if (service == null ) {
				service = new ServiceBuilder()
				.provider(MisfitApi.class)
				.apiKey(oauth_token)
				.apiSecret(oauth_secret)
				.signatureType(SignatureType.Header)
				//.build();
				.callback("")
				.build();
			}
			Date date = new Date();
			try {
				logger.finer("dateString: " + dateString);
				if (dateString.endsWith("Z")) {
					dateString = dateString.substring(0, dateString.length() - 1) + " UTC";
				}
				if (dateString.contains("T")) {
					dateString = dateString.replaceAll("([0-9])T([0-9])", "$1 $2");
				}
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				date = format.parse(dateString);
			} catch (ParseException e) {
				logger.finer(e.getMessage());
			}
			logger.finer("Parsed date.");
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DATE, -1);
			Date previousDate = cal.getTime();

			if (startDateString != null && !startDateString.equals("")) {
				logger.finer("startDateString is present: " + startDateString);
				try {
					if (startDateString.endsWith("Z")) {
						startDateString = startDateString.substring(0, startDateString.length() - 1) + " UTC";
					}
					if (startDateString.contains("T")) {
						startDateString = startDateString.replaceAll("([0-9])T([0-9])", "$1 $2");
					}
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
					previousDate = format.parse(startDateString);
				} catch (ParseException e) {
					logger.finer(e.getMessage());
				}
				logger.finer("Parsed startDateString");
			}

			List<Metric> newMetrics = new ArrayList<Metric>();

			if (notificationType.equals("sessions") || notificationType.equals("summary")) {
				if (dataObjectId != null && !dataObjectId.equals("")) {
					logger.finer("Fetching specific " + notificationType + " record: " + dataObjectId);
					newMetrics.addAll(getActivities(date, null, dataObjectId));
					logger.finer("Found " + newMetrics.size() + " records.");
				} else {
					logger.finer("Fetching date-based " + notificationType + " records");
					newMetrics.addAll(getActivities(previousDate, date, dataObjectId));
					logger.finer("Found " + newMetrics.size() + " records.");
				}
			} else if (notificationType.equals("sleeps")) {
				if (dataObjectId != null && !dataObjectId.equals("")) {
					logger.finer("Fetching specific " + notificationType + " record: " + dataObjectId);
					newMetrics.addAll(getSleeps(date, null, dataObjectId));
					logger.finer("Found " + newMetrics.size() + " records.");
				} else {
					logger.finer("Fetching date-based " + notificationType + " records");
					newMetrics.addAll(getSleeps(previousDate, date, dataObjectId));
					logger.finer("Found " + newMetrics.size() + " records.");
				}
			}

			String subscriptionId = ""; 
			if (token.getUser().contains("https:")) {
				subscriptionId = token.getUser()
						.replaceAll("<" + CARREVocabulary.SECURE_BASE_URL, "")
						.replaceAll(">", "");
			} else {
				subscriptionId = token.getUser()
						.replaceAll("<" + CARREVocabulary.BASE_URL, "")
						.replaceAll(">", "");
			}

			String userId = subscriptionId.substring(subscriptionId.lastIndexOf("/") + 1);


			String rdf = "";
			for (Metric metric : newMetrics) {
				rdf += metric.getMeasuredByRDF(PROVENANCE, userId);
				rdf += metric.toRDFString(userId);
			}
			accessToken = oldAccessToken;
			
			logger.finer(rdf);
			//rdf = "";
			if (!rdf.equals("")) {
				CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
				boolean success = true;
				List<String> triples = Service.chunkRDF(rdf);
				for (String tripleSet : triples) {
					success &= connector.insertTriples(userId, tripleSet);
				}
				if (!success) {
					logger.finer("Failed to insert triples.");
				}
			}
		} else {
			logger.finer("Token was null!");
		}
	}

	public RDFAbleToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?user ?oauth_token  WHERE {\n" + 
				"GRAPH ?user {\n ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				" ?connection <" + 
				CARREVocabulary.USER_ID_PREDICATE + "> \"" + userId + "\"" + CARREVocabulary.STRING_TYPE +  " .\n" +
				" ?connection <" 
				+ CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n}\n}\n";

		logger.finer(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			Resource userResource = solution.getResource("user");
			if (tokenLiteral == null || userResource == null) {
				logger.finer("Token or user id is null!");
				return null;
			}
			String oauth_token = tokenLiteral.getString();
			String user = userResource.getURI();
			logger.finer("token literal is " + oauth_token + 
					", user is " + user);
			RDFAbleToken token = new RDFAbleToken(oauth_token, "");
			token.setUser(user);
			return token;
		}
		return null;
	}

	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		if (service == null ) {
			service = new ServiceBuilder()
			.provider(MisfitApi.class)
			.apiKey(oauth_token)
			.apiSecret(oauth_secret)
			.signatureType(SignatureType.Header)
			//.build();
			.callback(callback)
			.build();
		}
		String code = request.getParameter("code");
		logger.finer("code is " + code);

		if (code != null && !code.equals("")) {


			String authURL= accessTokenURL;
			String parameters =	"grant_type=authorization_code"
					+ "&code=" + code
					+ "&redirect_uri=" + callback 
					+ "&client_id=" + oauth_token
					+ "&client_secret=" + oauth_secret;
			HttpURLConnection conn;
			try {
				conn = (HttpURLConnection) (new URL(authURL)).openConnection();

				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				conn.setRequestProperty("Content-Length", "" + parameters.length());
				conn.setDoOutput(true);
				conn.getOutputStream().write(parameters.getBytes());
				conn.getOutputStream().flush();

				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
				}

				BufferedReader br = new BufferedReader(new InputStreamReader(
						(conn.getInputStream())));

				String jsonString = "";
				String output;
				logger.finer("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					logger.finer(output);
					jsonString += output;
				}
				JSONObject json = (JSONObject) JSONValue.parse(jsonString);
				String accessTokenString = (String) json.get("access_token");
				String tokenType = (String) json.get("token_type");
				RDFAbleToken token = new RDFAbleToken(accessTokenString, "");
				accessToken = token;
				logger.finer(machineName + " access_token is " + accessTokenString);
				logger.finer(machineName + " token_type is " + tokenType);
				
				conn.disconnect();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.finer(authURL);



			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,10,10);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				logger.finer(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.finer(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}

			makeApiCall("profile", null, null);
			makeApiCall("device", null, null);

			//			makeApiCall("activity/summary", startDate, today);
			//			makeApiCall("activity/sessions", startDate, today);
			//			makeApiCall("activity/sleeps", startDate, today);


			return "";
		} else {
			Token token = new Token("","");
			String authoriseURL = service.getAuthorizationUrl(token);

			return authoriseURL;

		}
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> allMetrics = new ArrayList<Metric>();
		allMetrics.addAll(getSleeps(startDate, endDate, null));
		allMetrics.addAll(getActivities(startDate, endDate, null));
		return allMetrics;
	}



	public List<Metric> getSleeps(Date startDate, Date endDate, String dataObjectId) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = "";
		if (dataObjectId != null && !dataObjectId.equals("")) {
			jsonString = makeApiCall("activity/sleeps/" + dataObjectId, startDate, endDate);
			if (!jsonString.contains("sleeps")) {
				jsonString = "{\"sleeps\":[" + jsonString + "]}";
			}
		} else {
			jsonString = makeApiCall("activity/sleeps", startDate, endDate);
		}
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray sleeps = (JSONArray) json.get("sleeps");

		for (int i = 0; i < sleeps.size(); i++) {
			JSONObject sleepJson = (JSONObject) sleeps.get(i);
			String logId = "";
			try {
				logId = (String) sleepJson.get("id");
			} catch (ClassCastException e) {
				logId = "" + (Long) sleepJson.get("id");
			}
			String startTime = (String) sleepJson.get("startTime");
			Date sleepStartDate = new Date(startDate.getTime());
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				sleepStartDate = format.parse(startTime.substring(0, startTime.length() - 4));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Sleep sleep = new Sleep(machineName + logId, sleepStartDate);
			long timeInBed = (Long) sleepJson.get("duration");

			sleep.setSleepTime(timeInBed);

			JSONArray minuteRecords = (JSONArray) sleepJson.get("sleepDetails");
			for (int j = 0; j < minuteRecords.size(); j++) {
				JSONObject sleepPeriod = (JSONObject) minuteRecords.get(j);

				Date dateTime = new Date(startDate.getTime());
				String dateTimeString = (String) sleepPeriod.get("datetime");
				try {

					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					sleepStartDate = format.parse(dateTimeString.substring(0, dateTimeString.length() - 4));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long status;
				try {
					status = (Long) sleepPeriod.get("value");
				} catch (ClassCastException e) {
					String statusString = (String) sleepPeriod.get("value");
					status = Long.parseLong(statusString);
				}
				SleepRecord record = new SleepRecord(machineName + logId, startDate);
				record.setStartDate(dateTime);

				if (status == MisfitService.ASLEEP) {
					record.setSleepStatus(SleepRecord.ASLEEP);
				} else if (status == MisfitService.AWAKE) {
					record.setSleepStatus(SleepRecord.AWAKE);
				} else if (status == MisfitService.DEEP_SLEEP) {
					record.setSleepStatus(SleepRecord.DEEP_SLEEP);
				}

				sleep.addSleepRecord(record);
			}

			results.add(sleep);
		}
		return results;
	}

	public List<Metric> getActivities(Date startDate, Date endDate, String dataObjectId) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = ""; 
		String loggedJsonString = "";

		if (dataObjectId != null && !dataObjectId.equals("")) {
			loggedJsonString = makeApiCall("activity/sessions/" + dataObjectId, startDate, endDate);
			if (!loggedJsonString.contains("sessions")) {
				loggedJsonString = "{\"sessions\":[" + loggedJsonString + "]}";
			}
			if (endDate != null ) {
				jsonString = makeApiCall("activity/summary", startDate, endDate);
			} else {
				jsonString = makeApiCall("activity/summary", startDate, startDate);
			}
		} else {
			loggedJsonString = makeApiCall("activity/sessions", startDate, endDate);
			jsonString = makeApiCall("activity/summary", startDate, endDate);
		}

		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray summaryArray = (JSONArray) json.get("summary");
		for (int i = 0; i < summaryArray.size(); i++) {
			JSONObject summary = (JSONObject) summaryArray.get(i);
			Number activityCalories = (Number) summary.get("activityCalories");
			Number calories = (Number) summary.get("calories");
			Number distance = (Number) summary.get("distance");
			Number points = (Number) summary.get("points");
			String activityDateString = (String) summary.get("date");
			Date activityDate = new Date(startDate.getTime());
			logger.finer("Trying to parse date " + activityDateString);
			logger.finer("Pre-parsing: " + DateFormatUtils.format(startDate, "yyyy-MM-dd"));//,TimeZone.getTimeZone("UTC")));
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				activityDate = format.parse(activityDateString);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.finer("Post-parsing: " + DateFormatUtils.format(activityDate, "yyyy-MM-dd"));//,TimeZone.getTimeZone("UTC")));
			long steps = (Long) summary.get("steps");

			Activity activity = new Activity(machineName, activityDate);
			activity.setActivityCalories(activityCalories.floatValue());
			activity.setCalories(calories.floatValue());
			activity.setSteps(new Long(steps).intValue());
			activity.setActivityPoints(points.floatValue());
			activity.setDistance(distance.floatValue());


			results.add(activity);
		}

		logger.finer("loggedJsonString: " + loggedJsonString);
		JSONObject loggedJson = (JSONObject) JSONValue.parse(loggedJsonString);
		JSONArray sessionsArray = (JSONArray) loggedJson.get("sessions");


		for (int j = 0; j < sessionsArray.size(); j++) {
			JSONObject activityJson = (JSONObject) sessionsArray.get(j);

			Number calories = (Number) activityJson.get("calories");
			Number distance = (Number) activityJson.get("distance");
			Number points = (Number) activityJson.get("points");
			String activityDateString = (String) activityJson.get("startTime");
			Date activityDate = new Date(startDate.getTime());
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

				activityDate = format.parse(activityDateString.substring(0, activityDateString.length() - 4));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			long steps = (Long) activityJson.get("steps");
			long duration = (Long) activityJson.get("duration");
			String activityId = (String) activityJson.get("id");
			String activityType = (String) activityJson.get("activityType");

			Activity activity = new Activity(machineName + activityId, activityDate);
			activity.setCalories( calories.floatValue());
			activity.setSteps(new Long(steps).intValue());
			activity.setActivityPoints( points.floatValue());
			activity.setLoggedActivityDistance( distance.floatValue());
			activity.setLoggedActivityDuration(duration);
			activity.setLoggedActivityName(activityType);
			results.add(activity);
		}

		return results;
	}

	public String makeApiCall(String keyword, Date startDate, Date endDate) {
		if (startDate != null && endDate != null) {
			long daysDifference = (endDate.getTime() - startDate.getTime()) / MILLISECONDS_PER_DAY ;
			logger.finer("Days difference is " + daysDifference);
			if (daysDifference < MAX_DAYS_RETRIEVABLE) {
				return makeApiCallLessThanMaxDays(keyword, startDate, endDate);
			} else {
				Date currentDate = new Date(startDate.getTime());
				String jsonResults = "";
				while (endDate.getTime() - currentDate.getTime() > 0) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(currentDate);
					cal.add(Calendar.DATE, 30);
					Date nextDate = cal.getTime();
					String currentResults = makeApiCallLessThanMaxDays(keyword, currentDate, nextDate);
					if (jsonResults.equals("")) {
						jsonResults = currentResults;
					} else {
						String typeForKeyword = getJSONTypeForApiCall(keyword);
						jsonResults = mergeJsons(typeForKeyword, jsonResults, currentResults);
					}
					currentDate = nextDate;
				}
				return jsonResults;
			}
		} else {
			return makeApiCallLessThanMaxDays(keyword, startDate, endDate);
		}
	}
	public String makeApiCallLessThanMaxDays(String keyword, Date startDate, Date endDate) {
		//Token accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

		logger.finer("accessToken: " + accessToken.getToken());
		//https://api.misfitwearables.com/move/resource/v1/user/me/profile
		String reqURLS = baseURL + "/move/resource/v1/user/me/" + keyword;
		logger.finer(reqURLS);

		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, reqURLS);

		if (startDate != null && endDate != null) {
			String startDateString = DateFormatUtils.format(startDate, "yyyy-MM-dd");//,TimeZone.getTimeZone("UTC"));
			String endDateString = DateFormatUtils.format(endDate, "yyyy-MM-dd");//,TimeZone.getTimeZone("UTC"));
			logger.finer(startDateString);
			logger.finer(endDateString);
			serviceRequest.addQuerystringParameter("start_date", startDateString);
			serviceRequest.addQuerystringParameter("end_date", endDateString);
			serviceRequest.addQuerystringParameter("detail", "true");
		}
		serviceRequest.addHeader("access_token", accessToken.getToken());
		Response requestResponse = serviceRequest.send();
		logger.finer("Response body: " + requestResponse.getBody());
		String results = requestResponse.getBody();
		logger.finer("Results: " + results);
		return results;

	}

	public String mergeJsons(String type, String jsonAString, String jsonBString) {
		JSONObject jsonA = (JSONObject) JSONValue.parse(jsonAString);
		JSONObject jsonB = (JSONObject) JSONValue.parse(jsonBString);

		JSONArray aArray = (JSONArray) jsonA.get(type);
		JSONArray bArray = (JSONArray) jsonB.get(type);

		aArray.addAll(bArray);

		jsonA.remove(type);
		jsonA.put(type, aArray);
		return jsonA.toJSONString();
	}

	public String getJSONTypeForApiCall(String keyword) {
		String result = "";
		//goals, summary, sessions, sleeps
		if (keyword.equals("activity/goals")) {
			result = "goals";
		} else if (keyword.equals("activity/summary")) {
			result = "summary";
		} else if (keyword.equals("activity/sessions")) {
			result = "sessions";
		} else if (keyword.equals("activity/sleeps")) {
			result = "sleeps";
		} else {
			result = keyword;
		}
		return result;
	}

	@Override
	public String getProvenance() {
		return PROVENANCE;
	}
}
