package de.cesr.crafty.core.updaters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.Timestep;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

/**
 * @author Mohamed Byari
 *
 */
public class AftsUpdater extends AbstractUpdater {
	private static final CustomLogger LOGGER = new CustomLogger(AftsUpdater.class);
	public AFTsLoader AFtsSet;

	public AftsUpdater() {
		AFtsSet = new AFTsLoader();
	}

	@Override
	public void toSchedule() {
		modelRunner.scheduleRepeating(this);
	}

	@Override
	public void step() {
		updateProduction();
		updateBehaviour();
	}

	private void updateProduction() {
		Path pFolderToUpdate = ProjectLoader.getProjectPath().resolve("production").resolve(ProjectLoader.getScenario())
				.resolve("update_production_" + Timestep.getCurrentYear());
		if (!pFolderToUpdate.toFile().exists()) {
			pFolderToUpdate = ProjectLoader.getProjectPath().resolve("agents").resolve("default_production")
					.resolve("update_behaviour_" + Timestep.getCurrentYear());
		}
		if (pFolderToUpdate.toFile().exists()) {
			List<Path> pFiles = PathTools.fileFilter(pFolderToUpdate.toString());
			pFiles.forEach(f -> {
				File file = f.toFile();
				updateAFTProduction(AFTsLoader.getAftHash().get(file.getName().replace(".csv", "")), file);
			});
		} else {
			LOGGER.info("AFT production parameters not updated (no folder found)");
		}
	}

	private void updateBehaviour() {
		Path bFolderToUpdate = ProjectLoader.getProjectPath().resolve("agents").resolve(ProjectLoader.getScenario())
				.resolve("update_behaviour_" + Timestep.getCurrentYear());

		if (!bFolderToUpdate.toFile().exists()) {
			bFolderToUpdate = ProjectLoader.getProjectPath().resolve("agents").resolve("default_behaviour")
					.resolve("update_behaviour_" + Timestep.getCurrentYear());
		}
		if (bFolderToUpdate.toFile().exists()) {
			List<Path> bFiles = PathTools.fileFilter(bFolderToUpdate.toString());
			bFiles.forEach(f -> {
				File file = f.toFile();
				try {
					updateAFTBehevoir(
							AFTsLoader.getAftHash().get(file.getName().replace(".csv", "").replace("AftParams_", "")),
							file);
				} catch (NullPointerException e) {
					LOGGER.error("AFT Not in the List: " + file);
				}
			});
		} else {
			LOGGER.info("AFT behaviour parameters not updated (no folder found)");
		}
	}

	public static void updateAFTProduction(Aft a, File file) {
		String[][] m = CsvTools.csvReader(file.toPath());
		for (int i = 0; i < m.length; i++) {
			if (!m[i][0].isBlank()) {
				if (ServiceSet.getServicesList().contains(m[i][0])) {
					a.getProductivityLevel().put(m[i][0], Utils.sToD(m[i][Utils.indexof("Production", m[0])]));
				} else {
					LOGGER.warn(m[i][0] + "  is not existe in Services List, will be ignored");
				}
			}
		}

		updateSensitivty(a, file);
	}

	public static void updateSensitivty(Aft a, File file) {
		try {
			CsvReadOptions options = CsvReadOptions.builder(file).separator(',').build();
			Table T = Table.read().usingOptions(options);
			CapitalUpdater.getCapitalsList().forEach((Cn) -> {
				ServiceSet.getServicesList().forEach((Sn) -> {
					Object s = T.column(Cn).get(T.column(0).indexOf(Sn));
					if (s instanceof Double) {
						a.getSensitivity().put((Cn + "|" + Sn), (double) s);
					} else if (s instanceof Integer) {
						a.getSensitivity().put((Cn + "|" + Sn), ((Integer) s).doubleValue());
					}
				});
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void updateAFTBehevoir(Aft a, File file) {
		Map<String, List<String>> reder = CsvProcessors.ReadAsaHash(file.toPath());
		a.setGiveInMean(Utils.sToD(reder.get("givingInDistributionMean").get(0)));
		a.setGiveUpMean(Utils.sToD(reder.get("givingUpDistributionMean").get(0)));
		a.setGiveInSD(Utils.sToD(reder.get("givingInDistributionSD").get(0)));
		a.setGiveUpSD(Utils.sToD(reder.get("givingUpDistributionSD").get(0)));
		a.setServiceLevelNoiseMin(Utils.sToD(reder.get("serviceLevelNoiseMin").get(0)));
		a.setServiceLevelNoiseMax(Utils.sToD(reder.get("serviceLevelNoiseMax").get(0)));
		a.setGiveUpProbabilty(Utils.sToD(reder.get("givingUpProb").get(0)));
	}

}
