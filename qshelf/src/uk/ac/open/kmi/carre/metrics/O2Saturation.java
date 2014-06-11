package uk.ac.open.kmi.carre.metrics;

import java.util.Date;

public class O2Saturation extends Metric {

	private double o2saturation;
	
	public O2Saturation(String identifier) {
		super(identifier);
	}

	public O2Saturation(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setO2saturation(NO_VALUE_PROVIDED);
	}

	public double getO2saturation() {
		return o2saturation;
	}

	public void setO2saturation(double o2saturation) {
		this.o2saturation = o2saturation;
	}

}
