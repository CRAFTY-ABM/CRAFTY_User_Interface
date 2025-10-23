package de.cesr.crafty.core.dataLoader.afts;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.AftCategory;
import de.cesr.crafty.core.crafty.ManagerTypes;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
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

	public static Map<String, Map<String, Path>> aft_production_paths = new HashMap<>();// <aftName,default/year,path>
	public static Map<String, Map<String, Path>> aft_behevoir_paths = new HashMap<>();;

	Map<String, Map<String, Path>> productionPaths(String pORb) {
		Map<String, Map<String, Path>> data = new HashMap<>();
		String aftparams = pORb.equals("agents") ? "AftParams_" : "";
		String cofigpORb = pORb.equals("agents") ? ConfigLoader.config.aft_behevoir_directory
				: ConfigLoader.config.aft_production_directory;

		if (cofigpORb.isEmpty()) {
			hashAFTs.keySet().forEach(label -> {
				ArrayList<Path> pfiles = PathTools.fileFilter(PathTools.asFolder(pORb),
						File.separator + aftparams + label + ".csv");
				if (pfiles != null) {
					Map<String, Path> temp = new HashMap<>();
					pfiles.forEach(p -> {
						if (p.toString()
								.contains(ProjectLoader.getScenario() + File.separator + aftparams + label + ".csv")) {
							temp.put(ProjectLoader.getScenario(), p);
						} else if (p.toString().contains(ProjectLoader.getScenario())) {
							for (int i = Timestep.getStartYear(); i < Timestep.getEndtYear(); i++) {
								if (p.toString().contains(PathTools.asFolder(String.valueOf(i)))) {
									temp.put(ProjectLoader.getScenario() + "|" + i, p);
								}
							}
						} else if (p.toString()
								.contains("default_" + pORb + File.separator + aftparams + label + ".csv")) {
							temp.put("default_" + pORb, p);
						} else if (p.toString().contains("default_" + pORb)) {
							for (int i = Timestep.getStartYear(); i < Timestep.getEndtYear(); i++) {
								if (p.toString().contains(PathTools.asFolder(String.valueOf(i)))
										&& p.toString().contains("default_" + pORb)) {
									temp.put("default_" + pORb + "|" + i, p);
								}
							}
						}
					});
					data.put(label, temp);
				}
			});
		} else {
			ArrayList<Path> directory = PathTools.findAllFilePaths(Paths.get(cofigpORb));

			hashAFTs.keySet().forEach(label -> {
				Map<String, Path> yPath = new HashMap<>();
				directory.forEach(p -> {
					if (p.toString().contains(File.separator + aftparams + label + ".csv")) {
						if (p.toString().contains(cofigpORb + File.separator + aftparams + label + ".csv")) {
							yPath.put("default_" + pORb, p);
						}
						for (int i = Timestep.getStartYear(); i < Timestep.getEndtYear(); i++) {
							if (p.toString().contains(String.valueOf(i))) {
								yPath.put(ProjectLoader.getScenario() + "|" + i, p);
							}
						}
					}
				});
				data.put(label, yPath);
			});
		}
		return data;
	}

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
		aft_production_paths = productionPaths("production");
		aft_behevoir_paths = productionPaths("agents");
		hashAFTs.forEach((Label, a) -> {
			LOGGER.trace("Import Production and behaviour for AFT: " + Label);
			if (a.isInteract()) {
				Path pFile = Optional.ofNullable(aft_production_paths.get(Label)).map(paths -> {
					String scenarioKey = ProjectLoader.getScenario() + "|" + Timestep.getStartYear();
					if (paths.containsKey(scenarioKey))
						return paths.get(scenarioKey);
					if (paths.containsKey(ProjectLoader.getScenario()))
						return paths.get(ProjectLoader.getScenario());
					return paths.get("default_production");
				}).orElse(null);

				if (pFile != null) {
					initializeAFTProduction(pFile);
					LOGGER.trace("production file for (" + Label + "): " + pFile);
				} else {
					LOGGER.fatal("Could NOT found AFT Production file for initialisation: " + Label);
				}

				Path bFile = Optional.ofNullable(aft_behevoir_paths.get(Label)).map(paths -> {
					String scenarioKey = ProjectLoader.getScenario() + "|" + Timestep.getStartYear();
					if (paths.containsKey(scenarioKey))
						return paths.get(scenarioKey);
					if (paths.containsKey(ProjectLoader.getScenario()))
						return paths.get(ProjectLoader.getScenario());
					return paths.get("default_agents");
				}).orElse(null);

				if (pFile != null) {
					initializeAFTBehevoir(bFile);
					LOGGER.trace("giveIn-giveUp file for (" + Label + "): " + bFile);
				} else {
					LOGGER.fatal("Could NOT found AFT Production file for initialisation: " + Label);
				}

			}
		});
	}

	private void initializeAFTBehevoir(Path aftPath) {
		File file = aftPath.toFile();
		Aft a = hashAFTs.get(file.getName().replace(".csv", "").replace("AftParams_", ""));
		AftsUpdater.updateAFTBehevoir(a, file);
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

	private void addAbandonedAftIfNotExiste() {
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
		LOGGER.info("Number of cells for each AFT: " + hashAgentNbr);
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
