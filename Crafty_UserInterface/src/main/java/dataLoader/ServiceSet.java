package dataLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import model.RegionClassifier;
import model.Service;
import utils.analysis.CustomLogger;
import utils.filesTools.PathTools;
import utils.filesTools.ReaderFile;

public class ServiceSet {
	private static final CustomLogger LOGGER = new CustomLogger(ServiceSet.class);
	private static List<String> servicesList;
	public static ConcurrentHashMap<String, Service> worldService = new ConcurrentHashMap<>();

	public static void initialseServices() {
		RegionClassifier.regions.values().forEach(r -> {
			getServicesList().forEach(n -> {
				r.getServicesHash().put(n, new Service(n));
			});
		});
		getServicesList().forEach((ns) -> {
			worldService.put(ns, new Service(ns));
		});
	}

	public static void loadServiceList() {
		servicesList = Collections.synchronizedList(new ArrayList<>());
		HashMap<String, ArrayList<String>> servicesFile = ReaderFile
				.ReadAsaHash(PathTools.fileFilter(File.separator + "Services.csv").get(0));
		String label = servicesFile.keySet().contains("Label") ? "Label" : "Name";
		servicesList = servicesFile.get(label);
		LOGGER.info("Services size=  " + servicesList.size() + "  CellsSet.getServicesList()=" + servicesList);
	}

	public static boolean isRegionalServicesExisted() {
		for (String r : RegionClassifier.regions.keySet()) {
			ArrayList<Path> paths = PathTools.fileFilter(PathsLoader.getScenario(), PathTools.asFolder("demand"), r);
			if (paths == null) {
				return false;
			}
		}
		return true;
	}

	public static List<String> getServicesList() {
		return servicesList;
	}
}
