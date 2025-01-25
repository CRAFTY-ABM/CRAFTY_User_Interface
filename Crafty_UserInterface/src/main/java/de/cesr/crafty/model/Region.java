package de.cesr.crafty.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Region {
	private String name;
	private ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
	private Set<Cell> unmanageCellsR = ConcurrentHashMap.newKeySet();
	private ConcurrentHashMap<String, Service> servicesHash = new ConcurrentHashMap<>();;

	public Region(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ConcurrentHashMap<String, Cell> getCells() {
		return cells;
	}

	public void setCells(ConcurrentHashMap<String, Cell> cells) {
		this.cells = cells;
	}

	public Set<Cell> getUnmanageCellsR() {
		return unmanageCellsR;
	}

	public ConcurrentHashMap<String, Service> getServicesHash() {
		return servicesHash;
	}

	public ConcurrentMap<String, Double> getServiceCalibration_Factor() {
		return getServicesHash().entrySet().stream().collect(
				Collectors.toConcurrentMap(Map.Entry::getKey, entry -> entry.getValue().getCalibration_Factor()));
	}
}
