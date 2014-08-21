package uk.ac.open.kmi.carre.qs.service.iHealth;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateFormatUtils;
import org.scribe.model.Token;

import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class IHealthAccessToken extends RDFAbleToken {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6337316461864084285L;

	public static final String RDF_PREFIX = CARREVocabulary.SENSOR_RDF_PREFIX;
	public static final String[] IGNORED_FIELDS = {"IGNORED_FIELDS", "RDF_PREFIX", "serialVersionUID"};
	
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
	
	public String toRDFString(String id) {
		String rdf = "";
		String obj = "<" + RDF_PREFIX + id + ">";
		Class<?> thisClass = this.getClass();
		Field[] fields = thisClass.getDeclaredFields();
		for (Field field : fields) {
			boolean ignore = false;
			for (String ignoreString : IGNORED_FIELDS){
				if (field.getName().equals(ignoreString)) {
					ignore = true;
				}
				if (ignore) {
					break;
				}
			}
			if (!ignore) {
				String triple = obj + " ";
				
				String propertyName = RDF_PREFIX + "has" + 
						Character.toUpperCase(field.getName().charAt(0)) 
						+ field.getName().substring(1);
				triple += "<" + propertyName + ">" + " ";
				
				String literal = "";
				Class<?> fieldType = field.getType();
				if (fieldType.equals(String.class)) {
					try {
						String value = (String) field.get(this);
						if (value.equals("")) {
							continue;
						} else {
							literal = "\"" + value + "\"";
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} 
				if (!literal.equals("")) {
					triple += literal + ".";
					rdf += triple + "\n"; 
				}
			}
		}
		String typeTriple = obj + " <" + RDF_PREFIX + "hasTokenType> <" + 
				RDF_PREFIX + "oauth2Token>.\n" ;
		rdf += typeTriple;
		return rdf;
	}

}
