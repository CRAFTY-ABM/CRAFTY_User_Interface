package de.cesr.crafty.core.dataLoader.serivces;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.crafty.Region;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

// Service Weight loader calss
public class ServiceWeightLoader {
	private static final CustomLogger LOGGER = new CustomLogger(ServiceWeightLoader.class);

	public static void updateRegionsWeight() {
		CellsLoader.regions.values().forEach(r -> {
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
			LOGGER.info("Utility Weight file not fund for |" + R.getName()
					+ "| default value will use: Utility_Weights = 1");
			ServiceSet.getServicesList().forEach((serviceName) -> {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < Timestep.getEndtYear() - Timestep.getStartYear() + 1; i++) {
					dv.put(i + Timestep.getStartYear(), 1.);
				}
				R.getServicesHash().get(serviceName).setWeights(dv);
			});
			return;
		}
		LOGGER.info("Update Weight for [" + R + "]: ");
		Map<String, List<String>> hashWeight = CsvProcessors.ReadAsaHash(path);

		hashWeight.forEach((serviceName, vect) -> {
			if (ServiceSet.getServicesList().contains(serviceName)) {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < Timestep.getEndtYear() - Timestep.getStartYear() + 1; i++) {
					if (i < vect.size()) {
						dv.put(i + Timestep.getStartYear(), Utils.sToD(vect.get(i)));
					}
				}
				R.getServicesHash().get(serviceName).setWeights(dv);
			}
		});
	}

	// change the WorldWeight for plot
	public static Map<String, List<Double>> serialisationWorldWeight() {
		Map<String, List<Double>> serviceSerialisation = new HashMap<>();
		ServiceSet.worldService.forEach((serviceName, service) -> {
			serviceSerialisation.put(serviceName, new ArrayList<>(service.getWeights().values()));
		});
		return serviceSerialisation;
	}

}
