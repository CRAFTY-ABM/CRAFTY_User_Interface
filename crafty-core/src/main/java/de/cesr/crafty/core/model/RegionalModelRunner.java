package de.cesr.crafty.core.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.output.ListenerByRegion;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.general.Utils;

/**
 * @author Mohamed Byari
 *
 */

public class RegionalModelRunner {
	private static final CustomLogger LOGGER = new CustomLogger(RegionalModelRunner.class);
	ConcurrentHashMap<String, Double> regionalSupply;
	ConcurrentHashMap<String, Double> marginal = new ConcurrentHashMap<>();
	ConcurrentHashMap<Aft, Double> distributionMean;
	public Region R;

	public ListenerByRegion listner;

	public RegionalModelRunner(String regionName) {
		R = RegionClassifier.regions.get(regionName);
		listner = new ListenerByRegion(R);
		listner.initializeListeners();
	}

	private void calculeRegionsSupply() {
		regionalSupply = new ConcurrentHashMap<>();
		R.getCells().values().parallelStream().forEach(c -> {
			c.currentProductivity.forEach((s, v) -> {
				regionalSupply.merge(s, v, Double::sum);
			});
		});
	}

	private void productivityForAll() {
		R.getCells().values().parallelStream().forEach(cell -> cell.calculateCurrentProductivity(R));
	}

	private void calculeDistributionMean() {
		LOGGER.info("Region: [" + R.getName() + "] Calculating Distribution Mean & Land abandonment");
		distributionMean = new ConcurrentHashMap<>();
		R.getCells().values().parallelStream().forEach(c -> {
			if (c.getOwner() != null) {
				distributionMean.merge(c.getOwner(), Competitiveness.utility(c, c.getOwner(), this), Double::sum);
			}
		});
		AFTsLoader.getActivateAFTsHash().values().forEach(a -> distributionMean.computeIfAbsent(a, key -> 0.));

		// Calculate the mean distribution
		distributionMean.forEach((a, total) -> {
			distributionMean.put(a, total / AFTsLoader.hashAgentNbrRegions.get(R.getName()).get(a.label));
		});
	}

	private void calculeMarginal(int year) {
		LOGGER.info("Rigion: [" + R.getName() + "] Total Supply = " + regionalSupply);
		regionalSupply.forEach((serviceName, serviceSupply) -> {
			Service s = R.getServicesHash().get(serviceName);
			double serviceDemand = s.getDemands().get(year);
			double marg = ConfigLoader.config.remove_negative_marginal_utility
					? Math.max(serviceDemand - serviceSupply, 0)
					: serviceDemand - serviceSupply;
			if (ConfigLoader.config.averaged_residual_demand_per_cell) {
				marg = marg / R.getCells().size();
			}
			marg = marg * s.getWeights().get(year);
			marginal.put(serviceName, marg);
		});
	}

	void takeOverUnmanageCells() {
		LOGGER.trace("Region: [" + R.getName() + "] Take over unmanaged cells & Launching the competition process...");
		R.getUnmanageCellsR().parallelStream().forEach(c -> {
			Competitiveness.competition(c, this);
			if (c.getOwner() != null && !c.getOwner().isAbandoned()) {
				R.getUnmanageCellsR().remove(c);
			}
		});
	}

	public void regionalSupply() {
		if (RegionClassifier.regionalization) {
			productivityForAll();
		} else {
			productivityForAllExecutor();
		}
		calculeRegionsSupply();
		LOGGER.info("Region: [" + R.getName() + "] Total Supply calculation" + regionalSupply);

	}

	public void initialDSEquilibriumFactorCalculation() {
		// update the capital first year
		regionalSupply();
		regionalSupply.forEach((serviceName, serviceSuplly) -> {
			double factor = 1;
			if (serviceSuplly != 0) {
				if (R.getServicesHash().get(serviceName).getDemands().get(ProjectLoader.getStartYear()) == 0) {
					LOGGER.warn("Demand for " + serviceName + " = 0");
				} else {
					factor = R.getServicesHash().get(serviceName).getDemands().get(ProjectLoader.getStartYear())
							/ (serviceSuplly);
				}
			} else {
				// factor = Double.MAX_VALUE;
				LOGGER.warn("Supply for " + serviceName + " = 0 (The AFT baseline map is unable to produce  "
						+ serviceName + " service => this service will not be standardised");
			}
			R.getServicesHash().get(serviceName).setCalibration_Factor(factor);
		});
		listner.fillDSEquilibriumListener(R.getServicesHash());
		LOGGER.info(
				"Initial Demand Service Equilibrium Factor= " + R.getName() + ": " + R.getServiceCalibration_Factor());
	}

	public void step(int year) {
		listner.exportFiles(year, regionalSupply);

		calculeMarginal(year);
		calculeDistributionMean();
		calculeMarginal(year);
		giveUp();
		takeOverUnmanageCells();
		competition(year);
		AFTsLoader.hashAgentNbr(R.getName());
	}

	private void giveUp() {
		if (ConfigLoader.config.use_abandonment_threshold) {
			ConcurrentHashMap<String, Cell> randomCellsubSetForGiveUp = CellsLoader.getRandomSubset(R.getCells(),
					ConfigLoader.config.land_abandonment_percentage);
			if (randomCellsubSetForGiveUp != null) {
				randomCellsubSetForGiveUp.values().parallelStream().forEach(c -> {
					c.giveUp(this, distributionMean);
				});
			}
		}
	}

	private void competition(int year) {
		// Randomly select % of the land available for competition
		ConcurrentHashMap<String, Cell> randomCellsubSet = CellsLoader.getRandomSubset(R.getCells(),
				ConfigLoader.config.participating_cells_percentage);
		if (randomCellsubSet != null) {
			List<ConcurrentHashMap<String, Cell>> subsubsets = Utils.splitIntoSubsets(randomCellsubSet,
					ConfigLoader.config.marginal_utility_calculations_per_tick);
			ConcurrentHashMap<String, Double> servicesBeforeCompetition = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, Double> servicesAfterCompetition = new ConcurrentHashMap<>();
			subsubsets.forEach(subsubset -> {
				if (subsubset != null) {
					subsubset.values().parallelStream().forEach(c -> {
						if (c.getOwner() != null && c.getOwner().isActive()) {
							if (c.getCurrentProductivity().size() > 0) {
								c.getCurrentProductivity().forEach(
										(key, value) -> servicesBeforeCompetition.merge(key, value, Double::sum));
								Competitiveness.competition(c, this);
								c.calculateCurrentProductivity(R);
								c.getCurrentProductivity().forEach(
										(key, value) -> servicesAfterCompetition.merge(key, value, Double::sum));
							}
						}
					});
				}
				servicesBeforeCompetition.forEach((key, value) -> regionalSupply.merge(key, -value, Double::sum));
				servicesAfterCompetition.forEach((key, value) -> regionalSupply.merge(key, value, Double::sum));
				calculeMarginal(year);
			});
		}
	}

	private void productivityForAllExecutor() {
		LOGGER.info("Productivity calculation for all cells ");
		final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Map<String, Cell>> partitions = Utils.partitionMap(R.getCells(), 10); // Partition into 10
																					// sub-maps
		try {
			for (Map<String, Cell> subMap : partitions) {
				executor.submit(
						() -> subMap.values().parallelStream().forEach(c -> c.calculateCurrentProductivity(R)));
			}
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
			} // Wait for all tasks to complete
		}
	}

}
