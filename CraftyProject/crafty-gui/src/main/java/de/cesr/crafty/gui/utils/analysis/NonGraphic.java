package de.cesr.crafty.gui.utils.analysis;

import java.util.ArrayList;

public class NonGraphic {

	public static ArrayList<Double> generateNormalData(double mean, double sd, double maxMean, double maxsd) {
		if (sd <= 0) {
			throw new IllegalArgumentException("Standard deviation must be positive.");
		}

		ArrayList<Double> pdfValues = new ArrayList<>();

		// Choose how wide around the mean you want to sample.
		// Here, we sample from [mean - 3σ, mean + 3σ] in 100 steps.
		double start = maxMean - 4 * maxsd;
		double end = maxMean + 4 * maxsd;
		int numSteps = 100;
		double stepSize = (end - start) / numSteps;

		for (int i = 0; i <= numSteps; i++) {
			double x = start + i * stepSize;
			// Probability Density Function (PDF) for Normal Distribution
			double pdf = (1.0 / (sd * Math.sqrt(2.0 * Math.PI))) * Math.exp(-0.5 * Math.pow((x - mean) / sd, 2));
			pdfValues.add(pdf);
		}

		return pdfValues;
	}
}
