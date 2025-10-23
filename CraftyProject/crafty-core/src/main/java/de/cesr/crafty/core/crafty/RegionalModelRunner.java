package de.cesr.crafty.core.crafty;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.output.ListenerByRegion;
import de.cesr.crafty.core.updaters.ServicesUpdater;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.general.Selector;
import de.cesr.crafty.core.utils.general.Utils;

/**
 * @author Mohamed Byari
 *
 */

public class RegionalModelRunner {
	private static final CustomLogger LOGGER = new CustomLogger(RegionalModelRunner.class);
	private ConcurrentHashMap<String, Double> regionalSupply;
	ConcurrentHashMap<String, Double> marginal = new ConcurrentHashMap<>();
	ConcurrentHashMap<Aft, Double> distributionMean;
	double maxUtility, minUtility;
	public Region R;

	public ListenerByRegion listner;

	public RegionalModelRunner(String regionName) {
		R = CellsLoader.regions.get(regionName);
		listner = new ListenerByRegion(R);
		listner.initializeListeners();
	}

	private void calculeRegionsSupply() {
		setRegionalSupply(new ConcurrentHashMap<>());
		R.getCells().values()/**/.parallelStream().forEach(c -> {
			c.currentProductivity.forEach((s, v) -> {
				getRegionalSupply().merge(s, v, Double::sum);
			});
		});
	}

	private void productivityForAll() {
		R.getCells().values()/**/.parallelStream().forEach(cell -> cell.calculateCurrentProductivity(R));
	}

	private void calculeDistributionMean() {
		distributionMean = new ConcurrentHashMap<>();
		// Calculate the mean distribution
		R.getCells().values()/**/.parallelStream().forEach(c -> {
			if (c.getOwner() != null) {
				distributionMean.merge(c.getOwner(), Competitiveness.utility(c, c.getOwner(), this)
						/ AFTsLoader.hashAgentNbrRegions.get(R.getName()).get(c.getOwner().label), Double::sum);
			}
		});
		AFTsLoader.getActivateAFTsHash().values().forEach(a -> distributionMean.computeIfAbsent(a, key -> 0.));

		StringJoiner joiner = new StringJoiner(", ", "Region: [" + R.getName() + "]: Distribution Mean: {", "}");
		for (Aft a : distributionMean.keySet()) {
			joiner.add(a.getLabel() + "= " + distributionMean.get(a));
		}
		LOGGER.info(joiner.toString());
//		System.out.println(joiner.toString());
	}

	private void calculeMaxMinUtility() {
		DoubleSummaryStatistics stats = R.getCells().values().parallelStream().filter(c -> c.getOwner() != null)
				.mapToDouble(c -> Competitiveness.utility(c, c.getOwner(), this)).summaryStatistics();
		minUtility = stats.getMin();
		maxUtility = stats.getMax();
	}

	private void calculeMarginal(int year) {
		getRegionalSupply().forEach((serviceName, serviceSupply) -> {
//			Service s = R.getServicesHash().get(serviceName);
			double serviceDemand = ServicesUpdater.getDemandByRegions().get(R.getName()).get(serviceName); // s.getDemands().get(year);
			double serviceWeight = ServicesUpdater.getWeightByRegions().get(R.getName()).get(serviceName);// s.getWeights().get(year)
			double marg = ServiceSet.getPenalise_Oversupply().get(serviceName) ? serviceDemand - serviceSupply
					: Math.max(serviceDemand - serviceSupply, 0);
//			  marg= ConfigLoader.config.remove_negative_marginal_utility ?
//			  Math.max(serviceDemand - serviceSupply, 0) : serviceDemand - serviceSupply;

			if (ConfigLoader.config.averaged_residual_demand_per_cell) {
				marg = marg / R.getCells().size();
			}
			marg = marg * serviceWeight;
			marginal.put(serviceName, marg);
		});
	}

	void takeOverUnmanageCells() {
		LOGGER.trace("Region: [" + R.getName() + "] Take over unmanaged cells & Launching the competition process...");
		R.getUnmanageCellsR()/**/.parallelStream().forEach(c -> {
			if (Math.random() < ConfigLoader.config.takeOverUnmanageCells_percentage) {
				Competitiveness.competition(c, this);
				if (c.getOwner() != null && !c.getOwner().isAbandoned()) {
					R.getUnmanageCellsR().remove(c);
					Listener.landUseChangeCounter.getAndIncrement();
				}
			}
		});
	}

