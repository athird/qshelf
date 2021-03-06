package uk.ac.open.kmi.carre.qs.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.service.withings.WithingsService;

public class DefaultService {
	private static Logger logger = Logger.getLogger(DefaultService.class.getName());
	
	public static Service defaultService;

	public DefaultService(String propertiesPath) {
		defaultService = new WithingsService(propertiesPath);
	}
	public static Service getDefaultService(String propertiesPath) {
		if (defaultService != null) {
			return defaultService;
		} else {
			defaultService = new WithingsService(propertiesPath);
			return defaultService;
		}
	}

	public static Service getServiceWithMachineName(String machineName, String propertiesPath) {
		String toUpper = ("" + machineName.charAt(0)).toUpperCase() 
				+ machineName.substring(1);

		try {
			Class serviceClass = Class.forName("uk.ac.open.kmi.carre.qs.service." + machineName + "." + toUpper + "Service");
			Constructor<?> constructor = serviceClass.getConstructor(String.class);
			Service service = (Service) constructor.newInstance(propertiesPath);
			service.oauth_token = Service.getOAuthToken(propertiesPath, machineName);
			service.oauth_secret = Service.getOAuthSecret(propertiesPath, machineName);
			//Service service = (Service) serviceClass.newInstance();
			return service;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
