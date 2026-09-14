package de.cesr.crafty.core.updaters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.cli.CustomLogger;
import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.dataLoader.CsvKind;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.costs.GlobalCostData;
import de.cesr.crafty.core.utils.file.PathTools;

public class ProductionCostUpdater extends AbstractUpdater {
	private static final CustomLogger LOGGER = new CustomLogger(ProductionCostUpdater.class);

	/**
	 * The service whose production level doubles as the livestock stocking rate. An
	 * AFT that produces it is stocked, whatever category it belongs to.
	 */
	private static final String PASTURE_SERVICE = "Pasture";

	private static GlobalCostData globalCostData;
	private static List<String> nfertAftLabels = Collections.synchronizedList(new ArrayList<>());
	private static List<String> irrigatedAftLabels = Collections.synchronizedList(new ArrayList<>());
	private static List<String> intensityAftLabels = Collections.synchronizedList(new ArrayList<>());
	private static List<String> stockingAftLabels = Collections.synchronizedList(new ArrayList<>());

	private Map<Integer, Path> nfertCostPaths = new TreeMap<>();
	private Map<Integer, Path> irrigationCostPaths = new TreeMap<>();
	private Map<Integer, Path> intensityCostPaths = new TreeMap<>();
	private Map<Integer, Path> stockingCostPaths = new TreeMap<>();

	public ProductionCostUpdater() {
		if (!ConfigLoader.isUseProductionCosts()) {
			LOGGER.info("Production costs disabled");
			return;
		}

		buildAftLists();
		loadGlobalCosts();

		if (!ConfigLoader.isSpatialProductionCosts()) {
			cacheGlobalNfertCosts();
			cacheGlobalStockingCosts();
			cacheGlobalIntensityCosts();
			loadStaticIrrigationCost();
		} else {
			buildSpatialPathMaps();
		}
	}

	/*
	 * CLEANUP: which AFTs pay which cost is now decided by each AFT's own data
	 * rather than by its category name. These lists used to be gated on the literal
	 * strings "Agri_Crops", "Uncategorized" and "Natural", which meant an AFT that
	 * both cropped and grazed could never receive a fertiliser cost - the category
	 * was the gate, so a mixed AFT had to pick a side. The costs are independent and
	 * additive, and nothing here stops one AFT appearing in several lists.
	 */
	private void buildAftLists() {
		nfertAftLabels.clear();
		irrigatedAftLabels.clear();
		intensityAftLabels.clear();
		stockingAftLabels.clear();

		AFTsLoader.getAftHash().forEach((label, aft) -> {
			if (!aft.isInteract()) return;
			if (aft.getNfertRate() > 0) {
				nfertAftLabels.add(label);
			}
			if (aft.getProductivityLevel().getOrDefault(PASTURE_SERVICE, 0.0) > 0) {
				stockingAftLabels.add(label);
			}
			if (aft.isIrrigated()) {
				irrigatedAftLabels.add(label);
			}
			/*
			 * Intensity applies to every interacting AFT; no exclusion list is needed.
			 * In global mode the productivity-weighted sum is already zero for an AFT
			 * that produces nothing priced, and in spatial mode a column missing from
			 * the cost CSV is skipped. Note this is a behaviour change for AFTs that
			 * were previously excluded by category but do produce a priced service.
			 */
			intensityAftLabels.add(label);
		});
		LOGGER.info("Nfert AFTs: " + nfertAftLabels);
		LOGGER.info("Stocking AFTs: " + stockingAftLabels);
		LOGGER.info("Irrigated AFTs: " + irrigatedAftLabels);
		LOGGER.info("Intensity AFTs: " + intensityAftLabels);
	}

	private void loadGlobalCosts() {
		Path globalCostsCsv = findCostFile("global", "global_costs", ".csv");
		if (globalCostsCsv == null) {
			LOGGER.fatal("global_costs.csv not found but use_production_costs is true");
			return;
		}
		globalCostData = new GlobalCostData(globalCostsCsv);
	}

