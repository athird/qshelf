package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Glucose extends Metric {
	private static Logger logger = Logger.getLogger(Glucose.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.GLUCOSE_METRIC;
	
	protected double glucose;
	protected double hbA1c;
	protected String dinnerSituation;
	protected String drugSituation; 
	
	public Glucose(String identifier) {
		super(identifier);
	}

	public Glucose(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setGlucose(NO_VALUE_PROVIDED);
		setHbA1c(NO_VALUE_PROVIDED);
		setDinnerSituation("");
		setDrugSituation("");
		setNote("");
	}

	public double getGlucose() {
		return glucose;
	}

	public void setGlucose(double glucose) {
		this.glucose = glucose;
	}

	public double getHbA1c() {
		return hbA1c;
	}

	public void setHbA1c(double hbA1c) {
		this.hbA1c = hbA1c;
	}

	public String getDinnerSituation() {
		return dinnerSituation;
	}

	public void setDinnerSituation(String dinnerSituation) {
		this.dinnerSituation = dinnerSituation;
	}

	public String getDrugSituation() {
		return drugSituation;
	}

	public void setDrugSituation(String drugSituation) {
		this.drugSituation = drugSituation;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
	
}
