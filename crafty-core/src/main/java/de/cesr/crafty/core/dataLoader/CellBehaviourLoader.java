package de.cesr.crafty.core.dataLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.model.Cell;
import de.cesr.crafty.core.model.CellBehaviour;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class CellBehaviourLoader {

	public static boolean behaviourUsed = false;
	public static ConcurrentHashMap<Cell, CellBehaviour> cellsBehevoir = new ConcurrentHashMap<>();

	public static void initialize() {
		if (AftCategorised.useCategorisationGivIn) {
			behaviourUsed = PathTools.fileFilter(PathTools.asFolder("behaviour"), ProjectLoader.getScenario(),
					"Cell_behaviour_parameters", ProjectLoader.getStartYear() + ".csv") != null;
			updateCellBehaviour();

		}
	}

	public static void updateCellBehaviour() {
		if (behaviourUsed) {
			ArrayList<Path> files = PathTools.fileFilter(PathTools.asFolder("behaviour"), ProjectLoader.getScenario(),
					"Cell_behaviour_parameters", ProjectLoader.getCurrentYear() + ".csv");
			if (files != null) {
				HashMap<String, ArrayList<String>> csv = ReaderFile.ReadAsaHash(files.get(0));
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
