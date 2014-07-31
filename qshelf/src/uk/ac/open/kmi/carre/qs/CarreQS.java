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
		Service currentService = null;//DefaultService.getDefaultService(
		//getServletContext().getRealPath("/WEB-INF/config.properties"));
		if (pathInfo != null && !(pathInfo.equals(""))) {
			pathInfo = pathInfo.replaceAll("/", "");
			currentService = DefaultService.getServiceWithMachineName(pathInfo, 
					getServletContext().getRealPath("/WEB-INF/config.properties"));
		} else {
			pathInfo = "";
			currentService = DefaultService.getDefaultService(
					getServletContext().getRealPath("/WEB-INF/config.properties"));

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



}
