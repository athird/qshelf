package uk.ac.open.kmi.carre.qs.service.fitbit;

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
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.Base64;
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
import uk.ac.open.kmi.carre.qs.service.iHealth.OAuth2AccessToken;
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
	public static final String authURL = "https://www.fitbit.com/oauth2/authorize";
	public static final String accessTokenURL = "https://api.fitbit.com/oauth2/token";

	public static final String REQUEST_TOKEN_SESSION = "FITBIT_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "FITBIT_ACCESS_TOKEN_SESSION";

	private OAuthService service = null;
	private String userId = "";
	private OAuth2AccessToken accessToken = null;

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
			logger.info(collectionType + ", " +dateString + ", " +ownerId + 
					", " +ownerType + ", " + subscriptionId);
			handleNotification(collectionType, dateString, ownerId, 
					ownerType, subscriptionId);
		}


	}

	public void handleNotification(String collectionType, String dateString,
			String ownerId, String ownerType, String carreUserId) {
		OAuth2AccessToken token = getTokenForUser(carreUserId);

		if (token != null ) {
			OAuth2AccessToken oldAccessToken = accessToken;
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
				try {
					newMetrics.addAll(getBody(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					newMetrics.addAll(getFoods(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					newMetrics.addAll(getActivities(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					newMetrics.addAll(getSleeps(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (collectionType.equals("foods")) {
				try {
					newMetrics.addAll(getFoods(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (collectionType.equals("activities")) {
				try {
					newMetrics.addAll(getActivities(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (collectionType.equals("body")) {
				try {
					newMetrics.addAll(getBody(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (collectionType.equals("sleep")) {
				try {
					newMetrics.addAll(getSleeps(date));
				} catch (Exception e) {
					e.printStackTrace();
				}
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
					logger.info("Failed to insert triples.");
				}
			}
		} else {
			logger.info("Token was null!");
		}
	}

	public OAuth2AccessToken getTokenV2ForUser(String userId) {
		RDFAbleToken original_token = getTokenForUser(userId);
		boolean upgradingTokens = false;
		OAuth2AccessToken token = new OAuth2AccessToken(original_token.getToken(), "");

		if (original_token.getRefreshToken() == null || original_token.getRefreshToken().equals("")) {
			upgradingTokens = true;
			token.setUser(original_token.getUser());
			token.setExpires("3600");
			token.setRefreshToken(original_token.getToken() + ":" + original_token.getSecret());

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 1);
			token.setExpiresAt(cal.getTime());		


		} else {
			token.setRefreshToken(original_token.getRefreshToken());
		}
		token.setConnectionURI(original_token.getConnection());
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String connection = "";

		if (!upgradingTokens) {
			String sparql = "SELECT ?connection  ?oauth_token ?expires ?refresh_token ?expires_at ?user WHERE "
					+ "{\n" + 
					"GRAPH <" + CARREVocabulary.USER_URL + userId + "> {\n ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
					+ RDF_SERVICE_NAME + ".\n " +
					" ?connection <" + 
					CARREVocabulary.USER_ID_PREDICATE + "> ?user .\n" +
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
			ResultSet results = connector.executeSPARQL(sparql);
			while (results.hasNext()) {
				String expires = "";
				String refresh_token = "";
				String expiresAt = "";

				QuerySolution solution =  results.next();
				Literal tokenLiteral = solution.getLiteral("oauth_token");
				Resource userResource = solution.getResource("user");
				if (tokenLiteral == null) {
					logger.info("Token or user id is null!");
					return null;
				}
				String user = "";
				if (userResource == null) {
					Literal userLiteral = solution.getLiteral("user");
					user = userLiteral.getString();
				}

				Literal expiresLiteral = solution.getLiteral("expires");
				Literal refreshLiteral = solution.getLiteral("refresh_token");
				Literal expiresAtLiteral = solution.getLiteral("expires_at");

				Resource connectionResource = solution.getResource("connection");

				String oauth_token = tokenLiteral.getString();
				user = userResource != null ? userResource.getURI() : user;

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
									expiresAtDate = Calendar.getInstance().getTime();
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
										expiresAtDate = Calendar.getInstance().getTime();
									}
								}
							}
						}
					}
				}
				token = new OAuth2AccessToken(oauth_token, "");
				token.setUser(user);
				token.setExpires(expires);
				token.setRefreshToken(refresh_token);
				token.setUserId(userId);
				if (expiresAtDate != null) {
					token.setExpiresAt(expiresAtDate);
				} else {
					logger.info("Expires at was null!");
				}
				token.setConnectionURI(connection);
			}
		}
		if (connection.equals("")) {
			connection = token.getConnectionURI();
		}

		Date today = Calendar.getInstance().getTime();
		OAuth2AccessToken backup = accessToken;
		accessToken = token;
		String testApi = makeApiCall("profile", today);
		accessToken = backup;
		
		if (testApi.contains("expired_token") || upgradingTokens || (token.getExpiresAt() == null ) || (token.getExpiresAt() != null && today.after(token.getExpiresAt()))) {
			OAuth2AccessToken newToken = getOAuth2AccessToken( token);
			logger.info("Retrieving new access token. ");
			logger.info("token literal is " + newToken.getToken() + 
					", user is " + newToken.getUser() + 
					", expires is " + newToken.getExpires() +
					", refresh_token is " + newToken.getRefreshToken() +
					", expires_at is " + formatDate(newToken.getExpiresAt()));
			String newDateString = newToken.getRDFDate(newToken.getExpiresAt());
			connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + ">", "\"" + newToken.getToken() + "\"" + CARREVocabulary.STRING_TYPE);
			connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.EXPIRES_AT_PREDICATE + ">",  newDateString);
			connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.REFRESH_TOKEN_PREDICATE +">", "\"" + newToken.getRefreshToken() + "\"" + CARREVocabulary.STRING_TYPE);
			connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.EXPIRES_TOKEN_PREDICATE +">", "\"" + newToken.getExpires() + "\"" + CARREVocabulary.STRING_TYPE);
			connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.USER_ID_PREDICATE +">", "\"" + newToken.getUser() + "\"" + CARREVocabulary.STRING_TYPE);


			token = newToken;
		} 

		return token;




	}
	public OAuth2AccessToken getOAuth2AccessToken( OAuth2AccessToken accessToken) {
		String url = accessTokenURL; 
		String parameters = "grant_type=refresh_token"
				+ "&refresh_token=" + accessToken.getRefreshToken();
		String authStringOriginal = oauth_token + ":" + oauth_secret;  
		String authString = org.apache.commons.codec.binary.Base64.encodeBase64String(authStringOriginal.getBytes());
		logger.info(url);
		OAuth2AccessToken token = null;
		try {
			URL getAccessToken = new URL(url);

			HttpURLConnection conn = (HttpURLConnection) getAccessToken.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic " + authString);
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
			//			token.setUserId(accessToken.getUserId());
			//token.setConnectionURI(accessToken.getConnectionURI());
			if (!token.getRefreshToken().equals(accessToken.getRefreshToken())) {
				logger.info("Update refresh tokens here...");
			}

			return token;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.info(e.getMessage());
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
		String newRefreshToken = (String) json.get("refresh_token");
		token.setRefreshToken(newRefreshToken);
		String user_id = (String) json.get("user_id");
		if (user_id != null) {
			token.setUserId(user_id);
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, expires);
		Date expiresAt = cal.getTime();
		token.setExpiresAt(expiresAt);

		return token;
	}


	public OAuth2AccessToken getTokenForUser(String userId) {
		CarrePlatformConnector connector = new CarrePlatformConnector(propertiesLocation);
		String sparql = "SELECT ?connection ?oauth_token ?oauth_secret ?refresh_token ?expires ?expires_at ?user FROM <" + CARREVocabulary.USER_URL 
				+ userId + "> WHERE { ?connection <"+ CARREVocabulary.HAS_MANUFACTURER + "> "
				+ RDF_SERVICE_NAME + ".\n " +
				"?connection <" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + "> ?oauth_token.\n" +
				"OPTIONAL { ?connection <" + 
				CARREVocabulary.ACCESS_TOKEN_SECRET_PREDICATE + "> ?oauth_secret. } \n" +
				" OPTIONAL { ?connection <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?refresh_token } \n" + 
				" OPTIONAL { ?connection <" + 
				CARREVocabulary.USER_ID_PREDICATE + "> ?user . }\n" +
				" OPTIONAL { ?connection <" 
				+ CARREVocabulary.EXPIRES_TOKEN_PREDICATE + "> ?expires. }\n" +
				" OPTIONAL { ?connection <" 
				+ CARREVocabulary.EXPIRES_AT_PREDICATE + "> ?expires_at. }\n" +
				"}";
		logger.info(sparql);
		ResultSet results = connector.executeSPARQL(sparql);
		while (results.hasNext()) {
			QuerySolution solution =  results.next();
			Literal tokenLiteral = solution.getLiteral("oauth_token");
			Literal secretLiteral = solution.getLiteral("oauth_secret");
			Literal refreshLiteral = solution.getLiteral("refresh_token");
			Literal expiresLiteral = solution.getLiteral("expires");
			Literal expiresAtLiteral = solution.getLiteral("expires_at");
			Literal userLiteral = solution.getLiteral("user");

			Resource connectionResource = solution.getResource("connection");
			if (tokenLiteral == null) {
				logger.info("Token literal is null!");
				return null;
			}
			String oauthToken = tokenLiteral.getString();
			String oauthSecret = secretLiteral == null ? "" : secretLiteral.getString();
			String refresh_token = refreshLiteral == null ? "" : refreshLiteral.getString();
			String expires = expiresLiteral == null ? "" :expiresLiteral.getString();
			String expiresAt = expiresAtLiteral == null ? "" :expiresAtLiteral.getString();
			String connection = connectionResource.getURI();
			String user = userLiteral.getString();
			logger.info("token literal is " + oauthToken + 
					", secret literal is " + oauthSecret);
			OAuth2AccessToken token = new OAuth2AccessToken(oauthToken, "");
			boolean updating = false;
			if (refresh_token.equals("")) {
				updating = true;
				OAuth2AccessToken updatingToken = new OAuth2AccessToken("", "");
				updatingToken.setRefreshToken(oauthToken + ":" + oauthSecret);
				OAuth2AccessToken updatedToken = getOAuth2AccessToken(updatingToken);
				token = updatedToken;

			} else {
				token.setRefreshToken(refresh_token);
				token.setExpires(expires);
			}
			token.setConnection(connection);
			token.setConnectionURI(connection);
			token.setUser(user);
			Date expiresAtDate = null;
			if (token.getExpiresAt() == null && !expiresAt.equals("")) {


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
			} else if (token.getExpiresAt() == null){
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, -1);//Integer.parseInt(expires));
				expiresAtDate = cal.getTime();

			} else {
				expiresAtDate = token.getExpiresAt();
			}
			token.setExpiresAt(expiresAtDate);

			if (updating) {
				connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + ">", "\"" + token.getToken() + "\"" + CARREVocabulary.STRING_TYPE);
				connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.EXPIRES_AT_PREDICATE + ">",  token.getRDFDate(token.getExpiresAt()));
				connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.REFRESH_TOKEN_PREDICATE +">", "\"" + token.getRefreshToken() + "\"" + CARREVocabulary.STRING_TYPE);
				connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.EXPIRES_TOKEN_PREDICATE +">", "\"" + token.getExpires() + "\"" + CARREVocabulary.STRING_TYPE);
				connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.USER_ID_PREDICATE +">", "\"" + token.getUser() + "\"" + CARREVocabulary.STRING_TYPE);
			} else {
				Date today = Calendar.getInstance().getTime();
				OAuth2AccessToken backup = accessToken;
				accessToken = token;
				String testApi = makeApiCall("profile", today);
				accessToken = backup; 
				
				if ((testApi.contains("expired_token")) || (token.getExpiresAt() == null ) || (token.getExpiresAt() != null && today.after(token.getExpiresAt()))) {
					OAuth2AccessToken newToken = getOAuth2AccessToken( token);
					logger.info("Retrieving new access token. ");
					logger.info("token literal is " + newToken.getToken() + 
							", user is " + newToken.getUser() + 
							", expires is " + newToken.getExpires() +
							", refresh_token is " + newToken.getRefreshToken() +
							", expires_at is " + formatDate(newToken.getExpiresAt()));
					String newDateString = newToken.getRDFDate(newToken.getExpiresAt());
					connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.ACCESS_TOKEN_PREDICATE + ">", "\"" + newToken.getToken() + "\"" + CARREVocabulary.STRING_TYPE);
					connector.updateTripleObject(userId,  connection , "<" + CARREVocabulary.EXPIRES_AT_PREDICATE + ">",  newDateString);
					connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.REFRESH_TOKEN_PREDICATE +">", "\"" + newToken.getRefreshToken() + "\"" + CARREVocabulary.STRING_TYPE);
					connector.updateTripleObject(userId, connection, "<" + CARREVocabulary.EXPIRES_TOKEN_PREDICATE +">", "\"" + newToken.getExpires() + "\"" + CARREVocabulary.STRING_TYPE);


					token = newToken;
				} 

			}

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
			logger.info("RequestToken:" + requestToken.getToken() 
					+ ", " + requestToken.getSecret());
			request.getSession().setAttribute(machineName + "reqtoken", requestToken.getToken());
			request.getSession().setAttribute(machineName + "reqsec", requestToken.getSecret());

			String authURL= service.getAuthorizationUrl(requestToken);

			logger.info(authURL);
			return response.encodeRedirectURL(authURL);
		} else {
			logger.info("oauthTokenSecret" + oauthTokenSecret);
			Verifier verifier = new Verifier(oauthTokenSecret);
			Token requestToken = new Token((String) request.getSession().getAttribute(machineName + "reqtoken"),
					(String) request.getSession().getAttribute(machineName + "reqsec"));

			Token tmpAccessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);
			accessToken = new OAuth2AccessToken(tmpAccessToken.getToken(), tmpAccessToken.getSecret());

			logger.info("accessToken: " + accessToken.getToken());
			logger.info("accessTokenSecret: " + accessToken.getSecret());

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
			//cal.set(2014, 03, 28);
			cal.clear();
			cal.set(2014,06,10);
			Date startDate = cal.getTime();

			List<Metric> metrics = getMetrics(startDate, today);

			for (Metric metric : metrics) {
				logger.info(metric.getMeasuredByRDF(PROVENANCE, CARREVocabulary.DEFAULT_USER_FOR_TESTING));
				logger.info(metric.toRDFString(CARREVocabulary.DEFAULT_USER_FOR_TESTING));
			}
			return "";
		}
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		List<Metric> allMetrics = new ArrayList<Metric>();
		//DEPRECATED by Fitbit allMetrics.addAll(getBPs(startDate));
		//DEPRECATED by Fitbit allMetrics.addAll(getPulses(startDate));
		//allMetrics.addAll(getWaters(startDate));
		//allMetrics.addAll(getFoods(startDate));
		allMetrics.addAll(getFat(startDate));
		//DEPRECATED by Fitbit allMetrics.addAll(getBody(startDate));
		allMetrics.addAll(getWeight(startDate));
		allMetrics.addAll(getSleeps(startDate));
		allMetrics.addAll(getActivities(startDate));
		//DEPRECATED by Fitbit allMetrics.addAll(getGlucoses(startDate));
		//DEPRECATED by Fitbit allMetrics.addAll(getBPs(startDate));
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

		logger.info("accessToken: " + accessToken.getToken());
		//logger.info("accessTokenSecret: " + accessToken.getSecret());

		String reqURLS = baseURL + "/user/-/profile.json";
		logger.info(reqURLS);

		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd",TimeZone.getTimeZone("UTC"));
		OAuthRequest serviceRequest;
		if (keyword.equals("profile")) {
			serviceRequest = new OAuthRequest(Verb.GET, reqURLS);
		} else {
			serviceRequest = new OAuthRequest(Verb.GET, baseURL 
					//+ "/user/-/profile.json");
					//+ "/user/-/activities/date/2014-06-05.json");
					+ "/user/-/" + keyword + "/date/" + dateString + ".json");
		}
		serviceRequest.addHeader("Authorization", "Bearer " + accessToken.getToken());
		//service.signRequest(accessToken, serviceRequest); 
		logger.info(serviceRequest.getUrl());
		logger.info(serviceRequest.getCompleteUrl());

		Response requestResponse = serviceRequest.send();
		logger.info(requestResponse.getBody());

		return requestResponse.getBody();


	}

	public String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		return format.format(date);

	}


	@Override
	public String getProvenance() {
		return PROVENANCE;
	}
}
