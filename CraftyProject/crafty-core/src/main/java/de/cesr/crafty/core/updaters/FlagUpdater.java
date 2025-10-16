package de.cesr.crafty.core.updaters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.file.DirectoryWatcher;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FlagUpdater extends AbstractUpdater {

	HashMap<Integer, Path> flags = new HashMap<>();

	public FlagUpdater() {
		// Update year -> flag file

		// Checke if there is config-waitingflagfile
		// check
		ArrayList<Path> p = PathTools.fileFilter(PathTools.asFolder("config"), "waitingFlags.csv");
		Path csv = p != null ? p.iterator().next() : null;
		if (csv != null) {
			Map<String, List<String>> hash = CsvProcessors.ReadAsaHash(csv);
			for (int i = 0; i < hash.values().iterator().next().size(); i++) {
				flags.put(Utils.sToI(hash.get("Year").get(i)), Paths.get(hash.get("Waiting_Flag").get(i)));
			}
		}
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
//		wait For a flag
		if (flags != null && flags.size() > 0) {
			Path p = flags.get(Timestep.getCurrentYear());
			if (p != null) {
				DirectoryWatcher.waitForYearFolder(p);
			}
		}

	}

}
