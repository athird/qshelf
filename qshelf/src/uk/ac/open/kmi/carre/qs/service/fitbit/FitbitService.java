package uk.ac.open.kmi.carre.qs.service.fitbit;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import uk.ac.open.kmi.carre.metrics.Activity;
import uk.ac.open.kmi.carre.metrics.BloodPressure;
import uk.ac.open.kmi.carre.metrics.Body;
import uk.ac.open.kmi.carre.metrics.Food;
import uk.ac.open.kmi.carre.metrics.Glucose;
import uk.ac.open.kmi.carre.metrics.Height;
import uk.ac.open.kmi.carre.metrics.Metric;
import uk.ac.open.kmi.carre.metrics.O2Saturation;
import uk.ac.open.kmi.carre.metrics.Pulse;
import uk.ac.open.kmi.carre.metrics.Sleep;
import uk.ac.open.kmi.carre.metrics.SleepRecord;
import uk.ac.open.kmi.carre.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.Service;

public class FitbitService extends Service {

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
	private Token accessToken = null;

	public static final int ASLEEP = 1;
	public static final int AWAKE = 2;
	public static final int REALLY_AWAKE = 3;

	public static final int MILLISECONDS_PER_SECOND = 1000;


	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		/*		Date date = Calendar.getInstance().getTime();
		List<Metric> results = getMetrics(date, date);
		for (Metric metric : results) {
			System.err.println(metric.toRDFString());
		}
		return "";*/
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

			accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

