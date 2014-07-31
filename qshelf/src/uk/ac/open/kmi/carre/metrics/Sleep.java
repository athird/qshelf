package uk.ac.open.kmi.carre.metrics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Sleep extends Metric {
	
	protected List<SleepRecord> sleepRecords;
	protected long timesRemAsleep;
	protected long timesDeeplyAsleep;
	protected long timesLightlyAsleep;
	protected long timesAwake;
	protected long timesRestless;
	protected long timeToFallAsleep;
	protected long sleepTime;
	protected double sleepEfficiency;
	protected long awakeDuration;
	protected long asleepDuration;
	protected long restlessDuration;
	protected long remDuration;
	protected long deepSleepDuration;
	protected long lightSleepDuration;
	protected long minutesAfterWakeup;
	protected Date morningTime;
	
	public Sleep(String identifier) {
		super(identifier);
	}

	public Sleep(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setSleepRecords(new ArrayList<SleepRecord>());
		timesRemAsleep = NO_VALUE_PROVIDED;
		timesDeeplyAsleep = NO_VALUE_PROVIDED;
		timesLightlyAsleep = NO_VALUE_PROVIDED;
		timesAwake = NO_VALUE_PROVIDED;
		timesRestless = NO_VALUE_PROVIDED;
		timeToFallAsleep = NO_VALUE_PROVIDED;
		sleepTime = NO_VALUE_PROVIDED;
		sleepEfficiency = NO_VALUE_PROVIDED;
		awakeDuration = NO_VALUE_PROVIDED;
		asleepDuration = NO_VALUE_PROVIDED;
		restlessDuration = NO_VALUE_PROVIDED;
		remDuration = NO_VALUE_PROVIDED;
		setDeepSleepDuration(NO_VALUE_PROVIDED);
		lightSleepDuration = NO_VALUE_PROVIDED;
		minutesAfterWakeup = NO_VALUE_PROVIDED;
		morningTime = null;
		setNote("");
	}

	public long getTimesRemAsleep() {
		return timesRemAsleep;
	}

	public void setTimesRemAsleep(long timesRemAsleep) {
		this.timesRemAsleep = timesRemAsleep;
	}

	public long getTimesDeeplyAsleep() {
		return timesDeeplyAsleep;
	}

	public void setTimesDeeplyAsleep(long timesDeeplyAsleep) {
		this.timesDeeplyAsleep = timesDeeplyAsleep;
	}

	public long getTimesLightlyAsleep() {
		return timesLightlyAsleep;
	}

	public void setTimesLightlyAsleep(long timesLightlyAsleep) {
		this.timesLightlyAsleep = timesLightlyAsleep;
	}

	public long getTimesAwake() {
		return timesAwake;
	}

	public void setTimesAwake(long timesAwake) {
		this.timesAwake = timesAwake;
	}

	public long getTimesRestless() {
		return timesRestless;
	}

	public void setTimesRestless(long timesRestless) {
		this.timesRestless = timesRestless;
	}

	public long getTimeToFallAsleep() {
		return timeToFallAsleep;
	}

	public void setTimeToFallAsleep(long timeToFallAsleep) {
		this.timeToFallAsleep = timeToFallAsleep;
	}

	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	public double getSleepEfficiency() {
		return sleepEfficiency;
	}

	public void setSleepEfficiency(double sleepEfficiency) {
		this.sleepEfficiency = sleepEfficiency;
	}

	public long getAwakeDuration() {
		return awakeDuration;
	}

	public void setAwakeDuration(long awakeDuration) {
		this.awakeDuration = awakeDuration;
	}

	public long getAsleepDuration() {
		return asleepDuration;
	}

	public void setAsleepDuration(long asleepDuration) {
		this.asleepDuration = asleepDuration;
	}

	public long getRestlessDuration() {
		return restlessDuration;
	}

	public void setRestlessDuration(long restlessDuration) {
		this.restlessDuration = restlessDuration;
	}

	public long getRemDuration() {
		return remDuration;
	}

	public void setRemDuration(long remDuration) {
		this.remDuration = remDuration;
	}

	public long getDeepSleepDuration() {
		return deepSleepDuration;
	}

	public void setDeepSleepDuration(long deepSleepDuration) {
		this.deepSleepDuration = deepSleepDuration;
	}

	public long getLightSleepDuration() {
		return lightSleepDuration;
	}
	
	public void setLightSleepDuration(long lightSleepDuration) {
		this.lightSleepDuration = lightSleepDuration;
	}
	
	public void addSleepRecord(SleepRecord record) {
		getSleepRecords().add(record);
	}
	
	public List<SleepRecord> getSleepRecords() {
		return sleepRecords;
	}

	public void setSleepRecords(List<SleepRecord> sleepRecords) {
		this.sleepRecords = sleepRecords;
	}
	
	public long getMinutesAfterWakeup() {
		return minutesAfterWakeup;
	}

	public void setMinutesAfterWakeup(long minutesAfterWakeup) {
		this.minutesAfterWakeup = minutesAfterWakeup;
	}

	@Override
	public String toRDFString() {
		String rdf = super.toRDFString();
		for (SleepRecord record : getSleepRecords()) {
			rdf += record.toRDFString() + "\n";
		}
		return rdf;
	}
}
