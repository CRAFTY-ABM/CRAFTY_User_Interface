package de.cesr.crafty.dataLoader;

import de.cesr.crafty.utils.analysis.CustomLogger;
import de.cesr.crafty.utils.file.PathTools;
import de.cesr.crafty.utils.file.ReaderFile;
import de.cesr.crafty.utils.graphical.Tools;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mohamed Byari
 *
 */

public final class PathsLoader {
	private static final CustomLogger LOGGER = new CustomLogger(PathsLoader.class);
	private static int startYear;
	private static int endtYear;
	private static int currentYear = startYear;
	private static Path projectPath;

	private static ArrayList<String> scenariosList = new ArrayList<>();
	private static HashMap<String, String> scenariosHash = new HashMap<>();
	static ArrayList<Path> allfilesPathInData;
	private static String scenario;
	public static String WorldName = "";

	public static void initialisation(Path p) {
		projectPath = p;
		allfilesPathInData = PathTools.findAllFiles(projectPath);
		initialSenarios();
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
		PathsLoader.startYear = startYear;
	}

	public static int getEndtYear() {
		return endtYear;
	}

	public static void setEndtYear(int endtYear) {
		PathsLoader.endtYear = endtYear;
	}

	public static int getCurrentYear() {
		return currentYear;
	}

	public static void setCurrentYear(int currentYear) {
		PathsLoader.currentYear = currentYear;
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
		PathsLoader.allfilesPathInData = allfilesPathInData;
	}

	public static String getScenario() {
		return scenario;
	}

	public static void setScenario(String scenario) {
		PathsLoader.scenario = scenario;
		String[] temp = scenariosHash.get(scenario).split("_");
		startYear = (int) Tools.sToD(temp[0]);
		endtYear = (int) Tools.sToD(temp[1]);
		LOGGER.info(scenario + "--> startYear= " + startYear + ", endtYear " + endtYear);
	}

}
