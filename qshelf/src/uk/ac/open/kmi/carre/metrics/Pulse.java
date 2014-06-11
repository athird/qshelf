package uk.ac.open.kmi.carre.metrics;

import java.util.Calendar;
import java.util.Date;

public class Pulse extends Metric {

	protected long pulse;
	
	public Pulse(String identifier) {
		super(identifier);
	}
	
	public Pulse(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}
	
	@Override
	protected void initialiseEmpty() {
		pulse = 0;
	}

}
