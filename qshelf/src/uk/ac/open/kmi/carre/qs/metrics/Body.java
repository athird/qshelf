package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Body extends Metric {

	public static final String METRIC_TYPE = CARREVocabulary.BODY_METRIC;
			
	protected double bicep;
	protected double calf;
	protected double chest;
	protected double forearm;
	protected double hips;
	protected double neck;
	protected double thigh;
	protected double waist;
	
	public Body(String identifier) {
		super(identifier);
	}

	public Body(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		bicep = NO_VALUE_PROVIDED;
		calf = NO_VALUE_PROVIDED;
		chest = NO_VALUE_PROVIDED;
		forearm = NO_VALUE_PROVIDED;
		hips = NO_VALUE_PROVIDED;
		neck = NO_VALUE_PROVIDED;
		thigh = NO_VALUE_PROVIDED;
		waist = NO_VALUE_PROVIDED;
		setNote("");
	}

	public double getBicep() {
		return bicep;
	}

	public void setBicep(double bicep) {
		this.bicep = bicep;
	}

	public double getCalf() {
		return calf;
	}

	public void setCalf(double calf) {
		this.calf = calf;
	}

	public double getChest() {
		return chest;
	}

	public void setChest(double chest) {
		this.chest = chest;
	}

	public double getForearm() {
		return forearm;
	}

	public void setForearm(double forearm) {
		this.forearm = forearm;
	}

	public double getHips() {
		return hips;
	}

	public void setHips(double hips) {
		this.hips = hips;
	}

	public double getNeck() {
		return neck;
	}

	public void setNeck(double neck) {
		this.neck = neck;
	}

	public double getThigh() {
		return thigh;
	}

	public void setThigh(double thigh) {
		this.thigh = thigh;
	}

	public double getWaist() {
		return waist;
	}

	public void setWaist(double waist) {
		this.waist = waist;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
}
