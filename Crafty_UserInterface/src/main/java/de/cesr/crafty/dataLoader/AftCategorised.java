package de.cesr.crafty.dataLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.model.Aft;
import de.cesr.crafty.model.AftCategory;
import de.cesr.crafty.utils.file.PathTools;
import de.cesr.crafty.utils.file.ReaderFile;
import javafx.scene.paint.Color;

/**
 * @author Mohamed Byari
 *
 */
public class AftCategorised {

	public static ConcurrentHashMap<String, Set<Aft>> aftCategories = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, Set<String>> CategoriesIntestisy = new ConcurrentHashMap<>();
	public static HashMap<String, Color> categoriesColor = new HashMap<>();

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
				categoriesColor.put(ca, Color.web(csv.get("Category_Color").get(i)));
				AftCategory category = new AftCategory(ca);
				a.setCategory(category);
				a.getCategory().setIntensity(csv.get("Intesity_name").get(i));
				CategoriesIntestisy.get(ca).add(a.getCategory().getIntensity());
			}
		}
	}

}
