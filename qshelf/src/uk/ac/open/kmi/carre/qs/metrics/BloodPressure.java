package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class BloodPressure extends Metric {
	private static Logger logger = Logger.getLogger(BloodPressure.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.BLOOD_PRESSURE_METRIC;
			
	protected long bloodPressureSystolic;
	protected long bloodPressureDiastolic;
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
		bloodPressureSystolic = NO_VALUE_PROVIDED;
		bloodPressureDiastolic = NO_VALUE_PROVIDED;
		whoBPLevel = NO_VALUE_PROVIDED;
		setArrhythmiaType("");
		setNote("");
	}

	public long getSystolicBloodPressure() {
		return bloodPressureSystolic;
	}

	public void setSystolicBloodPressure(long systolicBloodPressure) {
		this.bloodPressureSystolic = systolicBloodPressure;
	}

	public long getDiastolicBloodPressure() {
		return bloodPressureDiastolic;
	}

	public void setDiastolicBloodPressure(long diastolicBloodPressure) {
		this.bloodPressureDiastolic = diastolicBloodPressure;
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
