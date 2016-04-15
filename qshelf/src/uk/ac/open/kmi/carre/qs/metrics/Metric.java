package uk.ac.open.kmi.carre.qs.metrics;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateFormatUtils;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public abstract class Metric {
	private static Logger logger = Logger.getLogger(Metric.class.getName());
	
	public static final int NO_VALUE_PROVIDED = -1;
	public static final String ACTUALITY_ACTUAL = CARREVocabulary.SENSOR_RDF_PREFIX + "actual_measurement";
	public static final String ACTUALITY_GOAL = CARREVocabulary.SENSOR_RDF_PREFIX + "goal_measurement";
	public static final String[] IGNORED_FIELDS = {"IGNORED_FIELDS","id", "user", "RDF_PREFIX",
		"ACTUALITY_GOAL","ACTUALITY_ACTUAL","NO_VALUE_PROVIDED", "REM_SLEEP", "DEEP_SLEEP", 
		"LIGHT_SLEEP", "ASLEEP", "RESTLESS", "AWAKE", "METRIC_TYPE" };

	protected String user;
	protected String id;
	protected Date date;
	protected String provenance = "";
	protected String actuality = "";
	protected String note = "";
	protected float latitude = NO_VALUE_PROVIDED;
	protected float longitude = NO_VALUE_PROVIDED;

	public Metric(String identifier) {
		setId(identifier);
		setDate(Calendar.getInstance().getTime());
		provenance = "";
		setActuality("");
		initialiseEmpty();
	}

	public Metric(String source, Date dateMeasured) {
		setDate(dateMeasured);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	    //df.setTimeZone(TimeZone.getTimeZone("UTC"));
		setId(source + 
				df.format(getDate()).replaceAll(" ", "_"));
		provenance = "";
		setActuality("");
		user = "";
		initialiseEmpty();
	}

	protected abstract void initialiseEmpty();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id.replaceAll("-", "").replaceAll(":", "");
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getProvenance() {
		return provenance;
	}

	public void setProvenance(String provenance) {
		this.provenance = provenance;
	}

	public void setProvenance(int provenanceCode) {
		switch (provenanceCode) {
		case 0:
			provenance = CARREVocabulary.UNIQUE_DEVICE_PROVENANCE;
			break;
		case 1:
			provenance = CARREVocabulary.AMBIGUOUS_DEVICE_PROVENANCE;
			break;
		case 2:
			provenance = CARREVocabulary.MANUAL_PROVENANCE;
			break;
		case 4:
			provenance = CARREVocabulary.MANUAL_PROFILE_PROVENANCE;
			break;
		default:
			provenance = CARREVocabulary.MANUAL_PROVENANCE;
			break;
		}
	}

	public String getActuality() {
		return actuality;
	}

	public void setActuality(String actuality) {
		this.actuality = actuality;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}


	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	public abstract String getMetricType();

	public String getMeasuredByRDF(String source, String userId) {
		user = userId;
		String currentRDFPrefix = CARREVocabulary.USER_URL + userId + "/" + CARREVocabulary.MEASUREMENTS_SUFFIX;
		if (!toRDFString( userId).equals("")) {
			String rdf = ""; 
			String obj = "<" + currentRDFPrefix + getId() + ">";
			rdf += " " + obj + " <" + CARREVocabulary.MEASURED_BY + "> ";
			rdf += source + " .\n";
			rdf += " " + obj + " " + CARREVocabulary.RDF_TYPE + " ";
			rdf += getMetricType() + " .\n";
			return rdf;
		} else {
			return "";
		}
	}

	public String toRDFString(String userId) {
		String rdf = "";
		String currentRDFPrefix = CARREVocabulary.USER_URL + userId + "/" + CARREVocabulary.MEASUREMENTS_SUFFIX;
		
		String obj = "<" + currentRDFPrefix + getId() + ">";
		Class<?> thisClass = this.getClass();
		Field[] fields = thisClass.getDeclaredFields();
		Field[] superFields = thisClass.getSuperclass().getDeclaredFields();
		List<Field> allFields = new ArrayList<Field>();
		
		for (Field field : fields) {
			allFields.add(field);
		}
		for (Field field : superFields) {
			allFields.add(field);
		}
		for (Field field : allFields) {
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
				String valuePrefix = field.getName();
				valuePrefix = valuePrefix.replaceAll("([A-Z])", "_$1").toLowerCase();
				String valueObj = "";
				if (obj.endsWith(">")) {
					valueObj = obj.substring(0,obj.length() - 2);
					valueObj = valueObj + "_" + valuePrefix + ">";
				} else {
					valueObj = obj;
					valueObj = valueObj + "_" + valuePrefix;
				}
				
				String propertyName = CARREVocabulary.SENSOR_RDF_PREFIX + "has_" + valuePrefix;
				triple += propertyName + " " + valueObj + ". \n";
				
				triple += valueObj + " <" + CARREVocabulary.HAS_VALUE + "> ";

				String literal = "";
				Class<?> fieldType = field.getType();
				if (fieldType.equals(String.class)) {
					try {
						String value = (String) field.get(this);
						if (value.equals("")) {
							continue;
						} else if (value.startsWith(CARREVocabulary.SENSOR_RDF_PREFIX) 
								|| value.startsWith(CARREVocabulary.MANUFACTURER_RDF_PREFIX)) {
							literal = value;
						} else if (value.startsWith(currentRDFPrefix)) {
							literal = "<" + value + ">";
						} else {
							literal = "\"" + value + "\"" + CARREVocabulary.STRING_TYPE;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} else if (fieldType.equals(Float.TYPE)) {
					try {
						Float value = (Float) field.get(this);
						if (value == -1 || value == 0) {
							continue;
						} else {
							literal = "\"" + value.toString() + "\"" + CARREVocabulary.FLOAT_TYPE;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} else if (fieldType.equals(Double.TYPE)) {
					try {
						Double value = (Double) field.get(this);
						if (value == -1 || value == 0) {
							continue;
						} else {
							literal = "\"" + value.toString() + "\"" + CARREVocabulary.DOUBLE_TYPE;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} else if (fieldType.equals(Long.TYPE)) {
					try {
						Long value = (Long) field.get(this);
						if (value == -1 || value == 0) {
							continue;
						} else {
							literal = "\"" + value.toString() + "\"" + CARREVocabulary.LONG_TYPE;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} else if (fieldType.equals(Integer.TYPE)) {
					try {
						Integer value = (Integer) field.get(this);
						if (value == -1 || value == 0) {
							continue;
						} else {
							literal = "\"" + value.toString() + "\"" + CARREVocabulary.INT_TYPE;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					}
				} else if (fieldType.equals(Date.class)) {
					try {
						Date value = (Date) field.get(this);
						if (value == null) {
							continue;
						} else {
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
						    //df.setTimeZone(TimeZone.getTimeZone("UTC"));
							literal = "\"" + 
									df.format(value)
									+ "\"" + CARREVocabulary.DATE_TYPE;
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
					triple += literal + " .";
					rdf += triple + "\n"; 
				}
			}
		}
		int numLines = rdf.split(System.getProperty("line.separator")).length;
		if (rdf != null && numLines < 3) {
			if (rdf.contains("hasDate") && numLines < 2) {
				rdf = "";
			} else if (rdf.contains("hasSedentaryActivityDuration") &&
					rdf.contains("1440")) {
				rdf = "";
			}
		}
		return rdf;
	}

}
