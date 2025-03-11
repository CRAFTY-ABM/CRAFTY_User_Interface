package de.cesr.crafty.core.main;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.cli.CraftyOptions;
import de.cesr.crafty.core.cli.OptionsParser;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

public class MainHeadless {
	private static final CustomLogger LOGGER = new CustomLogger(MainHeadless.class);

	public static void main(String[] args) {
		System.out.println("--Starting CRAFTY execution--");
		initializeConfig(args);
		ProjectLoader.modelInitialisation();
		runHeadless();
	}

	public static void initializeConfig(String[] args) {
		LOGGER.info("--Starting CRAFTY execution--");
		// Load config using the path from CraftyOptions
		CraftyOptions options = OptionsParser.parseArguments(args);
		ConfigLoader.configPath = options.getConfigFilePath();
		ConfigLoader.init();
		// If the user specified a project directory and scenario name, override the
		// ones in the YAML
		String projectDirectoryPath = options.getProjectDirectoryPath();
		if (projectDirectoryPath != null) {
			ConfigLoader.config.project_path = projectDirectoryPath;
		}
		String scenarioName = options.getScenario_Name();
		if (scenarioName != null) {
			ConfigLoader.config.scenario = scenarioName;
		}
		String ouputPath = options.getOutput_path();
		if (ouputPath != null) {
			ConfigLoader.config.Output_path = ouputPath;
		}
	}

	static void runHeadless() {
		ModelRunner runner = new ModelRunner();
		ModelRunner.setup();
		AtomicInteger tick = new AtomicInteger(ProjectLoader.getStartYear());
		String generatedPath = PathTools.makeDirectory(ConfigLoader.config.Output_path);
		Listener.outputfolderPath(generatedPath, ConfigLoader.config.output_folder_name);
		if (ConfigLoader.config.export_LOGGER) {
			CustomLogger
					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
			PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "config.txt",
					Listener.exportConfigurationFile(), false);
		}

		ModelRunner.demandEquilibrium();
		for (int i = 0; i <= ProjectLoader.getEndtYear() - ProjectLoader.getStartYear(); i++) {
			ProjectLoader.setCurrentYear(tick.get());
			LOGGER.info("-------------   " + ProjectLoader.getCurrentYear() + "   --------------");
			System.out.println("-------------   " + ProjectLoader.getCurrentYear() + "   --------------");
			runner.step();
			tick.getAndIncrement();
		}
	}
}
