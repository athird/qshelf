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
import uk.ac.open.kmi.carre.qs.service.fitbit.FitbitService;
import uk.ac.open.kmi.carre.qs.service.iHealth.IHealthService;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;



/**
 * Servlet implementation class OseVPH
 */

public class QShelfSubscriptions extends HttpServlet {
	private static final long serialVersionUID = 1L;



	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public QShelfSubscriptions() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		Service currentService = null;
		System.err.println("Received post request! pathInfo " + pathInfo);
		if (pathInfo.toLowerCase().contains("iHealth".toLowerCase())) {
			response.setStatus(HttpServletResponse.SC_OK);
			System.err.println("Sent OK status.");
		} else {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			System.err.println("Sent NO_CONTENT status.");
		}

		if (pathInfo != null && !(pathInfo.equals(""))) {
			pathInfo = pathInfo.replaceAll("/", "");
			System.err.println("pathInfo isn't null: " + pathInfo);
			currentService = DefaultService.getServiceWithMachineName(pathInfo, 
					getServletContext().getRealPath("/WEB-INF/config.properties"));
			currentService.handleNotification(request, response);
		} 

	}

	private Service getServiceByName(String name, String propertiesPath) {
		if(name.equals("fitbit")) {
			System.err.println("Creating Fitbit service.");
			return new FitbitService(propertiesPath);
		} else if (name.equalsIgnoreCase("ihealth")) {
			return new IHealthService(propertiesPath);
		} else if (name.equals("withings")) {
			return new WithingsService(propertiesPath);
		}
		return null;
	}

}
