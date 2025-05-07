package de.cesr.crafty.core.model;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.output.Listener;

/**
 * @author Mohamed Byari
 *
 */

public class Cell extends AbstractCell {

	public Cell(int x, int y) {
		this.x = x;
		this.y = y;
	}

	// ----------------------------------//
	public double productivity(Aft a, String service) {
		if (a == null || !a.isInteract())
			return 0;
		if (a.getSensitivity().size() == 0) {
			return 0;
		}
		double product = capitals.entrySet().stream()
				.mapToDouble(e -> Math.pow(e.getValue(), a.getSensitivity().get(e.getKey() + "|" + service)))
				.reduce(1.0, (x, y) -> x * y);
		return product * a.getProductivityLevel().get(service);
	}

	public void productivity(Service service) {
		if (owner == null || !owner.isInteract())
			return;
		double pr = 1.0;
		for (Map.Entry<String, Double> entry : capitals.entrySet()) {
			Double sensitivity = owner.getSensitivity().get(entry.getKey() + "|" + service.getName());
			if (sensitivity != null) {
				double value = Math.pow(entry.getValue(), sensitivity);
				pr *= value;
			} else {
				return;
			}
		}
		if (owner.getProductivityLevel().get(service.getName()) != null) {
			pr = pr * owner.getProductivityLevel().get(service.getName());
		} else {
			return;
		}

		currentProductivity.put(service.getName(), pr);
	}

	public void calculateCurrentProductivity(Region R) {
		currentProductivity.clear();
		R.getServicesHash().values().forEach(serviceName -> {
			productivity(serviceName);
		});
	}

	void giveUp(RegionalModelRunner r, ConcurrentHashMap<Aft, Double> distributionMean) {
		if (getOwner() != null && getOwner().isInteract()) {
//			if (ProjectLoader.getCurrentYear() > 2030) {
//				if (getOwner().getCategory().getName().equals("forest")) {
//					return;
//				}
//			}
			double utility = Competitiveness.utility(this, owner, r);
			double averageutility = distributionMean.get(getOwner());
			if ((utility < averageutility
					* (getOwner().getGiveUpMean() + getOwner().getGiveUpSD() * new Random().nextGaussian())
					&& getOwner().getGiveUpProbabilty() > Math.random())) {
				setOwner(null);
				r.R.getUnmanageCellsR().add(this);
				Listener.landUseChangeCounter.getAndIncrement();
			}
		}
	}

	// --------------------------
	public void copyCell(Cell cell) {
		cell.x = this.x;
		cell.y = this.y;
		cell.color = this.color;
		cell.CurrentRegion = this.CurrentRegion;
		cell.capitals = this.capitals;
		cell.id = this.id;
		cell.owner = this.owner;
	}

}
