package uk.ac.open.kmi.carre.qs.service.moves;

import java.util.logging.Logger;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

public class MovesApi extends DefaultApi20 {
	private static Logger logger = Logger.getLogger(MovesApi.class.getName());
	
	@Override
	public String getAccessTokenEndpoint() {
		return MovesService.accessTokenURL;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return MovesService.authURL + "?client_id=" + config.getApiKey()
				+ "&response_type=code"
				+ "&redirect_uri=" + config.getCallback()
				+ "&scope=activity location";
	}

}
