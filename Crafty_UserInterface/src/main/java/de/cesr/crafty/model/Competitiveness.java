package de.cesr.crafty.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.main.ConfigLoader;

public class Competitiveness {
	static boolean utilityUsingPrice = true;

	static double utility(Cell c, Manager a, RegionalModelRunner r) {
		if (a == null || !a.isInteract()) {
			return 0;
		}
		return ServiceSet.getServicesList().stream()
				.mapToDouble(sname -> r.marginal.get(sname) * c.productivity(a, sname)).sum();
	}

	static double utilityPrice(Cell c, Manager a, RegionalModelRunner r) {
		if (a == null || !a.isInteract()) {
			return 0;
		}
		int tick = PathsLoader.getCurrentYear() - PathsLoader.getStartYear();
		return ServiceSet.getServicesList().stream()
				.mapToDouble(sname -> (r.R.getServicesHash().get(sname).getWeights().get(tick)
						/ r.R.getServicesHash().get(sname).getCalibration_Factor()) * c.productivity(a, sname))
				.sum();
	}

//	static double utility2(Cell c, Manager a, RegionalModelRunner r) {
//
//		if (utilityUsingPrice) {
//			return utilityPrice(c, a, r);
//		} else {
//			return utility(c, a, r); //utilityMarginal(c, a, r);
//		}
//	}

	static Manager mostCompetitiveAgent(Cell c, Collection<Manager> setAfts, RegionalModelRunner r) {
		if (setAfts.size() == 0) {
			return c.owner;
		}
		double uti = 0;
		Manager theBestAFT = setAfts.iterator().next();
		for (Manager agent : setAfts) {
			double u = utility(c, agent, r);
			if (u > uti) {
				uti = u;
				theBestAFT = agent;
			}
		}
		return theBestAFT;
	}

	private static void Competition(Cell c, Manager competitor, RegionalModelRunner r) {
		if (competitor == null || !competitor.isInteract()) {
			return;
		}
		boolean makeCopetition = true;
		if (c.getMaskType() != null) {
			HashMap<String, Boolean> mask = MaskRestrictionDataLoader.restrictions.get(c.getMaskType());
			if (mask != null) {
				if (c.owner == null) {
					if (mask.get(competitor.getLabel() + "_" + competitor.getLabel()) != null)
						makeCopetition = mask.get(competitor.getLabel() + "_" + competitor.getLabel());
				} else {
					if (mask.get(c.owner.getLabel() + "_" + competitor.getLabel()) != null)
						makeCopetition = mask.get(c.owner.getLabel() + "_" + competitor.getLabel());
				}
			}
		}

		if (makeCopetition) {
			double uC = utility(c, competitor, r);
			double uO = utility(c, c.owner, r);

			if (c.owner == null || c.owner.isAbandoned()) {
				if (uC > 0)
					c.owner = ConfigLoader.config.mutate_on_competition_win ? new Manager(competitor) : competitor;
			} else {
				double nbr = r.distributionMean != null
						? (r.distributionMean.get(c.owner)
								* (c.owner.getGiveInMean() + c.owner.getGiveInSD() * new Random().nextGaussian()))
						: 0;
				if ((uC - uO > nbr) && uC > 0) {
					c.owner = ConfigLoader.config.mutate_on_competition_win ? new Manager(competitor) : competitor;
				}
			}
		}
	}

	static void competition(Cell c, RegionalModelRunner r) {
		boolean Neighboor = ConfigLoader.config.use_neighbor_priority
				&& ConfigLoader.config.neighbor_priority_probability > Math.random();
		Collection<Manager> afts = Neighboor
				? CellsSubSets.detectExtendedNeighboringAFTs(c, ConfigLoader.config.neighbor_radius)
				: AFTsLoader.getActivateAFTsHash().values();

		if (Math.random() < ConfigLoader.config.MostCompetitorAFTProbability) {
			Competition(c, mostCompetitiveAgent(c, afts, r), r);
		} else {
			Competition(c, AFTsLoader.getRandomAFT(afts), r);
		}
	}

}