	private void cacheGlobalNfertCosts() {
		if (globalCostData == null) return;

		/*
		 * The "Nfert_rate column missing or all zero" fatal check used to live here.
		 * It is unreachable now that nfertAftLabels is built from the rates themselves:
		 * a non-empty list already guarantees at least one positive rate, and an empty
		 * one simply means no AFT declares that it fertilises.
		 */
		if (nfertAftLabels.isEmpty()) {
			LOGGER.info("No AFT declares a positive Nfert_rate; no fertiliser costs applied");
			return;
		}

		double unitCost = globalCostData.getNfertUnitCost();
		for (String label : nfertAftLabels) {
			Aft aft = AFTsLoader.getAftHash().get(label);
			if (aft != null) {
				aft.setNfertCostPerHa(unitCost * aft.getNfertRate());
				LOGGER.info("Global Nfert cost for " + label + ": " + aft.getNfertCostPerHa() + " $/ha");
			}
		}
	}

	private void cacheGlobalStockingCosts() {
		if (globalCostData == null) return;
		if (stockingAftLabels.isEmpty()) {
			LOGGER.info("No AFT produces " + PASTURE_SERVICE + "; no stocking costs applied");
			return;
		}

		double unitCost = globalCostData.getStockingUnitCost();
		if (unitCost <= 0.0) {
			LOGGER.warn("Stocking row missing or zero in global_costs.csv but " + stockingAftLabels.size()
					+ " AFT(s) produce " + PASTURE_SERVICE);
		}

		for (String label : stockingAftLabels) {
			Aft aft = AFTsLoader.getAftHash().get(label);
			if (aft == null) continue;
			// In global mode the production level IS the stocking rate.
			double stockingRate = aft.getProductivityLevel().getOrDefault(PASTURE_SERVICE, 0.0);
			aft.setStockingCostPerHa(unitCost * stockingRate);
			LOGGER.info("Global stocking cost for " + label + ": " + aft.getStockingCostPerHa() + " $/ha");
		}
	}

	private void cacheGlobalIntensityCosts() {
		if (globalCostData == null) return;
		Map<String, Double> serviceCosts = globalCostData.getIntensityCosts();

		for (String label : intensityAftLabels) {
			Aft aft = AFTsLoader.getAftHash().get(label);
			if (aft == null) continue;
			double totalCost = 0.0;
			for (Map.Entry<String, Double> entry : serviceCosts.entrySet()) {
				String serviceName = entry.getKey();
				double serviceCost = entry.getValue();
				double prodLevel = aft.getProductivityLevel().getOrDefault(serviceName, 0.0);
				totalCost += prodLevel * serviceCost;
			}
			/*
			 * Other_intensity scales the productivity-weighted sum, so two AFTs
			 * producing the same service at the same level can still differ in what
			 * their management costs. It defaults to 1.0, so this is a no-op for any
			 * project that does not populate the column.
			 */
			double scaledCost = totalCost * aft.getOtherIntensity();
			aft.setIntensityCostPerHa(scaledCost);
			LOGGER.info("Global intensity cost for " + label + ": " + scaledCost + " $/ha (unscaled "
					+ totalCost + " x Other_intensity " + aft.getOtherIntensity() + ")");
		}
	}

	private void loadStaticIrrigationCost() {
		Path irrigationCsv = findCostFile("spatial", "irrigation", "irrigation_cost", ".csv");
		if (irrigationCsv == null) {
			LOGGER.fatal("irrigation_cost.csv not found but use_production_costs is true (global mode)");
			return;
		}
		LOGGER.info("Loading static irrigation costs from: " + irrigationCsv);
		CsvProcessors.processCSV(irrigationCsv, CsvKind.IRRIGATION_COST);
	}

