package de.cesr.crafty.core.model;

import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.utils.analysis.Tracker;

/**
 * @author Mohamed Byari
 *
 */

public class ModelRunner {
//	private static final CustomLogger LOGGER = new CustomLogger(ModelRunner.class);

	public ConcurrentHashMap<String, Double> totalSupply;
	public static ConcurrentHashMap<String, RegionalModelRunner> regionsModelRunner;
	public static Listener listner = new Listener();

	public static void setup() {
		regionsModelRunner = new ConcurrentHashMap<>();
		RegionClassifier.initialation();
		AFTsLoader.hashAgentNbrRegions();
		//S_WeightLoader.updateWorldWeight();
		RegionClassifier.regions.keySet().forEach(regionName -> {
			regionsModelRunner.put(regionName, new RegionalModelRunner(regionName));
		});
		listner.initializeListeners();
	}

	public void step() {
		int year = Math.min(Math.max(ProjectLoader.getCurrentYear(), ProjectLoader.getStartYear()),
				ProjectLoader.getEndtYear());
		totalSupply = new ConcurrentHashMap<>();
		Listener.landUseChangeCounter.set(0);
		ProjectLoader.cellsSet.updateCapitals(year);
		AFTsLoader.updateAFTs();
		ProjectLoader.Maskloader.CellSetToMaskLoader(year);
		aggregateTotalSupply();
		Tracker.trackSupply(year);
		listnerOutput(year);
		regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.step(year);
		});
		Listener.updateLandUseEventConter();
		AFTsLoader.hashAgentNbr();
	}

	private void listnerOutput(int year) {
		if (ConfigLoader.config.generate_output_files) {
			listner.compositionAFT(year);
			listner.outPutserviceDemandToCsv(year, totalSupply);
			listner.writOutPutMap(year);
			listner.updateCSVFilesWolrd();
		}
	}

	private void aggregateTotalSupply() {
		regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.regionalSupply();
			RegionalRunner.regionalSupply.forEach((key, value) -> totalSupply.merge(key, value, Double::sum));
		});
	}

	public static void demandEquilibrium() {
		if (ConfigLoader.config.initial_demand_supply_equilibrium) {
			RegionalDemandEquilibrium_calculation();
			ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
				RegionalRunner.R.getServicesHash().forEach((ns, s) -> {
					s.getDemands().forEach((year, v) -> {
						s.getDemands().put(year, v / s.getCalibration_Factor());
					});
				});
			});
			initialTotalDSEquilibriumListrner();
			RegionClassifier.aggregateDemandToWorldServiceDemand();
		}
	}

	private static void RegionalDemandEquilibrium_calculation() {
		ProjectLoader.cellsSet.updateCapitals(ProjectLoader.getStartYear());
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.initialDSEquilibriumFactorCalculation();
		});
	}

	private static void initialTotalDSEquilibriumListrner() {
		ServiceSet.worldService.forEach((serviceName, service) -> {
			ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
				Service s = RegionalRunner.R.getServicesHash().get(serviceName);
				int i = ServiceSet.getServicesList().indexOf(serviceName);
				RegionalRunner.listner.DSEquilibriumListener[i + 1][0] = serviceName;
				RegionalRunner.listner.DSEquilibriumListener[i + 1][1] = String.valueOf(s.getCalibration_Factor());
			});
		});
	}
}
