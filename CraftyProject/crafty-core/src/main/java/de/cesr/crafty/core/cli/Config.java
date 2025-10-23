package de.cesr.crafty.core.cli;

import java.util.List;
import java.util.Random;

import de.cesr.crafty.core.main.MainHeadless;

public class Config {

	// Paths
	public String project_path = "";
	public String scenario = "";

	///
	public List<Integer> start_End_Year;

	public String BASELINE_path = "";
	public String CAPITALS_directory = "";
	public List<String> landControle_directories = null;
	public String Behevoir_Cells_directory;
	public String SHOCKS_Maps_directory;
	public String aft_production_directory = "";
	public String aft_behevoir_directory = "";
	public String service_demands_path;
	public String service_utility_weight_path;

	// Regionalisation
	public boolean regionalization = false;
	// CRAFTY Mechanisms
	public boolean initial_demand_supply_equilibrium = true;
	public boolean remove_negative_marginal_utility = false;
	public boolean use_abandonment_threshold = true;
	public boolean mutate_on_competition_win = false;
	public double mutation_interval = 0.01;
	public double MostCompetitorAFTProbability = 0.8;
	public boolean averaged_residual_demand_per_cell = false;
	// Neighboring Effects
	public boolean use_neighbor_priority = true;
	public double neighbor_priority_probability = 0.95;
	public int neighbor_radius = 2;
	// Competitiveness Process
	public long seedID;
	public double participating_cells_percentage = 0.03;
	public int marginal_utility_calculations_per_tick = 1;
	public double land_abandonment_percentage = 0.03;
	public double takeOverUnmanageCells_percentage = 0.8;

	// Output Configurati
	public String output_folder_name;
	public String Output_path;
	public boolean generate_output_files = true;
	public boolean generate_charts_plots_PNG = false;
	public boolean generate_charts_plots_PDF = false;
	public boolean generate_map_output_files = true;
	public boolean generate_map_plots_tif = false;
	public int map_output_frequency = 10;
	public boolean track_changes = false;
	public boolean export_LOGGER = true;
	public boolean LOGGER_info = true;
	public boolean LOGGER_warn = true;
	public boolean LOGGER_trace = false;
	public static boolean chartSynchronisation = true;
	public static int chartSynchronisationGap = 5;
	public static boolean mapSynchronisation = true;
	public static int mapSynchronisationGap = 5;
	public Object map_output_years = null;
	public String comments;

	@Override
	public String toString() {
		String jar = JarInfo.jarFileName(MainHeadless.class);
		if (!jar.isEmpty()) {
			jar = "CARFTY running from: " + jar;
		} else {
			jar = "CRAFTY running from: classes/IDE Not a JAR file";
		}
		
		if (seedID == 0) {
			seedID = new Random().nextLong();
		}

		return "\n # Configuration for CRAFTY Model \n\n" + jar + "\n\n" + "" + "project_path: \"" + project_path
				+ "\"\n" + "scenario: \"" + scenario + "\n" + "BASELINE_path: " + BASELINE_path + "\n"
				+ "CAPITALS_directory: " + CAPITALS_directory + "\n" + "landControle_directories: "
				+ landControle_directories + "\n" + "Behevoir_Cells_directory: " + Behevoir_Cells_directory + "\n"
				+ "SHOCKS_Maps_directory: " + SHOCKS_Maps_directory + "\n" + "aft_production_directory: "
				+ aft_production_directory + "\n" + "aft_behevoir_directory: " + aft_behevoir_directory + "\n"
				+ "service_demands_path: " + service_demands_path + "\n" + "service_utility_weight_path: "
				+ service_utility_weight_path + "\n\n" + "regionalization: " + regionalization + "\n" + "\n"
				+ "# CRAFTY Mechanisms \n" + "initial_demand_supply_equilibrium: " + initial_demand_supply_equilibrium
				+ "\n" + "remove_negative_marginal_utility: " + remove_negative_marginal_utility + "\n"
				+ "use_abandonment_threshold: " + use_abandonment_threshold + "\n" + "mutate_on_competition_win: "
				+ mutate_on_competition_win + "\n" + "mutation_interval: " + mutation_interval
				+ "MostCompetitorAFTProbability: " + MostCompetitorAFTProbability + "\n"
				+ "averaged_residual_demand_per_cell: " + averaged_residual_demand_per_cell + "\n" + "\n"
				+ "# Neighboring Effects \n" + "use_neighbor_priority: " + use_neighbor_priority + "\n"
				+ "neighbor_priority_probability: " + neighbor_priority_probability + "\n" + "neighbor_radius: "
				+ neighbor_radius + "\n \n" + "# Competitiveness Process \n" + "land_abandonment_percentage: "
				+ land_abandonment_percentage + "\n" + "seedID: " + seedID + "\n"
				+ "participating_cells_percentage: " + participating_cells_percentage + "\n"
				+ "takeOverUnmanageCells_percentage: " + takeOverUnmanageCells_percentage + "\n"
				+ "marginal_utility_calculations_per_tick: " + marginal_utility_calculations_per_tick + "\n" + "\n"
				+ "# Output Configuration \n" + "output_folder_name: " + output_folder_name + "\n"
				+ "map_output_years: " + map_output_years + "\n"
//				+ "Output_path: "+Output_path 
				+ "\n\n" + "comments: \"" + comments + "\"\n\n\n";
	}

}