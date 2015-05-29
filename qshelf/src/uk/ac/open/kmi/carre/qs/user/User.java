package uk.ac.open.kmi.carre.qs.user;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;

public class User {
	private static Logger logger = Logger.getLogger(User.class.getName());
	
	public static final String FOAF_PREFIX = "http://carre.kmi.open.ac.uk/people/";
	public static final String RDF_PREFIX = "http://carre.kmi.open.ac.uk/ontology/sensors.owl#";
	public static final String[] IGNORED_FIELDS = {"IGNORED_FIELDS", "RDF_PREFIX"};
	
	private String username;
	private String password; 
	
	private String foafID = "";
	private List<DeviceType> devices;
	
	public User(String user, String passwd) {
		username = user;
		password = passwd;
		foafID = FOAF_PREFIX + user;
		devices = new ArrayList<DeviceType>();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getFoafID() {
		return foafID;
	}

	public List<DeviceType> getDevices() {
		return devices;
	}

	public void setDevices(List<DeviceType> devices) {
		this.devices = devices;
	}

	public String toRDFString() {
		String rdf = "";
		String obj = foafID;
		String userTriple = obj + " <" + RDF_PREFIX + "hasUsername> \"" 
				+ getUsername() + "\".\n" ;
		for (DeviceType device : devices) {
			RDFAbleToken token = device.getToken();
			String tokenID = device.getName() + "Token";
			String tokenTriple = obj + " <" + RDF_PREFIX + "hasDeviceToken> " +
					" <" + RDF_PREFIX + tokenID + ">.\n";
			String tokenRDF = token.toRDFString(tokenID);
			rdf += tokenTriple;
			rdf += tokenRDF;
		}
		
		rdf += userTriple;
		rdf += "\n";
		return rdf;
	}

}
