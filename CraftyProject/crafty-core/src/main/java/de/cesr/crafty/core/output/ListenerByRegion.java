package de.cesr.crafty.core.output;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.crafty.Region;
import de.cesr.crafty.core.crafty.Service;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.updaters.RegionsModelRunnerUpdater;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class ListenerByRegion {
	Region R;
	private String[][] compositionAftListener;
	private String[][] servicedemandListener;
	public  String[][] DSEquilibriumListener;
	public  String[][] averageUtilities;

	public ListenerByRegion(Region R) {
		this.R = R;
	}

	public void initializeListeners() {
		compositionAftListener = new String[Timestep.getEndtYear() - Timestep.getStartYear()
				+ 2][AFTsLoader.getAftHash().size() + 1];
		servicedemandListener = new String[Timestep.getEndtYear() - Timestep.getStartYear()
				+ 2][ServiceSet.getServicesList().size() * 2 + 1];

		averageUtilities = new String[compositionAftListener.length][compositionAftListener[0].length];

		servicedemandListener[0][0] = "Year";
		for (int i = 1; i < ServiceSet.getServicesList().size() + 1; i++) {
			servicedemandListener[0][i] = "Supply:" + ServiceSet.getServicesList().get(i - 1);
			servicedemandListener[0][i + ServiceSet.getServicesList().size()] = "Demand:"
					+ ServiceSet.getServicesList().get(i - 1);
		}
		compositionAftListener[0][0] = "Year";
		averageUtilities[0][0] = "Year";
		int j = 1;
		for (String label : AFTsLoader.getAftHash().keySet()) {
			compositionAftListener[0][j] = label;
			averageUtilities[0][j++] = label;
		}
		DSEquilibriumListener = new String[ServiceSet.getServicesList().size() + 1][2];
		DSEquilibriumListener[0][0] = "Service";
		DSEquilibriumListener[0][1] = "Calibration_Factor";

	}

	public void fillDSEquilibriumListener(ConcurrentHashMap<String, Service> ServiceHash) {
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			DSEquilibriumListener[i + 1][0] = ServiceSet.getServicesList().get(i);
			DSEquilibriumListener[i + 1][1] = String
					.valueOf(ServiceHash.get(ServiceSet.getServicesList().get(i)).getCalibration_Factor());
		}
	}

	private void servicedemandListener(int year, ConcurrentHashMap<String, Double> regionalSupply) {
		AtomicInteger m = new AtomicInteger(1);
		int y = year - Timestep.getStartYear() + 1;
		servicedemandListener[y][0] = String.valueOf(year);
		ServiceSet.getServicesList().forEach(name -> {
			servicedemandListener[y][m.get()] = String.valueOf(regionalSupply.get(name));
			servicedemandListener[y][m.get() + ServiceSet.getServicesList().size()] = String
					.valueOf(R.getServicesHash().get(name).getDemands().get(year));
			m.getAndIncrement();
		});
	}

	private void compositionAFTListener(int year) {
		int y = year - Timestep.getStartYear() + 1;
		compositionAftListener[y][0] = String.valueOf(year);
		averageUtilities[y][0] = String.valueOf(year);
		AFTsLoader.hashAgentNbrRegions.get(R.getName()).forEach((name, value) -> {
			compositionAftListener[y][Utils.indexof(name, compositionAftListener[0])] = String.valueOf(value);
		});
		if(y>1) {
		AFTsLoader.getAftHash().forEach((name, aft) -> {
			if (RegionsModelRunnerUpdater.regionsModelRunner.get(R.getName()).getDistributionMean() != null) {
				averageUtilities[y-1][Utils.indexof(name, averageUtilities[0])] = String
						.valueOf(RegionsModelRunnerUpdater.regionsModelRunner.get(R.getName()).getDistributionMean().get(aft));
			}else {averageUtilities[y-1][Utils.indexof(name, averageUtilities[0])] ="null";}
		});}
	}

	private void CSVFilesWriter() {
		String dir = PathTools.makeDirectory(
				ConfigLoader.config.output_folder_name + File.separator + "region_" + R.getName() + File.separator);
		if (ConfigLoader.config.generate_output_files) {
			Path aggregateAFTComposition = Paths.get(dir + "region_" + R.getName() + "-AggregateAFTComposition.csv");
			CsvTools.writeCSVfile(compositionAftListener, aggregateAFTComposition);
			Path aggregateServiceDemand = Paths.get(dir + "region_" + R.getName() + "-AggregateServiceDemand.csv");
			CsvTools.writeCSVfile(servicedemandListener, aggregateServiceDemand);
			Path DSEquilibriumPath = Paths.get(dir + "region_" + R.getName() + "-DemandServicesEquilibrium.csv");
			CsvTools.writeCSVfile(DSEquilibriumListener, DSEquilibriumPath);
			Path averageUtilitiesPath = Paths.get(dir + "region_" + R.getName() + "-AverageUtilities.csv");
			CsvTools.writeCSVfile(averageUtilities, averageUtilitiesPath);
		}
	}

	public void exportFiles(int year, ConcurrentHashMap<String, Double> regionalSupply) {
		if (ConfigLoader.config.generate_output_files && CellsLoader.regions.size() > 1) {
			servicedemandListener(year, regionalSupply);
			compositionAFTListener(year);
			Tracker.trackSupply(year, R.getName());
			CSVFilesWriter();
		}
	}
}
