package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class BloodPressure extends Metric {

	public static final String METRIC_TYPE = CARREVocabulary.BLOOD_PRESSURE_METRIC;
			
	protected long systolicBloodPressure;
	protected long diastolicBloodPressure;
	protected String arrhythmiaType;
	protected int whoBPLevel;
	
	public BloodPressure(String identifier) {
		super(identifier);
	}

	public BloodPressure(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		systolicBloodPressure = NO_VALUE_PROVIDED;
		diastolicBloodPressure = NO_VALUE_PROVIDED;
		whoBPLevel = NO_VALUE_PROVIDED;
		setArrhythmiaType("");
		setNote("");
	}

	public long getSystolicBloodPressure() {
		return systolicBloodPressure;
	}

	public void setSystolicBloodPressure(long systolicBloodPressure) {
		this.systolicBloodPressure = systolicBloodPressure;
	}

	public long getDiastolicBloodPressure() {
		return diastolicBloodPressure;
	}

	public void setDiastolicBloodPressure(long diastolicBloodPressure) {
		this.diastolicBloodPressure = diastolicBloodPressure;
	}

	public String getArrhythmiaType() {
		return arrhythmiaType;
	}

	public void setArrhythmiaType(String arrhythmiaType) {
		this.arrhythmiaType = arrhythmiaType;
	}

	public int getWhoBPLevel() {
		return whoBPLevel;
	}

	public void setWhoBPLevel(int whoBPLevel) {
		this.whoBPLevel = whoBPLevel;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
	
}
