package de.cesr.crafty.core.dataLoader.afts;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.AftCategory;
import de.cesr.crafty.core.crafty.ManagerTypes;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.updaters.AftsUpdater;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

/**
 * @author Mohamed Byari
 *
 */

public class AFTsLoader extends HashSet<Aft> {

	private static final CustomLogger LOGGER = new CustomLogger(AFTsLoader.class);
	private static final long serialVersionUID = 1L;
	private static ConcurrentHashMap<String, Aft> hashAFTs = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, Aft> activateAFTsHash = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, Integer> hashAgentNbr = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> hashAgentNbrRegions = new ConcurrentHashMap<>();
	public static String unmanagedManagerLabel = "Abandoned";

	public AFTsLoader() {
		initializeAFTs();
		addAll(hashAFTs.values());
		addAbandonedAftIfNotExiste();
		activateAFTsHash.clear();
		hashAFTs.entrySet().stream().filter(entry -> entry.getValue().isActive())
				.forEach(entry -> activateAFTsHash.put(entry.getKey(), entry.getValue()));
		LOGGER.info(" AFTs: " + hashAFTs.keySet());
		LOGGER.info("Active AFTs: " + activateAFTsHash.keySet());
	}

	void initializeAFTs() {
		initializeAftTypes();
		AftCategorised.CategoriesLoader();
		AftCategorised.initializeBehevoirByCategories();
		hashAFTs.forEach((Label, a) -> {
			LOGGER.trace("Import Production and behaviour for AFT: " + Label);
			if (a.isInteract()) {
				Path pFile = null;
				ArrayList<Path> pfiles = PathTools.fileFilter(PathTools.asFolder("default_production"),
						PathTools.asFolder("production"), File.separator + Label + ".csv");
				if (pfiles != null && pfiles.size() > 0) {
					pFile = pfiles.get(0);
				} else {
					ArrayList<Path> pFileList = PathTools.fileFilter(PathTools.asFolder("production"),
							ProjectLoader.getScenario(), File.separator + Label + ".csv");
					pFile = pFileList.get(0);
					// LOGGER.warn("Default productivity folder not fund, will use: " + pFile);
				}
				LOGGER.trace("Production file Path: " + pFile);
				initializeAFTProduction(pFile);

				Path bFile = null;
				ArrayList<Path> bfiles = PathTools.fileFilter(PathTools.asFolder("default_behaviour"),
						PathTools.asFolder("agents"), "AftParams_" + Label + ".csv");
				if (bfiles != null && bfiles.size() > 0) {
					bFile = bfiles.get(0);
				} else {
					bFile = PathTools.fileFilter(PathTools.asFolder("agents"), ProjectLoader.getScenario(),
							"AftParams_" + Label + ".csv").get(0);
					// LOGGER.info("Default behaviour folder not fund, will use: " + bFile);
				}

				LOGGER.trace("Behaviour file Path: " + bFile);
				initializeAFTBehevoir(bFile);
			}
		});
	}

