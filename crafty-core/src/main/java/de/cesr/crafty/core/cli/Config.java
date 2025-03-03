package de.cesr.crafty.core.cli;

public class Config {

	public String project_path;
	public String scenario;
	public boolean regionalization;
	// CRAFTY Mechanisms
	public boolean initial_demand_supply_equilibrium;
	public boolean remove_negative_marginal_utility;
	public boolean use_abandonment_threshold;
	public boolean mutate_on_competition_win;
	public double mutation_interval;
	public double MostCompetitorAFTProbability;
	public boolean averaged_residual_demand_per_cell;
	// Neighboring Effects
	public boolean use_neighbor_priority;
	public double neighbor_priority_probability;
	public int neighbor_radius;
	// Competitiveness Process
	public double participating_cells_percentage;
	public int marginal_utility_calculations_per_tick;
	public double land_abandonment_percentage;
	// Output Configurati
	public String output_folder_name;
	public boolean generate_csv_files;
	public int csv_output_frequency;
	public boolean track_changes;
	public boolean export_LOGGER;
	public boolean LOGGER_info;
	public boolean LOGGER_warn;
	public boolean LOGGER_trace;
	public static boolean chartSynchronisation = true;
	public static int chartSynchronisationGap = 5;
	public static boolean mapSynchronisation = true;
	public static int mapSynchronisationGap = 5;

}