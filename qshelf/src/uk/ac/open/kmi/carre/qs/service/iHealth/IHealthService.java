package uk.ac.open.kmi.carre.qs.service.iHealth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.scribe.oauth.OAuthService;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import uk.ac.open.kmi.carre.qs.metrics.Activity;
import uk.ac.open.kmi.carre.qs.metrics.BloodPressure;
import uk.ac.open.kmi.carre.qs.metrics.Food;
import uk.ac.open.kmi.carre.qs.metrics.Glucose;
import uk.ac.open.kmi.carre.qs.metrics.Height;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.O2Saturation;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Sleep;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class IHealthService extends Service {

	public static final String name = "iHealth API";
	public static final String machineName = "iHealth";
	public static final int version = 2;

	//public static final String baseURL = "http://sandboxapi.ihealthlabs.com";
	public static final String baseURL = "https://api.ihealthlabs.com:8443";

	//public static final String requestTokenURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";
	public static final String requestTokenURL = "https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";
	//public static final String authURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";
	public static final String authURL = "https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";
	//public static final String accessTokenURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";
	public static final String accessTokenURL = "https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";
	public static final String callbackURL = "http://carre.kmi.open.ac.uk/devices/oauth/ihealth";//"http://localhost:8080/quantified-shelf/shelfie/ihealth";

	public static final String REQUEST_TOKEN_SESSION = "IHEALTH_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "IHEALTH_ACCESS_TOKEN_SESSION";

	private static final long MILLISECONDS_PER_SECOND = 1000;

	public static final String[] ALL_API_NAMES = {"OpenApiActivity", 
		"OpenApiBG", 
		"OpenApiBP", 
		"OpenApiSleep",
		"OpenApiSpO2", 
		"OpenApiUserInfo", 
		"OpenApiWeight",
	"OpenApiActivity"};

	private String apiSerialCode;
	private Map<String,String> apiSerialVersions;
	private Map<String,String> WHICH_API_NAME;

	private IHealthAccessToken accessToken = null;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + "ihealth";



	public static final String PROVENANCE = "carreManufacturer:iHealth";

	public IHealthService(String propertiesPath) {
		super(propertiesPath);
		apiSerialCode = Service.getProperty("SC", propertiesPath, machineName);
		apiSerialVersions = new HashMap<String,String>();
		for (String apiName : ALL_API_NAMES) {
			String apiSV = Service.getProperty(apiName + ".SV", propertiesPath, machineName);
			apiSerialVersions.put(apiName, apiSV);
		}
		WHICH_API_NAME = new HashMap<String, String>();
		WHICH_API_NAME.put("weight", "OpenApiWeight");
		WHICH_API_NAME.put("bp", "OpenApiBP");
		WHICH_API_NAME.put("glucose", "OpenApiBG");
		WHICH_API_NAME.put("sleep", "OpenApiSleep");
		WHICH_API_NAME.put("spo2", "OpenApiSpO2");
		WHICH_API_NAME.put("user-id", "OpenApiUserInfo");
		WHICH_API_NAME.put("activity", "OpenApiActivity");
	}

	@Override
	public void handleNotification(HttpServletRequest request, HttpServletResponse response) {
		String json = "";
		try {
			BufferedReader reader = request.getReader();
			String line = reader.readLine();
			while (line != null) {
				json += line;
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		JSONArray jsonArray = (JSONArray) JSONValue.parse(json);
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject notifyJson = (JSONObject) jsonArray.get(i);
			String collectionType = (String) notifyJson.get("CollectionType");
			String dateString = (String) notifyJson.get("MDate");
			String userId = (String) notifyJson.get("UserID");
			String subscriptionId = (String) notifyJson.get("SubscriptionId");
			String endDateString = (String) notifyJson.get("CARREEndDate");
			System.err.println(collectionType + ", " +dateString + ", " +userId + 
					", " + subscriptionId);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				Date endDate = null;
				if (endDateString != null && !endDateString.equals("")) {
					endDate = formatter.parse(endDateString);
				}
				Date mDate = formatter.parse(dateString);
				IHealthAccessToken userToken = getTokenForUser(userId);
				if (userToken == null ) {
					System.err.println("token is null (notify, beginning)");
				}

				if (userToken.getUser().equals("")) {
					System.err.println("Token has no user!");
				} else {
					System.err.println("User: " + userToken.getUser());
				}
				if (userToken.getUserId().equals("")) {
					System.err.println("Token has no userid!");
				} else {
					System.err.println("User Id: " + userToken.getUserId());
				}
				if (userToken != null && !userToken.getUser().equals("")) {
					String user = userToken.getUser();
					IHealthAccessToken oldAccessToken = accessToken;
					accessToken = userToken;

					OAuthService service = new ServiceBuilder()
					.provider(IHealthApi.class)
					.apiKey(oauth_token)
					.apiSecret(oauth_secret)
					.callback(this.callbackURL)
					.build();


					List<Metric> newMetrics = new ArrayList<Metric>();
					//bp,weight,glucose,spo2,activity,sleep,userinfo,food
					if (collectionType == null || collectionType.equals("")) {

						newMetrics.addAll(getMetrics(mDate, endDate));
					} else if (collectionType.equals("activity")) {

						newMetrics.addAll(getActivityBetween(mDate, endDate));
					} else if (collectionType.equals("weight")) {

						newMetrics.addAll(getWeightBetween(mDate, endDate));
					} else if (collectionType.equals("sleep")) {

						newMetrics.addAll(getSleepBetween(mDate, endDate));
					} else if (collectionType.equals("bp")) {

						newMetrics.addAll(getBPBetween(mDate, endDate));
					} else if (collectionType.equals("glucose")) {

						newMetrics.addAll(getGlucoseBetween(mDate, endDate));
					} else if (collectionType.equals("spo2")) {

						newMetrics.addAll(getOxygenBetween(mDate, endDate));
					} else if (collectionType.equals("food")) {

						newMetrics.addAll(getFoodBetween(mDate, endDate));
					} else if (collectionType.equals("sport")) {

						newMetrics.addAll(getSportBetween(mDate, endDate));
					} else {

						newMetrics.addAll(getMetrics(mDate, endDate));
					}


					String rdf = "";
					for (Metric metric : newMetrics) {
						rdf += metric.getMeasuredByRDF(PROVENANCE);
						rdf += metric.toRDFString();
					}
					accessToken = oldAccessToken;

					System.err.println(rdf);
					if (!rdf.equals("")) {
						CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
						boolean success = true;
						List<String> triples = Service.chunkRDF(rdf);
						for (String tripleSet : triples) {
							success &= connector.insertTriples(user, tripleSet);
						}
						if (!success) {
							System.err.println("Failed to insert triples.");
						}
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		}

	}

	public IHealthAccessToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?connection ?user ?oauth_token ?expires ?refresh_token WHERE {\n" + //?apinames 
				"GRAPH ?user {\n ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				" ?connection <" + 
				CARREVocabulary.USER_ID_PREDICATE + "> \"" + userId + "\"" + CARREVocabulary.STRING_TYPE +  " .\n" +
				" ?connection <" 
				+ CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n" +
				" ?connection <" 
				+ CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?refresh_token.\n" +
				" ?connection <" 
				+ CARREVocabulary.EXPIRES_TOKEN_PREDICATE + "> ?expires.\n" +
				//				" ?connection <" 
				//				+ CARREVocabulary.APINAMES_TOKEN_PREDICATE + "> ?apinames.\n" +
				////				" ?connection <" + 
				//				CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret." 
				" }\n}\n";

		System.err.println(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		IHealthAccessToken token = null;
		String connection = "";
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			//Literal secretLiteral = solution.getLiteral("oauth_secret");
			Literal expiresLiteral = solution.getLiteral("expires");
			Literal refreshLiteral = solution.getLiteral("refresh_token");
			//Literal apiNamesLiteral = solution.getLiteral("apinames");

			Resource userResource = solution.getResource("user");
			Resource connectionResource = solution.getResource("connection");
			if (tokenLiteral == null || userResource == null
					|| expiresLiteral == null || refreshLiteral == null
					|| connectionResource == null) {
				//|| apiNamesLiteral == null) {
				System.err.println("One of the authentication details is null!");
				return null;
			}
			String oauth_token = tokenLiteral.getString();
			//String oauth_secret = secretLiteral.getString();
			String expires = expiresLiteral.getString();
			String refresh_token = refreshLiteral.getString();
			String apiNames = refreshLiteral.getString();

			String user = userResource.getURI();
			connection = connectionResource.getURI();
			
			System.err.println("token literal is " + oauth_token + 
					//", secret literal is " + oauth_secret + 
					", user is " + user +
					", expires is " + expires +
					", refresh_token is " + refresh_token +
					", apiNames is " + apiNames);
			token = new IHealthAccessToken(oauth_token, "");
			//token.setApiNames(apiNames);
			token.setExpires(expires);
			token.setRefreshToken(refresh_token);
			token.setUserId(userId);
			token.setUser(user);
		}
		System.err.println("Returning access token.");
		if (token == null ) {
			System.err.println("token is null (sparql, end");
		}
		return getIHealthAccessToken(connection, token);
	}


	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		OAuthService service = new ServiceBuilder()
		.provider(IHealthApi.class)
		.apiKey(oauth_token)
		.apiSecret(oauth_secret)
		.callback(callback)
		.build();

		String code = request.getParameter("code");
		String error = request.getParameter("error");
		String errorDescription = request.getParameter("error_description");
		if (code == null || code.equals("")) {
			if (error == null || error.equals("")) {
				Token requestToken = new Token("", "");
				String url = service.getAuthorizationUrl(requestToken);
				if (url != null && !url.equals("")) {
					return url;
				}
			} else {
				return error + "ERRORSEP" + errorDescription;
			}
		} else {
			String url = accessTokenURL + "?"
					+ "client_id=" + oauth_token
					+ "&client_secret=" + oauth_secret
					+ "&grant_type=authorization_code"
					+ "&redirect_uri=" + callback
					+ "&code=" + code;

			try {
				URL getAccessToken = new URL(url);

				URLConnection conn = getAccessToken.openConnection();
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

				JSONObject results = (JSONObject) JSONValue.parse(br);
				accessToken = getIHealthAccessToken(results);
				System.err.println("APINames: " + accessToken.getApiNames());
				System.err.println("access token: " + accessToken.getToken());
				System.err.println("secret: " + accessToken.getSecret());
				System.err.println("userid: " + accessToken.getUserId());
				System.err.println("expires: " + accessToken.getExpires());
				System.err.println("refresh token: " + accessToken.getRefreshToken());

				Calendar cal = Calendar.getInstance();
				cal.set(2014, 06, 12);
				Calendar cal2 = Calendar.getInstance();
				cal.set(2014, 06, 30);
				List<Metric> metrics = getMetrics(null, null);//cal.getTime(), cal2.getTime());
				if (metrics != null ) {
					for (Metric metric : metrics) {
						System.err.println(metric.toRDFString());
					}
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}




		}
		return "";
	}

	private List<Activity> getActivityBetween(Date startDate, Date endDate) {
		List<Activity> results = new ArrayList<Activity>();

		String activityString = makeApiCall("activity", startDate, endDate);
		JSONObject activityJson = (JSONObject) JSONValue.parse(activityString);
		JSONArray pages = (JSONArray) activityJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray activityData = (JSONArray) page.get("ARDataList");
				if (activityData != null) {
					for (int j = 0; j < activityData.size(); j++) {
						JSONObject currentActivity = (JSONObject )activityData.get(j);
						if (currentActivity != null) {
							Number calories = (Number) currentActivity.get("Calories");
							String dataID = (String) currentActivity.get("DataID");
							Number distanceTravelled = (Number) currentActivity.get("DistanceTraveled");
							Number latitude = (Number) currentActivity.get("Lat");
							Number longitude = (Number) currentActivity.get("Lon");
							Number measurementDate = (Number) currentActivity.get("MDate");
							String note = (String) currentActivity.get("Note");
							Number steps = (Number) currentActivity.get("Steps");

							Activity activity = new Activity(dataID);
							activity.setId(dataID);
							activity.setCalories(calories.floatValue());
							activity.setDistance(distanceTravelled.floatValue());
							if (latitude != null && latitude.intValue() != -1) {
								activity.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								activity.setLongitude(longitude.floatValue());
							}
							activity.setSteps(steps.intValue());
							if (note != null) {
								activity.setNote(note);
							}
							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							activity.setDate(date);
							results.add(activity);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Metric> getBPBetween(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		String bpString = makeApiCall("bp", startDate, endDate);
		JSONObject bpJson = (JSONObject) JSONValue.parse(bpString);
		JSONArray pages = (JSONArray) bpJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray bpData = (JSONArray) page.get("BPDataList");
				if (bpData != null) {
					for (int j = 0; j < bpData.size(); j++) {
						JSONObject currentBP = (JSONObject )bpData.get(j);
						if (currentBP != null) {
							Number whoBPLevel = (Number) currentBP.get("BPL");
							String dataID = (String) currentBP.get("DataID");
							Number systolic = (Number) currentBP.get("HP");

							Number heartRate = (Number) currentBP.get("HR");
							Number isArrhythmic = (Number) currentBP.get("isArr");
							Number latitude = (Number) currentBP.get("Lat");
							Number longitude = (Number) currentBP.get("Lon");
							Number measurementDate = (Number) currentBP.get("MDate");
							String note = (String) currentBP.get("Note");
							Number diastolic = (Number) currentBP.get("LP");

							BloodPressure bp = new BloodPressure(dataID);
							bp.setId(dataID);
							bp.setWhoBPLevel(whoBPLevel.intValue());
							bp.setSystolicBloodPressure(systolic.intValue());
							bp.setDiastolicBloodPressure(diastolic.intValue());

							if (latitude != null) {
								bp.setLatitude(latitude.floatValue());
							}
							if (longitude != null) {
								bp.setLongitude(longitude.floatValue());
							}
							if (note != null) {
								bp.setNote(note);
							}

							Pulse pulse = new Pulse(dataID);
							if (isArrhythmic != null && isArrhythmic.intValue() != -1) {
								String arrhythmicStatus = "";
								switch (isArrhythmic.intValue()) {
								case 0:
									arrhythmicStatus = CARREVocabulary.SENSOR_RDF_PREFIX + "no_arrhythmia";
									break;
								case 1:
									arrhythmicStatus = CARREVocabulary.SENSOR_RDF_PREFIX + "arrhythmia_cordis";
									break;
								default: 
									arrhythmicStatus = "";
								}
								pulse.setHasArrhythmia(arrhythmicStatus);
								pulse.setPulse(heartRate.longValue());
							}
							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							bp.setDate(date);
							pulse.setDate(date);
							results.add(bp);
							results.add(pulse);
						}
					}
				}

			}
		}
		return results;
	}

	private List<Weight> getWeightBetween(Date startDate, Date endDate) {
		List<Weight> results = new ArrayList<Weight>();

		String weightString = makeApiCall("weight", startDate, endDate);
		JSONObject weightJson = (JSONObject) JSONValue.parse(weightString);
		JSONArray pages = (JSONArray) weightJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray weightData = (JSONArray) page.get("WeightDataList");
				if (weightData != null) {
					for (int j = 0; j < weightData.size(); j++) {
						JSONObject currentWeight = (JSONObject )weightData.get(j);
						if (currentWeight != null) {
							Number bmi = (Number) currentWeight.get("BMI");
							String dataID = (String) currentWeight.get("DataID");
							Number boneMass = (Number) currentWeight.get("BoneValue");
							Number dci = (Number) currentWeight.get("DCI");
							Number bodyFat = (Number) currentWeight.get("FatValue");
							Number muscleMass = (Number) currentWeight.get("MuscaleValue");
							if (muscleMass == null) {
								muscleMass = (Number) currentWeight.get("MuscleValue");
							}
							Number waterMass = (Number) currentWeight.get("WaterValue");
							Number totalMass = (Number) currentWeight.get("WeightValue");

							Number latitude = (Number) currentWeight.get("Lat");
							Number longitude = (Number) currentWeight.get("Lon");
							Number measurementDate = (Number) currentWeight.get("MDate");
							String note = (String) currentWeight.get("Note");

							Weight weight = new Weight(dataID);
							weight.setId(dataID);
							weight.setBmi(bmi.floatValue());
							weight.setBoneMass(boneMass.floatValue());
							weight.setBodyDCI(dci.floatValue());
							weight.setBodyFat(bodyFat.floatValue());
							weight.setMuscleMass(muscleMass.floatValue());
							weight.setWaterMass(waterMass.floatValue());
							if (latitude != null && latitude.intValue() != -1) {
								weight.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								weight.setLongitude(longitude.floatValue());
							}
							weight.setWeight(totalMass.floatValue());
							if (note != null) {
								weight.setNote(note);
							}
							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							weight.setDate(date);
							results.add(weight);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Glucose> getGlucoseBetween(Date startDate, Date endDate) {
		List<Glucose> results = new ArrayList<Glucose>();

		String glucoseString = makeApiCall("glucose", startDate, endDate);
		JSONObject glucoseJson = (JSONObject) JSONValue.parse(glucoseString);
		JSONArray pages = (JSONArray) glucoseJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray glucoseData = (JSONArray) page.get("BGDataList");
				if (glucoseData != null) {
					for (int j = 0; j < glucoseData.size(); j++) {
						JSONObject currentGlucose = (JSONObject )glucoseData.get(j);
						if (currentGlucose != null) {
							Number bloodGlucose = (Number) currentGlucose.get("BG");
							String dataID = (String) currentGlucose.get("DataID");
							String dinner = (String) currentGlucose.get("DinnerSituation");
							String drugs = (String) currentGlucose.get("DrugSituation");
							Number latitude = (Number) currentGlucose.get("Lat");
							Number longitude = (Number) currentGlucose.get("Lon");
							Number measurementDate = (Number) currentGlucose.get("MDate");
							String note = (String) currentGlucose.get("Note");

							Glucose glucose = new Glucose(dataID);
							glucose.setId(dataID);
							if (bloodGlucose == null || bloodGlucose.floatValue() == 0) {
								continue;
							}
							glucose.setGlucose(bloodGlucose.floatValue());
							if (latitude != null && latitude.intValue() != -1) {
								glucose.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								glucose.setLongitude(longitude.floatValue());
							}
							if (note != null) {
								glucose.setNote(note);
							}
							if (dinner != null && !dinner.equals("")) {
								glucose.setDinnerSituation(dinner);
							}
							if (drugs != null && !drugs.equals("")) {
								glucose.setDrugSituation(drugs);
							}
							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							glucose.setDate(date);
							results.add(glucose);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Metric> getOxygenBetween(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();

		String o2saturationString = makeApiCall("spo2", startDate, endDate);
		JSONObject o2saturationJson = (JSONObject) JSONValue.parse(o2saturationString);
		JSONArray pages = (JSONArray) o2saturationJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray o2saturationData = (JSONArray) page.get("BODataList");
				if (o2saturationData != null) {
					for (int j = 0; j < o2saturationData.size(); j++) {
						JSONObject currentO2Saturation = (JSONObject )o2saturationData.get(j);
						if (currentO2Saturation != null) {
							Number bloodO2Saturation = (Number) currentO2Saturation.get("BO");
							String dataID = (String) currentO2Saturation.get("DataID");
							Number heartRate = (Number) currentO2Saturation.get("HR");
							Number latitude = (Number) currentO2Saturation.get("Lat");
							Number longitude = (Number) currentO2Saturation.get("Lon");
							Number measurementDate = (Number) currentO2Saturation.get("MDate");
							String note = (String) currentO2Saturation.get("Note");

							O2Saturation o2saturation = new O2Saturation(dataID);
							o2saturation.setId(dataID);
							Pulse pulse = new Pulse(dataID);
							pulse.setId(dataID);
							if (bloodO2Saturation == null || bloodO2Saturation.floatValue() == 0) {
								continue;
							}
							o2saturation.setO2saturation(bloodO2Saturation.floatValue());
							if (latitude != null && latitude.intValue() != -1) {
								o2saturation.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								o2saturation.setLongitude(longitude.floatValue());
							}
							if (note != null) {
								o2saturation.setNote(note);
							}

							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							o2saturation.setDate(date);
							pulse.setDate(date);
							results.add(o2saturation);
							results.add(pulse);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Sleep> getSleepBetween(Date startDate, Date endDate) {
		List<Sleep> results = new ArrayList<Sleep>();

		String sleepString = makeApiCall("sleep", startDate, endDate);
		JSONObject sleepJson = (JSONObject) JSONValue.parse(sleepString);
		JSONArray pages = (JSONArray) sleepJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray sleepData = (JSONArray) page.get("SRDataList");
				if (sleepData != null) {
					for (int j = 0; j < sleepData.size(); j++) {
						JSONObject currentSleep = (JSONObject )sleepData.get(j);
						if (currentSleep != null) {
							Number timesAwake = (Number) currentSleep.get("Awaken");
							Number startTime = (Number) currentSleep.get("StartTime");
							Number endTime = (Number) currentSleep.get("EndTime");
							Number deepSleep = (Number) currentSleep.get("FallSleep");
							Number hoursSlept = (Number) currentSleep.get("HoursSlept");
							Number sleepEfficiency = (Number) currentSleep.get("SleepEfficiency");
							String dataID = (String) currentSleep.get("DataID");
							Number latitude = (Number) currentSleep.get("Lat");
							Number longitude = (Number) currentSleep.get("Lon");
							Number measurementDate = (Number) currentSleep.get("MDate");
							if (measurementDate == null ) {
								measurementDate = startTime;
							}
							String note = (String) currentSleep.get("Note");

							Sleep sleep = new Sleep(dataID);
							sleep.setId(dataID);

							sleep.setDeepSleepDuration(deepSleep.longValue());
							sleep.setAsleepDuration(hoursSlept.longValue());
							sleep.setSleepEfficiency(sleepEfficiency.floatValue());
							sleep.setTimesAwake(timesAwake.longValue());
							sleep.setSleepTime(endTime.longValue() - startTime.longValue());

							if (latitude != null && latitude.intValue() != -1) {
								sleep.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								sleep.setLongitude(longitude.floatValue());
							}
							if (note != null) {
								sleep.setNote(note);
							}

							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							sleep.setDate(date);
							results.add(sleep);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Food> getFoodBetween(Date startDate, Date endDate) {
		List<Food> results = new ArrayList<Food>();

		String foodString = makeApiCall("food", startDate, endDate);
		JSONObject foodJson = (JSONObject) JSONValue.parse(foodString);
		JSONArray pages = (JSONArray) foodJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray foodData = (JSONArray) page.get("FoodDataList");
				if (foodData != null) {
					for (int j = 0; j < foodData.size(); j++) {
						JSONObject currentFood = (JSONObject )foodData.get(j);
						if (currentFood != null) {
							Number amount = (Number) currentFood.get("Amount");
							Number calories = (Number) currentFood.get("Calories");
							String mealType = (String) currentFood.get("FoodKind");
							String foodName = (String) currentFood.get("FoodName");
							String dataID = (String) currentFood.get("DataID");
							Number latitude = (Number) currentFood.get("Lat");
							Number longitude = (Number) currentFood.get("Lon");
							Number measurementDate = (Number) currentFood.get("MDate");

							Food food = new Food(dataID);
							food.setId(dataID);

							food.setQuantity(amount.floatValue());
							food.setCalories(calories.floatValue());
							food.setFoodType(foodName);
							food.setMealType(mealType);

							if (latitude != null && latitude.intValue() != -1) {
								food.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								food.setLongitude(longitude.floatValue());
							}

							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							food.setDate(date);
							results.add(food);
						}
					}


				}
			}
		}
		return results;
	}

	private List<Activity> getSportBetween(Date startDate, Date endDate) {
		List<Activity> results = new ArrayList<Activity>();

		String activityString = makeApiCall("sport", startDate, endDate);
		JSONObject activityJson = (JSONObject) JSONValue.parse(activityString);
		JSONArray pages = (JSONArray) activityJson.get("pages");
		if (pages != null ) {
			for (int i = 0; i < pages.size(); i++) {
				JSONObject page = (JSONObject) pages.get(i);
				JSONArray activityData = (JSONArray) page.get("SPORTDataList");
				if (activityData != null) {
					for (int j = 0; j < activityData.size(); j++) {
						JSONObject currentActivity = (JSONObject )activityData.get(j);
						if (currentActivity != null) {
							Number startTime = (Number) currentActivity.get("SportStartTime");
							Number endTime = (Number) currentActivity.get("SportStartTime");
							Number calories = (Number) currentActivity.get("Calories");
							String activityName = (String) currentActivity.get("SportName");
							String dataID = (String) currentActivity.get("DataID");
							Number latitude = (Number) currentActivity.get("Lat");
							Number longitude = (Number) currentActivity.get("Lon");
							Number measurementDate = (Number) currentActivity.get("MDate");
							if (measurementDate == null ) {
								measurementDate = startTime;
							}

							Activity activity = new Activity(dataID);
							activity.setId(dataID);

							activity.setActivityCalories(calories.floatValue());
							activity.setLoggedActivityName(activityName);

							int duration = endTime.intValue() - startTime.intValue();
							activity.setLoggedActivityDuration(duration);

							if (latitude != null && latitude.intValue() != -1) {
								activity.setLatitude(latitude.floatValue());
							}
							if (longitude != null && longitude.intValue() != -1) {
								activity.setLongitude(longitude.floatValue());
							}

							Date date = new Date(measurementDate.longValue() * MILLISECONDS_PER_SECOND);
							activity.setDate(date);
							results.add(activity);
						}
					}


				}
			}
		}
		return results;
	}


	public IHealthAccessToken getIHealthAccessToken(String connectionURI, IHealthAccessToken accessToken) {
		String url = accessTokenURL + "?"
				+ "client_id=" + oauth_token
				+ "&client_secret=" + oauth_secret
				+ "&redirect_uri=" + callbackURL
				+ "&response_type=refresh_token"
				+ "&refresh_token=" + accessToken.getRefreshToken()
				+ "&UserID=" + accessToken.getUserId();
		IHealthAccessToken token = null;
		try {
			URL getAccessToken = new URL(url);

			URLConnection conn = getAccessToken.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			JSONObject results = (JSONObject) JSONValue.parse(br);
			System.err.println(results.toJSONString());
			token = getIHealthAccessToken(results);
			token.setUser(accessToken.getUser());
			if (!token.getRefreshToken().equals(accessToken.getRefreshToken())) {
				System.err.println("Update refresh tokens here...");
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
			System.err.println("token is null (Token, end)");
		}
		return token;
	}

	public static IHealthAccessToken getIHealthAccessToken(JSONObject results) {
		String accessToken = (String) results.get("AccessToken");
		IHealthAccessToken token = new IHealthAccessToken(accessToken, "");
		token.setApiNames((String) results.get("APIName"));
		token.setExpires("" + (Long) results.get("Expires"));
		token.setRefreshToken((String) results.get("RefreshToken"));
		if (results.get("UserID") != null) {
			token.setUserId((String) results.get("UserID"));
		}

		return token;
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		results.addAll(getActivityBetween(startDate, endDate));
		results.addAll(getBPBetween(startDate, endDate));
		results.addAll(getWeightBetween(startDate, endDate));
		results.addAll(getGlucoseBetween(startDate, endDate));
		results.addAll(getOxygenBetween(startDate, endDate));
		results.addAll(getSleepBetween(startDate, endDate));
		/*results.addAll(getFoodBetween(startDate, endDate));
		results.addAll(getSportBetween(startDate, endDate));
		 */return results;
	}

	public String makeApiCall(String method, Date startDate, Date endDate) {
		String fullResults = "{\"pages\":[\n";
		String fullResultsClose = "]}";

		String results = "";
		String url = baseURL + "/openapiv2/user/" + accessToken.getUserId()
				+ "/" + method + ".json/?";
		url += "client_id=" + oauth_token;
		url += "&client_secret=" + oauth_secret;
		url += "&access_token=" + accessToken.getToken();
		url += "&sc=" + apiSerialCode;
		String apiName = WHICH_API_NAME.get(method);
		url += "&sv=" + apiSerialVersions.get(apiName);
		if (startDate != null) {
			url += "&start_time=" + startDate.getTime() / MILLISECONDS_PER_SECOND;
		}
		if (endDate != null) {
			url += "&end_time=" + endDate.getTime() / MILLISECONDS_PER_SECOND;
		}
		/*if (pageIndex != -1) {
			url += "&page_index=" + pageIndex;
		}*/
		System.err.println(url);

		results = makeApiCall(url);
		fullResults += results;
		JSONObject jsonResults = (JSONObject) JSONValue.parse(results);
		String nextPageURL = (String) jsonResults.get("NextPageURL");
		if (nextPageURL != null && !nextPageURL.equals("")) {
			fullResults += ",\n";
		}
		while (nextPageURL != null && !nextPageURL.equals("")) {
			results = makeApiCall(nextPageURL);
			fullResults += results + ",\n";
		}
		fullResults += fullResultsClose;
		return fullResults;
	}

	String makeApiCall(String url) {
		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, url);

		Response requestResponse = serviceRequest.send();
		System.out.println(requestResponse.getBody());
		String results = requestResponse.getBody();
		System.err.println(results);
		return results;
	}

	@Override
	public String getProvenance() {
		return PROVENANCE;
	}
}
