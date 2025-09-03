package de.cesr.crafty.core.plumLinking.mains;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.main.MainHeadless;
import de.cesr.crafty.core.modelRunner.ModelRunner;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.plumLinking.Coupler;
import de.cesr.crafty.core.plumLinking.PlumConnecter;
import de.cesr.crafty.core.plumLinking.couplingUtils.DirectoryWatcher;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

public class MainUseFlags {

	// use .sh to controle models
	// use crfaty only to look if plum things is correcte and make a loop
	// .sh configuration.
	// 1. Run calibration FAO.
	// 3. Run plum config with crafty.
	// 1. carfty run always year 2020 [new plum output cofig file].
	// 2. calculate EQ for each iteration (check the EQ stability)?.
	// 3. return carfty file.
	// 5. Run the copling
	// 6. Generate .jar for copling

	// initilase the loops
	// Clean plum folder
	// Use only one time main function for the complete run

	// Create a Main that run crafty one iteration if there is Plum input then
	// convert data
	// write a carfty for plum
	// waiting for plum run
	// loop

	static ModelRunner runner;

	public static void main(String[] args) {
		System.out.println("--  Starting CRAFTY-PLUM execution  --");
		runner = new ModelRunner();
		MainHeadless.initializeConfig(args);
		ProjectLoader.pathInitialisation(Paths.get(ConfigLoader.config.project_path));
		runner = new ModelRunner();
		runner.start();
		outputPathConfig();
		run();
	}

	private static void run() {
		Path path2020 = Paths.get(ConfigLoader.config.plumOutPutPath + PathTools.asFolder("2020") + "done");
		DirectoryWatcher.waitForYearFolder(path2020);
		Coupler coupler = new Coupler();
		coupler.linkPlumOutputToCrfatyInput();
		ModelRunner.demandEquilibrium();
		PlumConnecter.initialze();
		for (int year = Timestep.getStartYear(); year <= Timestep.getEndtYear(); year++) {
			System.out.println("--------- carfty " + year + "---------- ");
			Timestep.setCurrentYear(year);
			Path path = Paths
					.get(ConfigLoader.config.plumOutPutPath + PathTools.asFolder(String.valueOf(year)) + "done");
			
			DirectoryWatcher.waitForYearFolder(path);
			coupler.AssocietePricesAndDemand(year);
			runner.step();
			PlumConnecter.associeteProduction(year + 1);
			coupler.writeDandP();
		}
	}

	static void outputPathConfig() {
		String generatedPath = PathTools.makeDirectory(ConfigLoader.config.Output_path);
		Listener.outputfolderPath(generatedPath, ConfigLoader.config.output_folder_name);
		if (ConfigLoader.config.export_LOGGER) {
			CustomLogger
					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
			PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "config.txt",
					Listener.exportConfigurationFile(), false);
		}
	}

}
