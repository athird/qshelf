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

	public static String getTypeFor(String fieldName) {
		switch(fieldName){
		case "steps":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "distance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "caloriesBMR":
			return "http://purl.bioontology.org/ontology/LNC/LP35952-8"; // calories metabolised
		case "caloriesMetabolised":
			return "http://purl.bioontology.org/ontology/LNC/LP35952-8"; // calories metabolised
		case "activityCalories": 
			return "http://purl.bioontology.org/ontology/LNC/LP35952-8"; // calories metabolised
		case "marginalCalories":
			return "http://purl.bioontology.org/ontology/LNC/LP35952-8"; // calories metabolised
		case "elevation":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "floors":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "sedentaryActivityDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "lightActivityDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "moderateActivityDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "intenseActivityDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "sedentaryActivityDistance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "lightActivityDistance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "moderateActivityDistance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "loggedActivityDistance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "trackedActivityDistance":
			return "http://purl.obolibrary.org/obo/CMO_0000955"; //distance moved by unspecified means
		case "loggedActivityDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "timezone":
			return "http://www.w3.org/2006/timezone#TimeZone"; // timezone
		case "bloodPressureSystolic":
			return "http://purl.obolibrary.org/obo/CMO_0000004"; // blood pressure systolic value 
		case "bloodPressureDiastolic":
			return "http://purl.obolibrary.org/obo/CMO_0000005"; // blood pressure diastolic value 
		case "bicep":
			return "http://purl.obolibrary.org/obo/CMO_0000317"; // bicep value measurement 
		case "calf":
			return "http://purl.obolibrary.org/obo/CMO_0000186"; // calf value measurement
		case "chest":
			return "http://purl.obolibrary.org/obo/CMO_0000316"; // chest value measurement 
		case "forearm":
			return "http://purl.obolibrary.org/obo/CMO_0000187"; // forearm value measurement 
		case "hips":
			return "http://purl.obolibrary.org/obo/CMO_0000014"; // hips value measurement
		case "neck":
			return "http://purl.obolibrary.org/obo/CMO_0000021"; // neck value measurement 
		case "thigh":
			return "http://purl.obolibrary.org/obo/CMO_0000019"; // thigh value measurement 
		case "waist":
			return "http://purl.obolibrary.org/obo/CMO_0000242"; // waist value measurement 
		case "calories":
			return "http://purl.obolibrary.org/obo/CMO_0002208"; // calories value measurement 
		case "water":
			return "http://purl.obolibrary.org/obo/CMO_0000774"; // water value measurement 
		case "glucose":
			return "http://purl.obolibrary.org/obo/CMO_0000046"; // glucose value measurement 
		case "hbA1c":
			return "http://purl.obolibrary.org/obo/CMO_0000508"; // hba1c value measurement 
		case "height":
			return "http://purl.obolibrary.org/obo/CMO_0000106"; // height value measurement 
		case "pulse":
			return "http://purl.obolibrary.org/obo/CMO_0000294"; // pulse value measurement 
		case "timeToFallAsleep":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "sleepTime":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "awakeDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "asleepDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "restlessDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "remDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "deepSleepDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "lightSleepDuration":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "minutesAfterWakeup":
			return "http://www.w3.org/2006/time#Interval"; // duration
		case "weight": 
			return "http://purl.obolibrary.org/obo/CMO_0000012"; // weight value measurement 
		case "bmi":
			return "http://purl.obolibrary.org/obo/CMO_0000105"; //  bmi value measurement 
		case "bodyFat":
			return "http://purl.obolibrary.org/obo/CMO_0000302"; // body fat value measurement 
		case "leanMass":
			return "http://purl.obolibrary.org/obo/CMO_0002184"; // calculated whole body lean mass measurement
		case "muscleMass":
			return "http://purl.obolibrary.org/obo/CMO_0000448"; // muscle mass value measurement type
		case "waterMass":
			return "http://purl.obolibrary.org/obo/CMO_0000000"; // calculated whole body water measurement.
		case "fatMass":
			return "http://purl.obolibrary.org/obo/CMO_0000305"; // fat mass value measurement 
		case "boneMass":
			return "http://purl.obolibrary.org/obo/CMO_0000461"; // bone mass value measurement 
		case "bodyDCI":
			return "http://purl.obolibrary.org/obo/CMO_0002253"; // body dci value measurement 
		case "morningTime":
		case "sleepStatus":
		case "temperature":
		case "arrhythmia":
			 //  manufacturer calculated arrhythmia
		case "condition":
			 //  device manufacturer calculated pulse condition
		case "sleepRecords":
			
		case "timesRemAsleep":
			 // occurrence count
		case "timesDeeplyAsleep":
			 // occurrence count
		case "timesLightlyAsleep":
			 // occurrence count
		case "timesAwake":
			 // occurrence count
		case "timesRestless":
			 // occurrence count
		case "sleepEfficiency":
			 // device manufacturer calculated sleep efficiency
		case "insulin":
			
		case "insulinType":
			
		case "location":
			
		case "startDate":
			
		case "endDate":
			
		case "locationType":
			
		case "foursquareId":
			
		case "locationName":
			
		case "o2saturation":
			
		case "dinnerSituation":
			 // dinner situation
		case "drugSituation":
			 // drug situation
		case "quantity":
			 // food quantity
		case "foodType":
			 // food
		case "mealType":
			 // meal
		case "carbs":
			 // carbohydrates nutrition value
		case "fat": 
			 // fat nutrition value
		case "fibre":
			 // fibre nutrition value.
		case "protein":
			 // protein nutrition value.
		case "sodium":
			 // sodium nutrition value
		case "arrhythmiaType":
			
		case "whoBPLevel":
			
		case "activityPoints":
			
		case "loggedActivityName":
			 // logged activity
		case "intenseActivityDistance":
			 // distance moved
		default:
			return "http://carre.kmi.open.ac.uk/external_measurement_type/CMO_" + fieldName.toUpperCase();
		}
	}

	public static String getUnitStringFor(String fieldName) {
		switch(fieldName){
		case "steps":
			return "count_unit";
		case "distance":
			return "kilometers_unit";
		case "caloriesBMR":
			return "calories_unit"; // calories metabolised
		case "caloriesMetabolised":
			return "calories_unit"; // calories metabolised
		case "activityCalories": 
			return "calories_unit"; // calories metabolised
		case "marginalCalories":
			return "calories_unit"; // calories metabolised
		case "elevation":
			return "meters_unit"; //distance moved by unspecified means
		case "floors":
			return "count_unit"; //distance moved by unspecified means
		case "sedentaryActivityDuration":
			return "seconds_unit"; // duration
		case "lightActivityDuration":
			return "seconds_unit"; // duration
		case "moderateActivityDuration":
			return "seconds_unit"; // duration
		case "intenseActivityDuration":
			return "seconds_unit"; // duration
		case "sedentaryActivityDistance":
			return "kilometers_unit"; //distance moved by unspecified means
		case "lightActivityDistance":
			return "kilometers_unit"; //distance moved by unspecified means
		case "moderateActivityDistance":
			return "kilometers_unit"; //distance moved by unspecified means
		case "intenseActivityDistance":
			return "kilometers_unit"; // distance moved
		case "loggedActivityDistance":
			return "kilometers_unit"; //distance moved by unspecified means
		case "trackedActivityDistance":
			return "kilometers_unit"; //distance moved by unspecified means
		case "loggedActivityDuration":
			return "seconds_unit"; // duration
		case "loggedActivityName":
			return ""; // logged activity
		case "timezone":
			return ""; // timezone
		case "activityPoints":
			return "count_unit";
		case "bloodPressureSystolic":
			return "millimeters_of_mercury_unit"; // blood pressure systolic value 
		case "bloodPressureDiastolic":
			return "millimeters_of_mercury_unit"; // blood pressure diastolic value 
		case "arrhythmiaType":
			return "";
		case "whoBPLevel":
			return "";
		case "bicep":
			return "centimeters_unit"; // bicep value measurement 
		case "calf":
			return "centimeters_unit"; // calf value measurement
		case "chest":
			return "centimeters_unit"; // chest value measurement 
		case "forearm":
			return "centimeters_unit"; // forearm value measurement 
		case "hips":
			return "centimeters_unit"; // hips value measurement
		case "neck":
			return "centimeters_unit"; // neck value measurement 
		case "thigh":
			return "centimeters_unit"; // thigh value measurement 
		case "waist":
			return "centimeters_unit"; // waist value measurement 
		case "calories":
			return "calories_unit"; // calories value measurement 
		case "carbs":
			return ""; // carbohydrates nutrition value
		case "fat": 
			return ""; // fat nutrition value
		case "fibre":
			return ""; // fibre nutrition value.
		case "protein":
			return ""; // protein nutrition value.
		case "sodium":
			return ""; // sodium nutrition value
		case "water":
			return "liters_unit"; // water value measurement 
		case "quantity":
			return "count_unit"; // food quantity
		case "foodType":
			return ""; // food
		case "mealType":
			return ""; // meal
		case "glucose":
			return "glucose_unit"; // glucose value measurement 
		case "hbA1c":
			return "hba1c_unit"; // hba1c value measurement 
		case "dinnerSituation":
			return ""; // dinner situation
		case "drugSituation":
			return ""; // drug situation
		case "height":
			return "meters_unit"; // height value measurement 
		case "insulin":
			return "insulin_unit";
		case "insulinType":
			return "";
		case "location":
			return "";
		case "startDate":
			return "";
		case "endDate":
			return "";
		case "locationType":
			return "";
		case "foursquareId":
			return "";
		case "locationName":
			return "";
		case "o2saturation":
			return "percentage_unit";
		case "pulse":
			return "count_per_minute_unit"; // pulse value measurement 
		case "arrhythmia":
			return ""; //  manufacturer calculated arrhythmia
		case "condition":
			return ""; //  device manufacturer calculated pulse condition
		case "sleepRecords":
			return "";
		case "timesRemAsleep":
			return "count_unit"; // occurrence count
		case "timesDeeplyAsleep":
			return "count_unit"; // occurrence count
		case "timesLightlyAsleep":
			return "count_unit"; // occurrence count
		case "timesAwake":
			return "count_unit"; // occurrence count
		case "timesRestless":
			return "count_unit"; // occurrence count
		case "timeToFallAsleep":
			return "seconds_unit"; // duration
		case "sleepTime":
			return "seconds_unit"; // duration
		case "sleepEfficiency":
			return "percentage_unit"; // device manufacturer calculated sleep efficiency
		case "awakeDuration":
			return "seconds_unit"; // duration
		case "asleepDuration":
			return "seconds_unit"; // duration
		case "restlessDuration":
			return "seconds_unit"; // duration
		case "remDuration":
			return "seconds_unit"; // duration
		case "deepSleepDuration":
			return "seconds_unit"; // duration
		case "lightSleepDuration":
			return "seconds_unit"; // duration
		case "minutesAfterWakeup":
			return "minutes_unit"; // duration
		case "morningTime":
			return ""; // time
		case "sleepStatus":
			return "";
		case "temperature":
			return "celsius_unit";
		case "weight": 
			return "kilograms_unit"; // weight value measurement 
		case "bmi":
			return "kilograms_per_square_meter_unit"; //  bmi value measurement 
		case "bodyFat":
			return "percentage_unit"; // body fat value measurement 
		case "leanMass":
			return "kilograms_unit"; // calculated whole body lean mass measurement
		case "muscleMass":
			return "kilograms_unit"; // muscle mass value measurement type
		case "waterMass":
			return "kilograms_unit"; // calculated whole body water measurement.
		case "fatMass":
			return "kilograms_unit"; // fat mass value measurement 
		case "boneMass":
			return "kilograms_unit"; // bone mass value measurement 
		case "bodyDCI":
			return "calories_unit"; // body dci value measurement 
		default:
			return "";
		}}

	public static String getUnitFor(String fieldName) {
		switch(fieldName){
		case "degrees_unit":
			return "http://purl.obolibrary.org/obo/UO_0000185";
		case "count_unit":
			return "http://purl.obolibrary.org/obo/UO_0000189";
		case "kilometers_unit":
			return "http://purl.obolibrary.org/obo/UO_1000008";
		case "calories_unit":
			return "http://qudt.org/vocab/unit#CalorieNutritional";
		case "seconds_unit":
			return "http://purl.obolibrary.org/obo/UO_0000010";
		case "minutes_unit":
			return "http://purl.obolibrary.org/obo/UO_0000031";
		case "meters_unit":
			return "http://purl.obolibrary.org/obo/UO_0000008";
		case "glucose_unit": //medisana mg/dL iHealth mg/dL (mmol/l optional)
			return "http://carre.kmi.open.ac.uk/external_measurement_unit/UO_MILLIGRAM_PER_DECILITER";
		case "centimeters_unit":
			return "http://purl.obolibrary.org/obo/UO_0000015";
		case "insulin_unit":
		case "grams_unit":
			return "http://purl.obolibrary.org/obo/UO_0000021";
		case "liters_unit":
			return "http://purl.obolibrary.org/obo/UO_0000099";
		case "count_per_minute_unit":
			return "http://purl.obolibrary.org/obo/UO_0000148";
		case "hba1c_unit": // observable expects a percentage?
		case "percentage_unit":
			return "http://purl.obolibrary.org/obo/UO_0000187";
		case "kilograms_unit":
			return "http://purl.obolibrary.org/obo/UO_0000009";
		case "kilograms_per_square_meter_unit":
			return "http://purl.obolibrary.org/obo/UO_0000086";
		case "millimeters_of_mercury_unit":
			return "http://purl.obolibrary.org/obo/UO_0000272";
		case "celsius_unit":
			return "http://purl.obolibrary.org/obo/UO_0000027";
		default:
			return "http://carre.kmi.open.ac.uk/external_measurement_unit/UO_" + fieldName.toUpperCase();
		}
	}

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
					String externalType = Metric.getTypeFor(field.getName());
					String unitTriple = "";
					String typeTriple = "";
					if (!externalType.equals("")) {
						String externalUnitString = Metric.getUnitStringFor(field.getName());
						if (!externalUnitString.equals("")) {
							String externalUnit = Metric.getUnitFor(externalUnitString);
							if (!externalUnit.equals("")) {
								unitTriple = valueObj + " <" + CARREVocabulary.EXTERNAL_UNIT_PREDICATE + "> <" + externalUnit + ">.\n";
							}
						}
						typeTriple = valueObj + " <" + CARREVocabulary.EXTERNAL_TYPE_PREDICATE + "> <" + externalType + ">.\n";
					}
					rdf += triple + "\n"; 
					if (!typeTriple.equals("")) {
						rdf += typeTriple;
					}
					if (!unitTriple.equals("")) {
						rdf += unitTriple;
					}
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
