package de.cesr.crafty.main;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceWeightLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.model.ModelRunner;
import de.cesr.crafty.model.RegionClassifier;
import de.cesr.crafty.output.Listener;
import de.cesr.crafty.utils.analysis.CustomLogger;
import de.cesr.crafty.utils.file.PathTools;

public class MainHeadless {
	private static final CustomLogger LOGGER = new CustomLogger(MainHeadless.class);

	public static void main(String[] args) {
		LOGGER.info("--Starting CRAFTY execution--");
		if (args.length > 0) {
			ConfigLoader.configPath = args[0];
			ConfigLoader.init();
		}
		PathsLoader.initialisation(Paths.get(ConfigLoader.config.project_path));
		PathsLoader.setScenario(ConfigLoader.config.scenario);
		modelInitialisation();
		runHeadless();
	}

	public static void modelInitialisation() {
		CellsLoader cellsLoader = new CellsLoader();
		CellsLoader.loadCapitalsList();
		ServiceSet.loadServiceList();
		cellsLoader.loadMap();
		RegionClassifier.initialation();
		ServiceWeightLoader.updateWorldWeight();
		AFTsLoader.hashAgentNbrRegions();
		CellsSet.setCellsSet(cellsLoader);
		MaskRestrictionDataLoader.allMaskAndRistrictionUpdate();
	}

	static void runHeadless() {
		ModelRunner runner = new ModelRunner();
		ModelRunner.setup();
		AtomicInteger tick = new AtomicInteger(PathsLoader.getStartYear());
		Listener.outputfolderPath(ConfigLoader.config.output_folder_name);
		if (ConfigLoader.config.export_LOGGER) {
			CustomLogger
					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
			PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "config.txt",
					Listener.exportConfigurationFile(), false);
		}
		ModelRunner.demandEquilibrium();

		for (int i = 0; i <= PathsLoader.getEndtYear() - PathsLoader.getStartYear(); i++) {
			PathsLoader.setCurrentYear(tick.get());
			LOGGER.info("-------------   " + PathsLoader.getCurrentYear() + "   --------------");
			System.out.println("-------------   " + PathsLoader.getCurrentYear() + "   --------------");
			runner.step();
			tick.getAndIncrement();
		}
	}

}
