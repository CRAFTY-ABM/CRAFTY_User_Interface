package de.cesr.crafty.core.cli;

import org.apache.commons.cli.*;



public class OptionsParser {

	/**
	 * Parse CLI arguments using Apache Commons CLI.
	 * 
	 * @param args command-line arguments
	 * @return a {@link CraftyOptions} object with parsed data
	 */
	public static CraftyOptions parseArguments(String[] args) {
		Options options = new Options();

		Option configOption = Option.builder("c").longOpt("config-file").desc("Path to the YAML config file.")
				.hasArg(true).argName("CONFIG_FILE").required(true).build();
		options.addOption(configOption);

		Option projectDirOption = Option.builder("p").longOpt("project-dir")
				.desc("Path to override the project directory in the config file.").hasArg(true).argName("PROJECT_DIR")
				.required(false).build();
		options.addOption(projectDirOption);
		Option scenarioOption = Option.builder("s").longOpt("scenario-name")
				.desc("scenario name to override the scenario name in the config file.").hasArg(true)
				.argName("SCENARIO_NAME").required(false).build();
		options.addOption(scenarioOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CraftyOptions craftyOptions = new CraftyOptions();

		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("config-file")) {
				craftyOptions.setConfigFilePath(cmd.getOptionValue("config-file"));
			}
			if (cmd.hasOption("project-dir")) {
				craftyOptions.setProjectDirectoryPath(cmd.getOptionValue("project-dir"));
			}
			if (cmd.hasOption("scenario-name")) {
				craftyOptions.setScenario_Name(cmd.getOptionValue("scenario-name"));
			}

		} catch (ParseException e) {
			System.err.println("Error parsing arguments: " + e.getMessage());
			formatter.printHelp("CRAFTY", options);
			System.exit(1);
		}

		return craftyOptions;
	}
}
