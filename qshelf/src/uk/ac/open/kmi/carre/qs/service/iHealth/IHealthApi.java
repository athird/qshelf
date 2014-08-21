package uk.ac.open.kmi.carre.qs.service.iHealth;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

public class IHealthApi extends DefaultApi20 {

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
