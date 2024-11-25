package dataLoader;

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

import javafx.scene.paint.Color;
import model.Manager;
import model.ManagerTypes;
import model.RegionClassifier;
import tech.tablesaw.api.Table;
import utils.analysis.CustomLogger;
import utils.filesTools.CsvTools;
import utils.filesTools.PathTools;
import utils.filesTools.ReaderFile;
import utils.graphicalTools.ColorsTools;
import utils.graphicalTools.Tools;

/**
 * @author Mohamed Byari
 *
 */

public class AFTsLoader extends HashSet<Manager> {

	private static final CustomLogger LOGGER = new CustomLogger(AFTsLoader.class);
	private static final long serialVersionUID = 1L;
	private static ConcurrentHashMap<String, Manager> hashAFTs = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, Manager> activateAFTsHash = new ConcurrentHashMap<>();
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
						a.setColor(Color.web(T.get("Color").get(i)));
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

	public void updateColorsInputData() {
		List<Path> colorFiles = PathTools.fileFilter(PathTools.asFolder("csv"), "AFTsMetaData");
		if (colorFiles.size() > 0) {
			HashMap<String, ArrayList<String>> T = ReaderFile.ReadAsaHash(colorFiles.iterator().next());
			ArrayList<String> tmp = new ArrayList<>();
			forEach(a -> {
				for (int i = 0; i < T.get("Color").size(); i++) {
					if (T.get("Label").get(i).replace("	", "").equalsIgnoreCase(a.getLabel())) {
						tmp.add(ColorsTools.toHex(a.getColor()));
					}
				}
			});
			T.put("Color", tmp);
			String[][] writer = new String[size() + 1][T.size()];
			for (int i = 0; i < writer[0].length; i++) {
				writer[0][i] = (String) T.keySet().toArray()[i];
			}
			for (int i = 0; i < writer[0].length; i++) {
				for (int j = 0; j < size(); j++) {
					writer[j + 1][i] = T.get(writer[0][i]).get(j).replace(",", ".");
				}
			}
			CsvTools.writeCSVfile(writer, colorFiles.iterator().next());
		}
	}

	void initializeAFTs() {
		updateAftTypes();
		hashAFTs.forEach((Label, a) -> {
			if (a.isInteract()) {
				Path pFile = null;
				try {
					pFile = PathTools.fileFilter(PathTools.asFolder("default_production"),
							PathTools.asFolder("production"), PathsLoader.getScenario(), Label + ".csv").get(0);
				} catch (NullPointerException e) {
					pFile = PathTools
							.fileFilter(PathTools.asFolder("production"), PathsLoader.getScenario(), Label + ".csv")
							.get(0);
					LOGGER.warn("Default productivity folder not fund, will use: " + pFile);
				}
				initializeAFTProduction(pFile);

				Path bFile = null;
				try {
					bFile = PathTools.fileFilter(PathTools.asFolder("default_behaviour"), PathTools.asFolder("agents"),
							PathsLoader.getScenario(), Label + ".csv").get(0);
				} catch (NullPointerException e) {
					bFile = PathTools
							.fileFilter(PathTools.asFolder("agents"), PathsLoader.getScenario(), Label + ".csv").get(0);
					LOGGER.warn("Default behaviour folder not fund, will use: " + bFile);
				}
				initializeAFTBehevoir(bFile);
			}
		});
//		checkAFTsBehevoireParametres(bFiles);
	}

	public void updateAFTsForsenario() {
		List<Path> pFiles = PathTools.fileFilter(PathTools.asFolder("production"), PathsLoader.getScenario(), ".csv");
		pFiles.forEach(f -> {
			File file = f.toFile();
			updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
		});
		List<Path> bFiles = PathTools.fileFilter(PathTools.asFolder("agents"), PathsLoader.getScenario(), ".csv");
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
		Path pFolderToUpdate = PathsLoader.getProjectPath().resolve("production").resolve(PathsLoader.getScenario())
				.resolve("update_production_" + PathsLoader.getCurrentYear());
		if (pFolderToUpdate.toFile().exists()) {
			List<Path> pFiles = PathTools.fileFilter(pFolderToUpdate.toString());
			pFiles.forEach(f -> {
				File file = f.toFile();
				updateAFTProduction(hashAFTs.get(file.getName().replace(".csv", "")), file);
			});
		} else {
			LOGGER.info("AFT production parameters not updated (no folder found:" + pFolderToUpdate + ")");
		}

		Path bFolderToUpdate = PathsLoader.getProjectPath().resolve("agents").resolve(PathsLoader.getScenario())
				.resolve("update_behaviour_" + PathsLoader.getCurrentYear());
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
		Manager a = hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", ""));
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

	public static void updateAFTBehevoir(Manager a, File file) {
		HashMap<String, ArrayList<String>> reder = ReaderFile.ReadAsaHash(file.toPath());
		a.setGiveInMean(Tools.sToD(reder.get("givingInDistributionMean").get(0)));
		a.setGiveUpMean(Tools.sToD(reder.get("givingUpDistributionMean").get(0)));
		a.setGiveInSD(Tools.sToD(reder.get("givingInDistributionSD").get(0)));
		a.setGiveUpSD(Tools.sToD(reder.get("givingUpDistributionSD").get(0)));
		a.setServiceLevelNoiseMin(Tools.sToD(reder.get("serviceLevelNoiseMin").get(0)));
		a.setServiceLevelNoiseMax(Tools.sToD(reder.get("serviceLevelNoiseMax").get(0)));
		a.setGiveUpProbabilty(Tools.sToD(reder.get("givingUpProb").get(0)));
	}

	public void initializeAFTProduction(Path aftPath) {
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
				Manager a = new Manager(label);
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
		Manager a = new Manager("Abandoned");
		a.setType(ManagerTypes.Abandoned);
		unmanagedManagerLabel = a.getLabel();
		a.setColor(Color.GREY);
		hashAFTs.put(a.getLabel(), a);
	}

	public static void updateSensitivty(Manager a, File file) {
		Table T;
		try {
			T = Table.read().csv(file);

			CellsLoader.getCapitalsList().forEach((Cn) -> {
				ServiceSet.getServicesList().forEach((Sn) -> {
					a.getSensitivity().put((Cn + "_" + Sn),
							Tools.sToD(T.column(Cn).getString(T.column(0).indexOf(Sn))));
				});
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void updateAFTProduction(Manager a, File file) {
		String[][] m = CsvTools.csvReader(file.toPath());
		for (int i = 0; i < m.length; i++) {
			if (ServiceSet.getServicesList().contains(m[i][0])) {
				a.getProductivityLevel().put(m[i][0], Tools.sToD(m[i][Tools.indexof("Production", m[0])]));
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

	public static ConcurrentHashMap<String, Manager> getAftHash() {
		return hashAFTs;
	}

	public static ConcurrentHashMap<String, Manager> getActivateAFTsHash() {
		return activateAFTsHash;
	}

	public static Manager getRandomAFT() {
		return getRandomAFT(activateAFTsHash.values());
	}

	public static Manager getRandomAFT(Collection<Manager> afts) {
		if (afts.size() != 0) {
			int index = ThreadLocalRandom.current().nextInt(afts.size());
			Manager aft = afts.stream().skip(index).findFirst().orElse(null);
			return aft;
		}
		return null;// select from outside "hash"
	}

}
