package de.cesr.crafty.core.crafty;

import java.util.concurrent.ConcurrentHashMap;

public class Service {

	private String name;
	private ConcurrentHashMap<Integer, Double> demands;
	private ConcurrentHashMap<Integer, Double> weights = new ConcurrentHashMap<>();
	private double calibration_Factor = 1;

	public Service(String name) {
		this.name = name;
		demands = new ConcurrentHashMap<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ConcurrentHashMap<Integer, Double> getDemands() {
		return demands;
	}

	public void setDemands(ConcurrentHashMap<Integer, Double> demands) {
		this.demands = demands;
	}

	public ConcurrentHashMap<Integer, Double> getWeights() {
		return weights;
	}

	public void setWeights(ConcurrentHashMap<Integer, Double> weights) {
		this.weights = weights;
	}

	public double getCalibration_Factor() {
		return calibration_Factor;
	}

	public void setCalibration_Factor(double calibration_Factor) {
		this.calibration_Factor = calibration_Factor;
	}


	@Override
	public String toString() {
		return "Service [name= " + name + ", calibration_Factor= " + calibration_Factor + ", Utility_Weight= " + weights
				+ "\n Demands= " + demands + "]";
	}

}
