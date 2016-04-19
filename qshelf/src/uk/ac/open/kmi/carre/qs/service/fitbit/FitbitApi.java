package uk.ac.open.kmi.carre.qs.service.fitbit;

import java.util.logging.Logger;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

public class FitbitApi extends DefaultApi20 {
	private static Logger logger = Logger.getLogger(FitbitApi.class.getName());
	
	@Override
	public String getAccessTokenEndpoint() {
		return FitbitService.accessTokenURL;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return FitbitService.authURL + "?client_id=" + config.getApiKey()
				+ "&response_type=code"
				+ "&redirect_uri=" + config.getCallback()
				+ "&scope=ctivity heartrate nutrition profile sleep weight";
	}

}
