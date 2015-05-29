package uk.ac.open.kmi.carre.qs.service.iHealth;

import java.util.logging.Logger;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;

public class IHealthApi extends DefaultApi20 {
	private static Logger logger = Logger.getLogger(IHealthApi.class.getName());
	
	@Override
	public String getAccessTokenEndpoint() {
		return IHealthService.accessTokenURL;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return IHealthService.authURL + "?client_id=" + config.getApiKey()
				+ "&response_type=code" 
				+ "&redirect_uri=" + config.getCallback()
				+ "&APIName=OpenApiActivity OpenApiBG OpenApiBP OpenApiSleep OpenApiSpO2 OpenApiUserInfo OpenApiWeight";
	}

}
