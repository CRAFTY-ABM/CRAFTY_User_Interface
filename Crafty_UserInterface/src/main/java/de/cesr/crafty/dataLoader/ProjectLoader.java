package de.cesr.crafty.dataLoader;

import de.cesr.crafty.cli.ConfigLoader;
import de.cesr.crafty.model.RegionClassifier;
import de.cesr.crafty.utils.analysis.CustomLogger;
import de.cesr.crafty.utils.file.PathTools;
import de.cesr.crafty.utils.file.ReaderFile;
import de.cesr.crafty.utils.graphical.Tools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mohamed Byari
 *
 */

public final class ProjectLoader {
	private static final CustomLogger LOGGER = new CustomLogger(ProjectLoader.class);
	private static int startYear;
	private static int endtYear;
	private static int currentYear = startYear;
	private static Path projectPath;

	private static ArrayList<String> scenariosList = new ArrayList<>();
	private static HashMap<String, String> scenariosHash = new HashMap<>();
	static ArrayList<Path> allfilesPathInData;
	private static String scenario;
	public static String WorldName = "";

	public static CellsLoader cellsSet = new CellsLoader();

	public static void pathInitialisation(Path p) {
		projectPath = p;
		allfilesPathInData = PathTools.findAllFiles(projectPath);
		initialSenarios();
	}

	public static void modelInitialisation() {
		pathInitialisation(Paths.get(ConfigLoader.config.project_path));
		setScenario(ConfigLoader.config.scenario);

		CellsLoader.loadCapitalsList();
		ServiceSet.loadServiceList();
		cellsSet.loadMap();
		RegionClassifier.initialation();
		ServiceWeightLoader.updateWorldWeight();
		AFTsLoader.hashAgentNbrRegions();
		MaskRestrictionDataLoader.allMaskAndRistrictionUpdate();
	}

	static void initialSenarios() {
		Path path = PathTools.fileFilter(File.separator + "scenarios.csv").iterator().next();
		HashMap<String, ArrayList<String>> hash = ReaderFile.ReadAsaHash(path);
		setScenariosList(hash.get("Name"));
		for (String scenario : scenariosList) {
			try {
				scenariosHash.put(scenario, hash.get("startYear").get(hash.get("Name").indexOf(scenario)) + "_"
						+ hash.get("endtYear").get(hash.get("Name").indexOf(scenario)));
			} catch (NullPointerException e) {
				LOGGER.fatal(
						"cannot find \"Name\", \"startYear\" and/or \"endtYear\" in the head of the file :" + path);
				break;
			}
		}
		setScenario(getScenariosList().get(1));

	}

	public static ArrayList<Path> getAllfilesPathInData() {
		return allfilesPathInData;
	}

	public static int getStartYear() {
		return startYear;
	}

	public static void setStartYear(int startYear) {
		ProjectLoader.startYear = startYear;
	}

	public static int getEndtYear() {
		return endtYear;
	}

	public static void setEndtYear(int endtYear) {
		ProjectLoader.endtYear = endtYear;
	}

	public static int getCurrentYear() {
		return currentYear;
	}

	public static void setCurrentYear(int currentYear) {
		ProjectLoader.currentYear = currentYear;
	}

	public static Path getProjectPath() {
		return projectPath;
	}

	public static ArrayList<String> getScenariosList() {
		return scenariosList;
	}

	public static void setScenariosList(ArrayList<String> list) {
		scenariosList = new ArrayList<>(list);
	}

	public static void setAllfilesPathInData(ArrayList<Path> allfilesPathInData) {
		ProjectLoader.allfilesPathInData = allfilesPathInData;
	}

	public static String getScenario() {
		return scenario;
	}

	public static void setScenario(String scenario) {
		ProjectLoader.scenario = scenario;
		String[] temp = scenariosHash.get(scenario).split("_");
		startYear = (int) Tools.sToD(temp[0]);
		endtYear = (int) Tools.sToD(temp[1]);
		LOGGER.info(scenario + "--> startYear= " + startYear + ", endtYear " + endtYear);
	}
	

}
