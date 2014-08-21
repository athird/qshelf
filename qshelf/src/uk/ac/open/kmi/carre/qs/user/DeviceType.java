package uk.ac.open.kmi.carre.qs.user;

import uk.ac.open.kmi.carre.qs.service.RDFAbleToken;

public class DeviceType {

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
