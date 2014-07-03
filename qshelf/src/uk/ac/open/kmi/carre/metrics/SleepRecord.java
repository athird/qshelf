package uk.ac.open.kmi.carre.metrics;

import java.util.Date;

public class SleepRecord extends Metric {

	public static int REM_SLEEP = 0;
	public static int DEEP_SLEEP = 1;
	public static int LIGHT_SLEEP = 2;
	public static int ASLEEP = 3;
	public static int RESTLESS = 4;
	public static int AWAKE = 5;
	
	protected Date startDate;
	protected Date endDate;
	protected int sleepStatus;
	
	public SleepRecord(String identifier) {
		super(identifier);
	}

	public SleepRecord(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setStartDate(null);
		setEndDate(null);
		setSleepStatus(NO_VALUE_PROVIDED);
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public int getSleepStatus() {
		return sleepStatus;
	}

	public void setSleepStatus(int sleepStatus) {
		this.sleepStatus = sleepStatus;
	}

}
