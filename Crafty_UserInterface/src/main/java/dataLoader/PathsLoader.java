package dataLoader;

import java.util.List;

import utils.analysis.CustomLogger;
import utils.filesTools.PathTools;
import utils.filesTools.ReaderFile;
import utils.graphicalTools.Tools;

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
	private static String[] foldersNecessary = { "agents", "csv", "production", "worlds" };
	private static int startYear;
	private static int endtYear;
	private static int currentYear = startYear;
	private static Path projectPath ;
	
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
		Path path = PathTools.fileFilter(File.separator+"scenarios.csv").iterator().next();
		HashMap<String, ArrayList<String>> hash = ReaderFile.ReadAsaHash(path);
		setScenariosList(hash.get("Name"));
		for (String scenario : scenariosList) {
			try {
				scenariosHash.put(scenario, hash.get("startYear").get(hash.get("Name").indexOf(scenario)) + "_"
						+ hash.get("endtYear").get(hash.get("Name").indexOf(scenario)));
			} catch (NullPointerException e) {
				LOGGER.fatal("cannot find \"Name\", \"startYear\" and/or \"endtYear\" in the head of the file :"+ path);
				break;
			}
		}
		setScenario(getScenariosList().get(1));

	}

	static public List<String> checkfolders(String path) {
		List<String> listOfFilesMissing = new ArrayList<>();
		List<File> folders = PathTools.detectFolders(path);
		List<String> foldersname = new ArrayList<>();
		folders.forEach(e -> {
			foldersname.add(e.getName());
		});
		for (int i = 0; i < foldersNecessary.length; i++) {
			if (!foldersname.contains(foldersNecessary[i])) {
				listOfFilesMissing.add(folders.get(0).getParent() + File.separator + foldersNecessary[i]);
			}
		}

		return listOfFilesMissing;
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
		 LOGGER.info(scenario+"--> startYear= "+ startYear+", endtYear "+
		 endtYear);
	}

}
