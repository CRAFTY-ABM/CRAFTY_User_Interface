package de.cesr.crafty.core.output;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.crafty.Region;
import de.cesr.crafty.core.crafty.RegionalModelRunner;
import de.cesr.crafty.core.crafty.Service;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.updaters.AbstractUpdater;
import de.cesr.crafty.core.updaters.RegionsModelRunnerUpdater;
import de.cesr.crafty.core.updaters.SupplyUpdater;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class Listener extends AbstractUpdater {
	public static String[][] compositionAftListener;
	public static Map<String, ArrayList<Double>> compositionAftHash = new HashMap<>();
	public static String[][] servicedemandListener;
	public static Map<String, Map<String, ArrayList<Double>>> servicedemandHash = new HashMap<>();
	private static String[][] DSEquilibriumListener;
	private static String[][] landEventCounter;
	private static String[][] averageUtilities;
	public static AtomicInteger landUseChangeCounter = new AtomicInteger();

	public static ArrayList<Integer> yearsMapExporting = new ArrayList<>();

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		if (ConfigLoader.config.generate_output_files) {
			compositionAFT(Timestep.getCurrentYear());
			outPutserviceDemandToCsv(Timestep.getCurrentYear(), SupplyUpdater.totalSupply);
			writOutPutMap(Timestep.getCurrentYear());
			updateCSVFilesWolrd();
			updateLandUseEventCounter();
		}
	}

	public Listener() {
		initializeListeners();
	}

	public void initializeListeners() {
		initializeListExportingYearsMap();
		servicedemandListener = new String[Timestep.getEndtYear() - Timestep.getStartYear()
				+ 2][ServiceSet.getServicesList().size() * 2 + 1];
		servicedemandListener[0][0] = "Year";
		for (int i = 1; i < ServiceSet.getServicesList().size() + 1; i++) {
			servicedemandListener[0][i] = "Supply:" + ServiceSet.getServicesList().get(i - 1);
			servicedemandListener[0][i + ServiceSet.getServicesList().size()] = "Demand:"
					+ ServiceSet.getServicesList().get(i - 1);
		}
		compositionAftListener = new String[Timestep.getEndtYear() - Timestep.getStartYear()
				+ 2][AFTsLoader.getAftHash().size() + 1];
		compositionAftListener[0][0] = "Year";
		averageUtilities = new String[compositionAftListener.length][compositionAftListener[0].length];
		averageUtilities[0][0] = "Year";
		int k = 1;
		for (String label : AFTsLoader.getAftHash().keySet()) {
			compositionAftListener[0][k] = label;
			averageUtilities[0][k++] = label;
			compositionAftHash.put(label, new ArrayList<>());
		}
		DSEquilibriumListener = new String[ServiceSet.getServicesList().size() + 1][CellsLoader.regions.size() + 1];
		DSEquilibriumListener[0][0] = "Service";
		int j = 1;
		for (String gerionName : CellsLoader.regions.keySet()) {
			DSEquilibriumListener[0][j++] = gerionName;
		}
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			DSEquilibriumListener[i + 1][0] = ServiceSet.getServicesList().get(i);
			Map<String, ArrayList<Double>> h = new HashMap<>();
			h.put("Supply", new ArrayList<>());
			h.put("Demand", new ArrayList<>());
			servicedemandHash.put(ServiceSet.getServicesList().get(i), h);
		}

		landEventCounter = new String[Timestep.getEndtYear() - Timestep.getStartYear() + 1][2];
		landEventCounter[0][0] = "year";
		landEventCounter[0][1] = "LU changed";
		for (int i = 0; i < Timestep.getEndtYear() - Timestep.getStartYear(); i++) {
			landEventCounter[i + 1][0] = String.valueOf(i + Timestep.getStartYear());
		}
	}

	public void outPutserviceDemandToCsv(int year, ConcurrentHashMap<String, Double> totalSupply) {
		AtomicInteger m = new AtomicInteger(1);
		int y = year - Timestep.getStartYear() + 1;
		servicedemandListener[y][0] = String.valueOf(year);
		ServiceSet.getServicesList().forEach(serviceName -> {
			servicedemandListener[y][m.get()] = String.valueOf(totalSupply.get(serviceName));
			Service ds = ServiceSet.worldService.get(serviceName);
			servicedemandListener[y][m.get() + ServiceSet.getServicesList().size()] = String
					.valueOf(ds.getDemands().get(year));
			m.getAndIncrement();
			servicedemandHash.get(serviceName).get("Supply").add(totalSupply.get(serviceName));
			servicedemandHash.get(serviceName).get("Demand").add(ds.getDemands().get(year));
		});
	}

	public void compositionAFT(int year) {
		int y = year - Timestep.getStartYear() + 1;
		compositionAftListener[y][0] = String.valueOf(year);
		averageUtilities[y][0] = String.valueOf(year);
		AFTsLoader.hashAgentNbr.forEach((name, value) -> {
			compositionAftListener[y][Utils.indexof(name, compositionAftListener[0])] = String.valueOf(value);
			compositionAftHash.get(name).add((double) value);
		});
		Region R = RegionsModelRunnerUpdater.regionsModelRunner.values().iterator().next().R;
		if (y > 1) {
			AFTsLoader.getAftHash().forEach((name, aft) -> {
				if (RegionsModelRunnerUpdater.regionsModelRunner.get(R.getName()).getDistributionMean() != null) {
					averageUtilities[y - 1][Utils.indexof(name, averageUtilities[0])] = String
							.valueOf(RegionsModelRunnerUpdater.regionsModelRunner.get(R.getName()).getDistributionMean()
									.get(aft));
				} else {
					averageUtilities[y - 1][Utils.indexof(name, averageUtilities[0])] = "null";
				}
			});
		}
	}

	public static void DSEquilibriumListener() {
		for (RegionalModelRunner rr : RegionsModelRunnerUpdater.regionsModelRunner.values()) {
			for (int j = 0; j < ServiceSet.getServicesList().size(); j++) {
				DSEquilibriumListener[j + 1][Utils.indexof(rr.R.getName(),
						DSEquilibriumListener[0])] = rr.listner.DSEquilibriumListener[j + 1][1];
			}
		}
	}

	public void updateCSVFilesWolrd() {
		Path aggregateAFTComposition = Paths.get(ConfigLoader.config.output_folder_name + File.separator
				+ ProjectLoader.getScenario() + "Total-AggregateAFTComposition.csv");
		CsvTools.writeCSVfile(compositionAftListener, aggregateAFTComposition);
		Path aggregateServiceDemand = Paths.get(ConfigLoader.config.output_folder_name + File.separator
				+ ProjectLoader.getScenario() + "Total-AggregateServiceDemand.csv");
		CsvTools.writeCSVfile(servicedemandListener, aggregateServiceDemand);
		Path DSEquilibriumPath = Paths.get(ConfigLoader.config.output_folder_name + File.separator
				+ ProjectLoader.getScenario() + "Total-AggregateDemandServicesEquilibrium.csv");
		DSEquilibriumListener();
		CsvTools.writeCSVfile(DSEquilibriumListener, DSEquilibriumPath);

		if (RegionsModelRunnerUpdater.regionsModelRunner.size() == 1) {
			Path averageUtilitiesPath = Paths.get(ConfigLoader.config.output_folder_name + File.separator
					+ ProjectLoader.getScenario() + "-AverageUtilities.csv");
			CsvTools.writeCSVfile(averageUtilities, averageUtilitiesPath);
		}
	}

	private void updateLandUseEventCounter() {
		if (Timestep.getCurrentYear() - Timestep.getStartYear() != 0) {
			landEventCounter[Timestep.getCurrentYear() - Timestep.getStartYear()][1] = landUseChangeCounter.toString();
			Path landChengePath = Paths.get(ConfigLoader.config.output_folder_name + File.separator
					+ ProjectLoader.getScenario() + "-landEventCounter.csv");
			CsvTools.writeCSVfile(landEventCounter, landChengePath);
			landUseChangeCounter.set(0);
		}
	}

	public void writOutPutMap(int year) {
		if (yearsMapExporting.contains(year)) {
			writeMap(year);

		}
	}

	public static void initializeListExportingYearsMap() {
		for (int year = Timestep.getStartYear(); year <= Timestep.getEndtYear(); year++) {
			if (ConfigLoader.config.generate_map_output_files) {
				if (ConfigLoader.config.map_output_years instanceof Integer) {
					ConfigLoader.config.map_output_frequency = (int) ConfigLoader.config.map_output_years;
					if (ConfigLoader.config.map_output_frequency != 0) {
						if ((Timestep.getCurrentYear() - Timestep.getStartYear())
								% ConfigLoader.config.map_output_frequency == 0
								|| Timestep.getCurrentYear() == Timestep.getEndtYear()) {
							yearsMapExporting.add(year);
						}
					}
				} else if (ConfigLoader.config.map_output_years instanceof List<?>) {
					@SuppressWarnings("unchecked")
					ArrayList<Integer> listYears = (ArrayList<Integer>) ConfigLoader.config.map_output_years;
					if (listYears.contains(year)) {
						yearsMapExporting.add(year);
					}
				} else if (ConfigLoader.config.map_output_frequency != 0) {
					if ((Timestep.getCurrentYear() - Timestep.getStartYear())
							% ConfigLoader.config.map_output_frequency == 0
							|| Timestep.getCurrentYear() == Timestep.getEndtYear()) {
						yearsMapExporting.add(year);
					}
				}
			}
		}
	}

	private void writeMap(int year) {
		CsvTools.exportToCSV(ConfigLoader.config.output_folder_name + File.separator + ProjectLoader.getScenario()
				+ "-Cell-" + year + ".csv");
//		if (year != Timestep.getStartYear())
//			CsvTools.writeCSVfile(Selector.seedMap,
//					Paths.get(ConfigLoader.config.output_folder_name + File.separator + "-SEED-" + year + ".csv"));
	}

	public static void outputfolderPath(String outputpath, String outputName) {
		if (outputName.equals("") || outputName.equalsIgnoreCase("Default")) {
			ConfigLoader.config.output_folder_name = "Default simulation folder";
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
			String formattedDate = now.format(formatter);
			ConfigLoader.config.output_folder_name = "Default_Run_Output_" + formattedDate;
		} else {
			ConfigLoader.config.output_folder_name = outputName;
		}
		if (outputpath == null) {
			outputpath = PathTools.makeDirectory(ProjectLoader.getProjectPath() + File.separator + "output");
		}
		outputpath = PathTools.makeDirectory(outputpath + File.separator + ProjectLoader.getScenario());
		outputpath = PathTools.makeDirectory(outputpath + File.separator + ConfigLoader.config.output_folder_name);
		ConfigLoader.config.output_folder_name = outputpath;
	}

	public static String exportConfigurationFile() {
		return ConfigLoader.config.toString();
	}
}
