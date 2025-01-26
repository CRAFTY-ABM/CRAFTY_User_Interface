package de.cesr.crafty.main;

import java.nio.file.Paths;

import de.cesr.crafty.cli.CraftyOptions;
import de.cesr.crafty.cli.OptionsParser;
import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.dataLoader.ServiceWeightLoader;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.model.RegionClassifier;
import de.cesr.crafty.utils.analysis.CustomLogger;

public class MainParser {

	private static final CustomLogger LOGGER = new CustomLogger(MainHeadless.class);

	public static void main(String[] args) {
		// Load config using the path from CraftyOptions
		CraftyOptions options = OptionsParser.parseArguments(args);
		ConfigLoader.configPath = options.getConfigFilePath();
		ConfigLoader.init();

		// If the user specified a project directory and scenario name, override the ones in the YAML
		String projectDirectoryPath = options.getProjectDirectoryPath();
		if (projectDirectoryPath != null) {
			ConfigLoader.config.project_path = projectDirectoryPath;
		}
		String scenarioName = options.getScenario_Name();
		if (scenarioName != null) {
			ConfigLoader.config.scenario = scenarioName;
		}

		PathsLoader.initialisation(Paths.get(ConfigLoader.config.project_path));
		PathsLoader.setScenario(ConfigLoader.config.scenario);
		modelInitialisation();
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
}
