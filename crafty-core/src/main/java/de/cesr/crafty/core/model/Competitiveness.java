package de.cesr.crafty.core.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.AftCategorised;
import de.cesr.crafty.core.dataLoader.CellBehaviourLoader;
import de.cesr.crafty.core.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;

public class Competitiveness {
	static boolean utilityUsingPrice = true;

	static double utility(Cell c, Aft a, RegionalModelRunner r) {
		if (a == null || !a.isInteract()) {
			return 0;
		}
		return ServiceSet.getServicesList().stream()
				.mapToDouble(sname -> r.marginal.get(sname) * c.productivity(a, sname)).sum();
	}

//	static double utilityPrice(Cell c, Aft a, RegionalModelRunner r) {
//		if (a == null || !a.isInteract()) {
//			return 0;
//		}
//		int tick = ProjectLoader.getCurrentYear() - ProjectLoader.getStartYear();
//		return ServiceSet.getServicesList().stream()
//				.mapToDouble(sname -> (r.R.getServicesHash().get(sname).getWeights().get(tick)
//						/ r.R.getServicesHash().get(sname).getCalibration_Factor()) * c.productivity(a, sname))
//				.sum();
//	}

	static Aft mostCompetitiveAgent(Cell c, Collection<Aft> setAfts, RegionalModelRunner r) {
		if (setAfts.size() == 0) {
			return c.owner;
		}
		double uti = 0;
		Aft theBestAFT = setAfts.iterator().next();
		for (Aft agent : setAfts) {
			double u = utility(c, agent, r);
			if (u > uti) {
				uti = u;
				theBestAFT = agent;
			}
		}
		return theBestAFT;
	}

	private static void Competition(Cell c, Aft competitor, RegionalModelRunner r) {
		if (competitor == null || !competitor.isInteract()) {
			return;
		}
		if (makeCompetition(c, competitor)) {
			if (AftCategorised.useCategorisationGivIn && CellBehaviourLoader.behaviourUsed) {
				landUsechangeNormalisedUtility(c, competitor, r);
			} else {
				landUsechange(c, competitor, r);
			}
		}
	}

	private static boolean makeCompetition(Cell c, Aft competitor) {
		boolean makeCompetition = true;
		if (c.getMaskType() != null) {
			HashMap<String, Boolean> mask = MaskRestrictionDataLoader.restrictions.get(c.getMaskType());
			if (mask != null) {
				if (c.owner == null) {
					if (mask.get(competitor.getLabel() + "_" + competitor.getLabel()) != null)
						makeCompetition = mask.get(competitor.getLabel() + "_" + competitor.getLabel());
				} else {
					if (mask.get(c.owner.getLabel() + "_" + competitor.getLabel()) != null)
						makeCompetition = mask.get(c.owner.getLabel() + "_" + competitor.getLabel());
				}
			}
		}
		return makeCompetition;
	}

	private static void landUsechange(Cell c, Aft competitor, RegionalModelRunner r) {
		double uC = utility(c, competitor, r);
		double uO = utility(c, c.owner, r);
		if (c.owner == null || c.owner.isAbandoned()) {
			if (uC > 0)
				takeOverAcell(c, competitor);
		} else {
			double nbr = r.distributionMean != null
					? (r.distributionMean.get(c.owner) * (giveInThreshold(c.owner, competitor)))
					: 0;
			if ((uC - uO > nbr) && uC > 0) {
				takeOverAcell(c, competitor);
			}
		}
	}

	private static void landUsechangeNormalisedUtility(Cell c, Aft competitor, RegionalModelRunner r) {
		double uC = utility(c, competitor, r) / r.maximumUtility.get(competitor);

		if (c.owner == null || c.owner.isAbandoned()) {
			if (uC > 0) {
				takeOverAcell(c, competitor);
			}
			return;
		}

		double uO = utility(c, c.owner, r) / r.maximumUtility.get(c.owner);

		double giveIn = CellBehaviourLoader.cellsBehevoir.get(c).give_In(competitor);
		if ((uC > uO + giveIn)) {
			takeOverAcell(c, competitor);
		}
	}

	private static void takeOverAcell(Cell c, Aft newOwner) {
		c.owner = ConfigLoader.config.mutate_on_competition_win ? new Aft(newOwner) : newOwner;
	}

	private static double giveInThreshold(Aft owner, Aft competitor) {
		if (AftCategorised.useCategorisationGivIn) {
			String key = owner.getCategory().getName() + "|" + competitor.getCategory().getName();
			Double mean = AftCategorised.getMean().get(key);
			Double sd = AftCategorised.getSD().get(key);
			// Only use the BehaviorLoader-based mean & sd if BOTH are present AND the
			// categories differ.
			if (mean != null && sd != null
					&& !owner.getCategory().getName().equals(competitor.getCategory().getName())) {
				return mean + sd * new Random().nextGaussian();
			}
		}
		// else Fallback to the owner's giveInMean
		return owner.getGiveInMean() + owner.getGiveInSD() * new Random().nextGaussian();
	}

	static void competition(Cell c, RegionalModelRunner r) {
		boolean Neighboor = ConfigLoader.config.use_neighbor_priority
				&& ConfigLoader.config.neighbor_priority_probability > Math.random();
		Collection<Aft> afts = Neighboor
				? CellsSubSets.detectExtendedNeighboringAFTs(c, ConfigLoader.config.neighbor_radius)
				: AFTsLoader.getActivateAFTsHash().values();

		if (Math.random() < ConfigLoader.config.MostCompetitorAFTProbability) {
			Competition(c, mostCompetitiveAgent(c, afts, r), r);
		} else {
			Competition(c, AFTsLoader.getRandomAFT(afts), r);
		}
	}

}
