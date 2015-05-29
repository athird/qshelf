package uk.ac.open.kmi.carre.qs.service.googlefit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import uk.ac.open.kmi.carre.qs.metrics.Activity;
import uk.ac.open.kmi.carre.qs.metrics.Height;
import uk.ac.open.kmi.carre.qs.metrics.Location;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.service.iHealth.OAuth2AccessToken;
import uk.ac.open.kmi.carre.qs.service.iHealth.IHealthApi;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

import uk.ac.open.kmi.carre.qs.service.googlefit.Haversine;

public class GooglefitService extends Service {
	public static final int SECONDS_PER_MINUTE = 60;

	public static final int MILLISECONDS_PER_SECOND = 1000;

	private static Logger logger = Logger.getLogger(GooglefitService.class.getName());

	public static final String name = "Google Fit API";
	public static final String machineName = "googlefit";
	public static final int version = 2;

	private OAuth2AccessToken accessToken = null;
	public static final String callbackURL = "https://carre.kmi.open.ac.uk/devices/oauth/googlefit";//"http://localhost:8080/quantified-shelf/shelfie/google";
	//https://accounts.google.com/o/oauth2/auth
	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + machineName;

	public static final String PROVENANCE = RDF_SERVICE_NAME;
	public static final String baseURL = "https://www.googleapis.com/fitness/v1/users/me/";
	public static final String oauthBaseURL = "https://accounts.google.com/o/oauth2";

	public static final String requestTokenURL = oauthBaseURL + "/auth/tokens/exchange";
	public static final String authURL = oauthBaseURL + "/auth";
	public static final String accessTokenURL = oauthBaseURL + "/token";
	//public static final String refreshTokenURL = "https://www.googleapis.com/oauth2/v3/token";
	public static final String refreshTokenURL = "https://www.googleapis.com/oauth2/v3/token";

	public static final String REQUEST_TOKEN_SESSION = "GOOGLEFIT_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "GOOGLEFIT_ACCESS_TOKEN_SESSION";

	public static final String GOOGLE_FIT_SCOPES = "https://www.googleapis.com/auth/fitness.activity.read https://www.googleapis.com/auth/fitness.body.read https://www.googleapis.com/auth/fitness.location.read";

	public static final int NANOSECONDS_PER_MILLISECOND = 1000000;

	public static final String STEP_COUNT_DATA_SOURCE = "derived:com.google.step_count.delta:com.google.android.gms:estimated_steps";
	public static final String ACTIVITY_DATA_SOURCE = "derived:com.google.activity.segment:com.google.android.gms:merge_activity_segments";
	public static final String DISTANCE_DATA_SOURCE = "derived:com.google.distance.delta:com.google.android.gms:pruned_distance";
	public static final String HEART_RATE_DATA_SOURCE = "derived:com.google.heart_rate.bpm:com.google.android.gms:merge_heart_rate_bpm";
	public static final String HEIGHT_DATA_SOURCE = "derived:com.google.height:com.google.android.gms:merge_height";
	public static final String LOCATION_DATA_SOURCE = "derived:com.google.location.sample:com.google.android.gms:merge_location_samples";
	public static final String WEIGHT_DATA_SOURCE = "derived:com.google.weight:com.google.android.gms:merge_weight";
	public static final Map<Integer,String> ACTIVITY_NAMES = getActivitiesMap();

	public static final int NOT_MOVING_ACTIVITY = 3;

	public static final int GOOGLE_MAPS_CALLS_PER_SECOND = 5;
	public static final long MILLISECONDS_BETWEEN_GOOGLE_MAPS_CALLS = MILLISECONDS_PER_SECOND / GOOGLE_MAPS_CALLS_PER_SECOND;

	private OAuthService service = null;
	private String userId = "";



