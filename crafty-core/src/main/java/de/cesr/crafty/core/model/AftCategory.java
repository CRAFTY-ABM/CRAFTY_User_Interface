package de.cesr.crafty.core.model;

public class AftCategory {

	private String name;
	private String intensity;

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

	@Override
	public String toString() {
		return "AftCategory [name=" + name + ", intensity=" + intensity + "]";
	}

}
