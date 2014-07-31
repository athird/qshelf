package uk.ac.open.kmi.carre.metrics;

import java.util.Date;

public class BloodPressure extends Metric {

	protected long systolicBloodPressure;
	protected long diastolicBloodPressure;
	
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

}
