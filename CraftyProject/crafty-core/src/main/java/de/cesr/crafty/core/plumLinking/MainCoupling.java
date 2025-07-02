package de.cesr.crafty.core.plumLinking;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import ac.ed.lurg.ModelConfig;
import ac.ed.lurg.ModelMain;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.main.MainHeadless;
import de.cesr.crafty.core.modelRunner.ModelRunner;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.plumLinking.couplingUtils.CsvUtilsP;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

public class MainCoupling {

	static ModelRunner runner;

	public static void main(String[] args) {
		System.out.println("--Starting CRAFTY-PLUM execution--");
		runner = new ModelRunner();
//		cleanPlumFolder();
//		ModelMain.main(args);
//		ModelMain.theModel.runNTick(1);
		MainHeadless.initializeConfig(args);
		ProjectLoader.pathInitialisation(Paths.get(ConfigLoader.config.project_path));
		runner = new ModelRunner();
		runner.start();
		outputPathConfig();
		run();
	}

	private static void run() {
		Coupler coupler = new Coupler();
		coupler.linkPlumOutputToCrfatyInput();
		ModelRunner.demandEquilibrium();
		PlumConnecter.initialze();
		for (int year = Timestep.getStartYear(); year <= Timestep.getEndtYear(); year++) {
			System.out.println("--------- carfty " + year + "---------- newStartYear" + ModelMain.newStartYear);
			Timestep.setCurrentYear(year);
			coupler.AssocietePricesAndDemand(year);
			runner.step();
//			PlumConnecter.associeteProduction(year + 1);
//			ModelMain.theModel.runNTick(1);
			coupler.writeDandP();
		}

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

	static void cleanPlumFolder() {
		Path path = Paths.get(ModelConfig.OUTPUT_DIR);
		System.out.println(path);
		for (int i = 2021; i < 2100; i++) {
			CsvUtilsP.deletePath(path + "\\" + i);
			CsvUtilsP.deletePath(Paths.get(ModelConfig.CRAFTY_PRODUCTION_DIR) + "\\" + i);

		}
		try {
			for (Path txt : Files.newDirectoryStream(path, "*.txt")) {
				Files.deleteIfExists(txt);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void copyPast() {
		Path source = Paths.get(
				"C:\\Users\\byari-m\\Desktop\\Crafty-Plum-coupling-workSpace\\PLUM\\PLUM_output\\calibration\\crafty\\2020\\production.csv");
		for (int i = 2020; i < 2086; i++) {
			CsvUtilsP.copyFile(source, Paths.get(
					"C:\\Users\\byari-m\\Desktop\\Crafty-Plum-coupling-workSpace\\PLUM\\PLUM_output\\supplyBaseline10\\crafty\\"
							+ i + "\\production.csv"));
		}

	}

}
