package de.cesr.crafty.core.updaters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.land.MaskLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

/**
 * @author Mohamed Byari
 *
 */
public class LandMaskUpdater extends AbstractUpdater {

	private static final CustomLogger LOGGER = new CustomLogger(LandMaskUpdater.class);

	public static HashMap<String, HashMap<String, Boolean>> restrictions = new HashMap<>();

	public LandMaskUpdater() {
		MaskLoader.initialize();

		if (MaskLoader.restriction_paths.size() > 0) {
			MaskLoader.restriction_paths.keySet().forEach(maskName -> {
				Path initialRestection = MaskLoader.restriction_paths.get(maskName).get(Timestep.getStartYear());
				Path defRestection = MaskLoader.restriction_paths.get(maskName).get(0);
				if (initialRestection != null) {
					restrictions.put(maskName, importResrection(initialRestection));
				} else if (defRestection != null) {
					restrictions.put(maskName, importResrection(defRestection));
				} else {
					LOGGER.error("Restriction file not found : " + maskName);
				}
			});

		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		MaskLoader.mask_paths.keySet().forEach(maskType -> {
			cellOneMaskUpdater(maskType, Timestep.getCurrentYear());
			updateRestrections(maskType);
		});
	}

	public static void updateRestrections(String maskType) {
		// check if the restriction is not null or 0 size (else error)
		if (MaskLoader.restriction_paths.get(maskType).size() > 0) {
			// if there is a corespondanet year use it
			Path updatedRestection = MaskLoader.restriction_paths.get(maskType).get(Timestep.getCurrentYear());
			if (updatedRestection != null) {
				restrictions.put(maskType, importResrection(updatedRestection));
				LOGGER.info("restrection updated for  (" + maskType + ") : " + updatedRestection);

			} else {
				LOGGER.info("No Resrection Update for: " + maskType);
			}
		} else {
			LOGGER.fatal("No restrection file fund: " + maskType);
		}

	}

	private static HashMap<String, Boolean> importResrection(Path path) {
		HashMap<String, Boolean> restric = new HashMap<>();
		String[][] matrix = CsvTools.csvReader(path);
		if (matrix != null) {
			for (int i = 1; i < matrix.length; i++) {
				for (int j = 1; j < matrix[0].length; j++) {
					restric.put(matrix[i][0] + "_" + matrix[0][j], matrix[i][j].contains("1"));
				}
			}
			return restric;
		} else
			return null;
	}

	public static void cellOneMaskUpdater(String maskType, int year) {
		Path path = MaskLoader.mask_paths.get(maskType).get(year);
		if (path != null) {
			Map<String, List<String>> csv = CsvProcessors.ReadAsaHash(path, true);
			if (csv != null) {
				cleanMaskType(maskType);
				for (int i = 0; i < csv.values().iterator().next().size(); i++) {
					Cell c = CellsLoader.getCell((int) Utils.sToD(csv.get("X").get(i)),
							(int) Utils.sToD(csv.get("Y").get(i)));
					int ii = i;
					csv.keySet().forEach(key -> {
						if (key.contains("Year_") && csv.get(key).get(ii).contains("1")) {
							if (c != null) {
								c.setMaskType(maskType);
								maskToOwner(c, maskType);
							}
						}
					});
				}
				LOGGER.info("Update Mask: " + maskType + "[" + path + "]");

			} else {
				LOGGER.warn("Cannot find the mask files..." + path);

			}
		} else {
			LOGGER.info("Mask file not found for  [" + maskType + " for the year:" + year
					+ "]  use the latest year available");
		}
	}
	
	public static void cleanMaskType(String maskType) {// move to updater
		CellsLoader.hashCell.values().parallelStream().forEach(c -> {
			if (c.getMaskType() != null && c.getMaskType().equals(maskType)) {
				c.setMaskType(null);
				if (c.getOwner() != null && maskType.contains(c.getOwner().getLabel())) {
					c.setOwner(null);
				}
			}
		});
	}

	private static void maskToOwner(Cell c, String maskType) {// need to be revised
		for (Aft a : AFTsLoader.getAftHash().values()) {
			if (maskType.contains(a.getLabel())) {
				c.setOwner(a);
				break;
			}
		}
	}

	public static void updateRestrections(String maskType, String currentyear, HashMap<String, Boolean> restriction) {
		ArrayList<Path> restrictionsFile = PathTools.fileFilter(currentyear, ProjectLoader.getScenario(),
				"LandUseControl", "Restrictions", maskType, ".csv");
		if (restrictionsFile == null || restrictionsFile.isEmpty()) {
			LOGGER.info(maskType + " Restrections updated ");
			return;
		}
		restriction.clear();
		String[][] matrix = CsvTools.csvReader(restrictionsFile.get(0));
		if (matrix != null) {
			for (int i = 1; i < matrix.length; i++) {
				for (int j = 1; j < matrix[0].length; j++) {
					restriction.put(matrix[i][0] + "_" + matrix[0][j], matrix[i][j].contains("1"));
				}
			}
		}
		LOGGER.info(maskType + " Restrections updated ");
	}
}
