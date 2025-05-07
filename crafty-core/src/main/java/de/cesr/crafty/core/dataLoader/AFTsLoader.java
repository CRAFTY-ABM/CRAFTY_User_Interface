package de.cesr.crafty.core.dataLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.model.ManagerTypes;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

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
		agentsColorinitialisation();
		LOGGER.info(" AFTs: " + hashAFTs.keySet());
		LOGGER.info("Active AFTs: " + activateAFTsHash.keySet());
	}

	public void agentsColorinitialisation() {
		List<Path> colorFiles = PathTools.fileFilter(PathTools.asFolder("csv"), "AFTsMetaData");
		if (colorFiles.size() > 0) {
			HashMap<String, ArrayList<String>> T = ReaderFile.ReadAsaHash(colorFiles.iterator().next());

			forEach(a -> {
				for (int i = 0; i < T.get("Color").size(); i++) {
					if (T.get("Label").get(i).equalsIgnoreCase(a.getLabel())) {
						a.setColor(T.get("Color").get(i));
						if (T.keySet().contains("Name")) {
							a.setCompleteName(T.get("Name").get(i));
						} else {
							a.setCompleteName("-");
						}
					}
				}
			});
		}

	}

	void initializeAFTs() {
		updateAftTypes();
		AftCategorised.CategoriesLoader();
		AftCategorised.initializeBehevoirByCategories();
		hashAFTs.forEach((Label, a) -> {
			LOGGER.trace("Import Production and behaviour for AFT: "+Label);
			if (a.isInteract()) {
				Path pFile = null;
				try {
					pFile = PathTools.fileFilter(PathTools.asFolder("default_production"),
							PathTools.asFolder("production"), ProjectLoader.getScenario(), Label + ".csv").get(0);
				} catch (NullPointerException e) {
					ArrayList<Path> pFileList = PathTools.fileFilter(PathTools.asFolder("production"),
							ProjectLoader.getScenario(), Label + ".csv");
					pFile = pFileList.get(0);
					LOGGER.warn("Default productivity folder not fund, will use: " + pFile);
				} 
				LOGGER.trace("Production file Path: "+pFile);
				initializeAFTProduction(pFile);

				Path bFile = null;
				try {
					bFile = PathTools.fileFilter(PathTools.asFolder("default_behaviour"), PathTools.asFolder("agents"),
							ProjectLoader.getScenario(), Label + ".csv").get(0);
				} catch (NullPointerException e) {
					bFile = PathTools
							.fileFilter(PathTools.asFolder("agents"), ProjectLoader.getScenario(), Label + ".csv")
							.get(0);
					
					LOGGER.warn("Default behaviour folder not fund, will use: " + bFile);
				}
				LOGGER.trace("Behaviour file Path: "+bFile);
				initializeAFTBehevoir(bFile);
			}
		});
//		checkAFTsBehevoireParametres(bFiles);
	}

	public void updateAFTsForsenario() {
		List<Path> pFiles = PathTools.fileFilter(PathTools.asFolder("production"), ProjectLoader.getScenario(), ".csv");
		pFiles.forEach(f -> {
			File file = f.toFile();
			updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
		});
		List<Path> bFiles = PathTools.fileFilter(PathTools.asFolder("agents"), ProjectLoader.getScenario(), ".csv");
		bFiles.forEach(f -> {
			File file = f.toFile();
			try {
				updateAFTBehevoir(hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", "")), file);
			} catch (NullPointerException e) {
				LOGGER.error("AFT Not in the List: " + file);
			}
		});
		checkAFTsBehevoireParametres(bFiles);
	}

	public static void updateAFTs() {
		Path pFolderToUpdate = ProjectLoader.getProjectPath().resolve("production").resolve(ProjectLoader.getScenario())
				.resolve("update_production_" + ProjectLoader.getCurrentYear());
		if (pFolderToUpdate.toFile().exists()) {
			List<Path> pFiles = PathTools.fileFilter(pFolderToUpdate.toString());
			pFiles.forEach(f -> {
				File file = f.toFile();
				updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
			});
		} else {
			LOGGER.info("AFT production parameters not updated (no folder found:" + pFolderToUpdate + ")");
		}

		Path bFolderToUpdate = ProjectLoader.getProjectPath().resolve("agents").resolve(ProjectLoader.getScenario())
				.resolve("update_behaviour_" + ProjectLoader.getCurrentYear());
		if (bFolderToUpdate.toFile().exists()) {
			List<Path> bFiles = PathTools.fileFilter(bFolderToUpdate.toString());
			bFiles.forEach(f -> {
				File file = f.toFile();
				try {
					updateAFTBehevoir(hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", "")), file);
				} catch (NullPointerException e) {
					LOGGER.error("AFT Not in the List: " + file);
				}
			});
		} else {
			LOGGER.info("AFT behaviour parameters not updated (no folder found:" + bFolderToUpdate + ")");
		}
	}

	public void initializeAFTBehevoir(Path aftPath) {
		File file = aftPath.toFile();
		Aft a = hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", ""));
		updateAFTBehevoir(a, file);
	}

	private void checkAFTsBehevoireParametres(List<Path> bFiles) {
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

	public static void updateAFTBehevoir(Aft a, File file) {
		HashMap<String, ArrayList<String>> reder = ReaderFile.ReadAsaHash(file.toPath());
		a.setGiveInMean(Utils.sToD(reder.get("givingInDistributionMean").get(0)));
		a.setGiveUpMean(Utils.sToD(reder.get("givingUpDistributionMean").get(0)));
		a.setGiveInSD(Utils.sToD(reder.get("givingInDistributionSD").get(0)));
		a.setGiveUpSD(Utils.sToD(reder.get("givingUpDistributionSD").get(0)));
		a.setServiceLevelNoiseMin(Utils.sToD(reder.get("serviceLevelNoiseMin").get(0)));
		a.setServiceLevelNoiseMax(Utils.sToD(reder.get("serviceLevelNoiseMax").get(0)));
		a.setGiveUpProbabilty(Utils.sToD(reder.get("givingUpProb").get(0)));
	}

	private void initializeAFTProduction(Path aftPath) {
		File file = aftPath.toFile();
		updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
	}

	void updateAftTypes() {// mask, AFT, or unmanaged //
		hashAFTs.clear();
		Path aftsmetadataPath = PathTools.fileFilter(PathTools.asFolder("csv"), "AFTsMetaData").iterator().next();
		HashMap<String, ArrayList<String>> matrix = ReaderFile.ReadAsaHash(aftsmetadataPath);
		if (matrix.get("Type") != null) {
			for (int i = 0; i < matrix.get("Label").size(); i++) {
				String label = matrix.get("Label").get(i);
				Aft a = new Aft(label);
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
		a.setColor("#848484");
		hashAFTs.put(a.getLabel(), a);
	}

	public static void updateSensitivty(Aft a, File file) {
		try {
			CsvReadOptions options = CsvReadOptions.builder(file).separator(',').build();
			Table T = Table.read().usingOptions(options);
			CellsLoader.getCapitalsList().forEach((Cn) -> {
				ServiceSet.getServicesList().forEach((Sn) -> {
					Object s = T.column(Cn).get(T.column(0).indexOf(Sn));
					if (s instanceof Double) {
						a.getSensitivity().put((Cn + "|" + Sn), (double) s);
					} else if (s instanceof Integer) {
						a.getSensitivity().put((Cn + "|" + Sn), ((Integer) s).doubleValue());
					}
				});
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void updateAFTProduction(Aft a, File file) {
		String[][] m = CsvTools.csvReader(file.toPath());
		for (int i = 0; i < m.length; i++) {
			if (ServiceSet.getServicesList().contains(m[i][0])) {
//				System.out.println(m[i][0]+"  "+Utils.indexof("Production", m[0]));
				a.getProductivityLevel().put(m[i][0], Utils.sToD(m[i][Utils.indexof("Production", m[0])]));
			} else {
				LOGGER.warn(m[i][0] + "  is not existe in Services List, will be ignored");
			}
		}

		updateSensitivty(a, file);
	}

	public static void hashAgentNbr() {
		LOGGER.info("Calculating the number of agents for each type");
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
	}

	public static void hashAgentNbrRegions() {
		RegionClassifier.regions.keySet().forEach(r -> {
			hashAgentNbr(r);
		});
	}

	public static void hashAgentNbr(String regionName) {
		ConcurrentHashMap<String, Integer> hashAgentNbr = new ConcurrentHashMap<>();
		RegionClassifier.regions.get(regionName).getCells().values().forEach(c -> {
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

		// LOGGER.info("Rigion: [" + regionName + "] NBR of AFTs: "+
		// hashAgentNbrRegions.get(regionName));
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
		return null;// select from outside "hash"
	}

}
