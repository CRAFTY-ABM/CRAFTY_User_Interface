package de.cesr.crafty.core.plumLinking;

import de.cesr.crafty.core.main.MainHeadless;



public class CouplingRunner {

	static PlumToCrafty plumMaper = new PlumToCrafty();

	public static void main(String[] args) {
		System.out.println("----------Crafty initialisation----------");
		MainHeadless.main(args);;
	//	System.out.println("----------PLUM initialisation---------");
	//	ModelMain.main(new String[] {});
	//	System.out.println("----------Plum run first iteration ---------"+PathsLoader.getStartYear()+"\n\n\n\n\n");
	//	ModelMain.theModel.runNTick(1);
		System.out.println("----------PLUM Mapper and Crafty deamnds initial Calibration---------");
		//plumMaper.initialize();
//		runHeadlessWithPlum();
	}

//	static void runHeadlessWithPlum() {
//		//ModelRunnerController.init();
//		Listener.outputfolderPath(ConfigLoader.config.output_folder_name);
//		if (ConfigLoader.config.export_LOGGER) {
//			CustomLogger
//					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
//		}
//		ModelRunnerController.tick = new AtomicInteger(PathsLoader.getStartYear());
//		for (int i = 0; i <= PathsLoader.getEndtYear() - PathsLoader.getStartYear(); i++) {
//			PathsLoader.setCurrentYear(ModelRunnerController.tick.get());
//			ModelRunnerController.runner.go();
//	//		ModelMain.theModel.runNTick(1);
//			plumMaper.iterative(ModelRunnerController.tick.get());
//			ModelRunnerController.tick.getAndIncrement();
//		}
//	}

}
