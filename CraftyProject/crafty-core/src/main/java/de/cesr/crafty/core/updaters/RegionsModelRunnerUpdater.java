package de.cesr.crafty.core.updaters;

import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.crafty.RegionalModelRunner;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.modelRunner.Timestep;

public class RegionsModelRunnerUpdater extends AbstractUpdater {

	public static ConcurrentHashMap<String, RegionalModelRunner> regionsModelRunner;

	public RegionsModelRunnerUpdater() {
		regionsModelRunner= new ConcurrentHashMap<>();
		CellsLoader.regions.keySet().forEach(regionName -> {
			regionsModelRunner.put(regionName, new RegionalModelRunner(regionName));
		});
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.step(Timestep.getCurrentYear());
		});
		AFTsLoader.hashAgentNbrRegions();
		AFTsLoader.hashAgentNbr();
	}

}
