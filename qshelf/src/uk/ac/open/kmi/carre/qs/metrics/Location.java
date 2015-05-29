package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Location extends Metric {

private static Logger logger = Logger.getLogger(Location.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.LOCATION_METRIC;
	
	protected String location;
	protected Date startDate;
	protected Date endDate;
	protected String locationType;
	protected String foursquareId;
	protected String locationName;
	
	public Location(String identifier) {
		super(identifier);
	}

	public Location(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		location = "";
		startDate = null;
		endDate = null;
		locationType = "";
		foursquareId = "";
		locationName = "";
	}

	@Override
	public String getMetricType() {
		return METRIC_TYPE;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getLocationType() {
		return locationType;
	}

	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	public String getFoursquareId() {
		return foursquareId;
	}

	public void setFoursquareId(String foursquareId) {
		this.foursquareId = foursquareId;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	@Override
	public String toRDFString(String userId) {
		String rdf = super.toRDFString( userId);
		return rdf;
	}
}
