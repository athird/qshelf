package uk.ac.open.kmi.carre.metrics;

import java.util.Date;


public class Pulse extends Metric {

	private long pulse;
	private String arrhythmia;
	private String condition;
	
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

}
