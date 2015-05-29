package uk.ac.open.kmi.carre.qs.user;

import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;

public class DeviceType {
	private static Logger logger = Logger.getLogger(DeviceType.class.getName());
	
	private String name;
	private RDFAbleToken token;
	
	public DeviceType(String manufacturer) {
		name = manufacturer;
		token = null;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public RDFAbleToken getToken() {
		return token;
	}

	public void setToken(RDFAbleToken token) {
		this.token = token;
	}

	

}
