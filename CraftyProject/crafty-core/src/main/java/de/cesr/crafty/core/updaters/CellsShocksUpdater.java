package de.cesr.crafty.core.updaters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.CsvKind;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;

public class CellsShocksUpdater extends AbstractUpdater {

	private static final CustomLogger LOGGER = new CustomLogger(CellsShocksUpdater.class);
	// This updater is responsible for associating shocks to cells based on the
	// current year and scenario.
	// <cell, <shock-capital, shockValue>>
	public static ConcurrentHashMap<Cell, ConcurrentHashMap<String, Double>> cellsShocks = new ConcurrentHashMap<>();

	public CellsShocksUpdater() {
		LOGGER.info("CellsShocksUpdater initialized");
		step();
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
		LOGGER.info("CellsShocksUpdater scheduled for repeating updates.");

	}

	@Override
	public void step() {
		LOGGER.info("Updating cells shocks...");
		ArrayList<Path> paths = PathTools.fileFilter(PathTools.asFolder("shocksMap"),
				Timestep.getCurrentYear() + ".csv");
		if (paths != null) {
			Path shockPath = paths.stream()
					.filter(path -> (path.toString().contains("cellsShocks_" + ProjectLoader.getScenario()))
							&& (path.toString().contains(Timestep.getCurrentYear() + ".csv")))
					.findFirst()
					.orElse(paths.stream()
							.filter(path -> (path.toString().contains("default_cellsShocks"))
									&& (path.toString().contains(Timestep.getCurrentYear() + ".csv")))
							.findFirst().orElse(null));
			if (shockPath != null) {
				LOGGER.info("Shock Map path found: " + shockPath);
				System.out.println("Shock Map path found: " + shockPath);
				CsvProcessors.processCSV(shockPath, CsvKind.SHOCKS);
				LOGGER.info("Cells shocks associated successfully.");
			} else {
				LOGGER.warn("Failure to read: " + shockPath);
			}
		} else {
			LOGGER.warn("No shocksMap folder found or no files found in the 'shocksMap' folder.");
		}
	}

}