	public static void updateAFTsForsenario() {
		List<Path> pFiles = PathTools.fileFilter(PathTools.asFolder("production"), ProjectLoader.getScenario(), ".csv");
		pFiles.forEach(f -> {
			File file = f.toFile();
			AftsUpdater.updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
		});
		List<Path> bFiles = PathTools.fileFilter(PathTools.asFolder("agents"), ProjectLoader.getScenario(), ".csv");
		bFiles.forEach(f -> {
			File file = f.toFile();
			try {
				AftsUpdater.updateAFTBehevoir(
						hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", "")), file);
			} catch (NullPointerException e) {
				LOGGER.error("AFT Not in the List: " + file);
			}
		});
		checkAFTsBehevoireParametres(bFiles);
	}

	public void initializeAFTBehevoir(Path aftPath) {
		File file = aftPath.toFile();
		Aft a = hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", ""));
		AftsUpdater.updateAFTBehevoir(a, file);
	}

	private static void checkAFTsBehevoireParametres(List<Path> bFiles) {
		List<String> bf = new ArrayList<>();
		bFiles.forEach(f -> {
			bf.add(f.toFile().getName().replace(".csv", "").replace("AftParams_", ""));
		});
		hashAFTs.keySet().forEach(label -> {
			if (!bf.contains(label)) {
				LOGGER.warn("no behevoir parametrs for the AFT:  " + label);
			}
		});
	}

	private void initializeAFTProduction(Path aftPath) {
		File file = aftPath.toFile();
		AftsUpdater.updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
	}

	void initializeAftTypes() {// mask, AFT, or unmanaged //
		hashAFTs.clear();
		Path aftsmetadataPath = PathTools.fileFilter(PathTools.asFolder("csv"), "AFTsMetaData").iterator().next();
		Map<String, List<String>> matrix = CsvProcessors.ReadAsaHash(aftsmetadataPath);
		if (matrix.get("Type") != null) {
			for (int i = 0; i < matrix.get("Label").size(); i++) {
				String label = matrix.get("Label").get(i);
				Aft a = new Aft(label);
				a.setColor(matrix.get("Color").get(i));
				if (matrix.keySet().contains("Name")) {
					a.setCompleteName(matrix.get("Name").get(i));
				} else {
					a.setCompleteName("-");
				}
				hashAFTs.put(label, a);
				switch (matrix.get("Type").get(i)) {
				case "Mask":
					a.setType(ManagerTypes.MASK);
					break;
				case "Abandoned":
					a.setType(ManagerTypes.Abandoned);
					unmanagedManagerLabel = a.getLabel();
					break;
				default:
					a.setType(ManagerTypes.AFT);
				}
			}
		}
	}

	void addAbandonedAftIfNotExiste() {
		Aft a = new Aft("Abandoned");
		a.setType(ManagerTypes.Abandoned);
		unmanagedManagerLabel = a.getLabel();
		a.setColor("#9133ff");
		a.setCategory(new AftCategory("Uncategorized"));
		hashAFTs.put(a.getLabel(), a);
	}

	public static void hashAgentNbr() {
		hashAgentNbr.clear();
		CellsLoader.hashCell.values().forEach(c -> {
			if (c.getOwner() != null)
				hashAgentNbr.merge(c.getOwner().getLabel(), 1, Integer::sum);
			else {
				hashAgentNbr.merge(unmanagedManagerLabel, 1, Integer::sum);
			}
		});
		if (!hashAgentNbr.containsKey("Abandoned") || !hashAgentNbr.containsKey(unmanagedManagerLabel)) {
			hashAgentNbr.put(unmanagedManagerLabel, 0);
		}
		LOGGER.info("Number of cells for each AFT: "+hashAgentNbr);
	}

	public static void hashAgentNbrRegions() {
		CellsLoader.regions.keySet().forEach(r -> {
			hashAgentNbr(r);
		});
	}

	public static void hashAgentNbr(String regionName) {
		ConcurrentHashMap<String, Integer> hashAgentNbr = new ConcurrentHashMap<>();
		CellsLoader.regions.get(regionName).getCells().values().forEach(c -> {
			if (c.getOwner() != null)
				hashAgentNbr.merge(c.getOwner().getLabel(), 1, Integer::sum);
			else {
				hashAgentNbr.merge(unmanagedManagerLabel, 1, Integer::sum);
			}
			if (!hashAgentNbr.containsKey("Abandoned") || !hashAgentNbr.containsKey(unmanagedManagerLabel)) {
				hashAgentNbr.put(unmanagedManagerLabel, 0);
			}
		});
		hashAgentNbrRegions.put(regionName, hashAgentNbr);

		getAftHash().values().forEach(a -> hashAgentNbrRegions.get(regionName).computeIfAbsent(a.getLabel(), key -> 0));

		LOGGER.trace("Rigion: [" + regionName + "] NBR of AFTs: " + hashAgentNbrRegions.get(regionName));
	}

	public static ConcurrentHashMap<String, Aft> getAftHash() {
		return hashAFTs;
	}

	public static ConcurrentHashMap<String, Aft> getActivateAFTsHash() {
		return activateAFTsHash;
	}

	public static Aft getRandomAFT() {
		return getRandomAFT(activateAFTsHash.values());
	}

	public static Aft getRandomAFT(Collection<Aft> afts) {
		if (afts.size() != 0) {
			int index = ThreadLocalRandom.current().nextInt(afts.size());
			Aft aft = afts.stream().skip(index).findFirst().orElse(null);
			return aft;
		}
		return null;
	}

}
