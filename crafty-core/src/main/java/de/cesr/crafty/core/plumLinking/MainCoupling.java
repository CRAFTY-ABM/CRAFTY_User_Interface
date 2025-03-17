package de.cesr.crafty.core.plumLinking;

import java.io.File;
import java.nio.file.Paths;
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
		MainHeadless.initializeConfig(args);
		ProjectLoader.modelInitialisation();
		outputPathConfig();
		ModelRunner.setup();
		Coupler coupler = new Coupler();
		coupler.initialEquilibruim();

		for (int year = ProjectLoader.getStartYear(); year <= ProjectLoader.getEndtYear(); year++) {
			System.out.println("--------- Run " + year + "----------");
			ProjectLoader.setCurrentYear(year);
			coupler.AssocietePricesAndDemand(year);
			runner.step();
		}
		coupler.writeRegion();
//		coupler.writeDemandAndPrice();
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

//	public static void main(String[] args) {
//	//	System.out.println("----------Crafty initialisation----------");
//		MainHeadless.initializeConfig(args);
//		ProjectLoader.modelInitialisation();
////		System.out.println("----------PLUM initialisation---------");
////		ModelMain.main(new String[] {});
////		System.out.println("--------Plum run first iteration ------" + ProjectLoader.getStartYear() + "\n\n\n");
////		ModelMain.theModel.runNTick(1);
////		System.out.println("----------PLUM Mapper and Crafty deamnds initial Calibration---------");
//		plumMaper.initialize();
//		OfflineCoupling off = new OfflineCoupling(plumMaper);
//		off.initialize();
//		//runHeadlessWithPlum();
//	}
//
//	static void runHeadlessWithPlum() {
//	//	ModelRunner runner = new ModelRunner();
//		ModelRunner.setup();
//		AtomicInteger tick = new AtomicInteger(ProjectLoader.getStartYear());
//
//		String generatedPath = PathTools.makeDirectory(ConfigLoader.config.Output_path);
//		Listener.outputfolderPath(generatedPath, ConfigLoader.config.output_folder_name);
//		if (ConfigLoader.config.export_LOGGER) {
//			CustomLogger
//					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
//			PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "config.txt",
//					Listener.exportConfigurationFile(), false);
//		}
//
//		ModelRunner.demandEquilibrium();
//
//		for (int i = 0; i <= ProjectLoader.getEndtYear() - ProjectLoader.getStartYear(); i++) {
//			System.out.println("---------"+tick.get()+ "----------");
//			ProjectLoader.setCurrentYear(tick.get());
//			//runner.step();
////			ModelMain.theModel.runNTick(1);
//			plumMaper.iterative(tick.get());
//			tick.getAndIncrement();
//		}
//	}

}
