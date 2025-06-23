package de.cesr.crafty.core.crafty;

import java.util.Random;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.updaters.CapitalUpdater;

/**
 * @author Mohamed Byari
 *
 */

public class Aft extends AbstractAft {

	public Aft() {
		label = "";
		completeName = "";
		CapitalUpdater.getCapitalsList().forEach((Cn) -> {
			ServiceSet.getServicesList().forEach((Sn) -> {
				sensitivity.put((Cn + "|" + Sn), 0.);
			});
		});
		ServiceSet.getServicesList().forEach(servicename -> {
			productivityLevel.put(servicename, 0.0);
		});
	}

	public Aft(Aft other) {
		if (other != null) {
			this.label = other.label;
			this.color = other.color;
			other.sensitivity.forEach((n, v) -> {
				this.sensitivity.put(n,
						v * (1 + ConfigLoader.config.mutation_interval * (2 * new Random().nextDouble() - 1)));

			});
			other.productivityLevel.forEach((n, v) -> {
				this.productivityLevel.put(n,
						v * (1 + ConfigLoader.config.mutation_interval * (2 * new Random().nextDouble() - 1)));
			});
			this.giveInMean = other.giveInMean
					* (1 + ConfigLoader.config.mutation_interval * (2 * new Random().nextDouble() - 1));
			this.giveUpMean = other.giveUpMean
					* (1 + ConfigLoader.config.mutation_interval * (2 * new Random().nextDouble() - 1));
			this.giveUpProbabilty = other.giveUpProbabilty
					* (1 + ConfigLoader.config.mutation_interval * (2 * new Random().nextDouble() - 1));
		}

	}

	public Aft(String label, double LevelIntervale) {
		this.label = label;
		this.color = "#848484";
		CapitalUpdater.getCapitalsList().forEach((Cn) -> {
			ServiceSet.getServicesList().forEach((Sn) -> {
				this.sensitivity.put((Cn + "|" + Sn), Math.random() > 0.5 ? Math.random() : 0);
			});
		});
		ServiceSet.getServicesList().forEach((Sn) -> {
			this.productivityLevel.put(Sn, LevelIntervale * Math.random());
		});
		this.giveInMean = Math.random();
		this.giveUpMean = Math.random();
		this.giveUpProbabilty = Math.random();
	}

	public Aft(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return "Aft [label=" + label + ", type=" + type + ", category=" + category + "]";
	}

//	@Override
//	public String toString() {
//		return "AFT [label=" + label + " ,\n sensitivty=" + sensitivity + ",\n productivityLevel=" + productivityLevel
//				+ ",\n giveIn=" + giveInMean + ", giveUp=" + giveUpMean + ", giveUpProbabilty=" + giveUpProbabilty
//				+ "]";
//	}
//	

}
