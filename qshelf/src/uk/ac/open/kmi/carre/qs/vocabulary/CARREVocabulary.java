package uk.ac.open.kmi.carre.qs.vocabulary;

public interface CARREVocabulary {

	public static final String BASE_URL = "http://carre.kmi.open.ac.uk/";
	public static final String USER_URL = BASE_URL + "users/";
	public static final String ACCESS_TOKEN_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasAccessToken";
	public static final String ACCESS_TOKEN_SECRET_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasAccessTokenSecret";
	public static final String REFRESH_TOKEN_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasRefreshToken";
	public static final String EXPIRES_TOKEN_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasExpiry";
	public static final String APINAMES_TOKEN_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasApiNames";
	public static final String USER_ID_PREDICATE = "http://carre.kmi.open.ac.uk/ontology/authentication#hasUserId";
	public static final String MEASURED_BY = "<http://carre.kmi.open.ac.uk/ontology#isMeasuredBy>";
	public static final String HAS_METRIC_TYPE = "<http://carre.kmi.open.ac.uk/ontology#hasMetricType>";
	public static final String HAS_MANUFACTURER = "http://carre.kmi.open.ac.uk/ontology/manufacturer#hasManufacturer";
	public static final String DATE_TYPE = "^^xsd:date";
	public static final String DOUBLE_TYPE = "^^xsd:double";
	public static final String FLOAT_TYPE = "^^xsd:float";
	public static final String LONG_TYPE = "^^xsd:long";
	public static final String INT_TYPE = "^^xsd:int";
	public static final String STRING_TYPE = "^^xsd:string";

	public static final String[] PREFICES = {"xsd: <http://www.w3.org/2001/XMLSchema#>",
		"carreSensors: <http://carre.kmi.open.ac.uk/ontology/sensors.owl#>",
	"carreManufacturer: <http://carre.kmi.open.ac.uk/manufacturers/>"};
	public static final String SENSOR_RDF_PREFIX = "carreSensors:";
	public static final String MANUFACTURER_RDF_PREFIX = "carreManufacturer:";


	public static final String REM_SLEEP = SENSOR_RDF_PREFIX + "REM_SLEEP";
	public static final String DEEP_SLEEP = SENSOR_RDF_PREFIX + "DEEP_SLEEP";
	public static final String LIGHT_SLEEP = SENSOR_RDF_PREFIX + "LIGHT_SLEEP";
	public static final String ASLEEP = SENSOR_RDF_PREFIX + "ASLEEP";
	public static final String RESTLESS = SENSOR_RDF_PREFIX + "RESTLESS";
	public static final String AWAKE = SENSOR_RDF_PREFIX + "AWAKE";

	public static final String ACTIVITY_METRIC = SENSOR_RDF_PREFIX + "activityMetric";
	public static final String BLOOD_PRESSURE_METRIC = SENSOR_RDF_PREFIX + "bloodPressureMetric";
	public static final String BODY_METRIC = SENSOR_RDF_PREFIX + "bodyMetric";
	public static final String FOOD_METRIC = SENSOR_RDF_PREFIX + "foodMetric";
	public static final String GLUCOSE_METRIC = SENSOR_RDF_PREFIX + "glucoseMetric";
	public static final String HEIGHT_METRIC = SENSOR_RDF_PREFIX + "heightMetric";
	public static final String O2SATURATION_METRIC = SENSOR_RDF_PREFIX + "O2saturationMetric";
	public static final String PULSE_METRIC = SENSOR_RDF_PREFIX + "pulseMetric";
	public static final String SLEEP_METRIC = SENSOR_RDF_PREFIX + "sleepMetric";
	public static final String SLEEP_RECORD_METRIC = SENSOR_RDF_PREFIX + "sleepRecordMetric";
	public static final String WEIGHT_METRIC = SENSOR_RDF_PREFIX + "weightMetric";
}
