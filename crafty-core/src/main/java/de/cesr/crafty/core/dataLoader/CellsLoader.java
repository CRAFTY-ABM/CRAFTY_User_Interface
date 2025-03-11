package de.cesr.crafty.core.dataLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.model.Cell;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

/**
 * @author Mohamed Byari
 *
 */

public class CellsLoader {
	private static final CustomLogger LOGGER = new CustomLogger(CellsLoader.class);
	public static Set<String> regionsNamesSet = new HashSet<>();
	public static ConcurrentHashMap<String, Cell> hashCell = new ConcurrentHashMap<>();
	private static List<String> capitalsList;
	public AFTsLoader AFtsSet;

	private static int nbrOfCells = 0;
 
	public void loadMap() {
		AFtsSet = new AFTsLoader();
		hashCell.clear();

		Path baseLindPath = PathTools.fileFilter(PathTools.asFolder("worlds"), "Baseline_map").iterator().next();
		ReaderFile.processCSV(this, baseLindPath, "Baseline");
		nbrOfCells = hashCell.size();
		if (nbrOfCells < 1000) {// temporal for the visualization of very small maps 
			Cell.setSize(200);
		}

		loadGisData();
		CellBehaviourLoader.initialize();
		AFTsLoader.hashAgentNbr();
// 
		LOGGER.info("Number of cells for each AFT: " + AFTsLoader.hashAgentNbr);
	}
	
	

	public static void loadCapitalsList() {
		capitalsList = Collections.synchronizedList(new ArrayList<>());
		HashMap<String, ArrayList<String>> capitalsFile = ReaderFile
				.ReadAsaHash(PathTools.fileFilter(File.separator + "Capitals.csv").get(0));
		String label = capitalsFile.keySet().contains("Label") ? "Label" : "Name";
		setCapitalsList(capitalsFile.get(label));
		LOGGER.info("Capitals size=" + getCapitalsList().size() + " CellsSet.getCapitalsName() " + getCapitalsList());
	}

	public static List<String> getCapitalsList() {
		return capitalsList;
	}

	public static void setCapitalsList(List<String> capitalsList) {
		CellsLoader.capitalsList = capitalsList;
	}

	public void loadGisData() {
		try {
			Path path = PathTools.fileFilter(true, File.separator + "GIS" + File.separator).get(0);
			ProjectLoader.WorldName = path.toFile().getName().replace("_Regions", "").replace(".csv", "");
			LOGGER.info("WorldName = " + ProjectLoader.WorldName);
			CsvReadOptions options = CsvReadOptions.builder(path.toFile()).separator(',').build();
			Table T = Table.read().usingOptions(options);

			for (int i = 0; i < T.columns().iterator().next().size(); i++) {
				String coor = T.column("X").get(i) + "," + T.column("Y").get(i);
				int ii = i;
				if (hashCell.get(coor) != null) {
					T.columnNames().forEach(name -> {
						if (T.column(name).get(ii) != null && name.contains("Region_Code")) {
							hashCell.get(coor).setCurrentRegion(T.column(name).get(ii).toString());
							regionsNamesSet.add(T.column(name).get(ii).toString());
						}
					});
				}
			}
		} catch (NullPointerException | IOException e) {
			RegionClassifier.regionalization = false;
			LOGGER.warn(
					"The Regionalization File is not Found in the GIS Folder, this Data Will be Ignored - No Regionalization Will be Possible.");

		}
	}

	public void updateCapitals(int year) {
		LOGGER.info("Cells.updateCapitals");
		year = Math.min(year, ProjectLoader.getEndtYear());

		if (!ProjectLoader.getScenario().equalsIgnoreCase("Baseline")) {
			Path path = PathTools.fileFilter(year + "", ProjectLoader.getScenario(), PathTools.asFolder("capitals"))
					.get(0);
			ReaderFile.processCSV(this, path, "Capitals");
		}
	}
	

	
	

	public void servicesAndOwneroutPut(String year, String outputpath) {
		ProjectLoader.setAllfilesPathInData(PathTools.findAllFiles(ProjectLoader.getProjectPath()));
		Path path = PathTools.fileFilter(year, ".csv").get(0);

		ReaderFile.processCSV(this, path, "Services");
	}

	public Cell getCell(int i, int j) {
		return hashCell.get(i + "," + j);
	}

	public static int getNbrOfCells() {
		return nbrOfCells;
	}

}
