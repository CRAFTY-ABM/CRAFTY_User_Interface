package de.cesr.crafty.core.dataLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import de.cesr.crafty.core.utils.file.PathTools;

public class BehaviourLoader {

	private static HashMap<String, Double> mean = new HashMap<>();
	private static HashMap<String, Double> SD = new HashMap<>();

	public static void initializeBehevoirByCategories() {
		if (AftCategorised.aftCategories.size() > 1) {
			ArrayList<Path> paths = PathTools.fileFilter(PathTools.asFolder("AFTs"), PathTools.asFolder("behaviour"),
					"categories_givingInDistribution");
			if (paths != null) {
				Path mean_path = paths.stream()
						.filter(path -> path.toString().contains("Mean_" + ProjectLoader.getScenario())).findFirst()
						.orElse(paths.stream().filter(path -> path.toString().contains("Mean_Default")).findFirst()
								.orElse(null));
				Path SD_path = paths.stream()
						.filter(path -> path.toString().contains("SD_" + ProjectLoader.getScenario())).findFirst()
						.orElse(paths.stream().filter(path -> path.toString().contains("SD_Default")).findFirst()
								.orElse(null));
				if (mean_path != null && SD_path != null) {
					mean = ReaderFile.readCsvToMatrixMap(mean_path);
					SD = ReaderFile.readCsvToMatrixMap(SD_path);
				}
				System.out.println(mean + "\n" + SD);
			}
		}

	}

	public static HashMap<String, Double> getMean() {
		return mean;
	}

	public static HashMap<String, Double> getSD() {
		return SD;
	}

}
