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

public class ServiceDemandLoader {
	private static final CustomLogger LOGGER = new CustomLogger(ServiceDemandLoader.class);

	public static void updateRegionsDemand() {
		CellsLoader.regions.values().forEach(r -> {
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
		LOGGER.info("Update Demand for [" + R.getName() + "]: ");
		Map<String, List<String>> hashDemand = CsvProcessors.ReadAsaHash(path);

		hashDemand.forEach((serviceName, vect) -> {
			if (ServiceSet.getServicesList().contains(serviceName)) {
				ConcurrentHashMap<Integer, Double> dv = new ConcurrentHashMap<>();
				for (int i = 0; i < Timestep.getEndtYear() - Timestep.getStartYear() + 1; i++) {
					if (i < vect.size()) {
						dv.put(Timestep.getStartYear() + i, Utils.sToD(vect.get(i)));
					}
				}
				R.getServicesHash().get(serviceName).getDemands().clear();
				R.getServicesHash().get(serviceName).setDemands(dv);
				LOGGER.trace("Demand for [" + serviceName + "]: " + R.getServicesHash().get(serviceName).getDemands());
			}
		});

	}

	public static void aggregateRegionalToWorldServiceDemand() {
		for (int i = Timestep.getStartYear(); i <= Timestep.getEndtYear(); i++) {
			int year = i;
			ServiceSet.worldService.forEach((ns, s) -> {
				s.getDemands().put(year, 0.);
			});
			CellsLoader.regions.values().forEach(r -> {
				r.getServicesHash().forEach((ns, s) -> {
					double nbr = s.getDemands().get(year) != null ? s.getDemands().get(year) : 0;
					ServiceSet.worldService.get(ns).getDemands().merge(year, nbr, Double::sum);
					ServiceSet.worldService.get(ns).getWeights().merge(year,
							s.getWeights().get(year) / CellsLoader.regions.size(), Double::sum);
				});
			});
		}
	}

	// change the WorldDemand for plot
	public static Map<String, List<Double>> serialisationWorldDemand() {
		Map<String, List<Double>> serviceSerialisation = new HashMap<>();
		ServiceSet.worldService.forEach((serviceName, service) -> {
			serviceSerialisation.put(serviceName, new ArrayList<>(service.getDemands().values()));
		});
		return serviceSerialisation;
	}

}
