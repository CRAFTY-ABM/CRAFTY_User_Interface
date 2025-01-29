package de.cesr.crafty.model;

import java.util.Random;

import de.cesr.crafty.cli.ConfigLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import javafx.scene.paint.Color;

/**
 * @author Mohamed Byari
 *
 */

public class Manager extends AbstractManager{

	public Manager() {
		label = "";
		completeName = "";
		CellsLoader.getCapitalsList().forEach((Cn) -> {
			ServiceSet.getServicesList().forEach((Sn) -> {
				sensitivity.put((Cn + "_" + Sn), 0.);
			});
		});
		ServiceSet.getServicesList().forEach(servicename->{
			productivityLevel.put(servicename, 0.0);
		});
	}

	public Manager(Manager other) {
		if (other != null) {
			this.label = other.label;
			this.color = other.color;
			other.sensitivity.forEach((n, v) -> {
				this.sensitivity.put(n, v * (1 + ConfigLoader.config.mutation_interval * (2*new Random().nextDouble() - 1)));

			});
			other.productivityLevel.forEach((n, v) -> {
				this.productivityLevel.put(n, v * (1 + ConfigLoader.config.mutation_interval * (2*new Random().nextDouble() - 1)));
			});
			this.giveInMean = other.giveInMean * (1 + ConfigLoader.config.mutation_interval * (2*new Random().nextDouble() - 1));
			this.giveUpMean = other.giveUpMean * (1 + ConfigLoader.config.mutation_interval * (2*new Random().nextDouble() - 1));
			this.giveUpProbabilty = other.giveUpProbabilty * (1 + ConfigLoader.config.mutation_interval * (2*new Random().nextDouble() - 1));
		}

	}

	public Manager(String label, double LevelIntervale) {
		this.label = label;
		this.color = Color.color(Math.random(), Math.random(), Math.random());// ColorsTools.colorlist(new
																				// Random().nextInt(17));
		CellsLoader.getCapitalsList().forEach((Cn) -> {
			ServiceSet.getServicesList().forEach((Sn) -> {
				this.sensitivity.put((Cn + "_" + Sn), Math.random() > 0.5 ? Math.random() : 0);
			});
		});
		ServiceSet.getServicesList().forEach((Sn) -> {
			this.productivityLevel.put(Sn, LevelIntervale * Math.random());
		});
		this.giveInMean = Math.random();
		this.giveUpMean = Math.random();
		this.giveUpProbabilty = Math.random();
	}


	public Manager(String label) {
		this.label = label;
	}



	@Override
	public String toString() {
		return "AFT [label=" + label + " ,\n sensitivty=" + sensitivity + ",\n productivityLevel=" + productivityLevel
				+ ",\n giveIn=" + giveInMean + ", giveUp=" + giveUpMean + ", giveUpProbabilty=" + giveUpProbabilty
				+ "]";
	}
}
