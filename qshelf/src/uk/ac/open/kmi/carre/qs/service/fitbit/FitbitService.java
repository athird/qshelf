package uk.ac.open.kmi.carre.qs.service.fitbit;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import uk.ac.open.kmi.carre.metrics.Height;
import uk.ac.open.kmi.carre.metrics.O2Saturation;
import uk.ac.open.kmi.carre.metrics.Pulse;
import uk.ac.open.kmi.carre.metrics.Sleep;
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
	
	public String createService(HttpServletRequest request, HttpServletResponse response,
				String callback) {
		OAuthService service = new ServiceBuilder()
		.provider(FitbitApi.class)
		.apiKey(oauth_token)
		.apiSecret(oauth_secret)
		.signatureType(SignatureType.Header)
		//.build();
		.callback(callback)
		.build();

		String oauthToken = request.getParameter("oauth_token");
		String oauthTokenSecret = request.getParameter("oauth_token_secret");
		if (oauthTokenSecret == null || oauthTokenSecret.equals("")) {
			oauthTokenSecret = request.getParameter("oauth_verifier");
		}
		
		String userid = request.getParameter("userid");
		
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
			
			Token accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

			System.err.println("accessToken: " + accessToken.getToken());
			System.err.println("accessTokenSecret: " + accessToken.getSecret());
			
			String reqURLS = baseURL + "/user/-/profile.json";
			System.err.println(reqURLS);
			
			OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, baseURL 
					//+ "/user/-/profile.json");
					+ "/user/-/activities/date/2014-06-05.json");
			service.signRequest(accessToken, serviceRequest); 
			System.err.println(serviceRequest.getUrl());
			System.err.println(serviceRequest.getCompleteUrl());
			
			Response requestResponse = serviceRequest.send();
			System.out.println(requestResponse.getBody());
			return "";
		}
	}
	
	@Override
	public Sleep getSleep(Date date) {
		return null;
	}

	@Override
	public Height getHeight(Date date) {
		return null;
	}

	@Override
	public BloodPressure getBloodPressure(Date date) {
		return null;
	}

	@Override
	public O2Saturation getO2Saturation(Date date) {
		return null;
	}

	@Override
	public Pulse getPulse(Date date) {
		return null;
	}

	@Override
	public Weight getWeight(Date date) {
		return null;
	}

	@Override
	public Activity getActivity(Date date) {
		return null;
	}
}
