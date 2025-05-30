package de.cesr.crafty.core.dataLoader.land;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;

public class MaskRestrictionDataLoader {

	public static HashMap<String, List<Path>> hashMasksPaths;
	public static HashMap<String, HashMap<String, Boolean>> restrictions = new HashMap<>();

	private static final CustomLogger LOGGER = new CustomLogger(MaskRestrictionDataLoader.class);

//	public static void maskAndRistrictionPathInitializer() {
//		hashMasksPaths = new HashMap<>();
//		List<File> LandUseControlFolder = PathTools
//				.detectFolders(ProjectLoader.getProjectPath() + PathTools.asFolder("worlds") + "LandUseControl");
//		if (LandUseControlFolder != null) {
//			for (File folder : LandUseControlFolder) {
//				maskAndRistrictionLaoder(folder.getName());
//				restrictionsInitialize(folder.getName());
//			}
//		}
//		LOGGER.info("Masks: " + hashMasksPaths.keySet());
//	}

	public static void maskAndRistrictionLaoder(String maskType) {
		ArrayList<Path> listOfMaskFilesInaScenario = PathTools.fileFilter(true,
				ProjectLoader.getProjectPath() + PathTools.asFolder("worlds") + "LandUseControl",
				ProjectLoader.getScenario(), PathTools.asFolder(maskType));
		if (listOfMaskFilesInaScenario != null) {
			List<Path> mask = new ArrayList<>();
			for (Path file : listOfMaskFilesInaScenario) {
				if (!file.toString().contains("Restrictions")) {
					mask.add(file);
				}
			}
			hashMasksPaths.put(maskType, mask);
		}
	}

	public static void cleanMaskType(String maskType) {
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
		LOGGER.trace("Mask name:" + maskType + ", restrictions File Path " + restrictionsFile);
		restrictions.put(maskType, importResrection(restrictionsFile.get(0)));
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
