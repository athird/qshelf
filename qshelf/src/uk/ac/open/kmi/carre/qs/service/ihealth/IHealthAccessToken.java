package uk.ac.open.kmi.carre.qs.service.ihealth;

import org.scribe.model.Token;

public class IHealthAccessToken extends Token {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6337316461864084285L;

	private String apiNames;
	private String expires;
	private String refreshToken;
	private String userId;
	
	public IHealthAccessToken(String token, String secret, String rawResponse) {
		super(token, secret, rawResponse);
	}

	public IHealthAccessToken(String token, String secret) {
		super(token, secret);
	}
	
	public void setApiNames(String apis) {
		apiNames = apis;
	}
	
	public void setExpires(String expiry) {
		expires = expiry;
	}
	
	public void setRefreshToken(String refresh) {
		refreshToken = refresh;
	}
	
	public void setUserId(String user) {
		userId = user;
	}
	
	public String getApiNames() {
		return apiNames;
	}
	
	public String getExpires() {
		return expires;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	
	public String getUserId() {
		return userId;
	}

}
