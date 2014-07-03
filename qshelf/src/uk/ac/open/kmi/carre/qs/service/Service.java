package uk.ac.open.kmi.carre.qs.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.open.kmi.carre.metrics.Activity;
import uk.ac.open.kmi.carre.metrics.BloodPressure;
import uk.ac.open.kmi.carre.metrics.Height;
import uk.ac.open.kmi.carre.metrics.Metric;
import uk.ac.open.kmi.carre.metrics.O2Saturation;
import uk.ac.open.kmi.carre.metrics.Pulse;
import uk.ac.open.kmi.carre.metrics.Sleep;
import uk.ac.open.kmi.carre.metrics.Weight;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;

public abstract class Service {

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

	public Service(String propertiesPath) {
		oauth_token = getOAuthToken(propertiesPath, machineName);
		oauth_secret = getOAuthSecret(propertiesPath, machineName);
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

}
