package de.cesr.crafty.core.plumLinking;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.main.MainHeadless;
import de.cesr.crafty.core.utils.file.PathTools;

public class MainMultiRuns {
	public static void main(String[] args) {
		MainHeadless.initializeConfig(args);
		configModefier("LOGGER_info", true);
//		for (int i = 0; i < 4; i++) {
			configModefier("output_folder_name", "'OnlyCRAFTY-test" + 111 + "'");
			configModefier("participating_cells_percentage", (double) (0.6));
			MainCoupling.main(args);

//		}
	}

	public static void configModefier(String name, Object value) {
		String str = PathTools.read(ConfigLoader.configPath);
		String[] strSplited = str.split("\n");
		String output = "";
		for (int i = 0; i < strSplited.length; i++) {
			String tmp = strSplited[i];
			tmp = tmp.split("#")[0];
			tmp = tmp.replace(" ", "");

			String[] n2v = tmp.split(":", 2);
			if (n2v.length == 2) {
				if (n2v[0].equals(name)) {
					n2v[1] = value.toString();
				}
				output = output + "" + n2v[0] + ": " + n2v[1] + "\n";
			}
		}
		PathTools.writeFile(ConfigLoader.configPath, output, false);
	}

}
