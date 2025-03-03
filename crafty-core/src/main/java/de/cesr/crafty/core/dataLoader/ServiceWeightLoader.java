package de.cesr.crafty.core.dataLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import de.cesr.crafty.core.model.Region;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

// Service Weight loader calss
public class ServiceWeightLoader {
	private static final CustomLogger LOGGER = new CustomLogger(ServiceWeightLoader.class);

	private static Path weighWolrldtPath() {
		AtomicReference<String> path = new AtomicReference<>("");
		try {
			path.set(PathTools.fileFilter(ProjectLoader.getScenario(), PathTools.asFolder("Service_Utility_Weights"),
					ProjectLoader.WorldName).get(0).toString());
		} catch (NullPointerException e) {
			LOGGER.warn("No Weight file fund for region: |" + ProjectLoader.WorldName
					+ "| will use 1 for all Service Utility Weights ");
			return null;
		}
		return Paths.get(path.get());
	}

	public static void updateWorldWeight() {
		if (RegionClassifier.regions.size() > 1) {
			Path path = weighWolrldtPath();
			if (path != null) {
				HashMap<String, ArrayList<String>> hashDemand = ReaderFile.ReadAsaHash(path);
				LOGGER.info("Update Demand: " + path);
				hashDemand.forEach((name, vect) -> {
					if (ServiceSet.getServicesList().contains(name)) {
						for (int i = 0; i < ProjectLoader.getEndtYear() - ProjectLoader.getStartYear() + 1; i++) {
							if (i < vect.size()) {
								ServiceSet.worldService.get(name).getWeights().put(i, Utils.sToD(vect.get(i)));
							} else {
								ServiceSet.worldService.get(name).getWeights().put(i,
										Utils.sToD(vect.get(vect.size() - 1)));
								LOGGER.info("There are no demand \'" + name + "\' for this year: \""
										+ (i + ProjectLoader.getStartYear()) + "\" using the latest available demands "
										+ (vect.size() - 1 + ProjectLoader.getStartYear()));
							}
						}
					}
				});
			} else {
				ServiceSet.getServicesList().forEach((serviceName) -> {
					for (int i = 0; i < ProjectLoader.getEndtYear() - ProjectLoader.getStartYear() + 1; i++) {
						ServiceSet.worldService.get(serviceName).getWeights().put(i, 1.);
					}

				});
			}
		}
	}

	public static void updateRegionsWeight() {
		RegionClassifier.regions.values().forEach(r -> {
			r.getServicesHash().values().forEach(s -> {
				s.getWeights().clear();
			});
			updateWeight(r);
		});
	}

	private static void updateWeight(Region R) {
		Path path;
		try {
			path = PathTools
					.fileFilter(ProjectLoader.getScenario(), PathTools.asFolder("Service_Utility_Weights"), R.getName())
					.get(0);
		} catch (NullPointerException e) {
			LOGGER.warn("No Weight file fund for region: |" + R.getName()
					+ "|  will use 1 for all Service Utility Weights ");
			ServiceSet.getServicesList().forEach((serviceName) -> {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < ProjectLoader.getEndtYear() - ProjectLoader.getStartYear() + 1; i++) {
					dv.put(i+ProjectLoader.getStartYear(), 1.);
				}
				R.getServicesHash().get(serviceName).setWeights(dv);

			});
			return;
		}
		HashMap<String, ArrayList<String>> hashWeight = ReaderFile.ReadAsaHash(path);
		LOGGER.info("Update Weight for [" + R + "]: " + path);

		hashWeight.forEach((serviceName, vect) -> {
			if (ServiceSet.getServicesList().contains(serviceName)) {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < ProjectLoader.getEndtYear() - ProjectLoader.getStartYear() + 1; i++) {
					if (i < vect.size()) {
						dv.put(i+ProjectLoader.getStartYear(), Utils.sToD(vect.get(i)));
					}
				}
				R.getServicesHash().get(serviceName).setWeights(dv);
			}
		});
	}

}