	/*
	 * CLEANUP: this method used to be three near-identical blocks inside one year
	 * loop, each resolving a per-year CSV and fatally erroring when it was missing.
	 * They differed only in the directory to look in, the filename prefix and the map
	 * to fill, which are the three parameters of resolveYearlyCostPaths() below.
	 *
	 * Each cost type's files are required only when at least one AFT is eligible for
	 * that cost. With eligibility gated on data rather than category, a project can
	 * legitimately have no fertilised, irrigated or pasture-producing AFTs, and must
	 * not be forced to supply files that no AFT would ever read.
	 */
	private void buildSpatialPathMaps() {
		String scenario = ProjectLoader.getScenario();
		if (!resolveYearlyCostPaths(scenario, "Nfert", "Nfert_costs", nfertCostPaths,
				!nfertAftLabels.isEmpty())) return;
		if (!resolveYearlyCostPaths(scenario, "irrigation", "Irrigation_costs", irrigationCostPaths,
				!irrigatedAftLabels.isEmpty())) return;
		if (!resolveYearlyCostPaths(scenario, "intensity", "Intensity_costs", intensityCostPaths,
				!intensityAftLabels.isEmpty())) return;
		resolveYearlyCostPaths(scenario, "stocking", "stocking_costs", stockingCostPaths,
				!stockingAftLabels.isEmpty());
	}

	/**
	 * Resolves one per-year cost CSV for every simulated year.
	 *
	 * @param required when true, a missing file for any year is fatal; when false the
	 *                 cost type is simply left unloaded, because no AFT would read it.
	 * @return false only when a <em>required</em> file is missing. A missing optional
	 *         file returns true, so that the caller carries on resolving the remaining
	 *         cost types rather than stopping at the first one it can skip.
	 */
	private boolean resolveYearlyCostPaths(String scenario, String dirName, String filePrefix,
			Map<Integer, Path> target, boolean required) {
		for (int year = Timestep.getStartYear(); year <= Timestep.getEndtYear(); year++) {
			Path path = findCostFile("spatial", dirName, scenario, filePrefix + "_" + year, ".csv");
			if (path == null) {
				if (required) {
					LOGGER.fatal("Spatial " + filePrefix + " CSV not found for year " + year);
					return false;
				}
				LOGGER.info("No spatial " + filePrefix + " CSV for year " + year
						+ "; no AFT is eligible for this cost, so it will not be applied");
				target.clear();
				return true;
			}
			target.put(year, path);
		}
		return true;
	}

	@Override
	public void step() {
		if (!ConfigLoader.isUseProductionCosts()) return;
		if (!ConfigLoader.isSpatialProductionCosts()) return;

		int year = Timestep.getCurrentYear();

		Path nfertPath = nfertCostPaths.get(year);
		if (nfertPath != null) {
			LOGGER.info("Loading spatial Nfert costs for year " + year);
			CsvProcessors.processCSV(nfertPath, CsvKind.NFERT_COST);
		}

		Path irrigationPath = irrigationCostPaths.get(year);
		if (irrigationPath != null) {
			LOGGER.info("Loading spatial irrigation costs for year " + year);
			CsvProcessors.processCSV(irrigationPath, CsvKind.IRRIGATION_COST);
		}

		Path intensityPath = intensityCostPaths.get(year);
		if (intensityPath != null) {
			LOGGER.info("Loading spatial intensity costs for year " + year);
			CsvProcessors.processCSV(intensityPath, CsvKind.INTENSITY_COST);
		}

		Path stockingPath = stockingCostPaths.get(year);
		if (stockingPath != null) {
			LOGGER.info("Loading spatial stocking costs for year " + year);
			CsvProcessors.processCSV(stockingPath, CsvKind.STOCKING_COST);
		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	private Path findCostFile(String... conditions) {
		String costsDir = ConfigLoader.config.costs_directory;
		ArrayList<Path> results;
		if (costsDir != null && !costsDir.isEmpty()) {
			ArrayList<Path> ps = PathTools.findAllFilePaths(Paths.get(costsDir));
			results = PathTools.fileFilter(ps, conditions);
		} else {
			results = PathTools.fileFilter(conditions);
		}
		if (results == null || results.isEmpty()) {
			return null;
		}
		return results.get(0);
	}

	public static GlobalCostData getGlobalCostData() {
		return globalCostData;
	}

	public static List<String> getNfertAftLabels() {
		return nfertAftLabels;
	}

	public static List<String> getIrrigatedAftLabels() {
		return irrigatedAftLabels;
	}

	public static List<String> getIntensityAftLabels() {
		return intensityAftLabels;
	}

	public static List<String> getStockingAftLabels() {
		return stockingAftLabels;
	}
}
