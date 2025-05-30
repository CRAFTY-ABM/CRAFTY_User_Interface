package de.cesr.crafty.core.updaters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.crafty.CellBehaviour;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.afts.AftCategorised;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

/**
 * @author Mohamed Byari
 *
 */
public class CellBehaviourUpdater extends AbstractUpdater {

	public static boolean behaviourUsed = false;
	public static ConcurrentHashMap<Cell, CellBehaviour> cellsBehevoir = new ConcurrentHashMap<>();

	public CellBehaviourUpdater() {
		if (AftCategorised.useCategorisationGivIn) {
			behaviourUsed = PathTools.fileFilter(PathTools.asFolder("behaviour"), ProjectLoader.getScenario(),
					"Cell_behaviour_parameters", Timestep.getStartYear() + ".csv") != null;
			step();
		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		if (behaviourUsed) {
			ArrayList<Path> files = PathTools.fileFilter(PathTools.asFolder("behaviour"), ProjectLoader.getScenario(),
					"Cell_behaviour_parameters", Timestep.getCurrentYear() + ".csv");
			if (files != null) {
				HashMap<String, ArrayList<String>> csv = CsvProcessors.ReadAsaHash(files.get(0));
				for (int i = 0; i < csv.get("X").size(); i++) {
					Cell c = CellsLoader.hashCell.get(csv.get("X").get(i) + "," + csv.get("Y").get(i));
					CellBehaviour behaviour = new CellBehaviour(c);
					behaviour.setAttitude_intensification(Utils.sToD(csv.get("Attitude_intensification").get(i)));
					behaviour.setWeight_inertia(Utils.sToD(csv.get("Weight_inertia").get(i)));
					behaviour.setWeight_social(Utils.sToD(csv.get("Weight-social").get(i)));
					behaviour.setCritical_mass(Utils.sToD(csv.get("Critical_mass").get(i)));
					behaviour.setNeighborhood_size((int) Utils.sToD(csv.get("Neighborhood_size").get(i)));
					behaviour.setMaxGive_in(Utils.sToD(csv.get("MaxGive_in").get(i)));
					cellsBehevoir.put(c, behaviour);
				}
			}
		}
	}

}
