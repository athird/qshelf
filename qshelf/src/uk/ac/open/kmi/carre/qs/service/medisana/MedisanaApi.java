package uk.ac.open.kmi.carre.qs.service.medisana;


import org.apache.commons.lang.RandomStringUtils;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;
import uk.ac.open.kmi.carre.qs.service.medisana.HMACSha256SignatureService;
import org.scribe.services.SignatureService;
import org.scribe.services.TimestampService;
import org.scribe.services.TimestampServiceImpl;


public class MedisanaApi extends DefaultApi10a {

    //private static final String AUTHORIZE_URL = "https://vitacloud.medisanaspace.com/desiredaccessrights/request?oauth_token=%s";
    private static final String AUTHORIZE_URL = "https://cloud.vitadock.com/desiredaccessrights/request?oauth_token=%s";
       
    public static final Integer NONCE_LENGTH = 36;
    
	public static class TimeService extends TimestampServiceImpl {
        @Override
        public String getTimestampInSeconds() {
			return String.valueOf(System.currentTimeMillis()); // Medisana uses timestamp in miliseconds instead of seconds
        }
        
        @Override
        public String getNonce() {
			return String.valueOf(RandomStringUtils.randomAlphanumeric(NONCE_LENGTH)); // Medisana uses 36 symbol length nonce
        }
    }		  
    public TimestampService getTimestampService()
    {
        return new TimeService();
    }
    public String getAccessTokenEndpoint() {
        //return "https://vitacloud.medisanaspace.com/auth/accesses/verify";
    	return "https://cloud.vitadock.com/auth/accesses/verify";
    }
    public String getRequestTokenEndpoint() {
        //return "https://vitacloud.medisanaspace.com/auth/unauthorizedaccesses";
        return "https://cloud.vitadock.com/auth/unauthorizedaccesses";
    } 
    public String getAuthorizationUrl(Token requestToken) {
        return String.format(AUTHORIZE_URL, requestToken.getToken());
    }
    public SignatureService getSignatureService() {
       return new HMACSha256SignatureService();
    }
      
}




