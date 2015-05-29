package uk.ac.open.kmi.carre.qs.vocabulary;

public interface CARREVocabulary {

	public static final String BASE_URL = "http://carre.kmi.open.ac.uk/";
	public static final String SECURE_BASE_URL = "https://carre.kmi.open.ac.uk/";
	public static final String USER_URL = SECURE_BASE_URL + "users/";
	public static final String CONNECTIONS_SUFFIX = "connections/";
	public static final String MEASUREMENTS_SUFFIX = "measurements/";
	public static final String CARRE_SENSORS_OWL = BASE_URL + "ontology/sensors.owl#";
	public static final String ACCESS_TOKEN_PREDICATE = CARRE_SENSORS_OWL + "has_access_token";
	public static final String ACCESS_TOKEN_SECRET_PREDICATE = CARRE_SENSORS_OWL + "has_access_token_secret";
	public static final String REFRESH_TOKEN_PREDICATE = CARRE_SENSORS_OWL + "has_refresh_token";
	public static final String EXPIRES_TOKEN_PREDICATE = CARRE_SENSORS_OWL + "has_expiry";
	public static final String EXPIRES_AT_PREDICATE = CARRE_SENSORS_OWL + "expires";
	public static final String APINAMES_TOKEN_PREDICATE = CARRE_SENSORS_OWL + "has_api_names";
	public static final String USER_ID_PREDICATE = CARRE_SENSORS_OWL + "has_user_id";
	public static final String MEASURED_BY = CARRE_SENSORS_OWL + "is_measured_by";
	public static final String HAS_METRIC_TYPE = CARRE_SENSORS_OWL + "has_metric_type";
	public static final String HAS_MANUFACTURER = CARRE_SENSORS_OWL + "has_manufacturer";
	public static final String HAS_VALUE = CARRE_SENSORS_OWL + "has_value";
	public static final String DATE_TYPE = "^^xsd:datetime";
	public static final String DOUBLE_TYPE = "^^xsd:double";
	public static final String FLOAT_TYPE = "^^xsd:float";
	public static final String LONG_TYPE = "^^xsd:long";
	public static final String INT_TYPE = "^^xsd:int";
	public static final String STRING_TYPE = "^^xsd:string";

	public static final String[] PREFICES = {"xsd: <http://www.w3.org/2001/XMLSchema#>",
		"rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
		"carreSensors: <" + CARRE_SENSORS_OWL + ">",
	"carreManufacturer: <" + BASE_URL + "manufacturers/>"};
	public static final String SENSOR_RDF_PREFIX = "carreSensors:";
	public static final String MANUFACTURER_RDF_PREFIX = "carreManufacturer:";
	
	public static final String RDF_TYPE = "rdfs:type";


	public static final String REM_SLEEP = SENSOR_RDF_PREFIX + "REM_SLEEP";
	public static final String DEEP_SLEEP = SENSOR_RDF_PREFIX + "DEEP_SLEEP";
	public static final String LIGHT_SLEEP = SENSOR_RDF_PREFIX + "LIGHT_SLEEP";
	public static final String ASLEEP = SENSOR_RDF_PREFIX + "ASLEEP";
	public static final String RESTLESS = SENSOR_RDF_PREFIX + "RESTLESS";
	public static final String AWAKE = SENSOR_RDF_PREFIX + "AWAKE";

	public static final String NO_ARRHYTHMIA = CARREVocabulary.SENSOR_RDF_PREFIX + "no_arrhythmia";
	public static final String ARRHYTHMIA = CARREVocabulary.SENSOR_RDF_PREFIX + "arrhythmia_cordis";

	public static final String UNIQUE_DEVICE_PROVENANCE = SENSOR_RDF_PREFIX + "unique_device_provenance";
	public static final String AMBIGUOUS_DEVICE_PROVENANCE = SENSOR_RDF_PREFIX + "ambiguous_device_provenance";
	public static final String MANUAL_PROVENANCE = SENSOR_RDF_PREFIX + "manual_provenance";
	public static final String MANUAL_PROFILE_PROVENANCE = SENSOR_RDF_PREFIX + "manual_profile_provenance";

	public static final String ACTIVITY_METRIC = SENSOR_RDF_PREFIX + "individual_activity_measurement";
	public static final String BLOOD_PRESSURE_METRIC = SENSOR_RDF_PREFIX + "individual_blood_pressure_measurement";
	public static final String BODY_METRIC = SENSOR_RDF_PREFIX + "individual_body_measurement";
	public static final String FOOD_METRIC = SENSOR_RDF_PREFIX + "individual_food_measurement";
	public static final String GLUCOSE_METRIC = SENSOR_RDF_PREFIX + "individual_glucose_measurement";
	public static final String HEIGHT_METRIC = SENSOR_RDF_PREFIX + "individual_height_measurement";
	public static final String O2SATURATION_METRIC = SENSOR_RDF_PREFIX + "individual_oxygen_saturation_measurement";
	public static final String PULSE_METRIC = SENSOR_RDF_PREFIX + "individual_pulse_measurement";
	public static final String SLEEP_METRIC = SENSOR_RDF_PREFIX + "individual_sleep_measurement";
	public static final String SLEEP_RECORD_METRIC = SENSOR_RDF_PREFIX + "individual_sleep_record_measurement";
	public static final String WEIGHT_METRIC = SENSOR_RDF_PREFIX + "individual_weight_measurement";
	public static final String LOCATION_METRIC = SENSOR_RDF_PREFIX + "individual_location_measurement";
	public static final String INSULIN_METRIC = SENSOR_RDF_PREFIX + "individual_insulin_measurement";
	public static final String TEMPERATURE_METRIC = SENSOR_RDF_PREFIX + "individual_temperature_measurement";
	
	public static final String PLACE_TYPE_UNKNOWN = SENSOR_RDF_PREFIX + "unknown_place";
	public static final String PLACE_TYPE_HOME = SENSOR_RDF_PREFIX + "home_place";
	public static final String PLACE_TYPE_SCHOOL = SENSOR_RDF_PREFIX + "school_place";
	public static final String PLACE_TYPE_WORK = SENSOR_RDF_PREFIX + "work_place";
	public static final String PLACE_TYPE_USER = SENSOR_RDF_PREFIX + "user_place";
	public static final String PLACE_TYPE_FOURSQUARE = SENSOR_RDF_PREFIX + "foursquare_place";
	public static final String PLACE_HAS_FOURSQUARE_ID_PREDICATE = SENSOR_RDF_PREFIX + "has_foursquare_id";
	
	public static final String DEFAULT_USER_FOR_TESTING = "athird";
	}
