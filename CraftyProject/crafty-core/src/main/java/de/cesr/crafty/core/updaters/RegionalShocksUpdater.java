package de.cesr.crafty.core.updaters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class RegionalShocksUpdater extends AbstractUpdater {
	private static final CustomLogger LOGGER = new CustomLogger(RegionalShocksUpdater.class);
	// This updater is responsible for associating Regional shocks to capitals based
	// on the current year and scenario.
	static ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Double>>> shocks = new ConcurrentHashMap<>(); // <region,capital,year,value>

	public RegionalShocksUpdater() {
		ArrayList<Path> paths = PathTools.fileFilter(PathTools.asFolder("shocks"));
		if (paths != null) {
			CellsLoader.regions.keySet().forEach(r -> {
				Path shockPath = paths.stream()
						.filter(path -> (path.toString().contains("shocks_" + ProjectLoader.getScenario()))
								&& (path.toString().contains("shocks_" + r)))
						.findFirst().orElse(paths.stream().filter(path -> (path.toString().contains("default_shocks"))
								&& (path.toString().contains("shocks_" + r))).findFirst().orElse(null));
				if (shockPath != null) {
					shocks.put(r, new ConcurrentHashMap<>());
					CapitalUpdater.getCapitalsList().forEach(capitalName -> {
						shocks.get(r).put(capitalName, new ConcurrentHashMap<>());
						Map<String, List<String>> csv = CsvProcessors.ReadAsaHash(shockPath);
						csv.get("Year").forEach(year -> {
							shocks.get(r).get(capitalName).put(Utils.sToI(year),
									Utils.sToD(csv.get(capitalName).get(csv.get("Year").indexOf(year))));
						});
					});
					LOGGER.info("Upload Shock File for: " + r + " |");
					System.out.println("Upload Shock File for: " + r + " |");

				} else {
					LOGGER.warn("No shocks file fund for | " + r + " |");
					shocks.put(r, null);
				}
			});
		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		System.out.println();
		if (shocks.size() > 0) {
			LOGGER.info("Associet Shocks to capital");
			CellsLoader.regions.values().forEach(r -> {
				r.getCells().values().forEach(c -> {
//					System.out.println(r + " || " + shocks.get(r.getName()) != null);
					if (r != null && shocks.get(r.getName()) != null) {
						CapitalUpdater.getCapitalsList().forEach(capitalName -> {
							double shockVelue = shocks.get(r.getName()).get(capitalName).get(Timestep.getCurrentYear());
							double oldCapital = c.getOneCapitals(capitalName);
							double newCapital = Math.max(0, oldCapital - (oldCapital * shockVelue));
//						if (capitalName.equals("BioenergyG2"))
//							System.out.println(
//									capitalName + ": " + oldCapital + "=>" + newCapital + "          " + shockVelue);

							c.setOneCapitals(capitalName, newCapital);
						});
					}
				});
			});
		}
	}

}
