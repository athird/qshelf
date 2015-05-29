package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Temperature extends Metric {
	private static Logger logger = Logger.getLogger(Temperature.class.getName());

	public static final String METRIC_TYPE = CARREVocabulary.TEMPERATURE_METRIC;

	protected float temperature;
	
	public Temperature(String identifier) {
		super(identifier);
	}

	public Temperature(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setTemperature(NO_VALUE_PROVIDED);
	}

	public float getTemperature() {
		return temperature;
	}

	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	@Override
	public String getMetricType() {
		return METRIC_TYPE;
	}

}
