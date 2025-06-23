package de.cesr.crafty.core.crafty;

public class AftCategory {

	private String name;
	private String intensity="-";
	private int intensityLevel= 0;

	public AftCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIntensity() {
		return intensity;
	}

	public void setIntensity(String intensity) {
		this.intensity = intensity;
	}

	public int getIntensityLevel() {
		return intensityLevel;
	}

	public void setIntensityLevel(int intensityLevel) {
		this.intensityLevel = intensityLevel;
	}

	@Override
	public String toString() {
		return "AftCategory [name=" + name + ", intensity=" + intensity + ", intensityLevel=" + intensityLevel + "]";
	}

	
}
