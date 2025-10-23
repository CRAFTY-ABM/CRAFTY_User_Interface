package de.cesr.crafty.core.updaters;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.CsvKind;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

/**
 * @author Mohamed Byari
 *
 */
public class CapitalUpdater extends AbstractUpdater {
	private static final CustomLogger LOGGER = new CustomLogger(CapitalUpdater.class);
	// add to the Schedule then run everything later
	// define the list of path will be use dusring the simulation HashMap<year,path>

	private static List<String> capitalsList;
	private static Map<Integer, Path> CAPITALS_directory = new TreeMap<>();

	public CapitalUpdater() {
		capitalsList = Collections.synchronizedList(new ArrayList<>());
		Map<String, List<String>> capitalsFile = CsvProcessors
				.ReadAsaHash(PathTools.fileFilter(File.separator + "Capitals.csv").get(0));
		String label = capitalsFile.keySet().contains("Label") ? "Label" : "Name";
		setCapitalsList(capitalsFile.get(label));
		LOGGER.info("Capitals size=" + getCapitalsList().size() + " =>" + getCapitalsList());
		// fill the path any way
		// <year,csv file>
		if (!ConfigLoader.config.CAPITALS_directory.isEmpty()) {
			ArrayList<Path> ps = PathTools.findAllFilePaths(Paths.get(ConfigLoader.config.CAPITALS_directory));
			for (int i = Timestep.getStartYear(); i <= Timestep.getEndtYear(); i++) {
				try {
					CAPITALS_directory.put(i, PathTools.fileFilter(ps, "_" + i, "capitals", ".csv").get(0));
				} catch (NullPointerException e) {
					LOGGER.fatal(
							"Capitals for " + i + " Not Fund in Directory: " + ConfigLoader.config.CAPITALS_directory);
				}
			}
		} else {
			ArrayList<Path> ps = PathTools.fileFilter(PathTools.asFolder(ProjectLoader.getScenario()),
					PathTools.asFolder("worlds"), PathTools.asFolder("capitals"));
			for (int i = Timestep.getStartYear(); i <= Timestep.getEndtYear(); i++) {
				try {
					CAPITALS_directory.put(i, PathTools.fileFilter(ps, "_" + i, "capitals", ".csv").get(0));
				} catch (NullPointerException e) {
					LOGGER.fatal(
							"Capitals for " + i + " Not Fund in Directory: " + ConfigLoader.config.CAPITALS_directory);
				}
			}
		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {

		Path path = CAPITALS_directory.get(Timestep.getCurrentYear());
		LOGGER.info("Cells.updateCapitals" + path);
		CsvProcessors.processCSV(path, CsvKind.CAPITALS);

	}

	public static List<String> getCapitalsList() {
		return capitalsList;
	}

	public static void setCapitalsList(List<String> capitalsList) {
		CapitalUpdater.capitalsList = capitalsList;
	}

}