			System.err.println("accessToken: " + accessToken.getToken());
			System.err.println("accessTokenSecret: " + accessToken.getSecret());

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,06,10);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				System.err.println(metric.toRDFString());
			}
			/*String reqURLS = baseURL + "/user/-/profile.json";
			System.err.println(reqURLS);

			OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL 
					//+ "/user/-/profile.json");
					+ "/user/-/activities/date/2014-06-05.json");
			service.signRequest(accessToken, serviceRequest); 
			System.err.println(serviceRequest.getUrl());
			System.err.println(serviceRequest.getCompleteUrl());

			Response requestResponse = serviceRequest.send();
			System.out.println(requestResponse.getBody());*/
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
				//String noMilliseconds = startTime.split(".")[0];
				startDate = format.parse(startTime.substring(0, startTime.length() - 4));
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
		float elevation = ((Number) summary.get("elevation")).floatValue();
		long fairlyActiveMinutes = (Long) summary.get("fairlyActiveMinutes");
		long floors = (Long) summary.get("floors");
		long lightlyActiveMinutes = (Long) summary.get("lightlyActiveMinutes");
		long marginalCalories = (Long) summary.get("marginalCalories");
		long sedentaryMinutes = (Long) summary.get("sedentaryMinutes");
		long steps = (Long) summary.get("steps");
		long veryActiveMinutes = (Long) summary.get("veryActiveMinutes");

		Activity activity = new Activity(machineName, date);
		activity.setActivityCalories(activityCalories);
		activity.setCaloriesBMR(caloriesBMR);
		activity.setCalories(caloriesOut);
		activity.setElevation(elevation);
		activity.setModerateActivityDuration(new Long(fairlyActiveMinutes).intValue());
		activity.setFloors(new Long(floors).intValue());
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

		System.err.println("accessToken: " + accessToken.getToken());
		System.err.println("accessTokenSecret: " + accessToken.getSecret());

		String reqURLS = baseURL + "/user/-/profile.json";
		System.err.println(reqURLS);

		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd");
		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL 
				//+ "/user/-/profile.json");
				//+ "/user/-/activities/date/2014-06-05.json");
				+ "/user/-/" + keyword + "/date/" + dateString + ".json");
		service.signRequest(accessToken, serviceRequest); 
		System.err.println(serviceRequest.getUrl());
		System.err.println(serviceRequest.getCompleteUrl());

		Response requestResponse = serviceRequest.send();
		System.out.println(requestResponse.getBody());
		return requestResponse.getBody();
		/*if (keyword.equals("activities")) {
			return "{\n" + 
					"    \"activities\":[\n" + 
					"        {\n" + 
					"            \"activityId\":51007,\n" + 
					"            \"activityParentId\":90019,\n" + 
					"            \"calories\":230,\n" + 
					"            \"description\":\"7mph\",\n" + 
					"            \"distance\":2.04,\n" + 
					"            \"duration\":1097053,\n" + 
					"            \"hasStartTime\":true,\n" + 
					"            \"isFavorite\":true,\n" + 
					"            \"logId\":1154701,\n" + 
					"            \"name\":\"Treadmill, 0% Incline\",\n" + 
					"            \"startTime\":\"00:25\",\n" + 
					"            \"steps\":3783\n" + 
					"        }\n" + 
					"    ],\n" + 
					"    \"goals\":{\n" + 
					"        \"caloriesOut\":2826,\n" + 
					"        \"distance\":8.05,\n" + 
					"        \"floors\":150,\n" + 
					"        \"steps\":10000\n" + 
					"     },\n" + 
					"    \"summary\":{\n" + 
					"        \"activityCalories\":230,\n" + 
					"        \"caloriesBMR\":1913,\n" + 
					"        \"caloriesOut\":2143,\n" + 
					"        \"distances\":[\n" + 
					"            {\"activity\":\"tracker\", \"distance\":1.32},\n" + 
					"            {\"activity\":\"loggedActivities\", \"distance\":0},\n" + 
					"            {\"activity\":\"total\",\"distance\":1.32},\n" + 
					"            {\"activity\":\"veryActive\", \"distance\":0.51},\n" + 
					"            {\"activity\":\"moderatelyActive\", \"distance\":0.51},\n" + 
					"            {\"activity\":\"lightlyActive\", \"distance\":0.51},\n" + 
					"            {\"activity\":\"sedentaryActive\", \"distance\":0.51},\n" + 
					"            {\"activity\":\"Treadmill, 0% Incline\", \"distance\":3.28}\n" + 
					"        ],\n" + 
					"        \"elevation\":48.77,\n" + 
					"        \"fairlyActiveMinutes\":0,\n" + 
					"        \"floors\":16,\n" + 
					"        \"lightlyActiveMinutes\":0,\n" + 
					"        \"marginalCalories\":200,\n" + 
					"        \"sedentaryMinutes\":1166,\n" + 
					"        \"steps\":0,\n" + 
					"        \"veryActiveMinutes\":0\n" + 
					"    }\n" + 
					"}";
		} else if (keyword.equals("sleep")) {
			return "{\n" + 
					"  \"sleep\":  [\n" + 
					"     {\n" + 
					"      \"awakeCount\": 0,\n" + 
					"      \"awakeDuration\": 10,\n" + 
					"      \"awakeningsCount\": 6,\n" + 
					"      \"duration\": 32220000,\n" + 
					"      \"efficiency\": 82,\n" + 
					"      \"isMainSleep\": true,\n" + 
					"      \"logId\": 76365766,\n" + 
					"      \"minuteData\":  [\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:01:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:02:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:03:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:04:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:05:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:06:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:07:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:08:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:09:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:10:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:11:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"00:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:09:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:10:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:11:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:12:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:13:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:14:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:15:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:16:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:17:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:18:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:19:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:20:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:21:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:22:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:23:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:24:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:25:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:26:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:27:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:28:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:29:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:30:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"01:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:33:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:34:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:35:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:36:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:37:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:38:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:39:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:40:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:41:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:42:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:43:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:44:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:45:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:46:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:47:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:48:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:49:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:50:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:51:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:52:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:53:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:54:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"02:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"03:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:02:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:03:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:04:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:05:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:06:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:07:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:08:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:09:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:10:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:11:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:12:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"04:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:20:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:21:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:22:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:23:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:24:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:25:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:26:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:27:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:28:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:29:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:30:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:31:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:32:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:33:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:34:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:35:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:36:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:37:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:38:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"05:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:44:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:45:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:46:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:47:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:48:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:49:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:50:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:51:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:52:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:53:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:54:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"06:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:24:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:25:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:26:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:27:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:28:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:42:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:43:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:44:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:45:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:46:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:47:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:48:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:49:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:50:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:51:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:52:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:53:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:54:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:55:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:56:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:57:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:58:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"07:59:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:00:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:01:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:02:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:03:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:04:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:05:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:06:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:07:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:08:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:09:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:10:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:11:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:12:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:13:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:14:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:15:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:16:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:17:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:18:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:19:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:20:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:21:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:22:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:23:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:24:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:25:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:26:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:27:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:28:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:29:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:30:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:31:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:32:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:33:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:34:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:35:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:36:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:37:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:38:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:39:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:40:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:41:00\",\n" + 
					"          \"value\": \"1\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:42:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:43:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:44:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:45:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:46:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:47:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:48:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:49:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:50:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:51:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:52:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:53:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:54:00\",\n" + 
					"          \"value\": \"3\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:55:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:56:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        },\n" + 
					"         {\n" + 
					"          \"dateTime\": \"08:57:00\",\n" + 
					"          \"value\": \"2\"\n" + 
					"        }\n" + 
					"      ],\n" + 
					"      \"minutesAfterWakeup\": 16,\n" + 
					"      \"minutesAsleep\": 420,\n" + 
					"      \"minutesAwake\": 90,\n" + 
					"      \"minutesToFallAsleep\": 11,\n" + 
					"      \"restlessCount\": 6,\n" + 
					"      \"restlessDuration\": 107,\n" + 
					"      \"startTime\": \"2013-12-01T00:01:00.000\",\n" + 
					"      \"timeInBed\": 537\n" + 
					"    }\n" + 
					"  ],\n" + 
					"  \"summary\":  {\n" + 
					"    \"totalMinutesAsleep\": 420,\n" + 
					"    \"totalSleepRecords\": 1,\n" + 
					"    \"totalTimeInBed\": 537\n" + 
					"  }\n" + 
					"}";
			return "{\n" + 
					"    \"sleep\":[\n" + 
					"        {\n" + 
					"            \"isMainSleep\":true,\n" + 
					"            \"logId\":29744,\n" + 
					"            \"efficiency\":98,\n" + 
					"            \"startTime\":\"2011-06-16T00:00:00.000\",\n" + 
					"            \"duration\":28800000,\n" + 
					"            \"minutesToFallAsleep\":0,\n" + 
					"            \"minutesAsleep\":480,\n" + 
					"            \"minutesAwake\":0,\n" + 
					"            \"minutesAfterWakeup\":0,\n" + 
					"            \"awakeningsCount\":0, // deprecated\n" + 
					"            \"awakeCount\":0,\n" + 
					"            \"awakeDuration\":0,\n" + 
					"            \"restlessCount\":0,\n" + 
					"            \"restlessDuration\":0,\n" + 
					"            \"timeInBed\":480\n" + 
					"            \"minuteData\":[\n" + 
					"                {\n" + 
					"                    \"dateTime\":\"00:00:00\",\n" + 
					"                    \"value\":\"3\"\n" + 
					"                },\n" + 
					"                {\n" + 
					"                    \"dateTime\":\"00:01:00\",\n" + 
					"                    \"value\":\"2\"\n" + 
					"                },\n" + 
					"                {\n" + 
					"                    \"dateTime\":\"00:02:00\",\n" + 
					"                    \"value\":\"1\"\n" + 
					"                }]\n" +
					"        },   \n" + 
					"        {\n" + 
					"            \"isMainSleep\":false,\n" + 
					"            \"logId\":29745,\n" + 
					"            \"efficiency\":93,\n" + 
					"            \"startTime\":\"2011-06-16T14:00:00.000\",\n" + 
					"            \"duration\":3600000,\n" + 
					"            \"minutesToFallAsleep\":20,\n" + 
					"            \"minutesAsleep\":38,\n" + 
					"            \"minutesAwake\":0,\n" + 
					"            \"minutesAfterWakeup\":2,\n" + 
					"            \"awakeningsCount\":0,\n" + 
					"            \"awakeCount\":0,\n" + 
					"            \"awakeDuration\":0,\n" + 
					"            \"restlessCount\":0,\n" + 
					"            \"restlessDuration\":0,\n" + 
					"            \"timeInBed\":60\n" + 
					"            \"minuteData\":[\n" + 
					"                {\n" + 
					"                    \"dateTime\":\"14:00:00\",\n" + 
					"                    \"value\":\"3\"\n" + 
					"                }]\n" + 
					"        }\n" + 
					"    ],\n" + 
					"    \"summary\":{\n" + 
					"        \"totalMinutesAsleep\":518,\n" + 
					"        \"totalSleepRecords\":2,\n" + 
					"        \"totalTimeInBed\":540\n" + 
					"    }\n" + 
					"}\n" + 
					"";
		} else if (keyword.equals("body")) {
			return "{\n" + 
					"    \"body\":\n" + 
					"    {\n" + 
					"        \"bicep\":40,\n" + 
					"        \"bmi\":16.14,\n" + 
					"        \"calf\":11.2,\n" + 
					"        \"chest\":50,\n" + 
					"        \"fat\":0,\n" + 
					"        \"forearm\":22.3,\n" + 
					"        \"hips\":34,\n" + 
					"        \"neck\":30,\n" + 
					"        \"thigh\":45,\n" + 
					"        \"waist\":60,\n" + 
					"        \"weight\":80.55\n" + 
					"    },\n" + 
					"    \"goals\":\n" + 
					"    {\n" + 
					"        \"weight\":75\n" + 
					"    }\n" + 
					"}\n" + 
					"";
		} else if (keyword.equals("heart")) {
			return "{\n" + 
					"     \"average\":[\n" + 
					"         {\n" + 
					"             \"heartRate\":70,\n" + 
					"             \"tracker\":\"Resting Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":83,\n" + 
					"             \"tracker\":\"Normal Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":130,\n" + 
					"             \"tracker\":\"Exertive Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":150,\n" + 
					"             \"tracker\":\"Running\"\n" + 
					"         }\n" + 
					"    ],\n" + 
					"    \"heart\":[\n" + 
					"         {\n" + 
					"             \"heartRate\":70,\n" + 
					"             \"logId\":1428,\n" + 
					"             \"tracker\":\"Resting Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":85,\n" + 
					"             \"logId\":1427,\n" + 
					"             \"tracker\":\"Normal Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":81,\n" + 
					"             \"logId\":1431,\n" + 
					"             \"tracker\":\"Normal Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":130,\n" + 
					"             \"logId\":1429,\n" + 
					"             \"tracker\":\"Exertive Heart Rate\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":150,\n" + 
					"             \"logId\":1425,\n" + 
					"             \"time\":\"12:20\",\n" + 
					"             \"tracker\":\"Running\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":145,\n" + 
					"             \"logId\":1432,\n" + 
					"             \"time\":\"12:21\",\n" + 
					"             \"tracker\":\"Running\"\n" + 
					"         },\n" + 
					"         {\n" + 
					"             \"heartRate\":157,\n" + 
					"             \"logId\":1433,\n" + 
					"             \"time\":\"12:22\",\n" + 
					"             \"tracker\":\"Running\"\n" + 
					"         }\n" + 
					"    ]\n" + 
					"}";
		} else if (keyword.equals("bp")) {
			return "{\n" + 
					"    \"average\":{\n" + 
					"            \"condition\":\"Prehypertension\",\n" + 
					"            \"diastolic\":85,\n" + 
					"            \"systolic\":115\n" + 
					"    },\n" + 
					"    \"bp\":[\n" + 
					"        {\n" + 
					"            \"diastolic\":80,\n" + 
					"            \"logId\":483697,\n" + 
					"            \"systolic\":120\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"diastolic\":90,\n" + 
					"            \"logId\":483699,\n" + 
					"            \"systolic\":110,\n" + 
					"            \"time\":\"08:00\"\n" + 
					"        }\n" + 
					"    ]\n" + 
					"}";
		} else if (keyword.equals("glucose")) {
			return "{\n" + 
					"    \"glucose\":[\n" + 
					"        {\n" + 
					"            \"glucose\":5,\n" + 
					"            \"tracker\":\"Morning\",\n" + 
					"            \"time\":\"08:00\"\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"glucose\":5.5,\n" + 
					"            \"tracker\":\"Afternoon\"\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"glucose\":6,\n" + 
					"            \"tracker\":\"Evening\"\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"glucose\":7,\n" + 
					"            \"tracker\":\"Custom\"\n" + 
					"        }\n" + 
					"    ],\n" + 
					"    \"hba1c\":4.5\n" + 
					"}";

		} else if (keyword.equals("body/log/weight")) {
			return "{\n" + 
					"    \"weight\":[\n" + 
					"        {\n" + 
					"            \"bmi\":23.57,\n" + 
					"            \"date\":\"2012-03-05\",\n" + 
					"            \"logId\":1330991999000,\n" + 
					"            \"time\":\"23:59:59\",\n" + 
					"            \"weight\":73\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"bmi\":22.57,\n" + 
					"            \"date\":\"2012-03-05\",\n" + 
					"            \"logId\":1330991999000,\n" + 
					"            \"time\":\"21:10:59\",\n" + 
					"            \"weight\":72.5\n" + 
					"        }\n" + 
					"    ]\n" + 
					"}";
		} else if (keyword.equals("body/log/fat")) {
			return "{\n" + 
					"    \"fat\":[\n" + 
					"        {\n" + 
					"            \"date\":\"2012-03-05\",\n" + 
					"            \"fat\":14,\n" + 
					"            \"logId\":1330991999000,\n" + 
					"            \"time\":\"23:59:59\"\n" + 
					"        },\n" + 
					"        {\n" + 
					"            \"date\":\"2012-03-05\",\n" + 
					"            \"fat\":13.5,\n" + 
					"            \"logId\":1330991999000,\n" + 
					"            \"time\":\"21:20:59\"\n" + 
					"        }\n" + 
					"    ]\n" + 
					"}";
		} else if (keyword.equals("foods/log")) {
			return "{\n" + 
					"    \"foods\":[\n" + 
					"        {\n" + 
					"            \"isFavorite\":true,\n" + 
					"            \"logDate\":\"2011-06-29\",\n" + 
					"            \"logId\":1820,\n" + 
					"            \"loggedFood\":{\n" + 
					"                \"accessLevel\":\"PUBLIC\",\n" + 
					"                \"amount\":132.57,\n" + 
					"                \"brand\":\"\",\n" + 
					"                \"calories\":752,\n" + 
					"                \"foodId\":18828,\n" + 
					"                \"mealTypeId\":4,\n" + 
					"                \"locale\":\"en_US\",\n" + 
					"                \"name\":\"Chocolate, Milk\",\n" + 
					"                \"unit\":{\"id\":147,\"name\":\"gram\",\"plural\":\"grams\"},\n" + 
					"                \"units\":[226,180,147,389]\n" + 
					"            },\n" + 
					"            \"nutritionalValues\":{\"calories\":752,\"carbs\":66.5,\"fat\":49,\"fiber\":0.5,\"protein\":12.5,\"sodium\":186}\n" + 
					"        }\n" + 
					"    ],\n" + 
					"    \"summary\":{\"calories\":752,\"carbs\":66.5,\"fat\":49,\"fiber\":0.5,\"protein\":12.5,\"sodium\":186,\"water\":0},\n" + 
					"    \"goals\":{\n" + 
					"        \"calories\": 2286\n" + 
					"    }\n" + 
					"}";
		} else if (keyword.equals("foods/log/water")) {
			return "{\n" + 
					"    \"summary\":{\n" + 
					"        \"water\":800\n" + 
					"    },\n" + 
					"    \"water\":[\n" + 
					"        {\"amount\":500,\"logId\":950},\n" + 
					"        {\"amount\":200,\"logId\":951},\n" + 
					"        {\"amount\":100,\"logId\":952}\n" + 
					"    ]\n" + 
					"}";
		}
		return "";*/
	}

	/*
	 * GET /1/user/-/<keyword>/date/YYYY-MM-DD.json
	 * where <keyword> is one of
	 * activities, sleep, body, heart, bp, glucose, body/log/weight, body/log/fat
	 * 
	 * GET /1/user/-/activities/date/2010-02-21.json
	 * GET /1/user/-/sleep/date/2010-02-21.json
	 * GET /1/user/-/body/date/2010-02-21.json
	 * GET /1/user/-/body/log/weight/date/2010-03-27/2010-04-15.xml 
	 * GET /1/user/-/body/log/fat/date/2010-03-27/2010-04-15.xml
	 * GET /1/user/-/foods/log/date/2010-02-21.json
	 * GET /1/user/-/foods/log/water/date/2010-02-21.json
	 * GET /1/user/-/heart/date/2010-02-21.json
	 * GET /1/user/-/bp/date/2010-02-21.json
	 * GET /1/user/-/glucose/date/2010-02-21.json
	 * {
    "glucose":[
        {
            "glucose":5,
            "tracker":"Morning",
            "time":"08:00"
        },
        {
            "glucose":5.5,
            "tracker":"Afternoon"
        },
        {
            "glucose":6,
            "tracker":"Evening"
        },
        {
            "glucose":7,
            "tracker":"Custom"
        }
    ],
    "hba1c":4.5
}

GET /1/user/-/bp/date/2010-02-21.json
{
    "average":{
            "condition":"Prehypertension",
            "diastolic":85,
            "systolic":115
    },
    "bp":[
        {
            "diastolic":80,
            "logId":483697,
            "systolic":120
        },
        {
            "diastolic":90,
            "logId":483699,
            "systolic":110,
            "time":"08:00"
        }
    ]
}

GET /1/user/-/heart/date/2010-02-21.json

{
     "average":[
         {
             "heartRate":70,
             "tracker":"Resting Heart Rate"
         },
         {
             "heartRate":83,
             "tracker":"Normal Heart Rate"
         },
         {
             "heartRate":130,
             "tracker":"Exertive Heart Rate"
         },
         {
             "heartRate":150,
             "tracker":"Running"
         }
    ],
    "heart":[
         {
             "heartRate":70,
             "logId":1428,
             "tracker":"Resting Heart Rate"
         },
         {
             "heartRate":85,
             "logId":1427,
             "tracker":"Normal Heart Rate"
         },
         {
             "heartRate":81,
             "logId":1431,
             "tracker":"Normal Heart Rate"
         },
         {
             "heartRate":130,
             "logId":1429,
             "tracker":"Exertive Heart Rate"
         },
         {
             "heartRate":150,
             "logId":1425,
             "time":"12:20",
             "tracker":"Running"
         },
         {
             "heartRate":145,
             "logId":1432,
             "time":"12:21",
             "tracker":"Running"
         },
         {
             "heartRate":157,
             "logId":1433,
             "time":"12:22",
             "tracker":"Running"
         }
    ]
}


GET /1/user/-/foods/log/water/date/2010-02-21.json

{
    "summary":{
        "water":800
    },
    "water":[
        {"amount":500,"logId":950},
        {"amount":200,"logId":951},
        {"amount":100,"logId":952}
    ]
}

GET /1/user/-/foods/log/date/2010-02-21.json
{
    "foods":[
        {
            "isFavorite":true,
            "logDate":"2011-06-29",
            "logId":1820,
            "loggedFood":{
                "accessLevel":"PUBLIC",
                "amount":132.57,
                "brand":"",
                "calories":752,
                "foodId":18828,
                "mealTypeId":4,
                "locale":"en_US",
                "name":"Chocolate, Milk",
                "unit":{"id":147,"name":"gram","plural":"grams"},
                "units":[226,180,147,389]
            },
            "nutritionalValues":{"calories":752,"carbs":66.5,"fat":49,"fiber":0.5,"protein":12.5,"sodium":186}
        }
    ],
    "summary":{"calories":752,"carbs":66.5,"fat":49,"fiber":0.5,"protein":12.5,"sodium":186,"water":0},
    "goals":{
        "calories": 2286
    }
}


GET /1/user/-/body/log/fat/date/2010-03-27/2010-04-15.xml

{
    "fat":[
        {
            "date":"2012-03-05",
            "fat":14,
            "logId":1330991999000,
            "time":"23:59:59"
        },
        {
            "date":"2012-03-05",
            "fat":13.5,
            "logId":1330991999000,
            "time":"21:20:59"
        }
    ]
}


GET /1/user/-/body/log/weight/date/2010-03-27/2010-04-15.xml 
{
    "weight":[
        {
            "bmi":23.57,
            "date":"2012-03-05",
            "logId":1330991999000,
            "time":"23:59:59",
            "weight":73
        },
        {
            "bmi":22.57,
            "date":"2012-03-05",
            "logId":1330991999000,
            "time":"21:10:59",
            "weight":72.5
        }
    ]
}

GET /1/user/-/body/date/2010-02-21.json
{
    "body":
    {
        "bicep":40,
        "bmi":16.14,
        "calf":11.2,
        "chest":50,
        "fat":0,
        "forearm":22.3,
        "hips":34,
        "neck":30,
        "thigh":45,
        "waist":60,
        "weight":80.55
    },
    "goals":
    {
        "weight":75
    }
}

GET /1/user/-/sleep/date/2010-02-21.json
{
    "sleep":[
        {
            "isMainSleep":true,
            "logId":29744,
            "efficiency":98,
            "startTime":"2011-06-16T00:00:00.000",
            "duration":28800000,
            "minutesToFallAsleep":0,
            "minutesAsleep":480,
            "minutesAwake":0,
            "minutesAfterWakeup":0,
            "awakeningsCount":0, // deprecated
            "awakeCount":0,
            "awakeDuration":0,
            "restlessCount":0,
            "restlessDuration":0,
            "timeInBed":480
            "minuteData":[
                {
                    "dateTime":"00:00:00",
                    "value":"3"
                },
                {
                    "dateTime":"00:01:00",
                    "value":"2"
                },
                {
                    "dateTime":"00:02:00",
                    "value":"1"
                },
                <...>
        },   
        {
            "isMainSleep":false,
            "logId":29745,
            "efficiency":93,
            "startTime":"2011-06-16T14:00:00.000",
            "duration":3600000,
            "minutesToFallAsleep":20,
            "minutesAsleep":38,
            "minutesAwake":0,
            "minutesAfterWakeup":2,
            "awakeningsCount":0,
            "awakeCount":0,
            "awakeDuration":0,
            "restlessCount":0,
            "restlessDuration":0,
            "timeInBed":60
            "minuteData":[
                {
                    "dateTime":"14:00:00",
                    "value":"3"
                },
                <...>
        }
    ],
    "summary":{
        "totalMinutesAsleep":518,
        "totalSleepRecords":2,
        "totalTimeInBed":540
    }
}

GET /1/user/-/activities/date/2010-02-21.json
{
    "activities":[
        {
            "activityId":51007,
            "activityParentId":90019,
            "calories":230,
            "description":"7mph",
            "distance":2.04,
            "duration":1097053,
            "hasStartTime":true,
            "isFavorite":true,
            "logId":1154701,
            "name":"Treadmill, 0% Incline",
            "startTime":"00:25",
            "steps":3783
        }
    ],
    "goals":{
        "caloriesOut":2826,
        "distance":8.05,
        "floors":150,
        "steps":10000
     },
    "summary":{
        "activityCalories":230,
        "caloriesBMR":1913,
        "caloriesOut":2143,
        "distances":[
            {"activity":"tracker", "distance":1.32},
            {"activity":"loggedActivities", "distance":0},
            {"activity":"total","distance":1.32},
            {"activity":"veryActive", "distance":0.51},
            {"activity":"moderatelyActive", "distance":0.51},
            {"activity":"lightlyActive", "distance":0.51},
            {"activity":"sedentaryActive", "distance":0.51},
            {"activity":"Treadmill, 0% Incline", "distance":3.28}
        ],
        "elevation":48.77,
        "fairlyActiveMinutes":0,
        "floors":16,
        "lightlyActiveMinutes":0,
        "marginalCalories":200,
        "sedentaryMinutes":1166,
        "steps":0,
        "veryActiveMinutes":0
    }
}

	 */

}
