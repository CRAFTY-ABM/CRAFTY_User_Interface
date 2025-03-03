package de.cesr.crafty.core.dataLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.model.Cell;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class MaskRestrictionDataLoader {

	public static HashMap<String, List<Path>> hashMasksPaths;
	public static HashMap<String, HashMap<String, Boolean>> restrictions = new HashMap<>();

	private static final CustomLogger LOGGER = new CustomLogger(MaskRestrictionDataLoader.class);

	public static void allMaskAndRistrictionUpdate() {
		hashMasksPaths = new HashMap<>();
		List<File> LandUseControlFolder = PathTools
				.detectFolders(ProjectLoader.getProjectPath() + PathTools.asFolder("worlds") + "LandUseControl");
		if (LandUseControlFolder != null) {
			for (File folder : LandUseControlFolder) {
				maskAndRistrictionLaoder(folder.getName());
				restrictionsInitialize(folder.getName());
			}
		}
		LOGGER.info("Masks: " + hashMasksPaths.keySet());
	}

	public static void maskAndRistrictionLaoder(String maskType) {
		ArrayList<Path> listOfMaskFilesInScenario = PathTools.fileFilter(true,
				ProjectLoader.getProjectPath() + PathTools.asFolder("worlds") + "LandUseControl",
				ProjectLoader.getScenario(), PathTools.asFolder(maskType));
		if (listOfMaskFilesInScenario != null) {
			List<Path> mask = new ArrayList<>();
			for (Path file : listOfMaskFilesInScenario) {
				if (!file.toString().contains("Restrictions")) {
					mask.add(file);
				}
			}
			hashMasksPaths.put(maskType, mask);
		}

	}

	public void CellSetToMaskLoader(String maskType, int year) {
		Path path = hashMasksPaths.get(maskType).stream()
				.filter(filePath -> filePath.toString().contains(String.valueOf(year))).findFirst().orElse(null);
		if (path != null) {
			HashMap<String, ArrayList<String>> csv = ReaderFile.ReadAsaHash(path, true);
			if (csv != null) {
				cleanMaskType(maskType);
				for (int i = 0; i < csv.values().iterator().next().size(); i++) {
					Cell c = ProjectLoader.cellsSet.getCell((int) Utils.sToD(csv.get("X").get(i)),
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

	void maskToOwner(Cell c, String maskType) {

		for (Aft a : AFTsLoader.getAftHash().values()) {
			if (maskType.contains(a.getLabel())) {
				c.setOwner(a);
				break;
			}
		}
	}

	public void CellSetToMaskLoader(int year) {
		hashMasksPaths.keySet().forEach(maskType -> {
			CellSetToMaskLoader(maskType, year);
			updateRestrections(maskType, year + "", restrictions.get(maskType));
		});

	}

	public void cleanMaskType(String maskType) {
		CellsLoader.hashCell.values().parallelStream().forEach(c -> {
			if (c.getMaskType() != null && c.getMaskType().equals(maskType)) {
				c.setMaskType(null);
				if (c.getOwner() != null && maskType.contains(c.getOwner().getLabel())) {
					c.setOwner(null);
				}
			}
		});
	}

	public static void restrictionsInitialize(String maskType) {
		String[] def = { "LandUseControl", "Restrictions", maskType, ".csv" };
		String[] defInScenario = PathTools.aggregateArrays(def, ProjectLoader.getScenario());
		ArrayList<Path> restrictionsFile = PathTools.fileFilter(defInScenario);
		if (restrictionsFile == null || restrictionsFile.isEmpty()) {
			restrictionsFile = PathTools.fileFilter(def);
		}
		restrictions.put(maskType, importResrection(restrictionsFile.get(0)));
	}

	public void updateRestrections(String maskType, String currentyear, HashMap<String, Boolean> restriction) {
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

	static HashMap<String, Boolean> importResrection(Path path) {
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

}
