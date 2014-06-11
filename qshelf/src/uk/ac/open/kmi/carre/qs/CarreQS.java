package uk.ac.open.kmi.carre.qs;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;



import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;*/

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.WithingsApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import uk.ac.open.kmi.carre.qs.service.DefaultService;
import uk.ac.open.kmi.carre.qs.service.Service;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;



/**
 * Servlet implementation class OseVPH
 */
//@WebServlet("/shelfie/*")
public class CarreQS extends HttpServlet {
	private static final long serialVersionUID = 1L;



	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CarreQS() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		Service currentService = DefaultService.getDefaultService(
				getServletContext().getRealPath("/WEB-INF/config.properties"));
		if (pathInfo != null && !(pathInfo.equals(""))) {
			pathInfo = pathInfo.replaceAll("/", "");
			currentService = DefaultService.getServiceWithMachineName(pathInfo, 
					getServletContext().getRealPath("/WEB-INF/config.properties"));
		} else {
			pathInfo = "";
		}

		String url = currentService.createService(request, response, 
				"http://localhost:8080/quantified-shelf/shelfie/" + pathInfo);
		if (url != null && !url.equals("")) {
			response.sendRedirect(url);
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	/*OAuthService service = new ServiceBuilder()
	.provider(WithingsApi.class)
	.apiKey(currentService.oauth_token)
	.apiSecret(currentService.oauth_secret)
	.signatureType(SignatureType.QueryString)
	//.build();
	.callback("http://localhost:8080/quantified-shelf/shelfie")
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
		request.getSession().setAttribute("reqtoken", requestToken.getToken());
		request.getSession().setAttribute("reqsec", requestToken.getSecret());
		
		String authURL= service.getAuthorizationUrl(requestToken);
		
		System.err.println(authURL);
		response.sendRedirect(response.encodeRedirectURL(authURL));
	} else {
		System.err.println("oauthTokenSecret" + oauthTokenSecret);
		Verifier verifier = new Verifier(oauthTokenSecret);
		//Token requestToken = new Token(oauthToken, oauthTokenSecret);
		Token requestToken = new Token((String) request.getSession().getAttribute("reqtoken"),
				(String) request.getSession().getAttribute("reqsec"));
		
		//AbstractMap.SimpleEntry<String,String> useridParameter = new AbstractMap.SimpleEntry<String, String>("userid", userid);
		Token accessToken = service.getAccessToken(requestToken, verifier);//, useridParameter);

		//Token accessToken = service.getAccessToken(requestToken, verifier);
	
		System.err.println("accessToken: " + accessToken.getToken());
		System.err.println("accessTokenSecret: " + accessToken.getSecret());
		
		String reqURLS = currentService.baseURL + "/account?action=getuserslist";//&userid=" + userid;
		System.err.println(reqURLS);
		
		OAuthRequest serviceRequest = new OAuthRequest(Verb.GET, currentService.baseURL + "/measure");
		serviceRequest.addQuerystringParameter("action", "getmeas");
		serviceRequest.addQuerystringParameter("userid", userid);
		//serviceRequest.addQuerystringParameter("devtype", "4");
		service.signRequest(accessToken, serviceRequest); 
		System.err.println(serviceRequest.getUrl());
		System.err.println(serviceRequest.getCompleteUrl());
		//{"status":0,"body":{"updatetime":1401864300,"measuregrps":[{"grpid":215026954,"attrib":-1,"date":1401259981,"category":1,"comment":"Pre Soylent","measures":[{"value":83,"type":9,"unit":0},{"value":126,"type":10,"unit":0},{"value":86,"type":11,"unit":0}]},{"grpid":214458840,"attrib":-1,"date":1401114679,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":127,"type":10,"unit":0},{"value":83,"type":11,"unit":0}]},{"grpid":214420307,"attrib":-1,"date":1401105701,"category":1,"measures":[{"value":89,"type":9,"unit":0},{"value":134,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":210803657,"attrib":-1,"date":1400142907,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":112,"type":10,"unit":0},{"value":93,"type":11,"unit":0}]},{"grpid":210792988,"attrib":-1,"date":1400139180,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":129,"type":10,"unit":0},{"value":90,"type":11,"unit":0}]},{"grpid":210584574,"attrib":-1,"date":1400084629,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":127,"type":10,"unit":0},{"value":91,"type":11,"unit":0}]},{"grpid":210553622,"attrib":-1,"date":1400068111,"category":1,"measures":[{"value":88,"type":9,"unit":0},{"value":130,"type":10,"unit":0},{"value":79,"type":11,"unit":0}]},{"grpid":210511949,"attrib":2,"date":1400068045,"category":1,"measures":[{"value":178,"type":4,"unit":-2}]}]}}
		//{"status":0,"body":{"updatetime":1401864426,"measuregrps":[{"grpid":216964980,"attrib":2,"date":1401774912,"category":1,"measures":[{"value":64300,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":216953280,"attrib":0,"date":1401744921,"category":1,"measures":[{"value":62,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":216006429,"attrib":2,"date":1401523301,"category":1,"measures":[{"value":64000,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":216094211,"attrib":0,"date":1401522930,"category":1,"measures":[{"value":66,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212924304,"attrib":0,"date":1400698827,"category":1,"measures":[{"value":59,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212473306,"attrib":0,"date":1400576669,"category":1,"measures":[{"value":82,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212473305,"attrib":0,"date":1400576641,"category":1,"measures":[{"value":82,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212429166,"attrib":2,"date":1400576575,"category":1,"measures":[{"value":64000,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":212429086,"attrib":2,"date":1400576550,"category":1,"measures":[{"value":63500,"type":1,"unit":-3},{"value":-1000,"type":18,"unit":-3}]},{"grpid":212200090,"attrib":0,"date":1400504441,"category":1,"measures":[{"value":77,"type":11,"unit":0},{"value":96,"type":54,"unit":0}]},{"grpid":212200089,"attrib":0,"date":1400504407,"category":1,"measures":[{"value":85,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212143598,"attrib":0,"date":1400503008,"category":1,"measures":[{"value":77,"type":11,"unit":0},{"value":98,"type":54,"unit":0}]},{"grpid":212095166,"attrib":2,"date":1400493947,"category":1,"measures":[{"value":1770,"type":4,"unit":-3}]},{"grpid":212095165,"attrib":2,"date":1400493947,"category":1,"measures":[{"value":63500,"type":1,"unit":-3}]}]}}

		//Map<String,String> oauthParams = serviceRequest.getOauthParameters();
		serviceRequest.addQuerystringParameter("oauth_consumer_key", oauthParams.get("oauth_consumer_key"));
		serviceRequest.addQuerystringParameter("oauth_nonce", oauthParams.get("oauth_nonce"));
		serviceRequest.addQuerystringParameter("oauth_signature", oauthParams.get("oauth_signature"));
		serviceRequest.addQuerystringParameter("oauth_signature_method", oauthParams.get("oauth_signature_method"));
		serviceRequest.addQuerystringParameter("oauth_timestamp", oauthParams.get("oauth_timestamp"));
		serviceRequest.addQuerystringParameter("oauth_token", oauthParams.get("oauth_token"));
		serviceRequest.addQuerystringParameter("oauth_version", oauthParams.get("oauth_version"));
		
		Response requestResponse = serviceRequest.send();
		System.out.println(requestResponse.getBody());
	}*/


}
