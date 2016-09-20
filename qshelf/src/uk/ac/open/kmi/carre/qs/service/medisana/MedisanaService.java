package uk.ac.open.kmi.carre.qs.service.medisana;


/**

As far as how to use the metrics, it�s probably best to look at an existing example. I see at the moment that you just print out the parsed values via System.out. 
Instead, add them to a Metric object of the appropriate type.

For example, the iHealth equivalent of your method

public List<Metric> getTargetscale(Date date) 

is 

public List<Metric> getWeightBetween(Date startDate, Date endDate)

It�s quite a short method, so here it is in full, with comments inserted:

private List<Weight> getWeightBetween(Date startDate, Date endDate) {
List<Weight> results = new ArrayList<Weight>();

String weightString = makeApiCall("weight", startDate, endDate);
JSONObject weightJson = (JSONObject) JSONValue.parse(weightString);
// iHealth returns results in pages, this just loops over them. iHealth-specific
JSONArray pages = (JSONArray) weightJson.get("pages");
if (pages != null ) {
for (int i = 0; i < pages.size(); i++) {
JSONObject page = (JSONObject) pages.get(i);
// Fetch the JSON array corresponding to data from scales. Again, the actual details are iHealth-specific
JSONArray weightData = (JSONArray) page.get("WeightDataList");
if (weightData != null) {
for (int j = 0; j < weightData.size(); j++) {
JSONObject currentWeight = (JSONObject )weightData.get(j);
if (currentWeight != null) {
// So here we're dealing with an individual reading from the iHealth scales.
// Extract each piece of data into a variable of the appropriate type and name.
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

// Now we have all the readings in appropriate variables. 
//Create a Weight object (Weight is a subclass of Metric)
//You can either pass in a (unique!) string of your choice to the constructor, or
//a 'source' string (e.g., "medisana") and the date of the measurement.
//whichever you choose, the Weight object should end up with a unique 
//identifier for this individual measurement. 


Weight weight = new Weight(dataID);


// this setId call is probably not needed, the constructor should do the same
weight.setId(dataID);


//Weight (and its superclass Metric) has methods to add individual pieces of data.
//add them in turn.
//*IF* there are any fields which are *not* supported by Weight or Metric but which
//Medisana returns, consider adding them to Metric or Weight (whichever is most appropriate)
//as well-named fields with get/set methods. (The naming is important: the RDF is generated *from*
//the Java identifiers, so if you want to add, e.g., mood, use something like
// protected String mood;
//modify initialiseEmpty() to set it to an empty value and provide getMood/setMood methods 
//the RDF will then end up containing a triple with the property "carreSensors:hasMood".
//(Not saying you have to add any data fields at all, if you think the Medisana-specific fields aren't likely
//to be useful to CARRE, you can just ignore them.)
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


//add the Weight object you've created to the List of Metrics which this method returns.
results.add(weight);
}
}


}
}
}
return results;
}

If you adapt each of your methods to create the appropriate kind of Metric object for each type of data, and add it to the results of that method, you should find that when you run your app, you�ll get RDF printed out to System.err representing the data that you�ve received from Medisana.

Hope that helps!

Allan


 **/



