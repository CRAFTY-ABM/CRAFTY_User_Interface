package de.cesr.crafty.core.updaters;

import java.io.File;
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
import de.cesr.crafty.core.dataLoader.land.MaskRestrictionDataLoader;
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

	public LandMaskUpdater() {
		MaskRestrictionDataLoader.hashMasksPaths = new HashMap<>();
		List<File> LandUseControlFolder = PathTools
				.detectFolders(ProjectLoader.getProjectPath() + PathTools.asFolder("worlds") + "LandUseControl");
		if (LandUseControlFolder != null) {
			for (File folder : LandUseControlFolder) {
				MaskRestrictionDataLoader.maskAndRistrictionLaoder(folder.getName());
				MaskRestrictionDataLoader.restrictionsInitialize(folder.getName());
			}
		}
		LOGGER.info("Masks: " + MaskRestrictionDataLoader.hashMasksPaths.keySet());
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		MaskRestrictionDataLoader.hashMasksPaths.keySet().forEach(maskType -> {
			cellOneMaskUpdater(maskType, Timestep.getCurrentYear());
			updateRestrections(maskType, String.valueOf(Timestep.getCurrentYear()),
					MaskRestrictionDataLoader.restrictions.get(maskType));
		});
	}

	public static void cellOneMaskUpdater(String maskType, int year) {
		Path path = MaskRestrictionDataLoader.hashMasksPaths.get(maskType).stream()
				.filter(filePath -> filePath.toString().contains(String.valueOf(year))).findFirst().orElse(null);
		if (path != null) {
			Map<String, List<String>> csv = CsvProcessors.ReadAsaHash(path, true);
			if (csv != null) {
				MaskRestrictionDataLoader.cleanMaskType(maskType);
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
