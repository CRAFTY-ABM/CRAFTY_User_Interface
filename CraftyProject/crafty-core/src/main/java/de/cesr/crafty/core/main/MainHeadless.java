package de.cesr.crafty.core.main;

import java.nio.file.Paths;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.cli.CraftyOptions;
import de.cesr.crafty.core.cli.OptionsParser;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.modelRunner.ModelRunner;
import de.cesr.crafty.core.utils.analysis.CustomLogger;

/*
 * @author Mohamed Byari
 *
 */
public class MainHeadless {
	private static final CustomLogger LOGGER = new CustomLogger(MainHeadless.class);
	public static ModelRunner runner;

	public static void main(String[] args) {
		System.out.println("--Starting CRAFTY execution--");
		initializeConfig(args);
		ProjectLoader.pathInitialisation(Paths.get(ConfigLoader.config.project_path));
		runner = new ModelRunner();
		runner.start();
		runner.run();
	}

	public static void initializeConfig(String[] args) {

		System.setProperty("java.awt.headless", "true");
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

}
