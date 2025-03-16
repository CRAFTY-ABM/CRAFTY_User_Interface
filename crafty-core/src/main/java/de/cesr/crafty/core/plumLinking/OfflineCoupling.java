package de.cesr.crafty.core.plumLinking;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.general.Utils;

public class OfflineCoupling {

	public static Map<String, ConcurrentHashMap<Integer, Double>> aggreDemandsAllyesrs;
	public static Map<String, ConcurrentHashMap<Integer, Double>> aggrePricesAllyesrs;

	PlumCommodityMapping mapper = new PlumCommodityMapping();

	public OfflineCoupling() {
		mapper.initialize();
		mapper.fromPlumToDemands(ProjectLoader.getStartYear());
		initializeContainres();
	}

	private void initializeContainres() {
		aggreDemandsAllyesrs = new HashMap<>();
		aggrePricesAllyesrs = new HashMap<>();

		ServiceSet.worldService.keySet().forEach(serviceName -> {
			aggreDemandsAllyesrs.put(serviceName, new ConcurrentHashMap<>());
			aggrePricesAllyesrs.put(serviceName, new ConcurrentHashMap<>());
		});
	}

	void initialPlumDemandEquilibriumFactor() {
		int year = ProjectLoader.getStartYear();
		mapper.fromPlumToDemands(ProjectLoader.getStartYear());
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.R.getServicesHash().forEach((serviceName, service) -> {
				service.getDemands().put(year, mapper.totalDemands.get(serviceName));
				service.getWeights().put(year, mapper.totalPrice.get(serviceName));
			});
		});
		ModelRunner.demandEquilibrium();
	}

//	private void replaceCraftyDemandsAndPrice() {
//		for (int year = ProjectLoader.getStartYear(); year <= ProjectLoader.getEndtYear(); year++) {
//			yearlyPricesAndDemands(year);
//		}
//		writeDemandAndPrice();
//	}

	public void yearlyPricesAndDemands(int year) {
		System.out.println("convert Demands and Price year: " + year);
		mapper.fromPlumToDemands(year);
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.R.getServicesHash().forEach((serviceName, service) -> {
				service.getDemands().put(year, mapper.totalDemands.get(serviceName) / service.getCalibration_Factor());
				aggreDemandsAllyesrs.get(serviceName).put(year,
						mapper.totalDemands.get(serviceName) / service.getCalibration_Factor());
				service.getWeights().put(year, mapper.totalPrice.get(serviceName));
				aggrePricesAllyesrs.get(serviceName).put(year, mapper.totalPrice.get(serviceName));
			});
		});
		RegionClassifier.aggregateDemandToWorldServiceDemand();
	}

	void writeDemandAndPrice() {
		String[][] dem = new String[aggreDemandsAllyesrs.values().iterator().next().size()][aggreDemandsAllyesrs.size()
				+ 1];
		dem[0][0] = "Year";
		int k = 1;
		for (String iterator : aggreDemandsAllyesrs.keySet()) {
			dem[0][k] = iterator;
			k++;
		}

		for (int i = 1; i < dem[0].length; i++) {
			for (int j = 1; j < dem.length; j++) {
				dem[j][0] = ProjectLoader.getStartYear() + j + "";
				dem[j][i] = aggreDemandsAllyesrs.get(dem[0][i]).get((int) Utils.sToD(dem[j][0])) + "";
			}
		}
		String[][] pri = new String[aggrePricesAllyesrs.values().iterator().next().size()][aggrePricesAllyesrs.size()
				+ 1];
		for (int i = 0; i < dem[0].length; i++) {
			for (int j = 0; j < dem.length; j++) {
				if (i == 0 || j == 0) {
					pri[j][i] = dem[j][i];
				} else {
					pri[j][i] = aggrePricesAllyesrs.get(pri[0][i]).get((int) Utils.sToD(pri[j][0])) + "";
				}
			}
		}
		CsvTools.writeCSVfile(dem,
				Paths.get(ConfigLoader.config.output_folder_name + File.separator + "PlumDemands.csv"));
		CsvTools.writeCSVfile(pri,
				Paths.get(ConfigLoader.config.output_folder_name + File.separator + "PlumPrice.csv"));

	}

}
