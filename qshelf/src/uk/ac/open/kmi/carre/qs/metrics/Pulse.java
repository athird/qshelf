package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;


public class Pulse extends Metric {

	public static final String METRIC_TYPE = CARREVocabulary.PULSE_METRIC;
	
	protected long pulse;
	protected String arrhythmia;
	protected String condition;
	
	public Pulse(String identifier) {
		super(identifier);
	}
	
	public Pulse(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}
	
	@Override
	protected void initialiseEmpty() {
		setPulse(NO_VALUE_PROVIDED);
		setHasArrhythmia("");
		setCondition("");
		setNote("");
	}

	public long getPulse() {
		return pulse;
	}

	public void setPulse(long pulse) {
		this.pulse = pulse;
	}

	public String getHasArrhythmia() {
		return arrhythmia;
	}

	public void setHasArrhythmia(String hasArrhythmia) {
		this.arrhythmia = hasArrhythmia;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
}
