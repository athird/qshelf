package uk.ac.open.kmi.carre.qs.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.open.kmi.carre.qs.metrics.Activity;
import uk.ac.open.kmi.carre.qs.metrics.BloodPressure;
import uk.ac.open.kmi.carre.qs.metrics.Height;
import uk.ac.open.kmi.carre.qs.metrics.Metric;
import uk.ac.open.kmi.carre.qs.metrics.O2Saturation;
import uk.ac.open.kmi.carre.qs.metrics.Pulse;
import uk.ac.open.kmi.carre.qs.metrics.Sleep;
import uk.ac.open.kmi.carre.qs.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;

public abstract class Service implements Runnable {
	private static Logger logger = Logger.getLogger(Service.class.getName());

	public static final String name = WithingsService.name;
	public static final String machineName = WithingsService.machineName;
	public static final int version = WithingsService.version;

	public String oauth_token = "";
	public String oauth_secret = "";

	public static final String baseURL = WithingsService.baseURL;

	public static final String requestTokenURL = WithingsService.requestTokenURL;
	public static final String authURL = WithingsService.authURL;
	public static final String accessTokenURL = WithingsService.accessTokenURL;
	public static final String REQUEST_TOKEN_SESSION = WithingsService.REQUEST_TOKEN_SESSION;
	public static final String ACCESS_TOKEN_SESSION = WithingsService.ACCESS_TOKEN_SESSION;

	public static final int STRING_MAX_LENGTH = 1950;

	protected HttpServletRequest threadedRequest;
	private String requestContent = "";
	
	public String propertiesLocation = "";

	public Service(String propertiesPath) {
		oauth_token = getOAuthToken(propertiesPath, machineName);
		oauth_secret = getOAuthSecret(propertiesPath, machineName);
		propertiesLocation = propertiesPath;
	}

	public abstract String createService(HttpServletRequest request, HttpServletResponse response,
			String callback);/* {
		return WithingsService.createService(request, response, callback);
	}*/

	public static String getOAuthToken(String propertiesPath, String machineName) {
		return getProperty("oauth_token", propertiesPath, machineName);
	}

	public static String getOAuthSecret(String propertiesPath, String machineName) {
		return getProperty("oauth_secret", propertiesPath, machineName);
	}


	public static String getProperty(String key, String path, String machineName) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(path));
			String token = properties.getProperty(machineName + "." + key);
			if (token != null) {
				return token;
			} else {
				return "";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public abstract List<Metric> getMetrics(Date startDate, Date endDate);

	public abstract void handleNotification(String request);

	public abstract String getProvenance();

	public static List<String> chunkRDF(String rdf) {
		List<String> results = new ArrayList<String>();
		if (rdf.length() < STRING_MAX_LENGTH) {
			results.add(rdf);
		} else {
			while (rdf.length() > STRING_MAX_LENGTH) {
				String current = rdf.substring(0, STRING_MAX_LENGTH);
				int lastTripleEndsAt = current.lastIndexOf("\n") + 1;
				current = rdf.substring(0, lastTripleEndsAt);
				rdf = rdf.substring(lastTripleEndsAt);
				results.add(current);
			}
			results.add(rdf);
		}
		return results;
	}

	public String getRequestContents(HttpServletRequest request) {
		String json = "";
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
		return json;
	}
	
	public void setThreadedRequest(HttpServletRequest request) {
		requestContent = getRequestContents(request);
	}

	public void run() {
		if (requestContent != null && !requestContent.equals("")) {
			handleNotification(requestContent);
		}
	}

}
