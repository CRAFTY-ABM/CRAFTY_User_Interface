package de.cesr.crafty.core.updaters;

import java.util.HashMap;
import java.util.Map;

import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;

/**
 * @author Mohamed Byari
 *
 */
public class ServicesUpdater extends AbstractUpdater {
	private static final CustomLogger LOGGER = new CustomLogger(ServicesUpdater.class);

	private static Map<String, Map<String, Double>> demandByRegions = new HashMap<>();
	private static Map<String, Map<String, Double>> weightByRegions = new HashMap<>();
	private static Map<String, Double> worldDemand = new HashMap<>();
	private static Map<String, Double> weightDemand = new HashMap<>();

//	public ServicesUpdater() {
//		ServiceSet.loadServiceList();
//	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		// update the service demand + weight (by regions and world service)
		demandByRegions.clear();
		worldDemand.clear();
		weightByRegions.clear();
		weightDemand.clear();
		CellsLoader.regions.forEach((regionName, r) -> {
			Map<String, Double> demand = new HashMap<>();
			Map<String, Double> weight = new HashMap<>();
			r.getServicesHash().forEach((serviceName, service) -> {
				demand.put(serviceName, service.getDemands().get(Timestep.getCurrentYear()));
				weight.put(serviceName, service.getWeights().get(Timestep.getCurrentYear()));
			});
			demandByRegions.put(regionName, demand);
			weightByRegions.put(regionName, weight);
		});

		ServiceSet.worldService.forEach((serviceName, service) -> {
			worldDemand.put(serviceName, service.getDemands().get(Timestep.getCurrentYear()));
			worldDemand.put(serviceName, service.getWeights().get(Timestep.getCurrentYear()));
		});

		LOGGER.trace("Update_demandByRegions: " + demandByRegions);
		LOGGER.trace("Update_weightByRegions: " + weightByRegions);
		LOGGER.trace("Update_worldDemand: " + worldDemand);
		LOGGER.trace("Update_weightDemand: " + weightDemand);

	}

	public static Map<String, Map<String, Double>> getDemandByRegions() {
		return demandByRegions;
	}

	public static Map<String, Double> getWorldDemand() {
		return worldDemand;
	}

	public static Map<String, Map<String, Double>> getWeightByRegions() {
		return weightByRegions;
	}

	public static Map<String, Double> getWeightDemand() {
		return weightDemand;
	}

}
