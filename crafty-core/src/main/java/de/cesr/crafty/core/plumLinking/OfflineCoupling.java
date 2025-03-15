package de.cesr.crafty.core.plumLinking;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;

public class OfflineCoupling {

	public static Map<String, ConcurrentHashMap<Integer, Double>> aggreDemandsAllyesrs ;
	public static Map<String, ConcurrentHashMap<Integer, Double>> aggrePricesAllyesrs ;

	PlumCommodityMapping mapper = new PlumCommodityMapping();

	public OfflineCoupling() {
		mapper.initialize();
		mapper.fromPlumToDemands(ProjectLoader.getStartYear());
	}

	public void replaceCraftyDemandsAndPrice() {
		 aggreDemandsAllyesrs = new HashMap<>();
		 aggrePricesAllyesrs = new HashMap<>();
		
		ServiceSet.worldService.keySet().forEach(serviceName -> {
			aggreDemandsAllyesrs.put(serviceName, new ConcurrentHashMap<>());
			aggrePricesAllyesrs.put(serviceName, new ConcurrentHashMap<>());

		});

		for (int year = ProjectLoader.getStartYear(); year < ProjectLoader.getEndtYear(); year++) {
			int y = year;
			System.out.println("convert year: " + y);
			mapper.fromPlumToDemands(year);
			mapper.totalDemands.forEach((serviceName, demandValue) -> {
				aggreDemandsAllyesrs.get(serviceName).put(y, demandValue);
			});
			mapper.totalPrice.forEach((serviceName, priceValue) -> {
				aggrePricesAllyesrs.get(serviceName).put(y, priceValue);
			});
		}
		ServiceSet.worldService.forEach((serviceName, service) -> {
			service.setDemands(aggreDemandsAllyesrs.get(serviceName));
			service.setWeights(aggrePricesAllyesrs.get(serviceName));
		});

	}

}
