package uk.ac.open.kmi.carre.qs.service;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import org.scribe.model.Token;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;

public class RDFAbleToken extends Token {
	private static Logger logger = Logger.getLogger(RDFAbleToken.class.getName());
	
	public static final String RDF_PREFIX = "http://carre.kmi.open.ac.uk/ontology/sensors.owl#";
	private String user = "";
	
	public RDFAbleToken(String token, String secret) {
		super(token, secret);
	}

	public RDFAbleToken(String token, String secret, String rawResponse) {
		super(token, secret, rawResponse);
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String toRDFString(String id) {
		String rdf = "";
		String obj = "<" + RDF_PREFIX + id + ">";
		String tokenTriple = obj + " <" + RDF_PREFIX + "hasToken> \"" 
				+ getToken() + "\".\n" ;
		String typeTriple = obj + " <" + RDF_PREFIX + "hasTokenType> <" + 
				RDF_PREFIX + "oauth1Token>.\n" ;

		String secretTriple = obj + " <" + RDF_PREFIX + "hasSecret> \"" 
				+ getSecret() + "\".\n" ;
		
		rdf += tokenTriple;
		rdf += typeTriple;
		rdf += secretTriple;
		rdf += "\n";
		return rdf;
	}

}
