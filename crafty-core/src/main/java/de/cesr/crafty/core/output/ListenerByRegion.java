package de.cesr.crafty.core.output;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.model.Region;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.model.Service;
import de.cesr.crafty.core.utils.analysis.Tracker;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class ListenerByRegion {
	Region R;
	private String[][] compositionAftListener;
	private String[][] servicedemandListener;
	public String[][] DSEquilibriumListener;

	public ListenerByRegion(Region R) {
		this.R = R;
	}

	public void initializeListeners() {
		compositionAftListener = new String[ProjectLoader.getEndtYear() - ProjectLoader.getStartYear()
				+ 2][AFTsLoader.getAftHash().size() + 1];
		servicedemandListener = new String[ProjectLoader.getEndtYear() - ProjectLoader.getStartYear()
				+ 2][ServiceSet.getServicesList().size() * 2 + 1];
		servicedemandListener[0][0] = "Year";
		for (int i = 1; i < ServiceSet.getServicesList().size() + 1; i++) {
			servicedemandListener[0][i] = "Supply:" + ServiceSet.getServicesList().get(i - 1);
			servicedemandListener[0][i + ServiceSet.getServicesList().size()] = "Demand:"
					+ ServiceSet.getServicesList().get(i - 1);
		}
		compositionAftListener[0][0] = "Year";
		int j = 1;
		for (String label : AFTsLoader.getAftHash().keySet()) {
			compositionAftListener[0][j++] = label;
		}
		DSEquilibriumListener = new String[ServiceSet.getServicesList().size() + 1][2];
		DSEquilibriumListener[0][0] = "Service";
		DSEquilibriumListener[0][1] = "Calibration_Factor";
	}

	public void fillDSEquilibriumListener(ConcurrentHashMap<String, Service> ServiceHash) {
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			DSEquilibriumListener[i + 1][0] = ServiceSet.getServicesList().get(i);
			DSEquilibriumListener[i + 1][1] = ServiceHash.get(ServiceSet.getServicesList().get(i))
					.getCalibration_Factor() + "";
		}
	}

	private void servicedemandListener(int year, ConcurrentHashMap<String, Double> regionalSupply) {
		AtomicInteger m = new AtomicInteger(1);
		int y = year - ProjectLoader.getStartYear() + 1;
		servicedemandListener[y][0] = year + "";
		ServiceSet.getServicesList().forEach(name -> {
			servicedemandListener[y][m.get()] = regionalSupply.get(name) + "";
			servicedemandListener[y][m.get() + ServiceSet.getServicesList().size()] = (R.getServicesHash().get(name)
					.getDemands().get(year)) + "";
			m.getAndIncrement();
		});
	}

	private void compositionAFTListener(int year) {
		int y = year - ProjectLoader.getStartYear() + 1;
		compositionAftListener[y][0] = year + "";
		AFTsLoader.hashAgentNbrRegions.get(R.getName()).forEach((name, value) -> {
			compositionAftListener[y][Utils.indexof(name, compositionAftListener[0])] = value + "";
		});
	}

	private void CSVFilesWriter() {
		String dir = PathTools.makeDirectory(ConfigLoader.config.output_folder_name + File.separator + "region_"
				+ R.getName() + File.separator + "");
		if (ConfigLoader.config.generate_csv_files) {
			Path aggregateAFTComposition = Paths.get(dir + "region_" + R.getName() + "-AggregateAFTComposition.csv");
			CsvTools.writeCSVfile(compositionAftListener, aggregateAFTComposition);
			Path aggregateServiceDemand = Paths.get(dir + "region_" + R.getName() + "-AggregateServiceDemand.csv");
			CsvTools.writeCSVfile(servicedemandListener, aggregateServiceDemand);
			Path DSEquilibriumPath = Paths.get(dir + "region_" + R.getName() + "-DemandServicesEquilibrium.csv");
			CsvTools.writeCSVfile(DSEquilibriumListener, DSEquilibriumPath);
		}
	}

	public void exportFiles(int year, ConcurrentHashMap<String, Double> regionalSupply) {
		if (ConfigLoader.config.generate_csv_files && RegionClassifier.regions.size() > 1) {
			servicedemandListener(year, regionalSupply);
			Tracker.trackSupply(year, R.getName());
			compositionAFTListener(year);
			CSVFilesWriter();
		}
	}
}
