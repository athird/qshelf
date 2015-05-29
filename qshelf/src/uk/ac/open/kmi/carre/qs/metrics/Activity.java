package uk.ac.open.kmi.carre.qs.metrics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateFormatUtils;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Activity extends Metric {
	private static Logger logger = Logger.getLogger(Activity.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.ACTIVITY_METRIC;
	
	protected int steps;
	protected float distance;
	protected float caloriesMetabolised;
	protected float caloriesBMR;
	protected float activityCalories;
	protected float marginalCalories;
	protected float elevation;
	protected int floors;
	protected int sedentaryActivityDuration;
	protected int lightActivityDuration;
	protected int moderateActivityDuration;
	protected int intenseActivityDuration;
	protected float sedentaryActivityDistance;
	protected float lightActivityDistance;
	protected float moderateActivityDistance;
	protected float intenseActivityDistance;
	protected float loggedActivityDistance;
	protected float trackedActivityDistance;
	protected long loggedActivityDuration;
	protected String loggedActivityName;
	protected String timezone;
	protected float activityPoints;

	public float getActivityPoints() {
		return activityPoints;
	}

	public void setActivityPoints(float activityPoints) {
		this.activityPoints = activityPoints;
	}

	public Activity(String identifier) {
		super(identifier);
	}

	public Activity(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	protected void initialiseEmpty() {
		setSteps(NO_VALUE_PROVIDED);
		setDistance(NO_VALUE_PROVIDED);
		setCalories(NO_VALUE_PROVIDED);
		setCaloriesBMR(NO_VALUE_PROVIDED);
		setActivityCalories(NO_VALUE_PROVIDED);
		setMarginalCalories(NO_VALUE_PROVIDED);
		setElevation(NO_VALUE_PROVIDED);
		setFloors(NO_VALUE_PROVIDED);
		setSedentaryActivityDuration(NO_VALUE_PROVIDED);
		setLightActivityDuration(NO_VALUE_PROVIDED);
		setModerateActivityDuration(NO_VALUE_PROVIDED);
		setIntenseActivityDuration(NO_VALUE_PROVIDED);
		setSedentaryActivityDistance(NO_VALUE_PROVIDED);
		setLightActivityDistance(NO_VALUE_PROVIDED);
		setModerateActivityDistance(NO_VALUE_PROVIDED);
		setIntenseActivityDistance(NO_VALUE_PROVIDED);
		setTrackedActivityDistance(NO_VALUE_PROVIDED);
		setLoggedActivityDuration(NO_VALUE_PROVIDED);
		setLoggedActivityName("");
		setActivityPoints(NO_VALUE_PROVIDED);
		setNote("");
		setTimezone("");
		
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getSteps() {
		return steps;
	}

	public void setSteps(int steps) {
		this.steps = steps;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public float getCalories() {
		return caloriesMetabolised;
	}

	public void setCalories(float calories) {
		this.caloriesMetabolised = calories;
	}

	public float getCaloriesBMR() {
		return caloriesBMR;
	}

	public void setCaloriesBMR(float caloriesBMR) {
		this.caloriesBMR = caloriesBMR;
	}

	public float getActivityCalories() {
		return activityCalories;
	}

	public void setActivityCalories(float activityCalories) {
		this.activityCalories = activityCalories;
	}

	public float getMarginalCalories() {
		return marginalCalories;
	}

	public void setMarginalCalories(float marginalCalories) {
		this.marginalCalories = marginalCalories;
	}

	public float getElevation() {
		return elevation;
	}

	public void setElevation(float elevation) {
		this.elevation = elevation;
	}

	public int getFloors() {
		return floors;
	}

	public void setFloors(int floors) {
		this.floors = floors;
	}

	public int getSedentaryActivityDuration() {
		return sedentaryActivityDuration;
	}

	public void setSedentaryActivityDuration(int sedentaryActivityDuration) {
		this.sedentaryActivityDuration = sedentaryActivityDuration;
	}

	public int getLightActivityDuration() {
		return lightActivityDuration;
	}

	public void setLightActivityDuration(int lightActivityDuration) {
		this.lightActivityDuration = lightActivityDuration;
	}

	public int getModerateActivityDuration() {
		return moderateActivityDuration;
	}

	public void setModerateActivityDuration(int moderateActivityDuration) {
		this.moderateActivityDuration = moderateActivityDuration;
	}

	public int getIntenseActivityDuration() {
		return intenseActivityDuration;
	}

	public void setIntenseActivityDuration(int intenseActivityDuration) {
		this.intenseActivityDuration = intenseActivityDuration;
	}

	public float getSedentaryActivityDistance() {
		return sedentaryActivityDistance;
	}

	public void setSedentaryActivityDistance(float sedentaryActivityDistance) {
		this.sedentaryActivityDistance = sedentaryActivityDistance;
	}

	public float getLightActivityDistance() {
		return lightActivityDistance;
	}

	public void setLightActivityDistance(float lightActivityDistance) {
		this.lightActivityDistance = lightActivityDistance;
	}

	public float getModerateActivityDistance() {
		return moderateActivityDistance;
	}

	public void setModerateActivityDistance(float moderateActivityDistance) {
		this.moderateActivityDistance = moderateActivityDistance;
	}

	public float getIntenseActivityDistance() {
		return intenseActivityDistance;
	}

	public void setIntenseActivityDistance(float intenseActivityDistance) {
		this.intenseActivityDistance = intenseActivityDistance;
	}

	public float getLoggedActivityDistance() {
		return loggedActivityDistance;
	}

	public void setLoggedActivityDistance(float loggedActivityDistance) {
		this.loggedActivityDistance = loggedActivityDistance;
	}

	public float getTrackedActivityDistance() {
		return trackedActivityDistance;
	}

	public void setTrackedActivityDistance(float trackedActivityDistance) {
		this.trackedActivityDistance = trackedActivityDistance;
	}

	public long getLoggedActivityDuration() {
		return loggedActivityDuration;
	}

	public void setLoggedActivityDuration(long loggedActivityDuration) {
		this.loggedActivityDuration = loggedActivityDuration;
	}

	public String getLoggedActivityName() {
		return loggedActivityName;
	}

	public void setLoggedActivityName(String loggedActivityName) {
		this.loggedActivityName = loggedActivityName;
	}
	
	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
	
	public String getMetricType() {
		return METRIC_TYPE;
	}
	

}
