package de.cesr.crafty.core.updaters;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
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

	public CapitalUpdater() {
		capitalsList = Collections.synchronizedList(new ArrayList<>());
		Map<String, List<String>> capitalsFile = CsvProcessors
				.ReadAsaHash(PathTools.fileFilter(File.separator + "Capitals.csv").get(0));
		String label = capitalsFile.keySet().contains("Label") ? "Label" : "Name";
		setCapitalsList(capitalsFile.get(label));
		LOGGER.info("Capitals size=" + getCapitalsList().size() + " =>" + getCapitalsList());
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		LOGGER.info("Cells.updateCapitals");
		if (!ProjectLoader.getScenario().equalsIgnoreCase("Baseline")) {
			/// should  be go to the list and read  the corespondant path and fines
			Path path = PathTools.fileFilter(String.valueOf(Timestep.getCurrentYear()),
					PathTools.asFolder(ProjectLoader.getScenario()), PathTools.asFolder("capitals")).get(0);
			
			CsvProcessors.processCSV(path, CsvKind.CAPITALS);
		}
	}

	public static List<String> getCapitalsList() {
		return capitalsList;
	}

	public static void setCapitalsList(List<String> capitalsList) {
		CapitalUpdater.capitalsList = capitalsList;
	}

}
