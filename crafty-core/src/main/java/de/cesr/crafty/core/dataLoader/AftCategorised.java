package de.cesr.crafty.core.dataLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.model.AftCategory;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

/**
 * @author Mohamed Byari
 *
 */
public class AftCategorised {

	public static ConcurrentHashMap<String, Set<Aft>> aftCategories = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, Set<String>> CategoriesIntestisy = new ConcurrentHashMap<>();
	public static HashMap<String, String> categoriesColor = new HashMap<>();

	private static HashMap<String, Double> mean = new HashMap<>();
	private static HashMap<String, Double> SD = new HashMap<>();
	public static boolean useCategorisationGivIn = false;

	public static void CategoriesLoader() {
		Path aftsmetadataPath = PathTools.fileFilter(PathTools.asFolder("csv"), "AFTsMetaData").iterator().next();
		HashMap<String, ArrayList<String>> csv = ReaderFile.ReadAsaHash(aftsmetadataPath);

		if (csv.get("Category") != null) {
			for (int i = 0; i < csv.get("Category").size(); i++) {
				aftCategories.put(csv.get("Category").get(i), new HashSet<>());
				CategoriesIntestisy.put(csv.get("Category").get(i), new HashSet<>());
			}
			for (int i = 0; i < csv.get("Label").size(); i++) {
				Aft a = AFTsLoader.getAftHash().get(csv.get("Label").get(i));
				String ca = csv.get("Category").get(i);
				aftCategories.get(ca).add(a);
				categoriesColor.put(ca, csv.get("Category_Color").get(i));
				AftCategory category = new AftCategory(ca);
				a.setCategory(category);
				a.getCategory().setIntensity(csv.get("Intesity_name").get(i));
				a.getCategory().setIntensityLevel((int) Utils.sToD(csv.get("Intesity_level").get(i)));
				CategoriesIntestisy.get(ca).add(a.getCategory().getIntensity());
			}
		}
	}

	public static void initializeBehevoirByCategories() {
		if (aftCategories.size() > 1) {
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
			}
			useCategorisationGivIn = mean != null && SD != null;
		}

	}

	public static HashMap<String, Double> getMean() {
		return mean;
	}

	public static HashMap<String, Double> getSD() {
		return SD;
	}

}
