package uk.ac.open.kmi.carre.qs;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
import uk.ac.open.kmi.carre.qs.service.medisana.MedisanaService;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;
import uk.ac.open.kmi.carre.qs.service.googlefit.GooglefitService;



/**
 * Servlet implementation class OseVPH
 */

public class QShelfSubscriptions extends HttpServlet {
	private static final int THREAD_POOL_SIZE = 10;

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(QShelfSubscriptions.class.getName());
	private static ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public QShelfSubscriptions() {
		super();
		if (executorService.isShutdown()) {
			executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		logger.info(request.getRemoteHost());
		Service currentService = null;
		logger.info("Received get request! pathInfo " + pathInfo);
		if (pathInfo.contains("fitbit") && request.getParameter("verify") != null) {
			String verificationCode = request.getParameter("verify");
			String storedCode = Service.getProperty("verification_code", 
					getServletContext().getRealPath("/WEB-INF/config.properties"), "fitbit");
			if (!storedCode.equals(verificationCode)) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				logger.info("Verification code " + verificationCode + " didn't match " 
				+ storedCode);
				return;
			}
		}
		
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		logger.info("Sent NO_CONTENT status.");
		try {
			if (pathInfo != null && !(pathInfo.equals(""))) {
				pathInfo = pathInfo.replaceAll("/", "");
				currentService = DefaultService.getServiceWithMachineName(pathInfo, 
						getServletContext().getRealPath("/WEB-INF/config.properties"));
				if (pathInfo.contains("googlefit")) {
					currentService.setThreadedRequest(request);
					executorService.submit(currentService); 
				} else {
					currentService.handleNotification(currentService.getRequestContents(request));
				}
			} 
		} catch (Exception e) {
			logger.info(e.getMessage());
			e.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		Service currentService = null;
		logger.info("Received post request! pathInfo " + pathInfo);
		if (pathInfo.toLowerCase().contains("iHealth".toLowerCase()) 
				|| pathInfo.toLowerCase().contains("misfit") 
				|| pathInfo.toLowerCase().contains("moves")) {
			response.setStatus(HttpServletResponse.SC_OK);
			logger.info("Sent OK status.");
		} else {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			logger.info("Sent NO_CONTENT status.");
		}
		try {
			if (pathInfo != null && !(pathInfo.equals(""))) {
				pathInfo = pathInfo.replaceAll("/", "");
				currentService = DefaultService.getServiceWithMachineName(pathInfo, 
						getServletContext().getRealPath("/WEB-INF/config.properties"));
				//currentService.setThreadedRequest(request);
				//executorService.submit(currentService);
				/*String json = "";
				try {
					BufferedReader reader = request.getReader();
					String line = reader.readLine();
					while (line != null) {
						json += line;
						line = reader.readLine();
					}
					logger.finer(json);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				currentService.handleNotification(json);*/
				if (pathInfo.contains("googlefit")) {
					currentService.setThreadedRequest(request);
					executorService.submit(currentService); 
				} else {
					currentService.handleNotification(currentService.getRequestContents(request));
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
		executorService.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executorService.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
