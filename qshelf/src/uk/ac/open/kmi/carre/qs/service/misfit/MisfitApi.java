package uk.ac.open.kmi.carre.qs.service.misfit;

import java.util.logging.Logger;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

public class MisfitApi extends DefaultApi20 {
	private static Logger logger = Logger.getLogger(MisfitApi.class.getName());
	
	@Override
	public String getAccessTokenEndpoint() {
		return MisfitService.accessTokenURL;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return MisfitService.authURL + "?client_id=" + config.getApiKey()
				+ "&response_type=code"
				+ "&redirect_uri=" + config.getCallback()
				+ "&scope=public,birthday,email";
	}

}
