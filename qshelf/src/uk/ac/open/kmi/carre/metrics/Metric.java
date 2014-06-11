package uk.ac.open.kmi.carre.metrics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateFormatUtils;

public abstract class Metric {
	public static final int NO_VALUE_PROVIDED = -1;
	public static final String RDF_PREFIX = "http://carre.kmi.open.ac.uk/ontology/sensors.owl#";
	public static final String ACTUALITY_ACTUAL = "<" + RDF_PREFIX + "ActualMeasurement" + ">";
	public static final String ACTUALITY_GOAL = "<" + RDF_PREFIX + "GoalMeasurement" + ">";
	public static final String[] IGNORED_FIELDS = {"IGNORED_FIELDS","id", "RDF_PREFIX",
		"ACTUALITY_GOAL","ACTUALITY_ACTUAL","NO_VALUE_PROVIDED"};
	
	protected String id;
	protected Date date;
	protected String provenance;
	protected String actuality;
	
	public Metric(String identifier) {
		setId(identifier);
		setDate(Calendar.getInstance().getTime());
		provenance = "";
		setActuality("");
		initialiseEmpty();
	}
	
	public Metric(String source, Date dateMeasured) {
		setDate(dateMeasured);
		setId(source + 
				DateFormatUtils.ISO_DATETIME_FORMAT.format(getDate()).replaceAll(" ", "_"));
		provenance = "";
		setActuality("");
		initialiseEmpty();
	}
	
	protected abstract void initialiseEmpty();
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
			provenance = "<" + RDF_PREFIX + "UniqueDeviceProvenance" + ">";
			break;
		case 1:
			provenance = "<" + RDF_PREFIX + "AmbiguousDeviceProvenance" + ">";
			break;
		case 2:
			provenance = "<" + RDF_PREFIX + "ManualProvenance" + ">";
			break;
		case 4:
			provenance = "<" + RDF_PREFIX + "ManualProfileProvenance" + ">";
			break;
		default:
			provenance = "<" + RDF_PREFIX + "ManualProvenance" + ">";
			break;
		}
	}

	public String getActuality() {
		return actuality;
	}

	public void setActuality(String actuality) {
		this.actuality = actuality;
	}

	public String toRDFString() {
		String rdf = "";
		String obj = "<" + RDF_PREFIX + getId() + ">";
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
			if (!field.getName().equals("id") &&
					!field.getName().equals("RDF_PREFIX") &&
					!field.getName().equals("ACTUALITY_GOAL") &&
					!field.getName().equals("ACTUALITY_ACTUAL") &&
					!field.getName().equals("NO_VALUE_PROVIDED")) {
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
				} else if (fieldType.equals(Float.TYPE)) {
					try {
						Float value = (Float) field.get(this);
						if (value == -1) {
							continue;
						} else {
							literal = value.toString();
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
						if (value == -1) {
							continue;
						} else {
							literal = value.toString();
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
						if (value == -1) {
							continue;
						} else {
							literal = value.toString();
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
						if (value == -1) {
							continue;
						} else {
							literal = value.toString();
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
							literal = "\"" + 
									DateFormatUtils
									.ISO_DATETIME_TIME_ZONE_FORMAT.format(value)
									+ "\"";
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
		return rdf;
	}
}
