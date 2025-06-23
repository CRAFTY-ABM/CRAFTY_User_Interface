package de.cesr.crafty.core.dataLoader.land;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.crafty.Region;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvKind;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

/**
 * @author Mohamed Byari
 *
 */

public class CellsLoader {
	private static final CustomLogger LOGGER = new CustomLogger(CellsLoader.class);
	public static Set<String> regionsNamesSet = new HashSet<>();
	public static ConcurrentHashMap<String, Cell> hashCell = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, Region> regions;
	public static boolean regionalization = ConfigLoader.config.regionalization;

	private static int nbrOfCells = 0;

	public CellsLoader() {
		initialize();

	}

	public void initialize() {
		hashCell.clear();
		Path baselinePath = PathTools.fileFilter(PathTools.asFolder("worlds"), "Baseline_map").iterator().next();
		CsvProcessors.processCSV(baselinePath, CsvKind.BASELINE);
		AFTsLoader.hashAgentNbr();
		new GisLoader().loadGisData();
		regionsInitialize();
		AFTsLoader.hashAgentNbrRegions();
		AFTsLoader.hashAgentNbr();
	}

	public void servicesAndOwneroutPut(String year, String outputpath) {
		ProjectLoader.setAllfilesPathInData(PathTools.findAllFilePaths(ProjectLoader.getProjectPath()));
		Path path = PathTools.fileFilter(year, ".csv").get(0);
		CsvProcessors.processCSV(path, CsvKind.SERVICES);
	}

	public Cell getCell(int i, int j) {
		return hashCell.get(i + "," + j);
	}

	public static int getNbrOfCells() {
		return nbrOfCells;
	}

	public static void regionsInitialize() {
		regions = new ConcurrentHashMap<>();
		if (regionalization) {
			CellsLoader.regionsNamesSet.forEach(regionName -> {
				regions.put(regionName, new Region(regionName));
			});
			CellsLoader.hashCell.values().parallelStream() /**/.forEach(c -> {
				if (c.getCurrentRegion() != null) {
					regions.get(c.getCurrentRegion()).getCells().put(c.getX() + "," + c.getY(), c);
				}
			});

			if (!ServiceSet.isRegionalServicesExisting()) {
				regionalization = false;
				regionsInitialize();
			}
		} else {
			String name = ProjectLoader.WorldName;
			regions.put(name, new Region(name));
			regions.get(name).setCells(CellsLoader.hashCell);
		}

		ServiceSet.initialseServices();
		ServiceSet.serviceupdater();

		LOGGER.info("Regions: " + regions.keySet());
	}

}
