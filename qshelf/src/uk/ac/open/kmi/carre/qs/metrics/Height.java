package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Height extends Metric {
	private static Logger logger = Logger.getLogger(Height.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.HEIGHT_METRIC;
	
	protected double height;
	
	public Height(String identifier) {
		super(identifier);
	}

	public Height(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setHeight(NO_VALUE_PROVIDED);
		setNote("");
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
	
}
