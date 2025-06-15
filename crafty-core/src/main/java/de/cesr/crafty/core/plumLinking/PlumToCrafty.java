package de.cesr.crafty.core.plumLinking;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.model.Region;
import de.cesr.crafty.core.model.RegionClassifier;

public class PlumToCrafty {
	PlumCommodityMapping mapper = new PlumCommodityMapping();

	public void initialize() {
		mapper.initialize();
		mapper.fromPlumDataToDemandsAndPrice(ProjectLoader.getStartYear());
		replaceCraftyDemands(ProjectLoader.getStartYear());

		ModelRunner.regionsModelRunner.values().forEach(rRunner -> {
			rRunner.initialDSEquilibriumFactorCalculation();
		});
	}

	public void iterative(int year) {
		System.out.println("mapper.fromPlumTickToCraftyDemands(year)..." + year);
		mapper.fromPlumDataToDemandsAndPrice(year);
		System.out.println("	replaceCraftyDemands(year);..." + year);
		replaceCraftyDemands(year);
		// updateCalibrator();
	}

	void replaceCraftyDemands(int year) {
		int y = year - ProjectLoader.getStartYear();
		mapper.finalCountriesDemands.forEach((country, map) -> {
			if (RegionClassifier.regions.keySet().contains(country)) {
				map.forEach((serviceName, value) -> {
					if (ServiceSet.getServicesList().contains(serviceName)) {
						Region R = RegionClassifier.regions.get(country);
						R.getServicesHash().get(serviceName).getDemands().put(y, value);
						
					}
				});
			}
		});
	}

//
//	public void updateCalibrator() {
//		ModelRunner.regionsModelRunner.values().forEach(rRunner -> {
//			rRunner.initialDSEquilibrium();
//		});
//	}

}
