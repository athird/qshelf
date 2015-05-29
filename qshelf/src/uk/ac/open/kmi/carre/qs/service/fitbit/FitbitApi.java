package uk.ac.open.kmi.carre.qs.service.fitbit;

import java.util.logging.Logger;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;

public class FitbitApi extends DefaultApi10a{
	private static Logger logger = Logger.getLogger(FitbitApi.class.getName());
	
	private static final String AUTHORIZE_URL = "https://www.fitbit.com/oauth/authorize?oauth_token=%s";

	public String getAccessTokenEndpoint(){
		return "https://api.fitbit.com/oauth/access_token";
	}
	public String getRequestTokenEndpoint(){
		return "https://api.fitbit.com/oauth/request_token";
	}
	public String getAuthorizationUrl(Token requestToken){
		return String.format(AUTHORIZE_URL, requestToken.getToken());
	}
}
