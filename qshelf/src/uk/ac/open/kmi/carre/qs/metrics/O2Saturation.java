package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class O2Saturation extends Metric {
	private static Logger logger = Logger.getLogger(O2Saturation.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.O2SATURATION_METRIC;
	
	protected double o2saturation;
	
	public O2Saturation(String identifier) {
		super(identifier);
	}

	public O2Saturation(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setO2saturation(NO_VALUE_PROVIDED);
		setNote("");
	}

	public double getO2saturation() {
		return o2saturation;
	}

	public void setO2saturation(double o2saturation) {
		this.o2saturation = o2saturation;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
}