import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.net.TelnetAppender;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.scribe.builder.ServiceBuilder;
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
import uk.ac.open.kmi.carre.qs.metrics.Insulin;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Sleep;
import uk.ac.open.kmi.carre.qs.metrics.SleepRecord;
import uk.ac.open.kmi.carre.qs.metrics.Temperature;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.service.iHealth.IHealthService;
import uk.ac.open.kmi.carre.qs.service.medisana.MedisanaApi;
import uk.ac.open.kmi.carre.qs.service.medisana.MedisanaService;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class MedisanaService extends Service {
	private static Logger logger = Logger.getLogger(MedisanaService.class.getName());

	public MedisanaService(String propertiesPath) {
		super(propertiesPath);
	}

	public static final String name = "Medisana API";
	public static final String machineName = "medisana";
	public static final int version = 1;

	public static final String baseURL = "https://cloud.vitadock.com";
	//public static final String baseURL = "https://vitacloud.medisanaspace.com";

	public static final String requestTokenURL = "https://cloud.vitadock.com/auth/unauthorizedaccesses";
	public static final String authURL = "https://cloud.vitadock.com/desiredaccessrights/request";
	public static final String accessTokenURL = "https://cloud.vitadock.com/auth/accesses/verify";

	//public static final String requestTokenURL = "https://vitacloud.medisanaspace.com/auth/unauthorizedaccesses";
	//public static final String authURL = "https://vitacloud.medisanaspace.com/desiredaccessrights/request";
	//public static final String accessTokenURL = "https://vitacloud.medisanaspace.com/auth/accesses/verify";

	public static final String REQUEST_TOKEN_SESSION = "MEDISANA_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "MEDISANA_ACCESS_TOKEN_SESSION";

	private OAuthService service = null;
	private String userId = "";
	private RDFAbleToken accessToken = null;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + "medisana";

	public static final String PROVENANCE = RDF_SERVICE_NAME;

	@Override
	public String getRequestContents(HttpServletRequest request) {

		String json = "";
		String headerContent = request.getHeader("authorization");
		String dateString = request.getParameter("date");
		json += headerContent;
		json += ",oauth_date="  + dateString; 
		return json;
	}
	
	@Override
	public void handleNotification(String requestContent) {
		String headerContent =  requestContent;
		String dateString = "";
		if (headerContent != null && !headerContent.equals("")) {
			boolean consumerKeyMatches = false;
			String userToken = "";
			String[] fields = headerContent.split(",oauth_");
			for (String field : fields) {
				if (field.contains("consumer_key=")) {
					String consumerKey = field.split("=")[1];
					if (consumerKey != null) {
						if (consumerKey.equals(oauth_token) 
								|| consumerKey.equals("\"" + oauth_token + "\"")) {
							consumerKeyMatches = true;
							logger.info("Matched consumer key!");
						} else {
							logger.info("Consumer keys didn't match: " +
									consumerKey + ", " + oauth_token);
						}
					}
				} else if (field.contains("token=")) {
					String token = field.split("=")[1];
					if (token != null) {
						userToken = token;
						if (userToken.contains("\"")) {
							userToken = userToken.replaceAll("\"", "");
						}
						logger.info("Found token " + token);
					} else {
						logger.info("Token is null in " + field);
					}
				} else if (field.contains("date=")) {
					dateString = field.split("=")[1];
					if (dateString == null) {
						dateString = "";
					}
				}
			}
			if (!consumerKeyMatches || userToken == null || userToken.equals("")) {
				return;
			}

			if (dateString != null && !dateString.equals("") && !dateString.equals("null")) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				try {
					Date date = formatter.parse(dateString);
					handleNotification(userToken, date);
				} catch (ParseException e) {
					logger.info(e.getMessage());
				}
			} else {
				handleNotification(userToken, null);
			}


		}
	}


	public void handleNotification(String userToken, Date dateRequested) {
		RDFAbleToken token = getTokenForUser(userToken);

		if (token != null ) {
			RDFAbleToken oldAccessToken = accessToken;
			accessToken = token;
			if (service == null ) {
				service = new ServiceBuilder()
				.provider(MedisanaApi.class)
				.apiKey(oauth_token)
				.apiSecret(oauth_secret)
				.signatureType(SignatureType.Header)
				.callback("none")
				.debug()
				.build();
			}
			Date date;

			if (dateRequested == null) {
				Calendar cal = Calendar.getInstance();
				date = cal.getTime();
			} else {
				date = dateRequested;
			}

			List<Metric> newMetrics = new ArrayList<Metric>();

			newMetrics.addAll(getCardiodock(date));
			newMetrics.addAll(getGlucodockGlucose(date));
			newMetrics.addAll(getGlucodockInsulin(date));
			newMetrics.addAll(getGlucodockMeal(date));
			newMetrics.addAll(getTargetscale(date));
			newMetrics.addAll(getThermodock(date));	
			newMetrics.addAll(getTrackerstat(date));

			String userId = accessToken.getUser();

			String rdf = "";
			for (Metric metric : newMetrics) {
				rdf += metric.getMeasuredByRDF(PROVENANCE, userId);
				rdf += metric.toRDFString(userId);
			}
			accessToken = oldAccessToken;

			logger.info(rdf);
			if (!rdf.equals("")) {
				CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
				boolean success = true;
				List<String> triples = Service.chunkRDF(rdf);
				for (String tripleSet : triples) {
					success &= connector.insertTriples(userId, tripleSet);
				}
				if (!success) {
					logger.info("Failed to insert triples.");
				}
			}
		} else {
			logger.info("Token was null!");
		}
	}


	public RDFAbleToken getTokenForUser(String userToken) {
		String userId = "";
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?oauth_secret ?user_graph WHERE { GRAPH ?user_graph {\n" + 
				" ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n" +
				" ?connection <" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> \"" + userToken + "\"" + CARREVocabulary.STRING_TYPE +".\n" +
				"?connection <" + CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret .\n" +
				"}\n" +
				"}";
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal secretLiteral = solution.getLiteral("oauth_secret");
			Resource userResource = solution.getResource("user_graph");

			String user = userResource.getURI();
			userId = user.substring(user.lastIndexOf("/") + 1);
			if (secretLiteral == null) {
				logger.info("Secret literal is null!");
				return null;
			}
			String oauth_secret = secretLiteral.getString();
			logger.info("token literal is " + userToken + 
					", secret literal is " + oauth_secret);
			RDFAbleToken token = new RDFAbleToken(userToken, oauth_secret);
			token.setUser(userId);
			return token;
		}
		return null;
	}


	@Override
	public String createService(HttpServletRequest request,	HttpServletResponse response, 
			String callback) {

		if (service == null ) {
			service = new ServiceBuilder()
			.provider(MedisanaApi.class)
			.apiKey(oauth_token)
			.apiSecret(oauth_secret)
			.signatureType(SignatureType.Header)
			.callback("none") // modified Scribe OAuth10aServiceImpl (no callback in Medisanas signature string)
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
			logger.info("OauthToken: " + requestToken.getToken() 
					+ ", " + "OauthTokenSecret: " + requestToken.getSecret());
			request.getSession().setAttribute(machineName + "reqtoken", requestToken.getToken());
			request.getSession().setAttribute(machineName + "reqsec", requestToken.getSecret());

			String authURL= service.getAuthorizationUrl(requestToken);

			logger.info(authURL);
			return "";//response.encodeRedirectURL(authURL);
		} else {
			logger.info("oauthTokenSecret: " + oauthTokenSecret);
			Verifier verifier = new Verifier(oauthTokenSecret);
			Token requestToken = new Token((String) request.getSession().getAttribute(machineName + "reqtoken"),
					(String) request.getSession().getAttribute(machineName + "reqsec"));


			Token tmpAccessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

			accessToken = new RDFAbleToken(tmpAccessToken.getToken(), tmpAccessToken.getSecret());

			logger.info("accessToken: " + accessToken.getToken());
			logger.info("accessTokenSecret: " + accessToken.getSecret());

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014, 06, 10);
			Date startDate = cal.getTime();

			/*List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				logger.info(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.info(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}*/
			return "";
		}	
	}


	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {

		List<Metric> allMetrics = new ArrayList<Metric>();

		allMetrics.addAll(getCardiodock(startDate));
		allMetrics.addAll(getGlucodockGlucose(startDate));
		allMetrics.addAll(getGlucodockInsulin(startDate));
		allMetrics.addAll(getGlucodockMeal(startDate));
		allMetrics.addAll(getTargetscale(startDate));
		allMetrics.addAll(getThermodock(startDate));	
		allMetrics.addAll(getTrackerstat(startDate));	

		return allMetrics;
	}


	public List<Metric> getCardiodock(Date startDate) {

		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("cardiodocks", startDate);	

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);


				String note =  (String) jsonObject.get("note");
				String moduleSerialId = (String) jsonObject.get("moduleSerialId");
				Number mood = (Number) jsonObject.get("mood");
				Number arrhythmic = (Number) jsonObject.get("arrhythmic");
				Boolean active = (Boolean) jsonObject.get("active");                        
				Number diastoleTargetMax = (Number) jsonObject.get("diastoleTargetMax");    
				Number updatedDate = (Number) jsonObject.get("updatedDate");  
				Number type= (Number) jsonObject.get("type"); 
				Number systoleTargetMin = (Number) jsonObject.get("systoleTargetMin");                     
				Number version = (Number) jsonObject.get("version");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Number pulseTargetMin = (Number) jsonObject.get("pulseTargetMin");
				Number systole = (Number) jsonObject.get("systole");
				Number diastoleTargetMin = (Number) jsonObject.get("diastoleTargetMin");
				Number pulse = (Number) jsonObject.get("pulse");
				Number activityStatus = (Number) jsonObject.get("activityStatus");
				String id = (String) jsonObject.get("id");
				Number pulseTargetMax = (Number) jsonObject.get("pulseTargetMax");
				Number diastole = (Number) jsonObject.get("diastole");
				Number systoleTargetMax = (Number) jsonObject.get("systoleTargetMax");

				BloodPressure bloodpressure = new BloodPressure(id);
				Pulse pulsem = new Pulse(id);
				Date date = new Date(measurementDate.longValue());

				bloodpressure.setId(id);
				bloodpressure.setSystolicBloodPressure(systole.longValue());
				bloodpressure.setDiastolicBloodPressure(diastole.longValue());

				String arrhythmicStatus = "";
				switch (arrhythmic.intValue()) {
				case 0:
					arrhythmicStatus = CARREVocabulary.NO_ARRHYTHMIA;
					break;
				case 1:
					arrhythmicStatus = CARREVocabulary.ARRHYTHMIA;
					break;
				default: 
					arrhythmicStatus = "";
				}

				bloodpressure.setArrhythmiaType(arrhythmicStatus);

				if (note != null) {
					bloodpressure.setNote(note);
				}
				bloodpressure.setDate(date);


				pulsem.setId(id);
				pulsem.setPulse(pulse.longValue());
				pulsem.setDate(date);

				results.add(bloodpressure);  
				results.add(pulsem);               
			}           

		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}


	public List<Metric> getGlucodockGlucose(Date date) {

		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("glucodockglucoses", date);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				Number glucoseValue = (Number) jsonObject.get("bloodGlucose");//"blood_glucose");

				String note = (String) jsonObject.get("note");
				String id = (String) jsonObject.get("id");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Date measuredDate = new Date(measurementDate.longValue());
				Glucose glucose = new Glucose(id);
				glucose.setDate(measuredDate);
				glucose.setGlucose(glucoseValue.doubleValue());
				if (note != null && !note.equals("")) {
					glucose.setNote(note);
				}
				results.add(glucose);
			}       
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;	
	}


	List<Metric> getGlucodockInsulin(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("glucodockinsulins", date);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String note = (String) jsonObject.get("note");
				String id = (String) jsonObject.get("id");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Date measuredDate = new Date(measurementDate.longValue());
				Number insulinValue = (Number) jsonObject.get("insulin");
				Number insulinType = (Number) jsonObject.get("insulin_type_index");
				String insulinTypeName = "";

				switch (insulinType.intValue()) {
				case 0:
					insulinTypeName = (String) jsonObject.get("insulin_type_name");
					break;
				case 1:
					insulinTypeName = (String) jsonObject.get("insulin_type_name_2");
					break;
				case 2:
					insulinTypeName = (String) jsonObject.get("insulin_type_name_3");
					break;
				default:
					insulinTypeName = "";
				}

				Insulin insulin = new Insulin(id);
				insulin.setDate(measuredDate);
				insulin.setInsulin(insulinValue.floatValue());
				if (!insulinTypeName.equals("")) {
					insulin.setInsulinType(insulinTypeName);
				}

				if (note != null && !note.equals("")) {
					insulin.setNote(note);
				}
				results.add(insulin);
			}       
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}


	public List<Metric> getGlucodockMeal(Date date) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("glucodockmeals", date);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String note = (String) jsonObject.get("note");
				String id = (String) jsonObject.get("id");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Date measuredDate = new Date(measurementDate.longValue());
				Number carbs = (Number) jsonObject.get("carbohydrates");
				Food food = new Food(id);
				food.setDate(measuredDate);
				food.setCarbs(carbs.doubleValue());

				if (note != null && !note.equals("")) {
					food.setNote(note);
				}

				results.add(food);
			}       
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}



	public List<Metric> getTargetscale(Date startDate) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("targetscales", startDate);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);                    

				String note = (String) jsonObject.get("note");
				String id = (String) jsonObject.get("id");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Date date = new Date(measurementDate.longValue());

				String moduleSerialId = (String) jsonObject.get("moduleSerialId");
				Number mood = (Number) jsonObject.get("mood");
				Boolean active = (Boolean) jsonObject.get("active");
				Number targetWeight = (Number) jsonObject.get("targetWeight");
				Number updatedDate = (Number) jsonObject.get("updatedDate");            
				Number version = (Number) jsonObject.get("version");
				Number kcal = (Number) jsonObject.get("kcal");
				Number bodyFat = (Number) jsonObject.get("bodyFat");
				Number boneMass = (Number) jsonObject.get("boneMass");
				Number bodyWater = (Number) jsonObject.get("bodyWater");
				Number bodyWeight = (Number) jsonObject.get("bodyWeight");
				Number muscleMass = (Number) jsonObject.get("muscleMass");
				Number athletic = (Number) jsonObject.get("athletic");
				Number bmi = (Number) jsonObject.get("bmi");
				Number mealStatus = (Number) jsonObject.get("mealStatus");

				Weight weight = new Weight(id);

				weight.setId(id);
				weight.setBmi(bmi.floatValue());
				if (boneMass != null) {
					weight.setBoneMass(boneMass.floatValue());
				}
				if (bodyFat != null) {
					weight.setBodyFat(bodyFat.floatValue());
				}
				if (muscleMass != null) {
					weight.setMuscleMass(muscleMass.floatValue());
				}
				if (bodyWater != null) {
					weight.setWaterMass(bodyWater.floatValue());
				}
				if (bodyWeight != null) {
					weight.setWeight(bodyWeight.floatValue());
				}
				if (note != null) {
					weight.setNote(note);
				}
				weight.setDate(date);

				results.add(weight);          

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}


	public List<Metric> getThermodock(Date startDate) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("thermodocks", startDate);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String note = (String) jsonObject.get("note");
				String id = (String) jsonObject.get("id");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Date measuredDate = new Date(measurementDate.longValue());
				Temperature temperature = new Temperature(id);
				temperature.setDate(measuredDate);			

				if (note != null && !note.equals("")) {
					temperature.setNote(note);
				}

				results.add(temperature);
			}       
		} catch (Exception e) {
			e.printStackTrace();
		}


		return results;
	}


	public List<Metric> getTrackerstat(Date startDate) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = makeApiCall("tracker/stats", startDate);

		JSONParser parser = new JSONParser();
		try {
			JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
			int size = jsonArray.size();

			for (int i = 0; i < size; i++) {

				JSONObject jsonObject = (JSONObject) jsonArray.get(i);            

				Number calories = (Number) jsonObject.get("calories");
				Number steps = (Number) jsonObject.get("steps");
				Number runningSteps = (Number) jsonObject.get("runningSteps");
				Number distance = (Number) jsonObject.get("distance");
				Number durationActivity = (Number) jsonObject.get("durationActivity");                          
				Number durationSleep = (Number) jsonObject.get("durationSleep");
				Number sleepQuality = (Number) jsonObject.get("sleepQuality");   
				Number badSleepQualityDuration = (Number) jsonObject.get("badSleepQualityDuration");     
				Number mediumSleepQualityDuration = (Number) jsonObject.get("mediumSleepQualityDuration");                                              
				Number goodSleepQualityDuration = (Number) jsonObject.get("goodSleepQualityDuration");                          
				Number excellentSleepQualityDuration = (Number) jsonObject.get("excellentSleepQualityDuration");                        
				Number moduleSerialId = (Number) jsonObject.get("moduleSerialId");
				String id = (String) jsonObject.get("id");
				Boolean active = (Boolean) jsonObject.get("active");
				Number version = (Number) jsonObject.get("version");
				Number measurementDate = (Number) jsonObject.get("measurementDate");
				Number updatedDate = (Number) jsonObject.get("updatedDate");

				Activity activity = new Activity(id);
				Sleep sleep = new Sleep(id);
				//SleepRecord sleeprecord = new SleepRecord(id);
				Date date = new Date(measurementDate.longValue());

				activity.setId(id);
				activity.setSteps(steps.intValue());
				activity.setActivityCalories(calories.floatValue());
				activity.setDistance(distance.floatValue());
				activity.setDate(date);

				sleep.setId(id);
				sleep.setDate(date);
				sleep.setAsleepDuration(durationSleep.longValue());
				sleep.setDeepSleepDuration(excellentSleepQualityDuration.longValue() + goodSleepQualityDuration.longValue());
				sleep.setLightSleepDuration(mediumSleepQualityDuration.longValue());
				sleep.setRestlessDuration(badSleepQualityDuration.longValue());
				sleep.setSleepEfficiency(sleepQuality.doubleValue());

				//sleeprecord.setId(id);

				results.add(activity); 
				results.add(sleep); 
				//				results.add(sleeprecord); 

			}       
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}



	public String makeApiCall(String keyword, Date date) {

		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd");

		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL 
				+ "/data/" + keyword + "/sync?start=1&max=10&date_since=" + dateString);

		logger.info(serviceRequest.getCompleteUrl());
		service.signRequest(accessToken, serviceRequest); 
		logger.info(serviceRequest.getCompleteUrl());

		Response requestResponse = serviceRequest.send();
		logger.info(requestResponse.getBody());
		return requestResponse.getBody();
	}


	@Override
	public String getProvenance() {
		return PROVENANCE;
	}



}
