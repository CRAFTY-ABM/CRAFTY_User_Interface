package de.cesr.crafty.core.model;

import java.util.Collection;
import java.util.function.Predicate;

import de.cesr.crafty.core.dataLoader.AftCategorised;
import de.cesr.crafty.core.dataLoader.CellBehaviourLoader;

/**
 * @author Mohamed Byari
 *
 */
public class CellBehaviour {

	double Attitude_intensification = 0;
	double Weight_inertia = 0.2;
	double weight_social = 0.5;
	double Critical_mass = 0.5;
	int neighborhood_size = 2;
	double steepness_logistic_eq = 7;
	double maxGive_in = 0.5;
	private Cell c;

	public CellBehaviour(Cell c) {
		this.c = c;
	}

	public double give_In(Aft competitor) {
		return maxGive_in / (1 + Math.exp(steepness_logistic_eq * ((1 - weight_social) * Attitude_influence(competitor)
				+ weight_social * social_influence(competitor))));
	}

	private double social_influence(Aft competitor) {
		if (AftCategorised.useCategorisationGivIn && CellBehaviourLoader.behaviourUsed) {
			if (c.owner.category.getName().equals(competitor.category.getName())) {
				if (c.owner.category.getIntensity().equals(competitor.category.getIntensity())) {
					return 0;
				}
				if (competitor.category.getIntensityLevel() > c.owner.category.getIntensityLevel()) {
					return Math.max(Math.min(2 * fractionOfNeighbors(
							neighbor -> neighbor.category.getIntensityLevel() > competitor.category.getIntensityLevel())
							- CellBehaviourLoader.cellsBehevoir.get(c).getCritical_mass(), 1), -1);
				} else {
					return Math.max(Math.min(2 * fractionOfNeighbors(
							neighbor -> neighbor.category.getIntensityLevel() < competitor.category.getIntensityLevel())
							- CellBehaviourLoader.cellsBehevoir.get(c).getCritical_mass(), 1), -1);
				}
			}
		}
		return 0;
	}

	private double fractionOfNeighbors(Predicate<Aft> neighborCondition) {
		Collection<Aft> neighbors = CellsSubSets.detectExtendedNeighboringAFTs(c, neighborhood_size);
		int count = 0;
		for (Aft neighbor : neighbors) {
			if (neighbor.getCategory().getName().equals(c.owner.getCategory().getName())
					&& neighborCondition.test(neighbor)) {
				count++;
			}
		}
		return neighbors.isEmpty() ? 0.0 : (double) count / neighbors.size();
	}

	private double Attitude_influence(Aft competitor) {
		int intesificationGap = competitor.getCategory().getIntensityLevel()
				- c.owner.getCategory().getIntensityLevel();
		return Math.max(Math.min(Math.signum(intesificationGap) * Attitude_intensification
				+ Weight_inertia * Math.abs(intesificationGap), 1), -1);
	}

	public double getAttitude_intensification() {
		return Attitude_intensification;
	}

	public void setAttitude_intensification(double attitude_intensification) {
		Attitude_intensification = attitude_intensification;
	}

	public double getWeight_inertia() {
		return Weight_inertia;
	}

	public void setWeight_inertia(double weight_inertia) {
		Weight_inertia = weight_inertia;
	}

	public double getWeight_social() {
		return weight_social;
	}

	public void setWeight_social(double weight_social) {
		this.weight_social = weight_social;
	}

	public double getCritical_mass() {
		return Critical_mass;
	}

	public void setCritical_mass(double critical_mass) {
		Critical_mass = critical_mass;
	}

	public int getNeighborhood_size() {
		return neighborhood_size;
	}

	public void setNeighborhood_size(int neighborhood_size) {
		this.neighborhood_size = neighborhood_size;
	}

	public double getMaxGive_in() {
		return maxGive_in;
	}

	public void setMaxGive_in(double maxGive_in) {
		this.maxGive_in = maxGive_in;
	}

	@Override
	public String toString() {
		return "CellBehaviour [Attitude_intensification=" + Attitude_intensification + ", Weight_inertia="
				+ Weight_inertia + ", weight_social=" + weight_social + ", Critical_mass=" + Critical_mass
				+ ", neighborhood_size=" + neighborhood_size + ", steepness_logistic_eq=" + steepness_logistic_eq
				+ ", maxGive_in=" + maxGive_in + "]";
	}

}
