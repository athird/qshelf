package uk.ac.open.kmi.carre.qs.service.ihealth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import uk.ac.open.kmi.carre.metrics.Activity;
import uk.ac.open.kmi.carre.metrics.BloodPressure;
import uk.ac.open.kmi.carre.metrics.Height;
import uk.ac.open.kmi.carre.metrics.Metric;
import uk.ac.open.kmi.carre.metrics.O2Saturation;
import uk.ac.open.kmi.carre.metrics.Pulse;
import uk.ac.open.kmi.carre.metrics.Sleep;
import uk.ac.open.kmi.carre.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.Service;

public class IHealthService extends Service {
	
	public IHealthService(String propertiesPath) {
		super(propertiesPath);
	}

	public static final String name = "iHealth API";
	public static final String machineName = "ihealth";
	public static final int version = 2;

	public static final String baseURL = "http://sandboxapi.ihealthlabs.com";//"https://api.iHealthLabs.com:8443";

	public static final String requestTokenURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";//"https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";
	public static final String authURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";//"https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";
	public static final String accessTokenURL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/";//"https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";

	public static final String REQUEST_TOKEN_SESSION = "IHEALTH_REQUEST_TOKEN_SESSION";
	public static final String ACCESS_TOKEN_SESSION = "IHEALTH_ACCESS_TOKEN_SESSION";

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
					+ "client_secret=" + oauth_secret
					+ "grant_type=authorization_code"
					+ "redirect_uri=" + callback
					+ "code=" + code;

			try {
				URL getAccessToken = new URL(url);

				URLConnection conn = getAccessToken.openConnection();

				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

				JSONObject results = (JSONObject) JSONValue.parse(br);
				IHealthAccessToken accessToken = getIHealthAccessToken(results);
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

	public static IHealthAccessToken getIHealthAccessToken(JSONObject results) {
		String accessToken = (String) results.get("AccessToken");
		IHealthAccessToken token = new IHealthAccessToken(accessToken, "");
		token.setApiNames((String) results.get("APIName"));
		token.setExpires((String) results.get("Expires"));
		token.setRefreshToken((String) results.get("RefreshToken"));
		if (results.get("UserID") != null) {
			token.setUserId((String) results.get("UserID"));
		}

		return token;
	}

	@Override
	public List<Metric> getMetrics(Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
