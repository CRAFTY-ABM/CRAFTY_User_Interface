package de.cesr.crafty.core.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cesr.crafty.core.cli.Config;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.cli.CustomLogger;
import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.ManagerTypes;
import de.cesr.crafty.core.crafty.Region;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.updaters.Timestep;

/**
 * Cleanup plan step 0.4 - the shape of the Listener's land-use change table.
 *
 * WHY THIS FILE EXISTS
 * The Listener builds its CSV outputs as plain String[][] arrays sized from
 * the simulation length. Bug B12 lived here: the land-use change table had one
 * row fewer than every sibling table, so the final simulated year had no row
 * at all and its count was silently lost.
 *
 * That bug has been fixed - and has already been re-broken once and re-fixed
 * in this repository, which is the strongest possible argument for pinning the
 * shape down in a test rather than relying on it staying right.
 *
 * These tests are cheap, need almost no fixture, and turn a repeat of that
 * mistake into a red build instead of a quietly truncated output file.
 */
class ListenerOutputShapeTest {

	private static final int START_YEAR = 2000;
	private static final int END_YEAR = 2010;
	private static final int YEARS = END_YEAR - START_YEAR + 1; // inclusive, so 11

	private Config configBackup;
	private ConcurrentHashMap<String, Region> regionsBackup;
	private List<String> servicesBackup;
	private int startYearBackup, endYearBackup, currentYearBackup, tickBackup, sizeBackup;

	@BeforeEach
	void setUp() {
		configBackup = ConfigLoader.config;
		regionsBackup = CellsLoader.regions;
		servicesBackup = new ArrayList<>(ServiceSet.getServicesList());
		startYearBackup = Timestep.getStartYear();
		endYearBackup = Timestep.getEndtYear();
		currentYearBackup = Timestep.getCurrentYear();
		tickBackup = Timestep.getTick();
		sizeBackup = Timestep.getSize();

		Config config = new Config();
		config.generate_output_files = false;
		// Map output is switched off deliberately: this file is only about the
		// land-use change table, and leaving it on would append to the static
		// Listener.yearsMapExporting list as a side effect.
		config.generate_map_output_files = false;
		config.logger_info = false;
		config.logger_warn = false;
		config.logger_trace = false;
		ConfigLoader.config = config;

		Timestep.setStartYear(START_YEAR);
		Timestep.setEndtYear(END_YEAR);
		Timestep.setCurrentYear(START_YEAR);
		Timestep.setTick(0);
		Timestep.setSize(YEARS);

		ServiceSet.getServicesList().clear();
		ServiceSet.getServicesList().addAll(List.of("food", "timber"));

		AFTsLoader.getAftHash().clear();
		AFTsLoader.getActivateAFTsHash().clear();
		Aft farmer = aft("Farmer");
		Aft forester = aft("Forester");
		AFTsLoader.getAftHash().put("Farmer", farmer);
		AFTsLoader.getAftHash().put("Forester", forester);
		AFTsLoader.getActivateAFTsHash().put("Farmer", farmer);
		AFTsLoader.getActivateAFTsHash().put("Forester", forester);

		CellsLoader.regions = new ConcurrentHashMap<>();
		CellsLoader.regions.put("R1", new Region("R1"));

		Listener.compositionAftHash.clear();
		Listener.yearsMapExporting.clear();
	}

	@AfterEach
	void tearDown() {
		ConfigLoader.config = configBackup;
		CellsLoader.regions = regionsBackup;
		ServiceSet.getServicesList().clear();
		ServiceSet.getServicesList().addAll(servicesBackup);
		Timestep.setStartYear(startYearBackup);
		Timestep.setEndtYear(endYearBackup);
		Timestep.setCurrentYear(currentYearBackup);
		Timestep.setTick(tickBackup);
		Timestep.setSize(sizeBackup);
		Listener.yearsMapExporting.clear();
		Listener.compositionAftHash.clear();
		CustomLogger.shutdownRunFileLoggers();
	}

	private static Aft aft(String label) {
		Aft a = new Aft(label);
		a.setType(ManagerTypes.AFT);
		a.setColor("#000000");
		return a;
	}

	@Test
	@DisplayName("the land-use change table has a header row plus one row per simulated year")
	void landEventCounterTable_hasOneRowPerSimulatedYear() throws Exception {
		new Listener();

		String[][] table = landEventCounter();

		assertNotNull(table, "The land-use change table should be built during initialisation");
		assertEquals(YEARS + 1, table.length,
				"Expected one header row plus one row per simulated year. A table one row short is bug B12: "
						+ "the final year silently loses its land-use change count.");
		assertEquals("year", table[0][0]);
		assertEquals("LU changed", table[0][1]);
	}

	@Test
	@DisplayName("every data row is labelled with its year, including the final one")
	void landEventCounterTable_labelsEveryYearIncludingTheLast() throws Exception {
		new Listener();

		String[][] table = landEventCounter();

		for (int row = 1; row < table.length; row++) {
			assertNotNull(table[row][0],
					"Row " + row + " has no year label - the labelling loop is stopping short (bug B12)");
			assertEquals(String.valueOf(START_YEAR + row - 1), table[row][0],
					"Row " + row + " is labelled with the wrong year");
		}

		assertEquals(String.valueOf(END_YEAR), table[table.length - 1][0],
				"The last row must be the last simulated year");
	}

	@Test
	@DisplayName("the table is sized like its sibling output tables")
	void landEventCounterTable_matchesTheSizingOfSiblingTables() throws Exception {
		new Listener();

		// Every other per-year table in the Listener is sized getSize() + 1.
		// B12 happened because this one alone was sized getSize(), so comparing
		// them against each other is the cheapest way to catch a repeat.
		assertEquals(Listener.compositionAftListener.length, landEventCounter().length,
				"The land-use change table should have the same number of rows as the AFT composition table");
	}

	/** The table is private, so the test reads it by name. */
	private static String[][] landEventCounter() throws Exception {
		Field field = Listener.class.getDeclaredField("landEventCounter");
		field.setAccessible(true);
		return (String[][]) field.get(null);
	}
}
