package de.cesr.crafty.core.crafty;

import java.util.concurrent.ConcurrentHashMap;

//import javafx.scene.paint.Color;

/**
 * @author Mohamed Byari
 *
 */
public abstract class AbstractCell {
	static int size = 1;
	String id;
	int x;
	int y;
	ConcurrentHashMap<String, Double> capitals = new ConcurrentHashMap<>();
	ConcurrentHashMap<String, Double> currentProductivity = new ConcurrentHashMap<>();
	double utilityValue = 0;
	String CurrentRegion;
	Aft owner;
	protected String color = "#848484";
	private String maskType;

	public String getCurrentRegion() {
		return CurrentRegion;
	}

	public void setCurrentRegion(String currentRegion) {
		CurrentRegion = currentRegion;
	}

	public double getUtilityValue() {
		return utilityValue;
	}

	public void setUtilityValue(double utilityValue) {
		this.utilityValue = utilityValue;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getMaskType() {
		return maskType;
	}

	public void setMaskType(String maskType) {
		this.maskType = maskType;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Aft getOwner() {
		return owner;
	}

	public void setOwner(Aft owner) {
		this.owner = owner;
	}

	public ConcurrentHashMap<String, Double> getCapitals() {
		return capitals;
	}

	public double getOneCapitals(String capitalName) {
		return capitals.get(capitalName);
	}

	public void setOneCapitals(String capitalName, double value) {
		capitals.put(capitalName, value);
	}

	public ConcurrentHashMap<String, Double> getCurrentProductivity() {
		return currentProductivity;
	}

	public static int getSize() {
		return size;
	}

	public static void setSize(int size) {
		AbstractCell.size = size;
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "\n Cell [index=" + id + ", x=" + x + ", y=" + y + ", CurrentRegion=" + CurrentRegion + "\n, Mask="
				+ getMaskType() + ", getOwner()=" + (getOwner() != null ? getOwner().getLabel() : " null")
				+ " category: " + (getOwner() != null ? getOwner().category : "null") + ", Color: " + color
				+ ", getCapitals()=" + getCapitals() + ", getCurrentProductivity()=" + getCurrentProductivity()
				+ "] \n "
		/* + CellBehaviourLoader.cellsBehevoir.get(this) */;
	}

}
