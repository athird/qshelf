package uk.ac.open.kmi.carre.metrics;

import java.util.Date;

public class Height extends Metric {

	protected double height;
	
	public Height(String identifier) {
		super(identifier);
	}

	public Height(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setHeight(0);
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

}
