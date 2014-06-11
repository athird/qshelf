package uk.ac.open.kmi.carre.qs.service.withings;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

import uk.ac.open.kmi.carre.metrics.Activity;
import uk.ac.open.kmi.carre.metrics.Metric;
import uk.ac.open.kmi.carre.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.Service;

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
	
	private OAuthService service = null;
	private String userId = "";
	private Token accessToken = null;

	public WithingsService(String propertiesPath) {
		super(propertiesPath);
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
			//Token requestToken = new Token(oauthToken, oauthTokenSecret);
			Token requestToken = new Token((String) request.getSession().getAttribute(machineName + "reqtoken"),
					(String) request.getSession().getAttribute(machineName + "reqsec"));

			//AbstractMap.SimpleEntry<String,String> useridParameter = new AbstractMap.SimpleEntry<String, String>("userid", userid);
			accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

			//Token accessToken = service.getAccessToken(requestToken, verifier);

			System.err.println("accessToken: " + accessToken.getToken());
			System.err.println("accessTokenSecret: " + accessToken.getSecret());
			Calendar cal = Calendar.getInstance(); 
			cal.set(2014, 04, 28);
			Date date = cal.getTime();
			Activity activity = getActivity(date);
			System.err.println(activity.toRDFString());
			
			Weight weight = getWeight(date);
			System.err.println(weight.toRDFString());
			/*String reqURLS = baseURL + "/account?action=getuserslist";//&userid=" + userid;
			System.err.println(reqURLS);

			OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL + "/v2/measure");
			//serviceRequest.addQuerystringParameter("action", "getmeas");
			serviceRequest.addQuerystringParameter("action", "getactivity");
			serviceRequest.addQuerystringParameter("userid", userId);
			serviceRequest.addQuerystringParameter("startdateymd", "2014-05-01");
			serviceRequest.addQuerystringParameter("enddateymd", "2014-06-05");
			//serviceRequest.addQuerystringParameter("devtype", "4");
			service.signRequest(accessToken, serviceRequest); 
			System.err.println(serviceRequest.getUrl());
			System.err.println(serviceRequest.getCompleteUrl());
			//{"status":0,"body":{"updatetime":1401864300,"measuregrps":[{"grpid":215026954,"attrib":-1,"date":1401259981,"category":1,"comment":"Pre Soylent","measures":[{"value":83,"type":9,"unit":0},{"value":126,"type":10,"unit":0},{"value":86,"type":11,"unit":0}]},{"grpid":214458840,"attrib":-1,"date":1401114679,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":127,"type":10,"unit":0},{"value":83,"type":11,"unit":0}]},{"grpid":214420307,"attrib":-1,"date":1401105701,"category":1,"measures":[{"value":89,"type":9,"unit":0},{"value":134,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":210803657,"attrib":-1,"date":1400142907,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":112,"type":10,"unit":0},{"value":93,"type":11,"unit":0}]},{"grpid":210792988,"attrib":-1,"date":1400139180,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":129,"type":10,"unit":0},{"value":90,"type":11,"unit":0}]},{"grpid":210584574,"attrib":-1,"date":1400084629,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":127,"type":10,"unit":0},{"value":91,"type":11,"unit":0}]},{"grpid":210553622,"attrib":-1,"date":1400068111,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":130,"type":10,"unit":0},{"value":79,"type":11,"unit":0}]},{"grpid":210511949,"attrib":2,"date":1400068045,"category":1,"measures":[{"value":178,"type":4,"unit":-2}]}]}}
			//{"status":0,"body":{"updatetime":1401864426,"measuregrps":[{"grpid":216964980,"attrib":2,"date":1401774912,"category":1,"measures":[{"value":64300,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":216953280,"attrib":0,"date":1401744921,"category":1,"measures":[{"value":62,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":216006429,"attrib":2,"date":1401523301,"category":1,"measures":[{"value":64000,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":216094211,"attrib":0,"date":1401522930,"category":1,"measures":[{"value":66,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212924304,"attrib":0,"date":1400698827,"category":1,"measures":[{"value":59,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212473306,"attrib":0,"date":1400576669,"category":1,"measures":[{"value":82,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212473305,"attrib":0,"date":1400576641,"category":1,"measures":[{"value":82,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212429166,"attrib":2,"date":1400576575,"category":1,"measures":[{"value":64000,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":212429086,"attrib":2,"date":1400576550,"category":1,"measures":[{"value":63500,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":212200090,"attrib":0,"date":1400504441,"category":1,"measures":[{"value":77,"type":11,"unit":0},{"value":96,"type":54,"unit":0}]},{"grpid":212200089,"attrib":0,"date":1400504407,"category":1,"measures":[{"value":85,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212143598,"attrib":0,"date":1400503008,"category":1,"measures":[{"value":77,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212095166,"attrib":2,"date":1400493947,"category":1,"measures":[{"value":1770,"type":4,"unit":-3}]},{"grpid":212095165,"attrib":2,"date":1400493947,"category":1,"measures":[{"value":63500,"type":1,"unit":-3}]}]}}
			//{"status":0,"body":{"activities":[{"date":"2014-06-02","steps":3496,"distance":2594.65,"calories":127.44,"elevation":48.25,"soft":1980,"moderate":600,"intense":60,"timezone":"Europe\/London"},{"date":"2014-05-22","steps":3771,"distance":2674.35,"calories":143.67,"elevation":71.25,"soft":2880,"moderate":300,"intense":60,"timezone":"Europe\/London"},{"date":"2014-05-23","steps":7052,"distance":5330.57,"calories":269.07,"elevation":124.22,"soft":2700,"moderate":2280,"intense":300,"timezone":"Europe\/Athens"},{"date":"2014-06-03","steps":3781,"distance":2830.69,"calories":151.35,"elevation":80.92,"soft":1740,"moderate":1020,"intense":120,"timezone":"Europe\/London"},{"date":"2014-05-24","steps":3759,"distance":2698.03,"calories":128.19,"elevation":39.54,"soft":1620,"moderate":1020,"intense":60,"timezone":"Europe\/Athens"},{"date":"2014-05-25","steps":4796,"distance":3538.5,"calories":155.3,"elevation":29.38,"soft":1620,"moderate":1680,"intense":0,"timezone":"Europe\/Athens"},{"date":"2014-06-04","steps":6316,"distance":4660.1,"calories":256.28,"elevation":140.75,"soft":2760,"moderate":1440,"intense":240,"timezone":"Europe\/London"},{"date":"2014-05-26","steps":1012,"distance":671.08,"calories":38.73,"elevation":23.16,"soft":660,"moderate":180,"intense":0,"timezone":"Europe\/Athens"},{"date":"2014-05-27","steps":8862,"distance":6016.39,"calories":294.34,"elevation":84.84,"soft":5280,"moderate":1200,"intense":180,"timezone":"Europe\/Athens"},{"date":"2014-06-05","steps":4588,"distance":3401.49,"calories":204.61,"elevation":140.35,"soft":1980,"moderate":1020,"intense":360,"timezone":"Europe\/London"},{"date":"2014-05-19","steps":2914,"distance":2225.1,"calories":98.04,"elevation":21.3,"soft":1200,"moderate":900,"intense":0,"timezone":"Europe\/London"},{"date":"2014-05-20","steps":7508,"distance":5785.43,"calories":266.86,"elevation":89.3,"soft":2700,"moderate":2520,"intense":180,"timezone":"Europe\/London"},{"date":"2014-05-21","steps":5464,"distance":4100.22,"calories":194.44,"elevation":66.51,"soft":2280,"moderate":1260,"intense":180,"timezone":"Europe\/London"},{"date":"2014-05-28","steps":8108,"distance":5982.239,"calories":294.31,"elevation":113.79,"soft":2760,"moderate":2460,"intense":180,"timezone":"Europe\/Athens"},{"date":"2014-05-29","steps":6233,"distance":4493.351,"calories":219.56,"elevation":79.21,"soft":1980,"moderate":2160,"intense":60,"timezone":"Europe\/Athens"},{"date":"2014-05-30","steps":3841,"distance":2830.831,"calories":158.21,"elevation":93.75,"soft":1680,"moderate":840,"intense":240,"timezone":"Europe\/London"},{"date":"2014-05-31","steps":2610,"distance":1873.11,"calories":94.23,"elevation":34.07,"soft":1380,"moderate":240,"intense":60,"timezone":"Europe\/London"},{"date":"2014-06-01","steps":935,"distance":656.48,"calories":43.1,"elevation":32.7,"soft":540,"moderate":120,"intense":60,"timezone":"Europe\/London"}]}}

			//Map<String,String> oauthParams = serviceRequest.getOauthParameters();
			serviceRequest.addQuerystringParameter("oauth_consumer_key", oauthParams.get("oauth_consumer_key"));
			serviceRequest.addQuerystringParameter("oauth_nonce", oauthParams.get("oauth_nonce"));
			serviceRequest.addQuerystringParameter("oauth_signature", oauthParams.get("oauth_signature"));
			serviceRequest.addQuerystringParameter("oauth_signature_method", oauthParams.get("oauth_signature_method"));
			serviceRequest.addQuerystringParameter("oauth_timestamp", oauthParams.get("oauth_timestamp"));
			serviceRequest.addQuerystringParameter("oauth_token", oauthParams.get("oauth_token"));
			serviceRequest.addQuerystringParameter("oauth_version", oauthParams.get("oauth_version"));
			 
			Response requestResponse = serviceRequest.send();
			System.out.println(requestResponse.getBody());*/
			return "";
		}
	}

	public Weight getWeight(Date date) {
		Weight weight = new Weight(machineName, date);
		Map<String,String> parameters = getDefaultParams("getmeas");
		/*long dateEpoch = date.getTime();
		parameters.put("startdate", "" + dateEpoch);
		parameters.put("enddate", "" + dateEpoch);*/
		parameters.put("devtype", "" + SCALE_DEVTYPE);
		parameters.put("category", "" + ACTUAL_MEASUREMENT);
		parameters.put("limit", "" + 1);
		String weightRecord = makeApiCall("/measure", parameters);
		weight = parseWeight(weight, weightRecord);
		return weight;
	}
	
	public Weight parseWeight(Weight weight, String weightString) {
		JSONObject json = getBodyJson(weightString);
		if (json == null) {
			return null;
		}
		return parseSingleWeight(weight, json);
	}
	
	public Weight parseSingleWeight(Weight weight, JSONObject weightJson) {
		JSONArray measureGroups = (JSONArray) weightJson.get("measuregrps");
		JSONObject measureGroup = (JSONObject) measureGroups.get(0);
		weight.setId(machineName + (Long) measureGroup.get("grpid"));
		weight.setProvenance(((Long) measureGroup.get("attrib")).intValue());
		long dateEpoch = (Long) measureGroup.get("date");
		Date date = new Date(dateEpoch);
		weight.setDate(date);
		long category = (Long) measureGroup.get("category");
		if (category == ACTUAL_MEASUREMENT) {
			weight.setActuality(Metric.ACTUALITY_ACTUAL);
		} else if (category == GOAL_MEASUREMENT) {
			weight.setActuality(Metric.ACTUALITY_GOAL);
		} else {
			weight.setActuality(Metric.ACTUALITY_ACTUAL);
		}
		JSONArray measures = (JSONArray) measureGroup.get("measures");
		for (int i = 0; i < measures.size(); i++) {
			JSONObject measure = (JSONObject) measures.get(i);
			weight = parseSingleWeightMeasure(weight, measure);
		}
		return weight;
	}
	
	public Weight parseSingleWeightMeasure(Weight weight, JSONObject measureJson) {
		int type = ((Long) measureJson.get("type")).intValue();
		long value = (Long) measureJson.get("value");
		int unit = ((Long) measureJson.get("unit")).intValue();
		double actualValue = value * Math.pow(10, unit);
		
		switch (type) {
		case WEIGHT_MEASUREMENT:
			weight.setWeight(actualValue);
			break;
		case LEAN_MASS:
			weight.setLeanMass(actualValue);
			break;
		case FAT_PERCENTAGE:
			weight.setBodyFat(actualValue);
			break;
		case FAT_MASS:
			weight.setFatMass(actualValue);
			break;
		default:
			break;
		}
		
		return weight;
	}
	
	public Activity getActivity(Date date) {
		Activity activity = new Activity(machineName, date);
		Map<String,String> parameters = getDefaultParams("getactivity");
		String dateString = DateFormatUtils.format(date, "yyyy-MM-dd");
		parameters.put("date", dateString);
		System.err.println(dateString);
		String activityRecord = makeApiCall("/v2/measure", parameters);
		System.err.println(activityRecord);
		return parseActivity(activity, activityRecord);
	}
	
	public Activity parseActivity(Activity activity, String activityString) {
		JSONObject json = getBodyJson(activityString);
		if (json == null) {
			return null;
		}
		return parseSingleActivity(activity, json);
	}
	
	public Activity parseSingleActivity(Activity activity, JSONObject activityJson) {
		activity.setSteps(((Long) activityJson.get("steps")).intValue());
		activity.setDistance(((Double) activityJson.get("distance")).floatValue());
		activity.setCalories(((Double) activityJson.get("calories")).floatValue());
		activity.setElevation(((Double) activityJson.get("elevation")).floatValue());
		activity.setLightActivityDuration(((Long) activityJson.get("soft")).intValue());
		activity.setModerateActivityDuration(((Long) activityJson.get("moderate")).intValue());
		activity.setIntenseActivityDuration(((Long) activityJson.get("intense")).intValue());
		activity.setTimezone((String) activityJson.get("timezone"));
		return activity;
	}

	public String makeApiCall(String apiMethod, Map<String,String> parameters) {
		OAuthRequest serviceRequest = new OAuthRequest(HTTP_METHOD, 
				baseURL + apiMethod);

		for (String paramName : parameters.keySet()) {
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
}
