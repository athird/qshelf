package uk.ac.open.kmi.carre.qs.service.fitbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class FitbitService extends Service {
	private static Logger logger = Logger.getLogger(FitbitService.class.getName());
	
	public FitbitService(String propertiesPath) {
		super(propertiesPath);
	}

	public static final String name = "Fitbit API";
	public static final String machineName = "fitbit";
	public static final int version = 1;

	public static final String baseURL = "https://api.fitbit.com/1";

	public static final String requestTokenURL = "https://api.fitbit.com/oauth/request_token";
	public static final String authURL = "https://www.fitbit.com/oauth/authorize";
	public static final String accessTokenURL = "https://api.fitbit.com/oauth/access_token";

	public static final String REQUEST_TOKEN_SESSION = "FITBIT_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "FITBIT_ACCESS_TOKEN_SESSION";

	private OAuthService service = null;
	private String userId = "";
	private RDFAbleToken accessToken = null;

	public static final int ASLEEP = 1;
	public static final int AWAKE = 2;
	public static final int REALLY_AWAKE = 3;

	public static final int MILLISECONDS_PER_SECOND = 1000;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + "fitbit";

	public static final String PROVENANCE = RDF_SERVICE_NAME;

	@Override
	public void handleNotification(String requestContent) {
		String json = requestContent;
		
		JSONArray jsonArray = (JSONArray) JSONValue.parse(json);
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject notifyJson = (JSONObject) jsonArray.get(i);
			String collectionType = (String) notifyJson.get("collectionType");
			String dateString = (String) notifyJson.get("date");
			String ownerId = (String) notifyJson.get("ownerId");
			String ownerType = (String) notifyJson.get("ownerType");
			String subscriptionId = (String) notifyJson.get("subscriptionId");
			logger.finer(collectionType + ", " +dateString + ", " +ownerId + 
					", " +ownerType + ", " + subscriptionId);
			handleNotification(collectionType, dateString, ownerId, 
					ownerType, subscriptionId);
		}


	}

	public void handleNotification(String collectionType, String dateString,
			String ownerId, String ownerType, String carreUserId) {
		RDFAbleToken token = getTokenForUser(carreUserId);
		if (token != null ) {
			RDFAbleToken oldAccessToken = accessToken;
			accessToken = token;
			if (service == null ) {
				service = new ServiceBuilder()
				.provider(FitbitApi.class)
				.apiKey(oauth_token)
				.apiSecret(oauth_secret)
				.signatureType(SignatureType.Header)
				//.build();
				.callback("")
				.build();
			}
			String[] dateComponents = dateString.split("-");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(dateComponents[0]),
					Integer.parseInt(dateComponents[1]) - 1, 
					Integer.parseInt(dateComponents[2]));
			Date date = cal.getTime();
			List<Metric> newMetrics = new ArrayList<Metric>();
			if (collectionType == null || collectionType.equals("")) {
				newMetrics.addAll(getBody(date));
				newMetrics.addAll(getFoods(date));
				newMetrics.addAll(getActivities(date));
				newMetrics.addAll(getSleeps(date));
			} else if (collectionType.equals("foods")) {
				newMetrics.addAll(getFoods(date));
			} else if (collectionType.equals("activities")) {
				newMetrics.addAll(getActivities(date));
			} else if (collectionType.equals("body")) {
				newMetrics.addAll(getBody(date));
			} else if (collectionType.equals("sleep")) {
				newMetrics.addAll(getSleeps(date));
			}
			String rdf = "";
			for (Metric metric : newMetrics) {
				rdf += metric.getMeasuredByRDF(PROVENANCE, carreUserId);
				rdf += metric.toRDFString(carreUserId);
			}
			accessToken = oldAccessToken;

			logger.info(rdf);
			if (!rdf.equals("")) {
				CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
				boolean success = true;
				List<String> triples = Service.chunkRDF(rdf);
				for (String tripleSet : triples) {
					success &= connector.insertTriples(carreUserId, tripleSet);
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
		String sparql = "SELECT ?oauth_token ?oauth_secret FROM <" + CARREVocabulary.USER_URL 
				+ userId + "> WHERE { ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				"?connection <" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n" +
				"?connection <" + 
				CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret. }";

		logger.finer(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			Literal secretLiteral = solution.getLiteral("oauth_secret");
			if (tokenLiteral == null || secretLiteral == null) {
				logger.finer("Token or secret literal is null!");
				return null;
			}
			String oauth_token = tokenLiteral.getString();
			String oauth_secret = secretLiteral.getString();
			logger.finer("token literal is " + oauth_token + 
					", secret literal is " + oauth_secret);
			RDFAbleToken token = new RDFAbleToken(oauth_token, oauth_secret);
			return token;
		}
		return null;
	}

	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		if (service == null ) {
			service = new ServiceBuilder()
			.provider(FitbitApi.class)
			.apiKey(oauth_token)
			.apiSecret(oauth_secret)
			.signatureType(SignatureType.Header)
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
			logger.finer("RequestToken:" + requestToken.getToken() 
					+ ", " + requestToken.getSecret());
			request.getSession().setAttribute(machineName + "reqtoken", requestToken.getToken());
			request.getSession().setAttribute(machineName + "reqsec", requestToken.getSecret());

			String authURL= service.getAuthorizationUrl(requestToken);

			logger.finer(authURL);
			return response.encodeRedirectURL(authURL);
		} else {
			logger.finer("oauthTokenSecret" + oauthTokenSecret);
			Verifier verifier = new Verifier(oauthTokenSecret);
			Token requestToken = new Token((String) request.getSession().getAttribute(machineName + "reqtoken"),
					(String) request.getSession().getAttribute(machineName + "reqsec"));

			Token tmpAccessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);
			accessToken = new RDFAbleToken(tmpAccessToken.getToken(), tmpAccessToken.getSecret());

			logger.finer("accessToken: " + accessToken.getToken());
			logger.finer("accessTokenSecret: " + accessToken.getSecret());

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,06,10);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				logger.finer(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.finer(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}
			return "";
		}
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> allMetrics = new ArrayList<Metric>();
		allMetrics.addAll(getBPs(startDate));
		allMetrics.addAll(getPulses(startDate));
		allMetrics.addAll(getWaters(startDate));
		allMetrics.addAll(getFoods(startDate));
		allMetrics.addAll(getFat(startDate));
		allMetrics.addAll(getBody(startDate));
		allMetrics.addAll(getWeight(startDate));
		allMetrics.addAll(getSleeps(startDate));
		allMetrics.addAll(getActivities(startDate));
		allMetrics.addAll(getGlucoses(startDate));
		allMetrics.addAll(getBPs(startDate));
		return allMetrics;
	}

	public List<Metric> getBPs(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("bp", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray bps = (JSONArray) json.get("bp");
		for (int i = 0; i < bps.size(); i++) {
			JSONObject bpJson = (JSONObject) bps.get(i);
			String logId = "";
			try {
				logId = (String) bpJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) bpJson.get("logId");
			}
			String time = (String) bpJson.get("time");
			if (time != null && !time.equals("")) {
				String[] timeSplit = time.split(":");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				date = cal.getTime();
			}
			BloodPressure bp = new BloodPressure(machineName + logId, date);
			long diastolic = (Long) bpJson.get("diastolic");
			long systolic = (Long) bpJson.get("systolic");

			bp.setDiastolicBloodPressure(diastolic);
			bp.setSystolicBloodPressure(systolic);
			bp.setActuality(BloodPressure.ACTUALITY_ACTUAL);
			results.add(bp);

		}
		return results;
	}

	public List<Metric> getPulses(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("heart", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray hearts = (JSONArray) json.get("heart");
		for (int i = 0; i < hearts.size(); i++) {
			JSONObject heartJson = (JSONObject) hearts.get(i);
			String logId = "";
			try {
				logId = (String) heartJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) heartJson.get("logId");
			}
			String time = (String) heartJson.get("time");
			String tracker = (String) heartJson.get("tracker");
			if (time != null && !time.equals("")) {
				String[] timeSplit = time.split(":");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				date = cal.getTime();
			}
			Pulse pulse = new Pulse(machineName + logId, date);
			pulse.setActuality(Pulse.ACTUALITY_ACTUAL);
			long heartRate = (Long) heartJson.get("heartRate");
			pulse.setPulse(heartRate);
			pulse.setNote(tracker);
			results.add(pulse);

		}
		return results;
	}

	List<Metric> getWaters(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("foods/log/water", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray waters = (JSONArray) json.get("water");
		for (int i = 0; i < waters.size(); i++) {
			JSONObject waterJson = (JSONObject) waters.get(i);
			String logId = "";
			try {
				logId = (String) waterJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) waterJson.get("logId");
			}
			Food food = new Food(machineName + logId, date);
			food.setActuality(Food.ACTUALITY_ACTUAL);
			long waterVolume = (Long) waterJson.get("amount");
			food.setWater(waterVolume);
			results.add(food);

		}
		return results;
	}

	public List<Metric> getFoods(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("foods/log", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONObject foods = (JSONObject) json.get("summary");

		Number calories = (Number) foods.get("calories");
		Number carbs = (Number) foods.get("carbs");
		Number fat = (Number) foods.get("fat");
		Number fibre = (Number) foods.get("fiber");
		Number protein = (Number) foods.get("protein");
		Number sodium = (Number) foods.get("sodium");
		Number water = (Number) foods.get("water");

		Food food = new Food(machineName, date);
		food.setCalories(calories.doubleValue());
		food.setCarbs(carbs.doubleValue());
		food.setFat(fat.doubleValue());
		food.setFibre(fibre.doubleValue());
		food.setProtein(protein.doubleValue());
		food.setSodium(sodium.doubleValue());
		food.setWater(water.doubleValue());		
		results.add(food);
		return results;
	}

	public List<Metric> getFat(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("body/log/fat", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray fats = (JSONArray) json.get("fat");
		for (int i = 0; i < fats.size(); i++) {
			JSONObject fatJson = (JSONObject) fats.get(i);
			String logId = "";
			try {
				logId = (String) fatJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) fatJson.get("logId");
			}
			String time = (String) fatJson.get("time");
			String dateString = (String) fatJson.get("date");
			if (time != null && !time.equals("")) {
				String[] timeSplit = time.split(":");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				if (timeSplit.length > 2) {
					cal.set(Calendar.SECOND, Integer.parseInt(timeSplit[2]));
				}
				date = cal.getTime();
			}
			if (dateString != null && !dateString.equals("")) {
				String[] dateSplit = dateString.split("-");
				int year = Integer.parseInt(dateSplit[0]);
				int month = Integer.parseInt(dateSplit[1]);
				int day = Integer.parseInt(dateSplit[2]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);	
				date = cal.getTime();
			}
			Weight weight = new Weight(machineName + logId, date);
			weight.setActuality(Weight.ACTUALITY_ACTUAL);
			Number fat = (Number) fatJson.get("fat");
			weight.setBodyFat(fat.doubleValue());
			results.add(weight);
		}
		return results;
	}

	public List<Metric> getBody(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("body", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONObject bodies = (JSONObject) json.get("body");

		Number bicep = (Number) bodies.get("bicep");
		//double bicep = (Double) bodies.get("bicep");
		Number calf = (Number) bodies.get("calf");
		Number chest = (Number) bodies.get("chest");
		Number forearm = (Number) bodies.get("forearm");
		Number hips = (Number) bodies.get("hips");
		Number neck = (Number) bodies.get("neck");
		Number thigh = (Number) bodies.get("thigh");
		Number waist = (Number) bodies.get("waist");

		Number bmi = (Number) bodies.get("bmi");
		Number fat = (Number) bodies.get("fat");
		Number weightValue = (Number) bodies.get("weight");

		Body body = new Body(machineName, date);
		body.setBicep(bicep.doubleValue());
		body.setCalf(calf.doubleValue());
		body.setChest(chest.doubleValue());
		body.setForearm(forearm.doubleValue());
		body.setHips(hips.doubleValue());
		body.setNeck(neck.doubleValue());
		body.setThigh(thigh.doubleValue());
		body.setWaist(waist.doubleValue());
		body.setActuality(Body.ACTUALITY_ACTUAL);

		results.add(body);

		Weight weight = new Weight(machineName, date);
		weight.setWeight(weightValue.doubleValue());
		weight.setBodyFat(fat.doubleValue());
		weight.setBmi(bmi.doubleValue());
		weight.setActuality(Weight.ACTUALITY_ACTUAL);

		results.add(weight);

		return results;

	}

	public List<Metric> getWeight(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("body/log/weight", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray weights = (JSONArray) json.get("weight");
		for (int i = 0; i < weights.size(); i++) {
			JSONObject weightJson = (JSONObject) weights.get(i);
			String logId = "";
			try {
				logId = (String) weightJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) weightJson.get("logId");
			}
			String time = (String) weightJson.get("time");
			String dateString = (String) weightJson.get("date");
			if (time != null && !time.equals("")) {
				String[] timeSplit = time.split(":");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				if (timeSplit.length > 2) {
					cal.set(Calendar.SECOND, Integer.parseInt(timeSplit[2]));
				}
				date = cal.getTime();
			}
			if (dateString != null && !dateString.equals("")) {
				String[] dateSplit = dateString.split("-");
				int year = Integer.parseInt(dateSplit[0]);
				int month = Integer.parseInt(dateSplit[1]);
				int day = Integer.parseInt(dateSplit[2]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);	
				date = cal.getTime();
			}
			Weight weight = new Weight(machineName + logId, date);
			weight.setActuality(Weight.ACTUALITY_ACTUAL);
			Number bmi = (Number) weightJson.get("bmi");
			Number weightValue = (Number) weightJson.get("weight");
			weight.setBmi(bmi.doubleValue());
			weight.setWeight(weightValue.doubleValue());
			results.add(weight);
		}
		return results;
	}

	public List<Metric> getSleeps(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("sleep", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray sleeps = (JSONArray) json.get("sleep");

		for (int i = 0; i < sleeps.size(); i++) {
			JSONObject sleepJson = (JSONObject) sleeps.get(i);
			String logId = "";
			try {
				logId = (String) sleepJson.get("logId");
			} catch (ClassCastException e) {
				logId = "" + (Long) sleepJson.get("logId");
			}
			String startTime = (String) sleepJson.get("startTime");
			Date startDate = new Date(date.getTime());
			try {
				//Unparseable date: "2014-07-10T00:08:00.000"
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				format.setTimeZone(TimeZone.getTimeZone("UTC"));
				//String noMilliseconds = startTime.split(".")[0];
				startDate = format.parse(startTime );
				//startDate = DateFormat.getInstance().parse(startTime);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//new Date(startTime);

			Sleep sleep = new Sleep(machineName + logId, startDate);
			Number efficiency = (Number) sleepJson.get("efficiency");
			//long duration = (Long) sleepJson.get("duration");
			long minutesToFallAsleep = (Long) sleepJson.get("minutesToFallAsleep");
			long minutesAsleep = (Long) sleepJson.get("minutesAsleep");
			long minutesAwake = (Long) sleepJson.get("minutesAwake");
			long minutesAfterWakeup = (Long) sleepJson.get("minutesAfterWakeup");
			long awakeCount = (Long) sleepJson.get("awakeCount");
			//long awakeDuration = (Long) sleepJson.get("awakeDuration");
			long restlessCount = (Long) sleepJson.get("restlessCount");
			long restlessDuration = (Long) sleepJson.get("restlessDuration");
			long timeInBed = (Long) sleepJson.get("timeInBed");

			sleep.setSleepEfficiency(efficiency.doubleValue());
			sleep.setSleepTime(timeInBed);
			sleep.setTimeToFallAsleep(minutesToFallAsleep);
			sleep.setAsleepDuration(minutesAsleep);
			sleep.setAwakeDuration(minutesAwake);
			sleep.setRestlessDuration(restlessDuration);
			sleep.setMinutesAfterWakeup(minutesAfterWakeup);
			sleep.setTimesAwake(awakeCount);
			sleep.setTimesRestless(restlessCount);

			JSONArray minuteRecords = (JSONArray) sleepJson.get("minuteData");
			Date minuteDate = new Date(date.getTime());
			for (int j = 0; j < minuteRecords.size(); j++) {
				JSONObject minute = (JSONObject) minuteRecords.get(j);
				String statusString = (String) minute.get("value");
				long status = Long.parseLong(statusString);
				SleepRecord record = new SleepRecord(machineName + logId, date);
				record.setStartDate(minuteDate);

				if (status == FitbitService.ASLEEP) {
					record.setSleepStatus(SleepRecord.ASLEEP);
				} else if (status == FitbitService.AWAKE) {
					record.setSleepStatus(SleepRecord.RESTLESS);
				} else if (status == FitbitService.REALLY_AWAKE) {
					record.setSleepStatus(SleepRecord.AWAKE);
				}

				minuteDate.setTime(minuteDate.getTime() + MILLISECONDS_PER_SECOND);
				sleep.addSleepRecord(record);
			}

			results.add(sleep);
		}
		return results;
	}

	public List<Metric> getActivities(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("activities", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONObject summary = (JSONObject) json.get("summary");
		long activityCalories = (Long) summary.get("activityCalories");
		long caloriesBMR = (Long) summary.get("caloriesBMR");
		long caloriesOut = (Long) summary.get("caloriesOut");
		float elevation = -1;
		if (summary.get("elevation") != null) {
			elevation = ((Number) summary.get("elevation")).floatValue();
		}
		long fairlyActiveMinutes = (Long) summary.get("fairlyActiveMinutes");
		long floors = -1;
		if (summary.get("floors") != null) {
			floors = (Long) summary.get("floors");
		}
		long lightlyActiveMinutes = (Long) summary.get("lightlyActiveMinutes");
		long marginalCalories = (Long) summary.get("marginalCalories");
		long sedentaryMinutes = (Long) summary.get("sedentaryMinutes");
		long steps = (Long) summary.get("steps");
		long veryActiveMinutes = (Long) summary.get("veryActiveMinutes");

		Activity activity = new Activity(machineName, date);
		activity.setActivityCalories(activityCalories);
		activity.setCaloriesBMR(caloriesBMR);
		activity.setCalories(caloriesOut);
		if (elevation != -1) {
			activity.setElevation(elevation);
		}
		activity.setModerateActivityDuration(new Long(fairlyActiveMinutes).intValue());
		if (floors != -1) {
			activity.setFloors(new Long(floors).intValue());
		}
		activity.setLightActivityDuration(new Long(lightlyActiveMinutes).intValue());
		activity.setSedentaryActivityDuration(new Long(sedentaryMinutes).intValue());
		activity.setSteps(new Long(steps).intValue());
		activity.setIntenseActivityDuration(new Long(veryActiveMinutes).intValue());
		activity.setMarginalCalories(new Float(marginalCalories));

		JSONArray activities = (JSONArray) summary.get("distances");

		for (int i = 0; i < activities.size(); i++) {
			JSONObject activityJson = (JSONObject) activities.get(i);
			String activityName = (String) activityJson.get("activity");
			float distance = ((Number) activityJson.get("distance")).floatValue();

			if (activityName.equals("total")) {
				activity.setDistance(distance);
			} else if (activityName.equals("veryActive")) {
				activity.setIntenseActivityDistance(distance);
			} else if (activityName.equals("moderatelyActive")) {
				activity.setModerateActivityDistance(distance);
			} else if (activityName.equals("lightlyActive")) {
				activity.setLightActivityDistance(distance);
			} else if (activityName.equals("sedentaryActive")) {
				activity.setSedentaryActivityDistance(distance);
			} else if (activityName.equals("loggedActivities")) {
				activity.setLoggedActivityDistance(distance);
			} else if (activityName.equals("tracker")) {
				activity.setTrackedActivityDistance(distance);
			}		
		}
		results.add(activity);
		return results;
	}

	public List<Metric> getGlucoses(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("glucose", date);
		JSONObject json = (JSONObject) JSONValue.parse(jsonString);
		JSONArray glucoses = (JSONArray) json.get("glucose");
		for (int i = 0; i < glucoses.size(); i++) {
			JSONObject glucoseJson = (JSONObject) glucoses.get(i);
			String time = (String) glucoseJson.get("time");
			if (time != null && !time.equals("")) {
				String[] timeSplit = time.split(":");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				date = cal.getTime();
			}
			Number glucoseValue = (Number) glucoseJson.get("glucose");
			String tracker = (String) glucoseJson.get("tracker");
			Glucose glucose = new Glucose(machineName, date);
			glucose.setGlucose(glucoseValue.doubleValue());
			glucose.setNote(tracker);
			results.add(glucose);
		}
		Number hbA1c = (Number) json.get("hba1c");
		if (hbA1c != null) {
			Glucose glucose = new Glucose(machineName, date);
			glucose.setHbA1c(hbA1c.doubleValue());
			results.add(glucose);
		}
		return results;
	}

	public String makeApiCall(String keyword, Date date) {
		//Token accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

		logger.finer("accessToken: " + accessToken.getToken());
		logger.finer("accessTokenSecret: " + accessToken.getSecret());

		String reqURLS = baseURL + "/user/-/profile.json";
		logger.finer(reqURLS);

		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL 
				//+ "/user/-/profile.json");
				//+ "/user/-/activities/date/2014-06-05.json");
				+ "/user/-/" + keyword + "/date/" + dateString + ".json");
		service.signRequest(accessToken, serviceRequest); 
		logger.finer(serviceRequest.getUrl());
		logger.finer(serviceRequest.getCompleteUrl());

		Response requestResponse = serviceRequest.send();
		logger.finer(requestResponse.getBody());
		return requestResponse.getBody();

	}



	@Override
	public String getProvenance() {
		return PROVENANCE;
	}
}
