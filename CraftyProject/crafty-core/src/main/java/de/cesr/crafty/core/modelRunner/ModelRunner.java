package de.cesr.crafty.core.modelRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.crafty.Service;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceDemandLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.output.Tracker;
import de.cesr.crafty.core.updaters.AftsUpdater;
import de.cesr.crafty.core.updaters.CapitalUpdater;
import de.cesr.crafty.core.updaters.CellBehaviourUpdater;
import de.cesr.crafty.core.updaters.CellsShocksUpdater;
import de.cesr.crafty.core.updaters.FlagUpdater;
import de.cesr.crafty.core.updaters.LandMaskUpdater;
import de.cesr.crafty.core.updaters.RegionsModelRunnerUpdater;
import de.cesr.crafty.core.updaters.ServicesUpdater;
import de.cesr.crafty.core.updaters.SupplyUpdater;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

/**
 * @author Mohamed Byari
 *
 */

public class ModelRunner extends AbstractModelRunner {

	private static final CustomLogger LOGGER = new CustomLogger(ModelRunner.class);
	public static CellsLoader cellsSet;
	public static CapitalUpdater capitalUpdater;
	public static AftsUpdater aftsUpdater;

	public void start() {
		ProjectLoader.setScenario(ConfigLoader.config.scenario);

		ServiceSet.loadServiceList();
		capitalUpdater = new CapitalUpdater();
		aftsUpdater = new AftsUpdater();
		cellsSet = new CellsLoader();
		capitalUpdater.step();
		getScheduled().clear();
		getScheduled().add(new FlagUpdater());
		getScheduled().add(new ServicesUpdater());
		getScheduled().add(capitalUpdater);
		getScheduled().add(aftsUpdater);
		getScheduled().add(new CellBehaviourUpdater());
		getScheduled().add(new LandMaskUpdater());
//		getScheduled().add(new RegionalShocksUpdater());
		getScheduled().add(new CellsShocksUpdater());
		getScheduled().add(new SupplyUpdater());
		getScheduled().add(new Listener());
		getScheduled().add(new Tracker());
		getScheduled().add(new RegionsModelRunnerUpdater());
	}

	public void run() {
		AtomicInteger tick = new AtomicInteger(Timestep.getStartYear());
		String generatedPath = PathTools.makeDirectory(ConfigLoader.config.Output_path);
		Listener.outputfolderPath(generatedPath, ConfigLoader.config.output_folder_name);
		if (ConfigLoader.config.export_LOGGER) {
			CustomLogger
					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
			PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "config.txt",
					Listener.exportConfigurationFile(), false);
		}
		demandEquilibrium();
		for (int i = 0; i <= Timestep.getEndtYear() - Timestep.getStartYear(); i++) {
			Timestep.setCurrentYear(tick.get());
			LOGGER.info("-------------   " + Timestep.getCurrentYear() + "   --------------");
			System.out.println("-------------   " + Timestep.getCurrentYear() + "   --------------");
			step();
//			GeoTiffExample.geoTiffWriter();// -----
			tick.getAndIncrement();
		}
//		exportChartsPlots();
	}

	public static void demandEquilibrium() {
		if (ConfigLoader.config.initial_demand_supply_equilibrium) {
			RegionalDemandEquilibrium_calculation();
			RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
				RegionalRunner.R.getServicesHash().forEach((ns, s) -> {
					s.getDemands().forEach((year, v) -> {
						s.getDemands().put(year, v / s.getCalibration_Factor());
					});
				});
			});
			initialTotalDSEquilibriumListrner();
			ServiceDemandLoader.aggregateRegionalToWorldServiceDemand();
		}
	}

	private static void RegionalDemandEquilibrium_calculation() {
		ModelRunner.capitalUpdater.step();

		// Calculate EQ
		// remumber the service has 0 supply hashMap<regionName, List<servicesNames>>
		// calculate the average EQ
		// go to 0 supply services and repleas them with the average

		RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
			ServiceSet.NoInitialSupplyServices.put(RegionalRunner.R.getName(), new ArrayList<>());
			RegionalRunner.initialDSEquilibriumFactorCalculation();
		});

		// calculate the average
		HashMap<String, Double> averageEQ = new HashMap<>();
		RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
			ServiceSet.getServicesList().forEach(serviceName -> {
				double av = RegionalRunner.R.getServicesHash().get(serviceName).getCalibration_Factor()
						/ RegionsModelRunnerUpdater.regionsModelRunner.size();
				averageEQ.merge(serviceName, av, Double::sum);
			});
		});

		// comeback to NoInitialSupplyServices-EQ with the averageEQ
		RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
			ServiceSet.getServicesList().forEach(serviceName -> {
				if (ServiceSet.NoInitialSupplyServices.get(RegionalRunner.R.getName()).contains(serviceName)) {
					RegionalRunner.R.getServicesHash().get(serviceName)
							.setCalibration_Factor(averageEQ.get(serviceName));
				}
			});

		});

	}

	private static void initialTotalDSEquilibriumListrner() {
		ServiceSet.worldService.forEach((serviceName, service) -> {
			RegionsModelRunnerUpdater.regionsModelRunner.values().forEach(RegionalRunner -> {
				Service s = RegionalRunner.R.getServicesHash().get(serviceName);
				int i = ServiceSet.getServicesList().indexOf(serviceName);
				RegionalRunner.listner.DSEquilibriumListener[i + 1][0] = serviceName;
				RegionalRunner.listner.DSEquilibriumListener[i + 1][1] = String.valueOf(s.getCalibration_Factor());
			});
		});
	}

//	private static void exportChartsPlots() {
//	if (ConfigLoader.config.generate_charts_plots_PNG || ConfigLoader.config.generate_charts_plots_PDF) {
//		String path = PathTools.makeDirectory(ConfigLoader.config.output_folder_name + File.separator + "plots");
//		Listener.servicedemandHash.forEach((serviceName, serviceHash) -> {
//			ChartExporter.createAndSaveChartAsPNG(serviceHash, ProjectLoader.getStartYear(), serviceName,
//					path + File.separator + serviceName);
//		});
//		Map<String, Color> hashColors = new HashMap<>();
//		AFTsLoader.getAftHash().forEach((name, aft) -> {
//			hashColors.put(name, Color.decode(aft.getColor()));
//		});
//
//		ChartExporter.createAndSaveChartAsPNG(Listener.compositionAftHash, hashColors, ProjectLoader.getStartYear(),
//				"LandUseTrends", path + File.separator + "Land_use_trends");
//	}
//}

}
