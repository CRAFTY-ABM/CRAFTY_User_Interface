package de.cesr.crafty.core.dataLoader;

import de.cesr.crafty.core.dataLoader.land.MaskRestrictionDataLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mohamed Byari
 *
 */

public final class ProjectLoader {
	private static final CustomLogger LOGGER = new CustomLogger(ProjectLoader.class);
	private static Path projectPath;

	private static ArrayList<String> scenariosList = new ArrayList<>();
	private static HashMap<String, String> scenariosHash = new HashMap<>();
	private static ArrayList<Path> allfilePathsInDataDirectory;
	private static String scenario;
	public static String WorldName = "";

	
	public static MaskRestrictionDataLoader Maskloader = new MaskRestrictionDataLoader();

	public static void pathInitialisation(Path p) {
		projectPath = p;
		allfilePathsInDataDirectory = PathTools.findAllFilePaths(projectPath);
		initialSenarios();
	}


	static void initialSenarios() {
		Path path = PathTools.fileFilter(File.separator + "scenarios.csv").iterator().next();
		HashMap<String, ArrayList<String>> hash = CsvProcessors.ReadAsaHash(path);
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
	}

	public static ArrayList<Path> getAllfilesPathInData() {
		return allfilePathsInDataDirectory;
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
		ProjectLoader.allfilePathsInDataDirectory = allfilesPathInData;
	}

	public static String getScenario() {
		return scenario;
	}

	public static void setScenario(String scenario) {
		ProjectLoader.scenario = scenario;
		String[] temp = scenariosHash.get(scenario).split("_");
		Timestep.setStartYear((int) Utils.sToD(temp[0]));
		Timestep.setCurrentYear(Timestep.getStartYear());
		Timestep.setEndtYear((int) Utils.sToD(temp[1]));
		LOGGER.info(scenario + "--> startYear= " + Timestep.getStartYear() + ", endtYear " + Timestep.getEndtYear());
	}

}
