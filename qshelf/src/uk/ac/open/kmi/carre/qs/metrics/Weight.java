package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateFormatUtils;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Weight extends Metric {
	private static Logger logger = Logger.getLogger(Weight.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.WEIGHT_METRIC;
	
	protected double weight;
	protected double bmi;
	protected double bodyFat;
	protected double leanMass;
	protected double muscleMass;
	protected double waterMass;
	protected double fatMass;
	protected double boneMass;
	protected double bodyDCI;
	protected Date lastChangeTime;
	
	public Weight(String identifier) {
		super(identifier);
	}
	
	public Weight(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}
	
	protected void initialiseEmpty() {
		weight = NO_VALUE_PROVIDED;
		bmi = NO_VALUE_PROVIDED;
		bodyFat = NO_VALUE_PROVIDED;
		leanMass = NO_VALUE_PROVIDED;
		muscleMass = NO_VALUE_PROVIDED;
		waterMass = NO_VALUE_PROVIDED;
		boneMass = NO_VALUE_PROVIDED;
		bodyDCI = NO_VALUE_PROVIDED;
		lastChangeTime = null;
		setNote("");
	}
	
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public double getBmi() {
		return bmi;
	}
	public void setBmi(double bmi) {
		this.bmi = bmi;
	}
	public double getBodyFat() {
		return bodyFat;
	}
	public void setBodyFat(double bodyFat) {
		this.bodyFat = bodyFat;
	}
	public double getLeanMass() {
		return leanMass;
	}
	public void setLeanMass(double leanMass) {
		this.leanMass = leanMass;
	}
	public double getMuscleMass() {
		return muscleMass;
	}
	public void setMuscleMass(double muscleMass) {
		this.muscleMass = muscleMass;
	}
	public double getWaterMass() {
		return waterMass;
	}
	public void setWaterMass(double waterMass) {
		this.waterMass = waterMass;
	}
	public double getBoneMass() {
		return boneMass;
	}
	public void setBoneMass(double boneMass) {
		this.boneMass = boneMass;
	}
	public double getBodyDCI() {
		return bodyDCI;
	}
	public void setBodyDCI(double bodyDCI) {
		this.bodyDCI = bodyDCI;
	}
	public Date getLastChangeTime() {
		return lastChangeTime;
	}
	public void setLastChangeTime(Date lastChangeTime) {
		this.lastChangeTime = lastChangeTime;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	public double getFatMass() {
		return fatMass;
	}

	public void setFatMass(double fatMass) {
		this.fatMass = fatMass;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
}