	public void regionalSupply() {
		if (CellsLoader.regionalization) {
			productivityForAll();
		} else {
			productivityForAllExecutor();
		}
		calculeRegionsSupply();
		LOGGER.info("Rigion: [" + R.getName() + "] Total Supply = " + getRegionalSupply());

	}

	public void initialDSEquilibriumFactorCalculation() {
		// update the capital first year
		regionalSupply();
		getRegionalSupply().forEach((serviceName, serviceSuplly) -> {
			double factor = 1;
			if (serviceSuplly != 0) {
				if (R.getServicesHash().get(serviceName).getDemands().get(Timestep.getStartYear()) == 0) {
					LOGGER.warn("Demand for " + serviceName + " = 0");
				} else {
					factor = R.getServicesHash().get(serviceName).getDemands().get(Timestep.getStartYear())
							/ (serviceSuplly);
				}
			} else {
				ServiceSet.NoInitialSupplyServices.get(R.getName()).add(serviceName);
				LOGGER.warn("Baseline supply for |" + serviceName + "| in |" + R.getName() + "| is 0"
						+ " - Use Default Calibration_Factor = 1");
			}
			R.getServicesHash().get(serviceName).setCalibration_Factor(factor);
		});

		listner.fillDSEquilibriumListener(R.getServicesHash());
		LOGGER.info(
				"Initial Demand Service Equilibrium Factor= " + R.getName() + ": " + R.getServiceCalibration_Factor());
	}

	public void step(int year) {
		listner.exportFiles(year, getRegionalSupply());
		calculeMarginal(year);
		calculeDistributionMean();
//		if (AftCategorised.useCategorisationGivIn && CellBehaviourUpdater.behaviourUsed) {
		calculeMaxMinUtility();
//		}
		giveUp();
//		System.out.println("getUnmanageCellsR.size:  " + R.getUnmanageCellsR().size());
		takeOverUnmanageCells();
		competition(year);
		AFTsLoader.hashAgentNbr(R.getName());
//		System.out.println("countR= "+countR);
//		System.out.println("countNR= "+countNR);

	}

	private void giveUp() {
		if (ConfigLoader.config.use_abandonment_threshold) {
			ConcurrentHashMap<String, Cell> randomCellsubSetForGiveUp = Selector.randomSeed(R.getCells(),
					ConfigLoader.config.land_abandonment_percentage, ConfigLoader.config.seedID+200);
			if (randomCellsubSetForGiveUp != null) {
				randomCellsubSetForGiveUp.values()/**/.parallelStream().forEach(c -> {
					c.giveUp(this, distributionMean);

				});
			}
		}
	}

	private void competition(int year) {
		// Randomly select % of the land available for competition
		ConcurrentHashMap<String, Cell> randomCellsubSet = Selector.randomSeed(R.getCells(),
				ConfigLoader.config.participating_cells_percentage, ConfigLoader.config.seedID);
		if (randomCellsubSet != null) {
			List<ConcurrentHashMap<String, Cell>> subsubsets = Utils.splitIntoSubsets(randomCellsubSet,
					ConfigLoader.config.marginal_utility_calculations_per_tick);
			ConcurrentHashMap<String, Double> servicesBeforeCompetition = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, Double> servicesAfterCompetition = new ConcurrentHashMap<>();
			subsubsets.forEach(subsubset -> {
				if (subsubset != null) {
					subsubset.values()/**/.parallelStream().forEach(c -> {
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
				servicesBeforeCompetition.forEach((key, value) -> getRegionalSupply().merge(key, -value, Double::sum));
				servicesAfterCompetition.forEach((key, value) -> getRegionalSupply().merge(key, value, Double::sum));
				calculeMarginal(year);
			});
		}
	}

	private void productivityForAllExecutor() {// double multithreding
		LOGGER.info("Productivity calculation for all cells ");
		final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Map<String, Cell>> partitions = Utils.partitionMap(R.getCells(), 10); // Partition into 10
																					// sub-maps
		try {
			for (Map<String, Cell> subMap : partitions) {
				executor.submit(() -> subMap.values().parallelStream().forEach(c -> c.calculateCurrentProductivity(R)));
			}
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
			} // Wait for all tasks to complete
		}
	}

	public ConcurrentHashMap<Aft, Double> getDistributionMean() {
		return distributionMean;
	}

	public ConcurrentHashMap<String, Double> getRegionalSupply() {
		return regionalSupply;
	}

	public void setRegionalSupply(ConcurrentHashMap<String, Double> regionalSupply) {
		this.regionalSupply = regionalSupply;
	}

}
