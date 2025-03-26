package de.cesr.crafty.core.plumLinking;

import java.io.File;
import java.nio.file.Paths;

import ac.ed.lurg.ModelConfig;
import ac.ed.lurg.ModelMain;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.main.MainHeadless;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

public class MainCoupling {

	static ModelRunner runner = new ModelRunner();;

	public static void main(String[] args) {

//		initialPlum(args);
		MainHeadless.initializeConfig(args);
		ProjectLoader.modelInitialisation();
		outputPathConfig();
		ModelRunner.setup();
		Coupler coupler = new Coupler();
		coupler.initialEquilibruim();
//		PlumConnecter.initialze();

		for (int year = ProjectLoader.getStartYear(); year <= ProjectLoader.getEndtYear(); year++) {
			System.out.println("--------- Run " + year + "----------");
			ProjectLoader.setCurrentYear(year);
			coupler.AssocietePricesAndDemand(year);
			runner.step();
//			PlumConnecter.associeteProduction(year );
//			ModelMain.theModel.runNTick(1);
			coupler.writeDandP();
		}

	}

	static void initialPlum(String[] args) {
		// ModelConfig.USE_CRAFTY_COUNTRIES = false;
		// ModelConfig.ENABLE_CRAFTY_IMPORTS_UPDATE = false;
		ModelMain.main(args);
		ModelMain.theModel.runNTick(1);
//		ModelConfig.USE_CRAFTY_COUNTRIES = true;
//		ModelConfig.ENABLE_CRAFTY_IMPORTS_UPDATE = true;
//		ModelMain.newStartYear = ModelConfig.START_TIMESTEP;
//		System.out.println("||" + ModelConfig.USE_CRAFTY_COUNTRIES);
//		System.out.println("||" + ModelConfig.ENABLE_CRAFTY_IMPORTS_UPDATE);
	}

	static private void outputPathConfig() {
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