	public GooglefitService(String propertiesPath) {
		super(propertiesPath);
		logger.setLevel(Level.FINER);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String createService(HttpServletRequest request,
			HttpServletResponse response, String callback) {
		OAuthService service = new ServiceBuilder()
		.provider(Google2Api.class)
		.apiKey(oauth_token)
		.apiSecret(oauth_secret)
		.callback(callback)
		.build();

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
				OAuth2AccessToken token = getOAuth2AccessToken(json, null);
				accessToken = token;

				conn.disconnect();
			} catch (MalformedURLException e) {
				logger.finer(e.getMessage());
			} catch (IOException e) {
				logger.finer(e.getMessage());
			}
			logger.finer(authURL);

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,11,14);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);


			for (Metric metric : metrics) {
				logger.finer(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.finer(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}



			return "";
		} else {
			Token token = new Token("","");
			String authoriseURL = service.getAuthorizationUrl(token);
			authoriseURL += "&access_type=offline";
			authoriseURL += "&approval_prompt=auto";
			authoriseURL += "&scope=" + GOOGLE_FIT_SCOPES;
			return authoriseURL;

		}
	}

	public String getDatasetID(Date startDate, Date endDate) {
		long startTimeNS = startDate.getTime() * NANOSECONDS_PER_MILLISECOND;
		long endTimeNS = endDate.getTime() * NANOSECONDS_PER_MILLISECOND;
		String dataSetID = "" + startTimeNS + "-" + endTimeNS;	
		return dataSetID;
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		List<Metric> activities = getActivity(startDate, endDate);
		if (activities != null) {
			results.addAll(activities);
		}
		List<Metric> body = getBody(startDate, endDate);
		if (body != null) {
			results.addAll(body);
		}
		List<Metric> heartRates = getHeartRates(startDate, endDate);
		if (heartRates != null) {
			results.addAll(heartRates);
		}
		List<Metric> locations = getLocations(startDate, endDate);
		if (locations != null) {
			results.addAll(locations);
		}
		return results;
	}

	public List<Metric> getActivity(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>(); 
		List<Metric> stepCounts = getStepCounts(startDate, endDate);
		if (stepCounts != null) {
			results.addAll(stepCounts);
		}
		List<Metric> distances = getDistances(startDate, endDate);
		if (distances != null) {
			results.addAll(distances);
		}
		List<Metric> loggedActivities = getLoggedActivities(startDate, endDate);
		if (loggedActivities != null) {
			results.addAll(loggedActivities);
		}
		return results;
	}

	public List<Metric> getStepCounts(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";
		List<Metric> results = new ArrayList<Metric>();
		startDate = setToMidnight(startDate);
		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}

			String stepCountJSON = makeApiCall("dataSources/" + STEP_COUNT_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);

			JSONObject stepCountJSONObject = (JSONObject) JSONValue.parse(stepCountJSON);

			if (stepCountJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(stepCountJSON);
			}
			nextPageToken = (String) stepCountJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}

			JSONArray stepCountValues = (JSONArray) stepCountJSONObject.get("point");
			Date nextDate = getNextDate(startDate);
			Date lastValueDate = endDate;

			int steps = 0;

			if (stepCountValues != null) {
				for (int i = 0; i < stepCountValues.size(); i++) {
					JSONObject currentValue = (JSONObject) stepCountValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					int currentSteps = ((Long) ((JSONObject) valueJSON.get(0)).get("intVal")).intValue();

					if (valueStart.getTime() > nextDate.getTime() || valueEnd.getTime() > endDate.getTime()) {
						logger.finer("Steps between " + formatDate(startDate) + " and " + formatDate(nextDate) + ": " + steps);
						Activity activity = new Activity(machineName, valueEnd);
						activity.setSteps(steps);
						results.add(activity);
						startDate = nextDate;
						nextDate = getNextDate(nextDate);
						steps = currentSteps;
					} else {
						steps += currentSteps;
					}
					lastValueDate = valueEnd;

				}
				Activity activity = new Activity(machineName, lastValueDate);
				activity.setSteps(steps);
				results.add(activity);

				return results;
			} 
		}
		return null;
	}

	public List<Metric> getLoggedActivities(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();

		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}
			String activityJSON = makeApiCall("dataSources/" + ACTIVITY_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject activityJSONObject = (JSONObject) JSONValue.parse(activityJSON);
			if (activityJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(activityJSON);
			}

			nextPageToken = (String) activityJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}



			JSONArray activityValues = (JSONArray) activityJSONObject.get("point");

			if (activityValues != null) {
				for (int i = 0; i < activityValues.size(); i++) {
					JSONObject currentValue = (JSONObject) activityValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					int loggedActivityCode = ((Long) ((JSONObject) valueJSON.get(0)).get("intVal")).intValue();
					if (loggedActivityCode != NOT_MOVING_ACTIVITY) {
						String loggedActivityName = getActivityFromCode(loggedActivityCode);
						logger.finer("Logged activity on " + formatDate(startDate) + ": " + loggedActivityName);
						Activity activity = new Activity(machineName, valueEnd);
						long loggedActivityDuration = minutesBetweenDates(valueStart, valueEnd);
						activity.setLoggedActivityName(loggedActivityName);
						activity.setLoggedActivityDuration(loggedActivityDuration);
						results.add(activity);
					}

				}
				return results;
			}
		}
		return null;
	}

	public List<Metric> getDistances(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();
		startDate = setToMidnight(startDate);
		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}
			String distancesJSON = makeApiCall("dataSources/" + DISTANCE_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject distancesJSONObject = (JSONObject) JSONValue.parse(distancesJSON);
			if (distancesJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(distancesJSON);
				return null;
			}

			nextPageToken = (String) distancesJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}


			JSONArray distancesValues = (JSONArray) distancesJSONObject.get("point");
			Date nextDate = getNextDate(startDate);
			float distance = 0;
			Date lastValueDate = endDate;
			if (distancesValues != null) {
				for (int i = 0; i < distancesValues.size(); i++) {
					JSONObject currentValue = (JSONObject) distancesValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);
					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					float currentDistance = ((Double) ((JSONObject) valueJSON.get(0)).get("fpVal")).floatValue();
					if (valueStart.getTime() > nextDate.getTime() || valueEnd.getTime() > endDate.getTime()) {
						Activity activity = new Activity(machineName, valueEnd);
						activity.setDistance(distance);
						results.add(activity);
						startDate = nextDate;
						nextDate = getNextDate(nextDate);
						distance = currentDistance;
					} else {
						distance += currentDistance;
					}
					lastValueDate = valueEnd;

				}
				Activity activity = new Activity(machineName, lastValueDate);
				activity.setDistance(distance);
				results.add(activity);
				return results;
			}
		}
		return null;
	}

	public List<Metric> getHeartRates(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();

		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}
			String heartRateJSON = makeApiCall("dataSources/" + HEART_RATE_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject heartRateJSONObject = (JSONObject) JSONValue.parse(heartRateJSON);
			if (heartRateJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(heartRateJSON);
			}

			nextPageToken = (String) heartRateJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}


			JSONArray heartRateValues = (JSONArray) heartRateJSONObject.get("point");

			if (heartRateValues != null) {
				for (int i = 0; i < heartRateValues.size(); i++) {
					JSONObject currentValue = (JSONObject) heartRateValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					float heartRate = ((Double) ((JSONObject) valueJSON.get(0)).get("fpVal")).floatValue();
					logger.finer("Logged heart rate on " + formatDate(startDate) + ": " + heartRate);
					Pulse pulse = new Pulse(machineName, valueEnd);
					pulse.setPulse(Math.round(heartRate));
					results.add(pulse);

				}
				return results;
			} 
		}
		return null;
	}

	public List<Metric> getBody(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>(); 
		List<Metric> heights = getHeights(startDate, endDate);
		if (heights != null) {
			results.addAll(heights);
		}
		List<Metric> weights = getWeights(startDate, endDate);
		if (weights != null) {
			results.addAll(weights);
		}
		return results;
	}

	public List<Metric> getHeights(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();

		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}

			String heightJSON = makeApiCall("dataSources/" + HEIGHT_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject heightJSONObject = (JSONObject) JSONValue.parse(heightJSON);
			if (heightJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(heightJSON);
			}


			nextPageToken = (String) heightJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}

			JSONArray heightValues = (JSONArray) heightJSONObject.get("point");
			if (heightValues != null) {
				for (int i = 0; i < heightValues.size(); i++) {
					JSONObject currentValue = (JSONObject) heightValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					float heightVal = ((Double) ((JSONObject) valueJSON.get(0)).get("fpVal")).floatValue();
					logger.finer("Logged height on " + formatDate(startDate) + ": " + heightVal);
					Height height = new Height(machineName, valueEnd);
					height.setHeight(heightVal);
					results.add(height);

				}
				return results;
			}
		}
		return null;
	}

	public List<Metric> getWeights(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();

		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}
			String weightJSON = makeApiCall("dataSources/" + HEIGHT_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject weightJSONObject = (JSONObject) JSONValue.parse(weightJSON);
			if (weightJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(weightJSON);
			}

			nextPageToken = (String) weightJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}


			JSONArray weightValues = (JSONArray) weightJSONObject.get("point");

			if (weightValues != null) {
				for (int i = 0; i < weightValues.size(); i++) {
					JSONObject currentValue = (JSONObject) weightValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					float weightVal = ((Double) ((JSONObject) valueJSON.get(0)).get("fpVal")).floatValue();
					logger.finer("Logged weight on " + formatDate(startDate) + ": " + weightVal);
					Weight weight = new Weight(machineName, valueEnd);
					weight.setWeight(weightVal);
					results.add(weight);

				}
				return results;
			} 
		}
		return null;
	}

	public List<Metric> getLocations(Date startDate, Date endDate) {
		boolean keepPaging = true;
		String nextPageToken = "";

		List<Metric> results = new ArrayList<Metric>();

		String datasetID = getDatasetID(startDate, endDate);
		while (keepPaging) {
			Map<String,String> parameters = null;
			if (nextPageToken != null && !nextPageToken.equals("")) {
				parameters = new HashMap<String,String>();
				parameters.put("pageToken", nextPageToken);
			}String locationJSON = makeApiCall("dataSources/" + LOCATION_DATA_SOURCE.replaceAll(" ", "%20") + "/datasets/" + datasetID, parameters);
			JSONObject locationJSONObject = (JSONObject) JSONValue.parse(locationJSON);
			if (locationJSONObject == null) {
				logger.finer("Couldn't parse JSON...");
				logger.finer(locationJSON);
			}

			nextPageToken = (String) locationJSONObject.get("nextPageToken");
			if (nextPageToken == null || nextPageToken.equals("")) {
				keepPaging = false;
			} else {
				keepPaging = true;
			}



			JSONArray locationValues = (JSONArray) locationJSONObject.get("point");

			Date currentStart = startDate;
			Date currentEnd = endDate;
			String currentLocationDetails = "";

			if (locationValues != null) {
				float previousLatitude = -1;
				float previousLongitude = -1;
				for (int i = 0; i < locationValues.size(); i++) {
					JSONObject currentValue = (JSONObject) locationValues.get(i);
					long startTimeNS = Long.parseLong((String) currentValue.get("startTimeNanos"));
					long endTimeNS = Long.parseLong((String) currentValue.get("endTimeNanos"));
					Date valueStart = new Date(startTimeNS / NANOSECONDS_PER_MILLISECOND);
					Date valueEnd = new Date(endTimeNS / NANOSECONDS_PER_MILLISECOND);


					JSONArray valueJSON = (JSONArray) currentValue.get("value");
					float latitudeVal = ((Double) ((JSONObject) valueJSON.get(0)).get("fpVal")).floatValue();
					float longitudeVal = ((Double) ((JSONObject) valueJSON.get(1)).get("fpVal")).floatValue();
					float accuracyVal = ((Double) ((JSONObject) valueJSON.get(2)).get("fpVal")).floatValue();
					boolean skipThisOne = false;
					if (previousLatitude != -1 && previousLongitude != -1) {
						double distance = Haversine.haversine(previousLatitude, previousLongitude, latitudeVal, longitudeVal);
						if (distance < 10) {
							skipThisOne = true;
							previousLatitude = latitudeVal;
							previousLongitude = longitudeVal;
						}
					} else {
						previousLatitude = latitudeVal;
						previousLongitude = longitudeVal;
					}
					if (!skipThisOne) {
						try {
							TimeUnit.MILLISECONDS.sleep(2* MILLISECONDS_BETWEEN_GOOGLE_MAPS_CALLS);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String locationURL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitudeVal + "," + longitudeVal + "&sensor=true";
						String locationContent = "";

						HttpURLConnection conn;
						try {
							conn = (HttpURLConnection) (new URL(locationURL)).openConnection();

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
								locationContent += output;
							}

							conn.disconnect();

						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if (locationContent != null && !locationContent.equals("")) {
							JSONObject locationNamesJSON = (JSONObject) JSONValue.parse(locationContent);
							if (locationNamesJSON != null) {
								JSONArray locationNamesResults = (JSONArray) locationNamesJSON.get("results");
								if (locationNamesResults != null && locationNamesResults.size() > 0) {
									JSONObject locationNamesComponents = (JSONObject) locationNamesResults.get(0);
									if (locationNamesComponents != null) {
										JSONArray addressComponents = (JSONArray) locationNamesComponents.get("address_components");
										String postalTown = "";
										String country = "";
										for (int j = 0; j < addressComponents.size(); j++) {
											JSONObject component = (JSONObject) addressComponents.get(j);
											JSONArray types = (JSONArray) component.get("types");
											for (int k = 0; k < types.size(); k++) {
												String type = (String) types.get(k);
												if (type.equals("postal_town")) {
													String longName = (String) component.get("long_name");
													postalTown = longName;
												} else if (type.equals("country")) {
													String longName = (String) component.get("long_name");
													country = longName;
												} 
											}
										}

										String locationDetailsString = "";
										if (!postalTown.equals("")) {
											locationDetailsString += postalTown;
											if (!country.equals("")) {
												locationDetailsString += ", ";
											}
										}
										if (!country.equals("")) {
											locationDetailsString += country;
										}
										logger.finer("Location details " + locationDetailsString + " on " + formatDate(valueStart));

										if (locationDetailsString.equals(currentLocationDetails) || locationDetailsString.equals("")) {
											currentEnd = valueEnd;
										} else if (!locationDetailsString.equals("")) {
											Location location = new Location(machineName, currentStart);
											location.setStartDate(currentStart);
											location.setEndDate(currentEnd);
											location.setLocation(currentLocationDetails);
											results.add(location);
											currentLocationDetails = locationDetailsString;
											currentStart = valueStart;
											currentEnd = valueEnd;
										} else {
											currentLocationDetails = locationDetailsString;
											currentStart = valueStart;
											currentEnd = valueEnd;
										}
									}
								}
							}
						}
					}
				}
				if (!currentLocationDetails.equals("")) {
					Location location = new Location(machineName, currentStart);
					location.setStartDate(currentStart);
					location.setEndDate(currentEnd);
					location.setLocation(currentLocationDetails);
					results.add(location);
				}
				return results;
			}
		}
		return null;
	}

	@Override
	public void handleNotification(String requestContent) {
		String json = requestContent;

		if (json != null || !json.equals("")) {
			JSONArray jsonArray = null;
			try {
				jsonArray = (JSONArray) JSONValue.parse(json);
			} catch (ClassCastException e) {
				jsonArray = (JSONArray) JSONValue.parse("[ " + json + "]");
			}
			logger.finer(json);
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject notifyJson = (JSONObject) jsonArray.get(i);

				String dateString = (String) notifyJson.get("start_date");
				if (dateString == null || dateString.equals("")) {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
					dateString = formatter.format(Calendar.getInstance().getTime());
				}
				String userId = (String) notifyJson.get("user_id");

				String endDateString = (String) notifyJson.get("end_date");
				logger.finer(dateString + ", " +userId);

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

				try {
					Date endDate = null;
					if (endDateString != null && !endDateString.equals("")) {
						endDate = formatter.parse(endDateString);
					}
					Date mDate = formatter.parse(dateString);
					OAuth2AccessToken userToken = getTokenForUser(userId);
					if (userToken == null ) {
						logger.finer("token is null (notify, beginning)");
					}

					if (userToken.getUser().equals("")) {
						logger.finer("Token has no user!");
					} else {
						logger.finer("User: " + userToken.getUser());
					}

					if (userToken != null && !userToken.getUser().equals("")) {
						String user = userToken.getUser();
						logger.finer(user);
						OAuth2AccessToken oldAccessToken = accessToken;
						accessToken = userToken;

						service = new ServiceBuilder()
						.provider(Google2Api.class)
						.apiKey(oauth_token)
						.apiSecret(oauth_secret)
						.callback(GooglefitService.callbackURL)
						.build();


						List<Metric> newMetrics = new ArrayList<Metric>();

						newMetrics.addAll(getMetrics(mDate, endDate));


						String rdf = "";
						for (Metric metric : newMetrics) {
							rdf += metric.getMeasuredByRDF(PROVENANCE, user);
							rdf += metric.toRDFString(user);
						}
						logger.finer(rdf);
						accessToken = oldAccessToken;

						if (!rdf.equals("")) {
							CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
							boolean success = true;
							List<String> triples = Service.chunkRDF(rdf);
							for (String tripleSet : triples) {
								success &= connector.insertTriples(user, tripleSet);
							}
							if (!success) {
								logger.finer("Failed to insert triples.");
							}
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}	
			}
		}


	}

	public OAuth2AccessToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?connection  ?oauth_token ?expires ?refresh_token ?expires_at FROM " +
				"<" + CARREVocabulary.USER_URL + userId + "> "
				+ "{\n" + //?apinames 
				"?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				" ?connection <" 
				+ CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n" +
				" ?connection <" 
				+ CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?refresh_token.\n" +
				" ?connection <" 
				+ CARREVocabulary.EXPIRES_TOKEN_PREDICATE + "> ?expires.\n" +
				" ?connection <" 
				+ CARREVocabulary.EXPIRES_AT_PREDICATE + "> ?expires_at.\n" +
				//				" ?connection <" 
				//				+ CARREVocabulary.APINAMES_TOKEN_PREDICATE + "> ?apinames.\n" +
				////				" ?connection <" + 
				//				CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret." 
				"}\n";

		logger.finer(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		OAuth2AccessToken token = null;
		String connection = "";
		boolean cheat = false;
		while (results.hasNext() || cheat) {
			String oauth_token = "";
			//String oauth_secret = secretLiteral.getString();
			String expires = "";
			String refresh_token = "";
			String expiresAt = "";

			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			//Literal secretLiteral = solution.getLiteral("oauth_secret");
			Literal expiresLiteral = solution.getLiteral("expires");
			Literal refreshLiteral = solution.getLiteral("refresh_token");
			//Literal apiNamesLiteral = solution.getLiteral("apinames");
			Literal expiresAtLiteral = solution.getLiteral("expires_at");

			Resource connectionResource = solution.getResource("connection");
			if (tokenLiteral == null 
					|| expiresLiteral == null || refreshLiteral == null
					|| connectionResource == null || expiresAtLiteral == null) {
				//|| apiNamesLiteral == null) {
				logger.finer("One of the authentication details is null!");
				return null;
			}
			oauth_token = tokenLiteral.getString();
			//String oauth_secret = secretLiteral.getString();
			expires = expiresLiteral.getString();
			refresh_token = refreshLiteral.getString();
			expiresAt = expiresAtLiteral.getString();

			connection = connectionResource.getURI();


			logger.finer("token literal is " + oauth_token + 
					//", secret literal is " + oauth_secret + 
					", expires is " + expires +
					", refresh_token is " + refresh_token +
					", expires_at is " + expiresAt);
			Date expiresAtDate = null;
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			try {
				expiresAtDate = format.parse(expiresAt);
			} catch (ParseException e) {
				try {
					//2015-01-22T10:38Z
					SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					expiresAtDate = format2.parse(expiresAt);
				} catch (ParseException f) {
					try {
						expiresAtDate = format.parse(expiresAt.substring(0,expiresAt.length() - 4));
					} catch (ParseException g) {
						try {
							SimpleDateFormat format3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							expiresAtDate = format3.parse(expiresAt);
						} catch (ParseException h) {
							try {
								String simplifiedDate = expiresAt.replaceAll("([0-9])T([0-9])", "$1 $2");
								simplifiedDate = simplifiedDate.replaceAll("Z", "");
								SimpleDateFormat format4 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								expiresAtDate = format4.parse(simplifiedDate);
							} catch (ParseException i) {
								logger.finer("Couldn't parse RDF date.");
							}
						}
					}
				}
			}
			if (expiresAtDate == null) {
				format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
				try {
					expiresAtDate = format.parse(expiresAt);
				} catch (ParseException e) {
					try {
						//2015-01-22T10:38Z
						SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
						expiresAtDate = format2.parse(expiresAt);
					} catch (ParseException f) {
						try {
							expiresAtDate = format.parse(expiresAt.substring(0,expiresAt.length() - 4));
						} catch (ParseException g) {
							try {
								SimpleDateFormat format3 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
								expiresAtDate = format3.parse(expiresAt);
							} catch (ParseException h) {
								try {
									String simplifiedDate = expiresAt.replaceAll("([0-9])T([0-9])", "$1 $2");
									simplifiedDate = simplifiedDate.replaceAll("Z", "");
									SimpleDateFormat format4 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
									expiresAtDate = format4.parse(simplifiedDate);
								} catch (ParseException i) {
									logger.finer("Couldn't parse RDF date.");
								}
							}
						}
					}
				}
			}

			token = new OAuth2AccessToken(oauth_token, "");
			//token.setApiNames(apiNames);
			token.setExpires(expires);
			token.setRefreshToken(refresh_token);
			token.setUserId(userId);
			token.setUser(userId);
			if (expiresAtDate != null) {
				token.setExpiresAt(expiresAtDate);
			} else {
				logger.finer("Expires at was null!");
			}
			token.setConnectionURI(connection);
		}

		logger.finer("Returning access token.");
		if (token == null ) {
			logger.finer("token is null (sparql, end");
		} else {

			Date today = Calendar.getInstance().getTime();
			if (token.getExpiresAt() != null && today.after(token.getExpiresAt())) {
				OAuth2AccessToken newToken = getOAuth2AccessToken( token);
				logger.finer("Retrieving new access token. ");
				logger.finer("token literal is " + newToken.getToken() + 
						//", secret literal is " + oauth_secret + 
						", expires is " + newToken.getExpires() +
						", refresh_token is " + newToken.getRefreshToken() +
						", expires_at is " + formatDate(newToken.getExpiresAt()));
				String newDateString = newToken.getRDFDate(newToken.getExpiresAt());
				connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + ">", "\"" + newToken.getToken() + "\"" + CARREVocabulary.STRING_TYPE);
				connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.EXPIRES_AT_PREDICATE + ">",  newDateString );
				return newToken;
			} else {
				return token;
			}
		}
		return token;
	}

	@Override
	public String getProvenance() {
		return PROVENANCE;
	}

	public OAuth2AccessToken getOAuth2AccessToken( OAuth2AccessToken accessToken) {
		String url = refreshTokenURL; 
		String parameters = "client_id=" + oauth_token
				+ "&client_secret=" + oauth_secret
				+ "&grant_type=refresh_token"
				+ "&refresh_token=" + accessToken.getRefreshToken();
		logger.finer(url);
		OAuth2AccessToken token = null;
		try {
			URL getAccessToken = new URL(url);

			HttpURLConnection conn = (HttpURLConnection) getAccessToken.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", "" + parameters.length());
			conn.setDoOutput(true);
			conn.getOutputStream().write(parameters.getBytes());
			conn.getOutputStream().flush();


			InputStream response = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(response));

			JSONObject results = (JSONObject) JSONValue.parse(br);
			logger.finer(results.toJSONString());
			token = getOAuth2AccessToken(results, accessToken.getRefreshToken());
			token.setUser(accessToken.getUser());
			token.setConnectionURI(accessToken.getConnectionURI());
			if (!token.getRefreshToken().equals(accessToken.getRefreshToken())) {
				logger.finer("Update refresh tokens here...");
			}

			return token;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (token == null ) {
			logger.finer("token is null (Token, end)");
		}
		return token;
	}

	public static OAuth2AccessToken getOAuth2AccessToken(JSONObject json, String refreshToken) {
		String accessToken = (String) json.get("access_token");
		OAuth2AccessToken token = new OAuth2AccessToken(accessToken, "");
		int expires = ((Long) json.get("expires_in")).intValue();
		token.setExpires("" + expires);
		if (refreshToken != null && refreshToken.equals("")) {
			String newRefreshToken = (String) json.get("refresh_token");
			if (newRefreshToken != null && !newRefreshToken.equals("")) {
				token.setRefreshToken(newRefreshToken);
			}
		} else if (refreshToken != null) {
			token.setRefreshToken(refreshToken);
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, expires);
		Date expiresAt = cal.getTime();
		token.setExpiresAt(expiresAt);

		return token;
	}

	public String makeApiCall(String keyword, Map<String,String> parameters) {
		String apiURL = baseURL + keyword;
		logger.finer(apiURL);
		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, apiURL);
		serviceRequest.addHeader("Authorization", "Bearer " + accessToken.getToken());

		if (parameters != null) {
			for (String paramName : parameters.keySet()) {
				logger.finer(paramName + "=" + parameters.get(paramName));
				serviceRequest.addQuerystringParameter(paramName, 
						parameters.get(paramName));
			}

		}
		Response requestResponse = serviceRequest.send();
		//logger.finer(requestResponse.getBody());
		String results = requestResponse.getBody();
		//	logger.finer(results);


		return results;
	}

	public Date setToMidnight(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public Date getNextDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		return format.format(date);
	}

	public long minutesBetweenDates(Date startDate, Date endDate) {
		long millisecondsDifference = endDate.getTime() - startDate.getTime();
		long minutes = (millisecondsDifference / MILLISECONDS_PER_SECOND) / SECONDS_PER_MINUTE;
		return minutes;
	}

	public String getActivityFromCode(int code) {
		return ACTIVITY_NAMES.get(code);
	}

	public static Map<Integer,String> getActivitiesMap() {
		Map<Integer,String> activityMap = new HashMap<Integer,String>();
		activityMap.put(9, "Aerobics");
		activityMap.put(10, "Badminton");
		activityMap.put(11, "Baseball");
		activityMap.put(12, "Basketball");
		activityMap.put(13, "Biathlon");
		activityMap.put(1, "Biking");
		activityMap.put(14, "Handbiking");
		activityMap.put(15, "Mountain biking");
		activityMap.put(16, "Road biking");
		activityMap.put(17, "Spinning");
		activityMap.put(18, "Stationary biking");
		activityMap.put(19, "Utility biking");
		activityMap.put(20, "Boxing");
		activityMap.put(21, "Calisthenics");
		activityMap.put(22, "Circuit training");
		activityMap.put(23, "Cricket");
		activityMap.put(106, "Curling");
		activityMap.put(24, "Dancing");
		activityMap.put(102, "Diving");
		activityMap.put(25, "Elliptical");
		activityMap.put(103, "Ergometer");
		activityMap.put(26, "Fencing");
		activityMap.put(27, "Football (American)");
		activityMap.put(28, "Football (Australian)");
		activityMap.put(29, "Football (Soccer)");
		activityMap.put(30, "Frisbee");
		activityMap.put(31, "Gardening");
		activityMap.put(32, "Golf");
		activityMap.put(33, "Gymnastics");
		activityMap.put(34, "Handball");
		activityMap.put(35, "Hiking");
		activityMap.put(36, "Hockey");
		activityMap.put(37, "Horseback riding");
		activityMap.put(38, "Housework");
		activityMap.put(104, "Ice skating");
		activityMap.put(0, "In vehicle");
		activityMap.put(39, "Jumping rope");
		activityMap.put(40, "Kayaking");
		activityMap.put(41, "Kettlebell training");
		activityMap.put(42, "Kickboxing");
		activityMap.put(43, "Kitesurfing");
		activityMap.put(44, "Martial arts");
		activityMap.put(45, "Meditation");
		activityMap.put(46, "Mixed martial arts");
		activityMap.put(2, "On foot");
		activityMap.put(47, "P90X exercises");
		activityMap.put(48, "Paragliding");
		activityMap.put(49, "Pilates");
		activityMap.put(50, "Polo");
		activityMap.put(51, "Racquetball");
		activityMap.put(52, "Rock climbing");
		activityMap.put(53, "Rowing");
		activityMap.put(54, "Rowing machine");
		activityMap.put(55, "Rugby");
		activityMap.put(8, "Running");
		activityMap.put(56, "Jogging");
		activityMap.put(57, "Running on sand");
		activityMap.put(58, "Running (treadmill)");
		activityMap.put(59, "Sailing");
		activityMap.put(60, "Scuba diving");
		activityMap.put(61, "Skateboarding");
		activityMap.put(62, "Skating");
		activityMap.put(63, "Cross skating");
		activityMap.put(105, "Indoor skating");
		activityMap.put(64, "Inline skating (rollerblading)");
		activityMap.put(65, "Skiing");
		activityMap.put(66, "Back-country skiing");
		activityMap.put(67, "Cross-country skiing");
		activityMap.put(68, "Downhill skiing");
		activityMap.put(69, "Kite skiing");
		activityMap.put(70, "Roller skiing");
		activityMap.put(71, "Sledding");
		activityMap.put(72, "Sleeping");
		activityMap.put(73, "Snowboarding");
		activityMap.put(74, "Snowmobile");
		activityMap.put(75, "Snowshoeing");
		activityMap.put(76, "Squash");
		activityMap.put(77, "Stair climbing");
		activityMap.put(78, "Stair-climbing machine");
		activityMap.put(79, "Stand-up paddleboarding");
		activityMap.put(3, "Still (not moving)");
		activityMap.put(80, "Strength training");
		activityMap.put(81, "Surfing");
		activityMap.put(82, "Swimming");
		activityMap.put(84, "Swimming (open water)");
		activityMap.put(83, "Swimming (swimming pool)");
		activityMap.put(85, "Table tenis (ping pong)");
		activityMap.put(86, "Team sports");
		activityMap.put(87, "Tennis");
		activityMap.put(5, "Tilting (sudden device gravity change)");
		activityMap.put(88, "Treadmill (walking or running)");
		activityMap.put(4, "Unknown (unable to detect activity)");
		activityMap.put(89, "Volleyball");
		activityMap.put(90, "Volleyball (beach)");
		activityMap.put(91, "Volleyball (indoor)");
		activityMap.put(92, "Wakeboarding");
		activityMap.put(7, "Walking");
		activityMap.put(93, "Walking (fitness)");
		activityMap.put(94, "Nording walking");
		activityMap.put(95, "Walking (treadmill)");
		activityMap.put(96, "Waterpolo");
		activityMap.put(97, "Weightlifting");
		activityMap.put(98, "Wheelchair");
		activityMap.put(99, "Windsurfing");
		activityMap.put(100, "Yoga");
		activityMap.put(101, "Zumba");
		return activityMap;
	}

}
