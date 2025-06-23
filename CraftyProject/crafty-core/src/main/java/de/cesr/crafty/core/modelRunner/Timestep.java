package de.cesr.crafty.core.modelRunner;


public class Timestep {
	private static int startYear;
	private static int endtYear;
	private static int currentYear;
	
	public static int getStartYear() {
		return startYear;
	}

	public static void setStartYear(int startYear) {
		Timestep.startYear = startYear;
	}

	public static int getEndtYear() {
		return endtYear;
	}

	public static void setEndtYear(int endtYear) {
		Timestep.endtYear = endtYear;
	}

	public static int getCurrentYear() {
		return currentYear;
	}

	public static void setCurrentYear(int currentYear) {
		Timestep.currentYear = currentYear;
	}
}
