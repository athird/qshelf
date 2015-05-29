package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class SleepRecord extends Metric {
	private static Logger logger = Logger.getLogger(SleepRecord.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.SLEEP_RECORD_METRIC;
	
	public static final int REM_SLEEP = 0;
	public static final int DEEP_SLEEP = 1;
	public static final int LIGHT_SLEEP = 2;
	public static final int ASLEEP = 3;
	public static final int RESTLESS = 4;
	public static final int AWAKE = 5;

	protected Date startDate;
	protected Date endDate;
	protected String sleepStatus;

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
		setSleepStatus("");
		setNote("");
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
		if (this.getId() != null && startDate != null) {
			this.setId(this.getId() + startDate.getTime());
		}
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getSleepStatus() {
		return sleepStatus;
	}

	public void setSleepStatus(String sleepStatus) {
		this.sleepStatus = sleepStatus;
	}

	public void setSleepStatus(int sleepState) {
		switch (sleepState) {
		case REM_SLEEP:
			this.sleepStatus = CARREVocabulary.REM_SLEEP;

			break;
		case DEEP_SLEEP:
			this.sleepStatus = CARREVocabulary.DEEP_SLEEP;

			break;
		case LIGHT_SLEEP:
			this.sleepStatus = CARREVocabulary.LIGHT_SLEEP;

			break;
		case ASLEEP:
			this.sleepStatus = CARREVocabulary.ASLEEP;

			break;
		case RESTLESS:
			this.sleepStatus = CARREVocabulary.RESTLESS;

			break;
		case AWAKE:
			this.sleepStatus = CARREVocabulary.AWAKE;

		default:
		}

	}
	
	public String getMetricType() {
		return METRIC_TYPE;
	}

}
