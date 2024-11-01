package dataLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.Cell;
import model.RegionClassifier;
import tech.tablesaw.api.Table;
import UtilitiesFx.filesTools.CsvTools;
import UtilitiesFx.filesTools.ReaderFile;
import UtilitiesFx.filesTools.PathTools;

/**
 * @author Mohamed Byari
 *
 */

public class CellsLoader {
	private static final Logger LOGGER = LogManager.getLogger(CellsLoader.class);
	public static Set<String> regionsNamesSet = new HashSet<>();
	public static ConcurrentHashMap<String, Cell> hashCell = new ConcurrentHashMap<>();
	public static boolean regionalization = true;
	private static List<String> capitalsList;
	public AFTsLoader AFtsSet;

	private static int nbrOfCells = 0;

	public void loadMap() {

		AFtsSet = new AFTsLoader();
		hashCell.clear();
		Path baseLindPath = PathTools.fileFilter(PathTools.asFolder("worlds"), "Baseline_map").iterator().next();
		ReaderFile.processCSV(this, baseLindPath, "Baseline");
		nbrOfCells = hashCell.size();
		if (nbrOfCells < 1000) {
			Cell.setSize(200);
		}

		loadGisData();
		RegionClassifier.initialation(false);
//		DemandModel.updateWorldDemand();
		S_WeightLoader.updateWorldWeight();
		AFTsLoader.hashAgentNbr();
		AFTsLoader.hashAgentNbrRegions();

		LOGGER.info("Number of cells for each AFT: " + AFTsLoader.hashAgentNbr);

	}

	public static void loadCapitalsList() {
		capitalsList = Collections.synchronizedList(new ArrayList<>());
		String[] line0 = CsvTools.columnFromscsv(0, PathTools.fileFilter(File.separator + "Capitals.csv").get(0));
		getCapitalsName().clear();
		for (int n = 1; n < line0.length; n++) {
			getCapitalsName().add(line0[n]);
		}
		LOGGER.info("Capitals size=" + getCapitalsName().size() + " CellsSet.getCapitalsName() " + getCapitalsName());

	}

	public static List<String> getCapitalsName() {
		return capitalsList;
	}

	public void loadGisData() {
		try {
			Path path = PathTools.fileFilter(true, File.separator + "GIS" + File.separator).get(0);
			PathsLoader.WorldName = path.toFile().getName().replace("_Regions", "").replace(".csv", "");
			LOGGER.info("WorldName = " + PathsLoader.WorldName);
			Table T = Table.read().csv(path.toFile());
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
			regionalization = false;
			LOGGER.warn(
					"The Regionalization File is not Found in the GIS Folder, this Data Will be Ignored - No Regionalization Will be Possible.");

		}
	}

	public void updateCapitals(int year) {
		year = Math.min(year, PathsLoader.getEndtYear());

		if (!PathsLoader.getScenario().equalsIgnoreCase("Baseline")) {
			Path path = PathTools.fileFilter(year + "", PathsLoader.getScenario(), PathTools.asFolder("capitals"))
					.get(0);
			ReaderFile.processCSV(this, path, "Capitals");
		}
	}

	public void servicesAndOwneroutPut(String year, String outputpath) {
		PathsLoader.setAllfilesPathInData(PathTools.findAllFiles(PathsLoader.getProjectPath()));
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
