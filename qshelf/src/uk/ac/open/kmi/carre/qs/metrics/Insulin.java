package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Insulin extends Metric {

private static Logger logger = Logger.getLogger(Insulin.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.INSULIN_METRIC;
	protected float insulin;
	protected String insulinType;
	
	public Insulin(String identifier) {
		super(identifier);
	}

	public Insulin(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setInsulin(NO_VALUE_PROVIDED);
		setInsulinType("");
	}

	public float getInsulin() {
		return insulin;
	}

	public void setInsulin(float insulin) {
		this.insulin = insulin;
	}

	public String getInsulinType() {
		return insulinType;
	}

	public void setInsulinType(String insulinType) {
		this.insulinType = insulinType;
	}

	@Override
	public String getMetricType() {
		return METRIC_TYPE;
	}

}
