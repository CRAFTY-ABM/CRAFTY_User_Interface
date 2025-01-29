package de.cesr.crafty.model;

import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.cli.Config;
import de.cesr.crafty.cli.ConfigLoader;
import de.cesr.crafty.controller.fxml.MasksPaneController;
import de.cesr.crafty.controller.fxml.TabPaneController;
import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.output.Listener;
import de.cesr.crafty.utils.analysis.Tracker;

/**
 * @author Mohamed Byari
 *
 */

public class ModelRunner {
//	private static final CustomLogger LOGGER = new CustomLogger(ModelRunner.class);
	public String colorDisplay = "AFT";
	public ConcurrentHashMap<String, Double> totalSupply;
	public static ConcurrentHashMap<String, RegionalModelRunner> regionsModelRunner;
	public static Listener listner = new Listener();

	public static void setup() {
		regionsModelRunner = new ConcurrentHashMap<>();
		RegionClassifier.initialation();
		AFTsLoader.hashAgentNbrRegions();
//		S_WeightLoader.updateWorldWeight();
		RegionClassifier.regions.keySet().forEach(regionName -> {
			regionsModelRunner.put(regionName, new RegionalModelRunner(regionName));
		});
		listner.initializeListeners();
	}

	public void step() {
		int year = Math.min(Math.max(PathsLoader.getCurrentYear(), PathsLoader.getStartYear()),
				PathsLoader.getEndtYear());

		totalSupply = new ConcurrentHashMap<>();
		TabPaneController.cellsLoader.updateCapitals(year);
		AFTsLoader.updateAFTs();
		MasksPaneController.Maskloader.CellSetToMaskLoader(year);
		aggregateTotalSupply();
		regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.step(year);
		});
		listnerOutput(year);
		mapSynchronisation();
		AFTsLoader.hashAgentNbr();
	}

	private void mapSynchronisation() {
		if (Config.mapSynchronisation
				&& ((PathsLoader.getCurrentYear() - PathsLoader.getStartYear()) % Config.mapSynchronisationGap == 0
						|| PathsLoader.getCurrentYear() == PathsLoader.getEndtYear())) {
			CellsSet.colorMap(colorDisplay);
		}
	}

	private void listnerOutput(int year) {
		if (ConfigLoader.config.generate_csv_files) {
			Tracker.trackSupply(year);
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
				RegionalRunner.listner.DSEquilibriumListener[i + 1][1] = s.getCalibration_Factor() + "";
			});
		});
	}
}
