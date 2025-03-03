package de.cesr.crafty.core.dataLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.model.Region;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;



public class DemandModel {
	private static final CustomLogger LOGGER = new CustomLogger(DemandModel.class);

	public static void updateRegionsDemand() {
		LOGGER.info("update Regions Demand...");
		RegionClassifier.regions.values().forEach(r -> {
			updateDemand(r);
		});
	}

	private static void updateDemand(Region R) {
		Path path;
		try {
			path = PathTools.fileFilter(ProjectLoader.getScenario(), PathTools.asFolder("demand"), "_" + R.getName())
					.get(0);
		} catch (NullPointerException e) {
			LOGGER.warn("No demand file fund for region: |" + R.getName() + "|");
			return;
		}

		HashMap<String, ArrayList<String>> hashDemand = ReaderFile.ReadAsaHash(path);
		LOGGER.info("Update Demand for [" + R.getName() + "]: " + path);
		hashDemand.forEach((serviceName, vect) -> {
			if (ServiceSet.getServicesList().contains(serviceName)) {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < ProjectLoader.getEndtYear() - ProjectLoader.getStartYear() + 1; i++) {
					if (i < vect.size()) {
						dv.put(ProjectLoader.getStartYear() + i, Utils.sToD(vect.get(i)));
					}
				}
				R.getServicesHash().get(serviceName).getDemands().clear();
				R.getServicesHash().get(serviceName).setDemands(dv);
			}
		});
	}

	public static Map<String, ArrayList<Double>> serialisationWorldDemand() {
		Map<String, ArrayList<Double>> serviceSerialisation = new HashMap<>();
		ServiceSet.worldService.forEach((serviceName, service) -> {
			serviceSerialisation.put(serviceName, new ArrayList<>(service.getDemands().values()));
		});
		return serviceSerialisation;
	}
}
