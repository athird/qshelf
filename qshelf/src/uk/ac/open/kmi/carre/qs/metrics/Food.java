package uk.ac.open.kmi.carre.qs.metrics;

import java.util.Date;
import java.util.logging.Logger;

import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;

public class Food extends Metric {
	private static Logger logger = Logger.getLogger(Food.class.getName());
	
	public static final String METRIC_TYPE = CARREVocabulary.FOOD_METRIC;
	
    protected double calories;
    protected double carbs;
    protected double fat;
	protected double fibre;
    protected double protein;
    protected double sodium;
    protected double water;
    protected double quantity;
    protected String foodType;
    protected String mealType;
    
	public Food(String identifier) {
		super(identifier);
	}

	public Food(String source, Date dateMeasured) {
		super(source, dateMeasured);
	}

	@Override
	protected void initialiseEmpty() {
		setCalories(NO_VALUE_PROVIDED);
	    setCarbs(NO_VALUE_PROVIDED);
	    setFat(NO_VALUE_PROVIDED);
	    setFibre(NO_VALUE_PROVIDED);
	    setProtein(NO_VALUE_PROVIDED);
	    setSodium(NO_VALUE_PROVIDED);
	    setWater(NO_VALUE_PROVIDED);
	    setQuantity(NO_VALUE_PROVIDED);
	    setMealType("");
	    setFoodType("");
	    setNote("");
	}

	public double getCalories() {
		return calories;
	}

	public void setCalories(double calories) {
		this.calories = calories;
	}

	public double getCarbs() {
		return carbs;
	}

	public void setCarbs(double carbs) {
		this.carbs = carbs;
	}

    public double getFat() {
		return fat;
	}

	public void setFat(double fat) {
		this.fat = fat;
	}

	public double getFibre() {
		return fibre;
	}

	public void setFibre(double fibre) {
		this.fibre = fibre;
	}

	public double getProtein() {
		return protein;
	}

	public void setProtein(double protein) {
		this.protein = protein;
	}

	public double getSodium() {
		return sodium;
	}

	public void setSodium(double sodium) {
		this.sodium = sodium;
	}

	public double getWater() {
		return water;
	}

	public void setWater(double water) {
		this.water = water;
	}
	
	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	public String getFoodType() {
		return foodType;
	}

	public void setFoodType(String foodType) {
		this.foodType = foodType;
	}

	public String getMealType() {
		return mealType;
	}

	public void setMealType(String mealType) {
		this.mealType = mealType;
	}

	public String getMetricType() {
		return METRIC_TYPE;
	}
}
