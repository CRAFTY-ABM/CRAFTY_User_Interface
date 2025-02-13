package de.cesr.crafty.model;

import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.cli.ConfigLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.DemandModel;
import de.cesr.crafty.dataLoader.ProjectLoader;
import de.cesr.crafty.dataLoader.ServiceWeightLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.utils.analysis.CustomLogger;

public class RegionClassifier {

	private static final CustomLogger LOGGER = new CustomLogger(RegionClassifier.class);
	public static ConcurrentHashMap<String, Region> regions;
	public static boolean regionalization = ConfigLoader.config.regionalization;

	public static void initialation() {
		regions = new ConcurrentHashMap<>();
		if (regionalization) {
			CellsLoader.regionsNamesSet.forEach(regionName -> {
				regions.put(regionName, new Region(regionName));
			});
			CellsLoader.hashCell.values()/* .parallelStream() */.forEach(c -> {
				if (c.getCurrentRegion() != null) {
					regions.get(c.getCurrentRegion()).getCells().put(c.getX() + "," + c.getY(), c);
				}
			});

			if (!ServiceSet.isRegionalServicesExisted()) {
				regionalization = false;
				initialation();
			}
		} else {
			String name = ProjectLoader.WorldName;
			regions.put(name, new Region(name));
			regions.get(name).setCells(CellsLoader.hashCell);
		}

		ServiceSet.initialseServices();
		serviceupdater();

		LOGGER.info("Regions: " + regions.keySet());
	}

	public static void serviceupdater() {
		DemandModel.updateRegionsDemand();
		ServiceWeightLoader.updateRegionsWeight();
		ServiceWeightLoader.updateWorldWeight();
		aggregateDemandToWorldServiceDemand();
	}

	public static void aggregateDemandToWorldServiceDemand() {
		for (int i = ProjectLoader.getStartYear(); i <= ProjectLoader.getEndtYear(); i++) {
			int year = i;
			ServiceSet.worldService.forEach((ns, s) -> {
				s.getDemands().put(year, 0.);
			});
			regions.values().forEach(r -> {
				r.getServicesHash().forEach((ns, s) -> {
					ServiceSet.worldService.get(ns).getDemands().merge(year, s.getDemands().get(year), Double::sum);
				});
			});
		}
	}

}
