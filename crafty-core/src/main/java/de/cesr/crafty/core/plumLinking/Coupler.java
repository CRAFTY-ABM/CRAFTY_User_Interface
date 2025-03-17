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
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class Coupler {

	public static Map<String, ConcurrentHashMap<Integer, Double>> aggreDemandsAllyesrs;
	public static Map<String, ConcurrentHashMap<Integer, Double>> aggrePricesAllyesrs;
	public Map<String, Map<String, Map<Integer, Double>>> aggreCountriesDemands;
	public Map<String, Map<String, Map<Integer, Double>>> aggreCountriesPrice;

	PlumCommodityMapping mapper = new PlumCommodityMapping();

	public Coupler() {
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

		if (ConfigLoader.config.regionalization) {
			aggreCountriesDemands = new HashMap<>();
			aggreCountriesPrice = new HashMap<>();
			ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
				Map<String, Map<Integer, Double>> tmpDemand = new HashMap<>();
				Map<String, Map<Integer, Double>> tmpPrice = new HashMap<>();
				RegionalRunner.R.getServicesHash().keySet().forEach((serviceName) -> {
					tmpDemand.put(serviceName, new HashMap<>());
					tmpPrice.put(serviceName, new HashMap<>());
				});
				aggreCountriesDemands.put(RegionalRunner.R.getName(), tmpDemand);
				aggreCountriesPrice.put(RegionalRunner.R.getName(), tmpPrice);
			});
		}
	}

	void initialEquilibruim() {
		if (ConfigLoader.config.regionalization) {
			initialPlumDemandsEquilibruimFactorRegion();
		} else {
			initialPlumDemandEquilibriumFactor();
		}
	}

	private void initialPlumDemandEquilibriumFactor() {
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

	private void initialPlumDemandsEquilibruimFactorRegion() {
		int year = ProjectLoader.getStartYear();
		mapper.fromPlumToDemands(ProjectLoader.getStartYear());
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.R.getServicesHash().forEach((serviceName, service) -> {
				service.getDemands().put(year,
						mapper.finalCountriesDemands.get(RegionalRunner.R.getName()).get(serviceName));
				service.getWeights().put(year,
						mapper.finalCountriesPrice.get(RegionalRunner.R.getName()).get(serviceName));
			});
		});
		ModelRunner.demandEquilibrium();
	}

	public void AssocietePricesAndDemand(int year) {
		if (ConfigLoader.config.regionalization) {
			yearlyPricesAndDemandsByRegion(year);
		} else {
			yearlyPricesAndDemands(year);
		}
	}

	private void yearlyPricesAndDemandsByRegion(int year) {
		System.out.println("convert Demands and Price year: " + year);
		mapper.fromPlumToDemands(year);
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			String country = RegionalRunner.R.getName();
			RegionalRunner.R.getServicesHash().forEach((serviceName, service) -> {
				double d = mapper.finalCountriesDemands.get(country).get(serviceName) / service.getCalibration_Factor();
				double p = mapper.finalCountriesPrice.get(country).get(serviceName);
				service.getDemands().put(year, d);
				service.getWeights().put(year, p);
				aggreCountriesDemands.get(country).get(serviceName).put(year, d);
				aggreCountriesPrice.get(country).get(serviceName).put(year, p);
			});
		});

		RegionClassifier.aggregateDemandToWorldServiceDemand();
	}

	private void yearlyPricesAndDemands(int year) {
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
		String[][] dem = new String[aggreDemandsAllyesrs.values().iterator().next().size()
				+ 1][aggreDemandsAllyesrs.size() + 1];
		dem[0][0] = "Year";
		int k = 1;
		for (String iterator : aggreDemandsAllyesrs.keySet()) {
			dem[0][k++] = iterator;
		}

		for (int i = 1; i < dem[0].length; i++) {
			for (int j = 1; j < dem.length; j++) {
				dem[j][0] = ProjectLoader.getStartYear() + j - 1 + "";
				dem[j][i] = aggreDemandsAllyesrs.get(dem[0][i]).get((int) Utils.sToD(dem[j][0])) + "";
			}
		}
		String[][] pri = new String[dem.length][dem[0].length];
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
				Paths.get(ConfigLoader.config.output_folder_name + File.separator + "PlumPriceMean.csv"));
	}

	void writeRegion() {
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			String country = RegionalRunner.R.getName();
			Map<String, Map<Integer, Double>> dataDemand = aggreCountriesDemands.get(country);
//			System.out.println(country + "=>" + dataDemand);
			String[][] dem = new String[dataDemand.values().iterator().next().size() + 1][dataDemand.size() + 1];
			dem[0][0] = "Year";
			int k = 1;
			for (String iterator : dataDemand.keySet()) {
				dem[0][k++] = iterator;
			}

			for (int i = 1; i < dem[0].length; i++) {
				for (int j = 1; j < dem.length; j++) {
					dem[j][0] = ProjectLoader.getStartYear() + j - 1 + "";
					dem[j][i] = dataDemand.get(dem[0][i]).get((int) Utils.sToD(dem[j][0])) + "";
				}
			}
			CsvTools.writeCSVfile(dem, Paths.get(ConfigLoader.config.output_folder_name
					+ PathTools.asFolder("region_" + country) + "PlumDemands_" + country + ".csv"));
			Map<String, Map<Integer, Double>> dataPrice = aggreCountriesPrice.get(country);
			String[][] pri = new String[dem.length][dem[0].length];
			for (int i = 0; i < dem[0].length; i++) {
				for (int j = 0; j < dem.length; j++) {
					if (i == 0 || j == 0) {
						pri[j][i] = dem[j][i];
					} else {
						pri[j][i] = dataPrice.get(pri[0][i]).get((int) Utils.sToD(pri[j][0])) + "";
					}
				}
			}
			CsvTools.writeCSVfile(pri, Paths.get(ConfigLoader.config.output_folder_name
					+ PathTools.asFolder("region_" + country) + "PlumPrices_" + country + ".csv"));
		});

	}

}
