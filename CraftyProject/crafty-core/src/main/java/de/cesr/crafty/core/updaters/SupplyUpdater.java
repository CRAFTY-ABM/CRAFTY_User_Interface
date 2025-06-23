package de.cesr.crafty.core.updaters;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Mohamed Byari
 *
 */
public class SupplyUpdater extends AbstractUpdater {
	public static ConcurrentHashMap<String, Double> totalSupply = new ConcurrentHashMap<>();

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		totalSupply.clear();
		RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.regionalSupply();
			RegionalRunner.getRegionalSupply().forEach((key, value) -> totalSupply.merge(key, value, Double::sum));
		});
	}
}
