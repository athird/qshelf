package uk.ac.open.kmi.carre.qs.service.moves;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
import uk.ac.open.kmi.carre.qs.metrics.Location;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.O2Saturation;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Sleep;
import uk.ac.open.kmi.carre.qs.metrics.SleepRecord;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.service.iHealth.OAuth2AccessToken;
import uk.ac.open.kmi.carre.qs.sparql.CarrePlatformConnector;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class MovesService extends Service {

	private static Logger logger = Logger.getLogger(MovesService.class.getName());

	public static final int MAX_DAYS_RETRIEVABLE = 31;

	public MovesService(String propertiesPath) {
		super(propertiesPath);
	}

	public static final String name = "Moves API";
	public static final String machineName = "moves";
	public static final int version = 1;

	public static final String baseURL = "https://api.moves-app.com";

	public static final String requestTokenURL = baseURL + "/oauth/v1/access_token";
	public static final String authURL = baseURL + "/oauth/v1/authorize";
	public static final String accessTokenURL = requestTokenURL;
	public static final String apiBaseURL = baseURL + "/api/1.1";

	public static final String REQUEST_TOKEN_SESSION = "MOVES_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "MOVES_ACCESS_TOKEN_SESSION";

	private OAuthService service = null;
	private String userId = "";
	private OAuth2AccessToken accessToken = null;

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
	public static final int GOOGLE_MAPS_CALLS_PER_SECOND = 5;
	public static final long MILLISECONDS_BETWEEN_GOOGLE_MAPS_CALLS = MILLISECONDS_PER_SECOND / GOOGLE_MAPS_CALLS_PER_SECOND;

	public static final String RDF_SERVICE_NAME = CARREVocabulary.MANUFACTURER_RDF_PREFIX + machineName;

	public static final String PROVENANCE = RDF_SERVICE_NAME;

	@Override
	public void handleNotification(String requestContent) {

		String json = requestContent;
		JSONObject jsonObject = (JSONObject) JSONValue.parse(json);
		if (jsonObject == null) {
			logger.info("json wouldn't parse.");
			logger.info(json);
		}
		logger.info(requestContent);
		String userId = ""; 
		try {
			userId = (String) jsonObject.get("userId");
		} catch (ClassCastException e) {
			userId = "" + (Long) jsonObject.get("userId");
		}
		if (userId == null || userId.equals("")) {
			try {
				userId = (String) jsonObject.get("userid");
			} catch (ClassCastException e) {
				userId = "" + (Long) jsonObject.get("userid");
			}
		}
		
		JSONArray storylineUpdates = (JSONArray) jsonObject.get("storylineUpdates");

		for (int i = 0; i < storylineUpdates.size(); i++) {
			JSONObject notifyJson = (JSONObject) storylineUpdates.get(i);
			String reason = (String) notifyJson.get("reason");
			String endDateString = (String) notifyJson.get("endTime");
			String startDateString = (String) notifyJson.get("startTime");
			String lastSegmentStartTime = (String) notifyJson.get("lastSegmentStartTime");
			String pastDaysString = (String) notifyJson.get("pastDays");
			
			Date startDate = null;
			Date endDate = null;

			if (startDateString != null) {
				logger.info("notification start date " + startDateString);
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
					startDate = format.parse(startDateString);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (endDateString != null) {
				logger.info("notification end date " + endDateString);
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
					endDate = format.parse(endDateString);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (lastSegmentStartTime != null) {
				logger.info("notification last segment start time " + lastSegmentStartTime);
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
					startDate = format.parse(lastSegmentStartTime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Integer pastDays = null;
			if (pastDaysString != null) {
				pastDays = Integer.parseInt(pastDaysString);
			}
			handleNotification(reason, userId, startDate, endDate, pastDays);
		}

	}


	public void handleNotification(String notificationType, String ownerId, 
			Date startDate, Date endDate, Integer pastDays) {
		OAuth2AccessToken token = getTokenForUser(ownerId);
		if (token != null ) {
			logger.info("Token is not null.");
			OAuth2AccessToken oldAccessToken = accessToken;
			accessToken = token;
			if (service == null ) {
				service = new ServiceBuilder()
				.provider(MovesApi.class)
				.apiKey(oauth_token)
				.apiSecret(oauth_secret)
				.signatureType(SignatureType.Header)
				//.build();
				.callback("")
				.build();
			}
			
			List<Metric> newMetrics = new ArrayList<Metric>();

			if (pastDays == null) {
				newMetrics = getMetrics(startDate, endDate);
			} else {
				newMetrics = getActivities(startDate, endDate, pastDays);
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

			logger.info(rdf);
			rdf = "";
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

	public OAuth2AccessToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);

		String sparql = "SELECT ?connection  ?oauth_token ?expires ?refresh_token ?expires_at ?user WHERE "
				+ "{\n" + 
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
				" ?connection <" 
				+ CARREVocabulary.EXPIRES_AT_PREDICATE + "> ?expires_at.\n" +
				"}\n" +
				"}\n";
		logger.info(sparql);
		String connection = "";
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			String expires = "";
			String refresh_token = "";
			String expiresAt = "";

			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			Resource userResource = solution.getResource("user");
			if (tokenLiteral == null || userResource == null) {
				logger.info("Token or user id is null!");
				return null;
			}

			Literal expiresLiteral = solution.getLiteral("expires");
			Literal refreshLiteral = solution.getLiteral("refresh_token");
			Literal expiresAtLiteral = solution.getLiteral("expires_at");

			Resource connectionResource = solution.getResource("connection");

			String oauth_token = tokenLiteral.getString();
			String user = userResource.getURI();

			expires = expiresLiteral.getString();
			refresh_token = refreshLiteral.getString();
			expiresAt = expiresAtLiteral.getString();

			connection = connectionResource.getURI();

			logger.info("token literal is " + oauth_token + 
					", user is " + user + 
					", expires is " + expires +
					", refresh_token is " + refresh_token +
					", expires_at is " + expiresAt);
			Date expiresAtDate = null;

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			try {
				expiresAtDate = format.parse(expiresAt);
			} catch (ParseException e) {
				try {
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
								logger.info("Couldn't parse RDF date.");
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
									logger.info("Couldn't parse RDF date.");
								}
							}
						}
					}
				}
			}

			OAuth2AccessToken token = new OAuth2AccessToken(oauth_token, "");
			token.setUser(user);
			token.setExpires(expires);
			token.setRefreshToken(refresh_token);
			token.setUserId(user);
			if (expiresAtDate != null) {
				token.setExpiresAt(expiresAtDate);
			} else {
				logger.info("Expires at was null!");
			}
			token.setConnectionURI(connection);


			Date today = Calendar.getInstance().getTime();
			if (token.getExpiresAt() != null && today.after(token.getExpiresAt())) {
				OAuth2AccessToken newToken = getOAuth2AccessToken( token);
				logger.info("Retrieving new access token. ");
				logger.info("token literal is " + newToken.getToken() + 
						", user is " + user + 
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
		return null;
	}

	public String createService(HttpServletRequest request, HttpServletResponse response,
			String callback) {
		if (service == null ) {
			service = new ServiceBuilder()
			.provider(MovesApi.class)
			.apiKey(oauth_token)
			.apiSecret(oauth_secret)
			.signatureType(SignatureType.Header)
			//.build();
			.callback(callback)
			.build();
		}
		String code = request.getParameter("code");
		logger.info("code is " + code);

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
				logger.info("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					logger.info(output);
					jsonString += output;
				}
				JSONObject json = (JSONObject) JSONValue.parse(jsonString);
				String accessTokenString = (String) json.get("access_token");
				String tokenType = (String) json.get("token_type");
				int expiresIn = ((Long) json.get("expires_in")).intValue();
				String refreshToken = (String) json.get("refresh_token");
				String userId = (String) json.get("user_id");
				OAuth2AccessToken token = new OAuth2AccessToken(accessTokenString, "");
				token.setUser(userId);
				token.setExpires("" + expiresIn);
				token.setRefreshToken(refreshToken);

				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, expiresIn);
				Date expiresAt = cal.getTime();
				token.setExpiresAt(expiresAt);

				accessToken = token;
				logger.info(machineName + " access_token is " + accessTokenString);
				logger.info(machineName + " token_type is " + tokenType);

				conn.disconnect();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info(authURL);



			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,10,10);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				logger.info(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.info(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}

			makeApiCall("profile", null, null, null);
			makeApiCall("device", null, null, null);

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
		allMetrics.addAll(getActivities(startDate, endDate, null));
		return allMetrics;
	}


	public List<Metric> getActivities(Date startDate, Date endDate, Integer pastDays) {
		List<Metric> results = new ArrayList<Metric>();
		String jsonString = ""; 

		jsonString = makeApiCall("user/storyline/daily", startDate, endDate, pastDays);
		
		JSONArray storylineArray = (JSONArray) JSONValue.parse(jsonString);
		for (int i = 0; i < storylineArray.size(); i++) {
			JSONObject storyline = (JSONObject) storylineArray.get(i);

			Number caloriesIdle = (Number) storyline.get("caloriesIdle");
			String dateString = (String) storyline.get("date");

			Date date = null;
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
				date = format.parse(dateString);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONArray summary = (JSONArray) storyline.get("summary");
			if (summary != null) {
				results.addAll(parseSummary(summary, date));
			}

			JSONArray segments = (JSONArray) storyline.get("segments");
			if (segments != null) {
				results.addAll(parseSegments(segments, date));
			}

			Activity activity = new Activity(machineName, date);
			if (caloriesIdle != null) {
				activity.setActivityCalories(caloriesIdle.floatValue());
			}


			results.add(activity);
		}
		return results;
	}

	public List<Metric> parseSummary(JSONArray summary, Date date) {
		List<Metric> results = new ArrayList<Metric>();

		for (int i = 0; i < summary.size(); i++) {
			JSONObject activity = (JSONObject) summary.get(i);
			results.addAll(parseActivity(activity, date, null));
		}

		return results;
	}

	public List<Metric> parseSegments(JSONArray segments, Date date) {
		List<Metric> results = new ArrayList<Metric>();

		for (int i = 0; i < segments.size(); i++) {
			JSONObject segment = (JSONObject) segments.get(i);
			String type = (String) segment.get("type");
			String startTime = (String) segment.get("startTime");
			String endTime = (String) segment.get("endTime");


			Date startTimeDate = null;
			if (startTime != null && !startTime.equals("")) {
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");

					startTimeDate = format.parse(endTime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			Date endTimeDate = null;
			if (endTime != null && !endTime.equals("")) {
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");

					endTimeDate = format.parse(endTime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			if (type.equals("place")) {
				JSONObject place = (JSONObject) segment.get("place");
				if (place != null) {
					results.addAll(parsePlace(place, startTimeDate, endTimeDate));
				}
			} 

			JSONArray activities = (JSONArray) segment.get("activities");
			if (activities != null) {
				for (int j = 0; j < activities.size(); j++) {
					JSONObject activity = (JSONObject) activities.get(j);
					if (startTimeDate == null) {
						startTimeDate = date;
					}
					results.addAll(parseActivity(activity, startTimeDate, endTimeDate));
				}
				if (startTimeDate != null && endTimeDate != null) {
					Activity activity = new Activity(machineName, startTimeDate);
					long duration = (endTimeDate.getTime() - startTimeDate.getTime()) * MILLISECONDS_PER_SECOND;
					activity.setLoggedActivityName(machineName + " segment");
					activity.setLoggedActivityDuration(duration);
					results.add(activity);
				}
			}
		}
		return results;
	}

	public List<Metric> parseActivity(JSONObject activity, Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		String activityName = (String) activity.get("activity");
		Number duration = (Number) activity.get("duration");


		String startTime = (String) activity.get("startDate");
		String endTime = (String) activity.get("endDate");

		Date startTimeDate = null;
		if (startTime != null && !startTime.equals("")) {
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");

				startTimeDate = format.parse(endTime);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		Date endTimeDate = null;
		if (endTime != null && !endTime.equals("")) {
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");

				endTimeDate = format.parse(endTime);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		long newDuration = 0;
		if (startTimeDate != null) {
			startDate = startTimeDate;
			if (endTimeDate != null) {
				newDuration = (endTimeDate.getTime() - startTimeDate.getTime()) * MILLISECONDS_PER_SECOND;
				if (newDuration != duration.longValue()) {
					logger.info("Durations don't match: stored is " + duration.longValue() 
							+ ", computed is " + newDuration);
				}
			}
		}

		Activity activityMetric = new Activity(machineName, startDate);
		activityMetric.setLoggedActivityName(activityName);
		if (newDuration != 0) {
			activityMetric.setLoggedActivityDuration(newDuration);
		} else {
			activityMetric.setLoggedActivityDuration(duration.longValue());
		}

		Number distance = (Number) activity.get("distance");
		if (distance != null) {
			activityMetric.setLoggedActivityDistance(distance.floatValue());
		}
		Number steps = (Number) activity.get("steps");
		if (steps != null) {
			activityMetric.setSteps(steps.intValue());
		}
		Number calories = (Number) activity.get("calories");
		if (calories != null) {
			activityMetric.setActivityCalories(calories.floatValue());
		}

		Boolean manual = (Boolean) activity.get("manual");
		if (manual != null) {
			activityMetric.setProvenance(CARREVocabulary.MANUAL_PROVENANCE);
		}

		results.add(activityMetric);
		return results;
	}

	public List<Metric> parsePlace(JSONObject place, Date startDate, Date endDate) {
		List<Metric> results = new ArrayList<Metric>();
		Long id = (Long) place.get("id");
		String name = (String) place.get("name");
		String type = (String) place.get("type");
		String foursquareId = (String) place.get("foursquareId");
		JSONObject locationObject = (JSONObject) place.get("location");
		Number latitude = (Number) locationObject.get("lat");
		Number longitude = (Number) locationObject.get("lon");

		String locationNameId = machineName;
		if (id != null && !id.equals("")) {
			locationNameId += id;
		}
		Location location = new Location(locationNameId, startDate);
		location.setLatitude(latitude.floatValue());
		location.setLongitude(longitude.floatValue());

		if (type.equals("unknown")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_UNKNOWN);
		} else if (type.equals("home")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_HOME);
		} else if (type.equals("school")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_SCHOOL);
		} else if (type.equals("work")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_WORK);
		} else if (type.equals("user")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_USER);
		} else if (type.equals("foursquare")) {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_FOURSQUARE);
			if (foursquareId != null && !foursquareId.equals("")) {
				location.setFoursquareId(foursquareId);
			}
		} else {
			location.setLocationType(CARREVocabulary.PLACE_TYPE_UNKNOWN);
		}

		if (name != null && !name.equals("")) {
			location.setLocationName(name);
		}

		String locationURL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=true";
		String locationContent = "";

		try {
			TimeUnit.MILLISECONDS.sleep(2* MILLISECONDS_BETWEEN_GOOGLE_MAPS_CALLS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


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
			logger.info("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				logger.info(output);
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
								String locationType = (String) types.get(k);
								if (locationType.equals("postal_town")) {
									String longName = (String) component.get("long_name");
									postalTown = longName;
								} else if (locationType.equals("country")) {
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
						logger.info("Location details " + locationDetailsString + ".");
						location.setLocation(locationDetailsString);

					}
				}
			}
		}


		return results;
	}

	public OAuth2AccessToken getOAuth2AccessToken( OAuth2AccessToken accessToken) {
		String url = accessTokenURL; 
		String parameters = "client_id=" + oauth_token
				+ "&client_secret=" + oauth_secret
				+ "&grant_type=refresh_token"
				+ "&refresh_token=" + accessToken.getRefreshToken();
		logger.info(url);
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
			logger.info(results.toJSONString());
			token = getOAuth2AccessToken(results, accessToken.getRefreshToken());
			token.setUser(accessToken.getUser());
			token.setUserId(accessToken.getUserId());
			token.setConnectionURI(accessToken.getConnectionURI());
			if (!token.getRefreshToken().equals(accessToken.getRefreshToken())) {
				logger.info("Update refresh tokens here...");
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
			logger.info("token is null (Token, end)");
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


	public String makeApiCall(String keyword, Date startDate, Date endDate, Integer pastDays) {
		logger.info("accessToken: " + accessToken.getToken());
		//https://api.moveswearables.com/move/resource/v1/user/me/profile
		String reqURLS = baseURL + "/api/1.1/" + keyword;
		logger.info(reqURLS);

		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, reqURLS);
		/*
		 * GET /user/storyline/daily?from=<from>&to=<to>
		 * [&trackPoints=true/false]
		 * [&updatedSince=<updatedSince>]
		 * [&timeZone=<timeZone>]
		 * 
		 * GET /user/storyline/daily?pastDays=<pastDays>
		 * [&trackPoints=true/false]
		 * [&updatedSince=<updatedSince>]
		 * [&timeZone=<timeZone>]
		 */
		if (pastDays != null) {
			logger.info("pastDays=" + pastDays);
			serviceRequest.addQuerystringParameter("pastDays", pastDays.toString());
		} else if (startDate != null && endDate != null) {
			String startDateString = DateFormatUtils.format(startDate, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
			String endDateString = DateFormatUtils.format(endDate, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
			logger.info(startDateString);
			logger.info(endDateString);
			serviceRequest.addQuerystringParameter("from", startDateString);
			serviceRequest.addQuerystringParameter("to", endDateString);
		} else if (startDate != null) {
			String startDateString = DateFormatUtils.format(startDate, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
			Calendar cal = Calendar.getInstance();
			endDate = cal.getTime();
			String endDateString = DateFormatUtils.format(endDate, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
			logger.info(startDateString);
			logger.info(endDateString);
			String updatedSinceString = DateFormatUtils.format(startDate, "yyyyMMdd'T'HHmmssZ");
			serviceRequest.addQuerystringParameter("from", startDateString);
			serviceRequest.addQuerystringParameter("to", endDateString);
			serviceRequest.addQuerystringParameter("updatedSince", updatedSinceString);
		}
		serviceRequest.addHeader("Authorization", "Bearer " + accessToken.getToken());
		Response requestResponse = serviceRequest.send();
		logger.info(requestResponse.getBody());
		String results = requestResponse.getBody();
		logger.info(results);
		return results;

	}

	public String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		return format.format(date);
	}

	@Override
	public String getProvenance() {
		return PROVENANCE;
	}

	public static void main(String[] args) {
		MovesService service = new MovesService("/Users/allanthird/Work/workspaces/git/qshelf2/qshelf/WebContent/WEB-INF/config.properties");
		service.getActivities(null, null, new Integer(31));
	}
	
}
