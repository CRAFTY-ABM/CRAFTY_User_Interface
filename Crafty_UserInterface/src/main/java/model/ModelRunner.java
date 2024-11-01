package model;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import UtilitiesFx.analysis.Tracker;
import UtilitiesFx.filesTools.CsvTools;
import UtilitiesFx.graphicalTools.Tools;
import dataLoader.AFTsLoader;
import dataLoader.CellsLoader;
import dataLoader.DemandModel;
import dataLoader.PathsLoader;
import dataLoader.ServiceSet;
import fxmlControllers.MasksPaneController;
import fxmlControllers.ModelRunnerController;

/**
 * @author Mohamed Byari
 *
 */

public class ModelRunner {
	private static final Logger LOGGER = LogManager.getLogger(ModelRunner.class);
	public CellsLoader cells;
	public String colorDisplay = "FR";
	public static boolean mapSynchronisation = true;
	public static int mapSynchronisationGap = 5;
	public static boolean writeCsvFiles = true;
	public static int writeCsvFilesGap = 10;
	public static boolean initialDSEquilibrium = true;
	public static boolean removeNegative = false;
	public static boolean usegiveUp = true;
	public static boolean isMutated = false;
	public static double MostCompetitorAFTProbability = 0.8;
	public static boolean isAveragedPerCellResidualDemand = false;
	public static boolean NeighboorEffect = true;
	public static double probabilityOfNeighbor = 0.95;
	public static int NeighborRaduis = 2;
	public static double percentageCells = 0.01;
	public static int nbrOfSubSet = 10;
	public static double mutationIntval = 0.1;
	public static double percentageOfGiveUp = 0.01;
	public static boolean traker = false;
	public ConcurrentHashMap<String, Double> totalSupply;

	public String[][] compositionAftListener;
	public String[][] servicedemandListener;
	public static ConcurrentHashMap<String, RegionalModelRunner> regions;

	public ModelRunner(CellsLoader cells) {
		this.cells = cells;
		initializeListeners();
		initializeRegions();
	}

	private void initializeListeners() {
		compositionAftListener = new String[PathsLoader.getEndtYear() - PathsLoader.getStartYear() + 2][AFTsLoader
				.getAftHash().size()];
		servicedemandListener = new String[PathsLoader.getEndtYear() - PathsLoader.getStartYear()
				+ 2][ServiceSet.getServicesList().size() * 2];
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			servicedemandListener[0][i] = "ServiceSupply:" + ServiceSet.getServicesList().get(i);
			servicedemandListener[0][i + ServiceSet.getServicesList().size()] = "Demand:"
					+ ServiceSet.getServicesList().get(i);
		}
		int i = 0;
		for (String label : AFTsLoader.getAftHash().keySet()) {
			compositionAftListener[0][i++] = label;
		}
	}

	public static void initializeRegions() {
		regions = new ConcurrentHashMap<>();
		RegionClassifier.regions.keySet().forEach(regionName -> {
			regions.put(regionName, new RegionalModelRunner(regionName));
		});
	}



	public void go() {
		int year = PathsLoader.getCurrentYear() < PathsLoader.getEndtYear() ? PathsLoader.getCurrentYear()
				: PathsLoader.getEndtYear();
//		ConcurrentHashMap<String, Cell> tmp1 = trackeMasks();/////
		totalSupply = new ConcurrentHashMap<>();
		LOGGER.info("Cells.updateCapitals");
		cells.updateCapitals(year);
		AFTsLoader.updateAFTs();
		MasksPaneController.Maskloader.CellSetToMaskLoader(year);
		regions.values()/* .parallelStream() */ .forEach(RegionalRunner -> {
			RegionalRunner.regionalSupply();
			RegionalRunner.totalSupply.forEach((key, value) -> totalSupply.merge(key, value, Double::sum));
		});

		if (writeCsvFiles) {
			outPutserviceDemandToCsv(year);
			if (traker) {
				Tracker.trackSupply(year);
			}
		}

		regions.values()/* .parallelStream() */ .forEach(RegionalRunner -> {
			RegionalRunner.go(year);
		});

		if (writeCsvFiles) {
			compositionAFT(year);
			writOutPutMap(year);
		}

		if (mapSynchronisation
				&& ((PathsLoader.getCurrentYear() - PathsLoader.getStartYear()) % mapSynchronisationGap == 0
						|| PathsLoader.getCurrentYear() == PathsLoader.getEndtYear())) {
			CellsSet.colorMap(colorDisplay);
		}
		AFTsLoader.hashAgentNbr();
	}

//	ConcurrentHashMap<String, Cell> trackeMasks() {
//		ConcurrentHashMap<String, Cell> hashMask = new ConcurrentHashMap<>();
//		CellsLoader.hashCell.values().forEach(c -> {
//			if (c.getOwner() != null&& c.getOwner().getLabel().equals("Water"))
//				hashMask.put(c.getX()+","+c.getY(), c);
//		});
//		return hashMask;
//	}
//	void trackeMasksNBR() {
//		ConcurrentHashMap<String, Integer> hashMaskNbr = new ConcurrentHashMap<>();
//		CellsLoader.hashCell.values().forEach(c -> {
//			if (c.getOwner() != null&& !c.getOwner().isActive())
//				hashMaskNbr.merge(c.getOwner().getLabel(), 1, Integer::sum);
//		});
//		System.out.println("|| "+hashMaskNbr);
//	}

	private void outPutserviceDemandToCsv(int year) {
		AtomicInteger m = new AtomicInteger();
		int y = year - PathsLoader.getStartYear() + 1;

		ServiceSet.getServicesList().forEach(service -> {
			servicedemandListener[y][m.get()] = totalSupply.get(service) + "";
			servicedemandListener[y][m.get() + ServiceSet.getServicesList().size()] = DemandModel.worldService
					.get(service).getDemands().get(y) + "";
			m.getAndIncrement();
		});
	}

	void compositionAFT(int year) {
		int y = year - PathsLoader.getStartYear() + 1;
		AFTsLoader.hashAgentNbr.forEach((name, value) -> {
			compositionAftListener[y][Tools.indexof(name, compositionAftListener[0])] = value + "";
		});
	}

	private void writOutPutMap(int year) {
		if ((PathsLoader.getCurrentYear() - PathsLoader.getStartYear()) % writeCsvFilesGap == 0
				|| PathsLoader.getCurrentYear() == PathsLoader.getEndtYear()) {
			CsvTools.exportToCSV(ModelRunnerController.outPutFolderName + File.separator + PathsLoader.getScenario()
					+ "-Cell-" + year + ".csv");
		}
	}

}
