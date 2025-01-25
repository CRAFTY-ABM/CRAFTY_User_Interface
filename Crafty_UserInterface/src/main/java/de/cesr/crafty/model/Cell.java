package de.cesr.crafty.model;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.paint.Color;

/**
 * @author Mohamed Byari
 *
 */

public class Cell extends AbstractCell {

	public Cell(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void ColorP() {
		ColorP(color);
	}

	public void ColorP(Color color) {
		CellsSet.pixelWriter.setColor(getX(), getY(), color);
	}

	// ----------------------------------//
	public double productivity(Manager a, String service) {
		if (a == null || !a.isInteract())
			return 0;
		double product = capitals.entrySet().stream()
				.mapToDouble(e -> Math.pow(e.getValue(), a.getSensitivity().get(e.getKey() + "_" + service)))
				.reduce(1.0, (x, y) -> x * y);
		return product * a.getProductivityLevel().get(service);
	}

	public void productivity(Service service) {
		if (owner == null || !owner.isInteract())
			return;
		double pr = 1.0;
		for (Map.Entry<String, Double> entry : capitals.entrySet()) {
			double value = Math.pow(entry.getValue(),
					owner.getSensitivity().get(entry.getKey() + "_" + service.getName()));
			pr *= value;
		}
		pr = pr * owner.getProductivityLevel().get(service.getName());

		currentProductivity.put(service.getName(), pr);
	}

	public void calculateCurrentProductivity(Region R) {
		currentProductivity.clear();
		R.getServicesHash().values().forEach(serviceName -> {
			productivity(serviceName);
		});
	}

	void giveUp(RegionalModelRunner r, ConcurrentHashMap<Manager, Double> distributionMean) {
		if (getOwner() != null && getOwner().isInteract()) {
			double utility = Competitiveness.utility(this, owner, r);
			double averageutility = distributionMean.get(getOwner());
			if ((utility < averageutility
					* (getOwner().getGiveUpMean() + getOwner().getGiveUpSD() * new Random().nextGaussian())
					&& getOwner().getGiveUpProbabilty() > Math.random())) {
				setOwner(null);
				r.R.getUnmanageCellsR().add(this);
			}
		}
	}

}
