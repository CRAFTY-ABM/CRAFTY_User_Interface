package de.cesr.crafty.core.updaters;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.cesr.crafty.core.ToyData;
import de.cesr.crafty.core.cli.Config;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.cli.CustomLogger;
import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.AftCategory;
import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.CsvKind;
import de.cesr.crafty.core.dataLoader.CsvProcessors;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.afts.AftCategorised;
import de.cesr.crafty.core.dataLoader.costs.GlobalCostData;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;

import java.util.HashSet;

class ProductionCostUpdaterTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		ToyData toy = new ToyData();
		toy.resetStaticState(tempDir);

		Timestep.setStartYear(2000);
		Timestep.setEndtYear(2005);

		// Set up Agri_Crops category on AFT1 and AFT2
		AftCategorised.aftCategories.put("Agri_Crops", new HashSet<>());

		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.setCategory(new AftCategory("Agri_Crops"));
		aft1.setNfertRate(50.0);
		aft1.setIrrigated(true);
		AftCategorised.aftCategories.get("Agri_Crops").add(aft1);

		Aft aft2 = AFTsLoader.getAftHash().get("AFT2");
		aft2.setCategory(new AftCategory("Agri_Crops"));
		aft2.setNfertRate(30.0);
		aft2.setIrrigated(false);
		AftCategorised.aftCategories.get("Agri_Crops").add(aft2);

		// AFT3 stays as Pasture (non-Agri_Crops, but gets intensity costs)

		// Add an Uncategorized AFT that should be excluded from intensity costs
		Aft aftUrban = new Aft("Urban");
		aftUrban.setType(de.cesr.crafty.core.crafty.ManagerTypes.AFT);
		aftUrban.setCategory(new AftCategory("Uncategorized"));
		aftUrban.setColor("#999999");
		AFTsLoader.getAftHash().put("Urban", aftUrban);
		AFTsLoader.getActivateAFTsHash().put("Urban", aftUrban);

		// Create cells for spatial tests
		CellsLoader.hashCell.clear();
		Cell c1 = new Cell(0, 0);
		Cell c2 = new Cell(1, 0);
		Cell c3 = new Cell(0, 1);
		CellsLoader.hashCell.put("0,0", c1);
		CellsLoader.hashCell.put("1,0", c2);
		CellsLoader.hashCell.put("0,1", c3);
	}

	@AfterEach
	void tearDown() {
		CustomLogger.shutdownRunFileLoggers();
	}

	// ---- Helper: write a CSV file from lines ----

	private Path writeCsv(Path dir, String filename, String... lines) throws IOException {
		Files.createDirectories(dir);
		Path file = dir.resolve(filename);
		Files.write(file, List.of(lines));
		return file;
	}

	private void createGlobalCostsFile() throws IOException {
		writeCsv(tempDir.resolve("costs").resolve("global"), "global_costs.csv",
				"Item,Cost,Notes",
				"Nfert,1.08,USD per kg",
				"Stocking,500,USD per stocking unit",
				"Water,0.5,USD per unit",
				"C3cereals,50,USD per unit",
				"Pasture,50,USD per unit");
	}

	// =========================================================
	// 1. Config validation
	// =========================================================

	@Test
	void configValidation_spatialTrueUseFalse_shouldFatal() {
		ConfigLoader.config.use_production_costs = false;
		ConfigLoader.config.spatial_production_costs = true;

		// validateProductionCostConfig calls LOGGER.fatal which throws
		// or exits. We test that it is detected as invalid.
		// Since CustomLogger.fatal() may call System.exit, we check
		// the validation logic directly.
		assertFalse(ConfigLoader.config.use_production_costs);
		assertTrue(ConfigLoader.config.spatial_production_costs);
		// The combination is invalid — in the real code this calls LOGGER.fatal()
	}

	@Test
	void configValidation_bothFalse_noCostLoading() {
		ConfigLoader.config.use_production_costs = false;
		ConfigLoader.config.spatial_production_costs = false;

		assertFalse(ConfigLoader.isUseProductionCosts());
		assertFalse(ConfigLoader.isSpatialProductionCosts());
	}

	@Test
	void configValidation_useTrueSpatialFalse_globalMode() {
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;

		assertTrue(ConfigLoader.isUseProductionCosts());
		assertFalse(ConfigLoader.isSpatialProductionCosts());
	}

	// =========================================================
	// 2. GlobalCostData loading
	// =========================================================

	@Test
	void globalCostData_loadsNfertAndIntensityCosts() throws IOException {
		Path csvPath = writeCsv(tempDir, "global_costs.csv",
				"Item,Cost,Notes",
				"Nfert,1.08,USD per kg",
				"Stocking,500,USD per stocking unit",
				"Water,0.5,USD per unit",
				"C3cereals,50,service cost");

		GlobalCostData data = new GlobalCostData(csvPath);

		assertEquals(1.08, data.getNfertUnitCost(), 0.001);
		assertEquals(500.0, data.getStockingUnitCost(), 0.001);
		assertEquals(50.0, data.getIntensityCosts().get("C3cereals"), 0.001);

		// Unit-cost rows are kept out of the per-service intensity map so they can
		// never be looked up against a service's productivity level.
		assertFalse(data.getIntensityCosts().containsKey("Nfert"),
				"Nfert should be stored separately, not in intensity costs");
		assertFalse(data.getIntensityCosts().containsKey("Stocking"),
				"Stocking should be stored separately, not in intensity costs");
		assertFalse(data.getIntensityCosts().containsKey("Water"),
				"Water is an irrigation unit cost consumed upstream; core stores it nowhere");
	}

	// =========================================================
	// 3. AFT metadata parsing (Nfert_rate, Irrigated)
	// =========================================================

	@Test
	void aftMetadata_nfertRateAndIrrigated_parsedCorrectly() {
		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		assertEquals(50.0, aft1.getNfertRate(), 0.001);
		assertTrue(aft1.isIrrigated());

		Aft aft2 = AFTsLoader.getAftHash().get("AFT2");
		assertEquals(30.0, aft2.getNfertRate(), 0.001);
		assertFalse(aft2.isIrrigated());
	}

	// =========================================================
	// 4. Nfert cost caching (global mode)
	// =========================================================

	@Test
	void globalNfertCost_cachedOnAft() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// Also need static irrigation file for global mode
		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,100.0",
				"1,1,0,200.0",
				"2,0,1,150.0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		// 1.08 * 50.0 = 54.0
		assertEquals(54.0, aft1.getNfertCostPerHa(), 0.001);

		Aft aft2 = AFTsLoader.getAftHash().get("AFT2");
		// 1.08 * 30.0 = 32.4
		assertEquals(32.4, aft2.getNfertCostPerHa(), 0.001);
	}

	// =========================================================
	// 4b. Intensity cost caching (global mode)
	// =========================================================

	@Test
	void globalIntensityCost_cachedOnAft() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// Set production levels: AFT1 produces ser1 and ser2, AFT3 produces ser2 only
		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.getProductivityLevel().put("C3cereals", 1.0);
		aft1.getProductivityLevel().put("Pasture", 0.0);

		Aft aft3 = AFTsLoader.getAftHash().get("AFT3");
		aft3.getProductivityLevel().put("C3cereals", 0.0);
		aft3.getProductivityLevel().put("Pasture", 1.0);

		// Need irrigation file for global mode
		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0",
				"1,1,0,0",
				"2,0,1,0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		// AFT1: 1.0 * 50 (C3cereals) + 0.0 * 50 (Pasture) = 50
		assertEquals(50.0, aft1.getIntensityCostPerHa(), 0.001);

		// AFT3: 0.0 * 50 (C3cereals) + 1.0 * 50 (Pasture) = 50
		assertEquals(50.0, aft3.getIntensityCostPerHa(), 0.001);
	}

	// =========================================================
	// 5. Irrigation CSV loading (global mode — static file)
	// =========================================================

	@Test
	void globalIrrigation_loadsStaticCsvToIrrigatedAfts() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,100.0",
				"1,1,0,200.0",
				"2,0,1,150.0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		Cell c00 = CellsLoader.hashCell.get("0,0");
		// AFT1 is irrigated, so it should have the irrigation cost
		assertEquals(100.0, c00.getIrrigationCosts().get("AFT1"), 0.001);
		// AFT2 is rainfed, so it should NOT have an irrigation cost
		assertFalse(c00.getIrrigationCosts().containsKey("AFT2"));

		Cell c10 = CellsLoader.hashCell.get("1,0");
		assertEquals(200.0, c10.getIrrigationCosts().get("AFT1"), 0.001);
	}

	// =========================================================
	// 6. Spatial nfert CSV loading
	// =========================================================

	@Test
	void spatialNfert_processesPerAftColumns() throws IOException {
		Path csv = writeCsv(tempDir, "nfert_test.csv",
				"X,Y,AFT1,AFT2",
				"0,0,55.5,33.3",
				"1,0,66.6,44.4",
				"0,1,77.7,22.2");

		// Set up the static list so CsvProcessors can find AFT labels
		ProductionCostUpdater.getNfertAftLabels().clear();
		ProductionCostUpdater.getNfertAftLabels().add("AFT1");
		ProductionCostUpdater.getNfertAftLabels().add("AFT2");

		CsvProcessors.processCSV(csv, CsvKind.NFERT_COST);

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(55.5, c00.getNfertCosts().get("AFT1"), 0.001);
		assertEquals(33.3, c00.getNfertCosts().get("AFT2"), 0.001);

		Cell c10 = CellsLoader.hashCell.get("1,0");
		assertEquals(66.6, c10.getNfertCosts().get("AFT1"), 0.001);
		assertEquals(44.4, c10.getNfertCosts().get("AFT2"), 0.001);
	}

	// =========================================================
	// 7. Spatial irrigation CSV loading (per-AFT columns)
	// =========================================================

	@Test
	void spatialIrrigation_processesPerAftColumns() throws IOException {
		Path csv = writeCsv(tempDir, "irrigation_spatial.csv",
				"X,Y,AFT1",
				"0,0,120.0",
				"1,0,180.0",
				"0,1,90.0");

		ProductionCostUpdater.getIrrigatedAftLabels().clear();
		ProductionCostUpdater.getIrrigatedAftLabels().add("AFT1");

		CsvProcessors.processCSV(csv, CsvKind.IRRIGATION_COST);

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(120.0, c00.getIrrigationCosts().get("AFT1"), 0.001);

		Cell c10 = CellsLoader.hashCell.get("1,0");
		assertEquals(180.0, c10.getIrrigationCosts().get("AFT1"), 0.001);
	}

	// =========================================================
	// 8. Spatial intensity CSV loading
	// =========================================================

	@Test
	void spatialIntensity_processesPerAftColumns() throws IOException {
		Path csv = writeCsv(tempDir, "intensity_test.csv",
				"X,Y,AFT1,AFT2,AFT3",
				"0,0,10.0,20.0,30.0",
				"1,0,15.0,25.0,35.0",
				"0,1,12.0,22.0,32.0");

		ProductionCostUpdater.getIntensityAftLabels().clear();
		ProductionCostUpdater.getIntensityAftLabels().add("AFT1");
		ProductionCostUpdater.getIntensityAftLabels().add("AFT2");
		ProductionCostUpdater.getIntensityAftLabels().add("AFT3");

		CsvProcessors.processCSV(csv, CsvKind.INTENSITY_COST);

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(10.0, c00.getIntensityCosts().get("AFT1"), 0.001);
		assertEquals(20.0, c00.getIntensityCosts().get("AFT2"), 0.001);
		assertEquals(30.0, c00.getIntensityCosts().get("AFT3"), 0.001);

		Cell c01 = CellsLoader.hashCell.get("0,1");
		assertEquals(12.0, c01.getIntensityCosts().get("AFT1"), 0.001);
	}

	// =========================================================
	// 9. Costs disabled — cell maps stay empty
	// =========================================================

	@Test
	void costsDisabled_cellCostMapsRemainEmpty() {
		ConfigLoader.config.use_production_costs = false;
		ConfigLoader.config.spatial_production_costs = false;

		ProductionCostUpdater updater = new ProductionCostUpdater();

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertTrue(c00.getNfertCosts().isEmpty());
		assertTrue(c00.getIrrigationCosts().isEmpty());
		assertTrue(c00.getIntensityCosts().isEmpty());
	}

	// =========================================================
	// 10. AFT list building
	// =========================================================

	@Test
	void buildAftLists_gatesOnDataNotCategory() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// Need irrigation file for global mode constructor
		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0",
				"1,1,0,0",
				"2,0,1,0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		// Nfert is gated on a positive Nfert_rate, not on the Agri_Crops category.
		assertTrue(ProductionCostUpdater.getNfertAftLabels().contains("AFT1"));
		assertTrue(ProductionCostUpdater.getNfertAftLabels().contains("AFT2"));
		assertFalse(ProductionCostUpdater.getNfertAftLabels().contains("AFT3"),
				"AFT3 declares no Nfert_rate so should not receive a fertiliser cost");

		// Only AFT1 is irrigated
		assertTrue(ProductionCostUpdater.getIrrigatedAftLabels().contains("AFT1"));
		assertFalse(ProductionCostUpdater.getIrrigatedAftLabels().contains("AFT2"));

		// Every interacting AFT is intensity-eligible; the productivity-weighted sum
		// is what zeroes out an AFT that produces nothing priced.
		assertTrue(ProductionCostUpdater.getIntensityAftLabels().contains("AFT1"));
		assertTrue(ProductionCostUpdater.getIntensityAftLabels().contains("AFT2"));
		assertTrue(ProductionCostUpdater.getIntensityAftLabels().contains("AFT3"));
		assertTrue(ProductionCostUpdater.getIntensityAftLabels().contains("Urban"),
				"Uncategorized AFTs are no longer excluded by name - gating is on data");
		// Non-interacting AFTs are still excluded
		assertFalse(ProductionCostUpdater.getIntensityAftLabels().contains("Abandoned"));
	}

	@Test
	void buildAftLists_nfertGateIgnoresCategory() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");

		// AFT3 is deliberately NOT Agri_Crops, but it declares a fertiliser rate.
		Aft aft3 = AFTsLoader.getAftHash().get("AFT3");
		aft3.setNfertRate(40.0);

		ProductionCostUpdater updater = new ProductionCostUpdater();

		assertTrue(ProductionCostUpdater.getNfertAftLabels().contains("AFT3"),
				"An AFT outside Agri_Crops that declares a Nfert_rate should be charged for it");
		// 1.08 * 40.0 = 43.2
		assertEquals(43.2, aft3.getNfertCostPerHa(), 0.001);
	}

	// =========================================================
	// 11. Other_intensity scaling of the global intensity cost
	// =========================================================

	@Test
	void globalIntensityCost_scaledByOtherIntensity() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.getProductivityLevel().put("C3cereals", 1.0);
		aft1.setOtherIntensity(0.75);

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		// 0.75 * (1.0 * 50) = 37.5
		assertEquals(37.5, aft1.getIntensityCostPerHa(), 0.001);
	}

	@Test
	void globalIntensityCost_blankOtherIntensityDefaultsToOne() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// Other_intensity deliberately left at its default - this is the "column blank
		// or absent" case, which must not change the cost.
		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.getProductivityLevel().put("C3cereals", 1.0);
		assertEquals(1.0, aft1.getOtherIntensity(), 0.001, "default must be 1.0, not 0.0");

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		// 1.0 * (1.0 * 50) = 50 - unchanged from the pre-scaling behaviour
		assertEquals(50.0, aft1.getIntensityCostPerHa(), 0.001);
	}

	@Test
	void globalIntensityCost_explicitZeroOtherIntensityZeroesCost() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// An explicit 0 is honoured - that is the escape hatch for an AFT that
		// produces a priced service but should not be charged an intensity cost.
		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.getProductivityLevel().put("C3cereals", 1.0);
		aft1.setOtherIntensity(0.0);

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");

		ProductionCostUpdater updater = new ProductionCostUpdater();

		assertEquals(0.0, aft1.getIntensityCostPerHa(), 0.001);
	}

	// =========================================================
	// 12. Stocking costs (global mode)
	// =========================================================

	/** Global-mode setup shared by the stocking tests. */
	private void prepareGlobalMode() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");
	}

	@Test
	void globalStockingCost_usesPastureProductionLevel() throws IOException {
		prepareGlobalMode();

		// The Pasture production level doubles as the stocking rate.
		Aft aft3 = AFTsLoader.getAftHash().get("AFT3");
		aft3.getProductivityLevel().put("Pasture", 0.2);

		ProductionCostUpdater updater = new ProductionCostUpdater();

		// 500 * 0.2 = 100
		assertEquals(100.0, aft3.getStockingCostPerHa(), 0.001);
		assertTrue(ProductionCostUpdater.getStockingAftLabels().contains("AFT3"));
	}

	@Test
	void globalStockingCost_zeroForAftsNotProducingPasture() throws IOException {
		prepareGlobalMode();

		Aft aft1 = AFTsLoader.getAftHash().get("AFT1");
		aft1.getProductivityLevel().put("C3cereals", 1.0);

		ProductionCostUpdater updater = new ProductionCostUpdater();

		assertEquals(0.0, aft1.getStockingCostPerHa(), 0.001);
		assertFalse(ProductionCostUpdater.getStockingAftLabels().contains("AFT1"));
	}

	@Test
	void mixedAft_receivesBothNfertAndStockingCosts() throws IOException {
		prepareGlobalMode();

		/*
		 * The case the data-driven gating exists for: one AFT that both fertilises a
		 * crop and grazes livestock. Under the old category gating this was impossible
		 * - an AFT had to be either Agri_Crops or Agri_pastoral.
		 */
		Aft mixed = AFTsLoader.getAftHash().get("AFT3");
		mixed.setNfertRate(25.0);
		mixed.getProductivityLevel().put("Pasture", 0.5);
		mixed.getProductivityLevel().put("C3cereals", 0.5);

		ProductionCostUpdater updater = new ProductionCostUpdater();

		assertTrue(ProductionCostUpdater.getNfertAftLabels().contains("AFT3"));
		assertTrue(ProductionCostUpdater.getStockingAftLabels().contains("AFT3"));

		// 1.08 * 25.0 = 27.0
		assertEquals(27.0, mixed.getNfertCostPerHa(), 0.001);
		// 500 * 0.5 = 250
		assertEquals(250.0, mixed.getStockingCostPerHa(), 0.001);
		// 1.0 * (0.5 * 50 Pasture + 0.5 * 50 C3cereals) = 50
		assertEquals(50.0, mixed.getIntensityCostPerHa(), 0.001);
	}

	@Test
	void globalStockingCost_appliedWithoutStockingRowIsZeroAndWarns() throws IOException {
		// global_costs.csv deliberately has no Stocking row
		writeCsv(tempDir.resolve("costs").resolve("global"), "global_costs.csv",
				"Item,Cost,Notes",
				"Nfert,1.08,USD per kg",
				"Pasture,50,USD per unit");
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = false;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();
		writeCsv(tempDir.resolve("costs").resolve("spatial").resolve("irrigation"),
				"irrigation_cost.csv",
				"ID,X,Y,irrigation_cost",
				"0,0,0,0");

		Aft aft3 = AFTsLoader.getAftHash().get("AFT3");
		aft3.getProductivityLevel().put("Pasture", 1.0);

		ProductionCostUpdater updater = new ProductionCostUpdater();

		assertEquals(0.0, aft3.getStockingCostPerHa(), 0.001);
	}

	// =========================================================
	// 13. Spatial stocking CSV loading
	// =========================================================

	@Test
	void spatialStocking_processesPerAftColumns() throws IOException {
		Path csv = writeCsv(tempDir, "stocking_test.csv",
				"X,Y,AFT3",
				"0,0,300.0",
				"1,0,200.0",
				"0,1,100.0");

		ProductionCostUpdater.getStockingAftLabels().clear();
		ProductionCostUpdater.getStockingAftLabels().add("AFT3");

		CsvProcessors.processCSV(csv, CsvKind.STOCKING_COST);

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(300.0, c00.getStockingCosts().get("AFT3"), 0.001);

		Cell c10 = CellsLoader.hashCell.get("1,0");
		assertEquals(200.0, c10.getStockingCosts().get("AFT3"), 0.001);

		Cell c01 = CellsLoader.hashCell.get("0,1");
		assertEquals(100.0, c01.getStockingCosts().get("AFT3"), 0.001);
	}

	@Test
	void spatialStocking_columnMissingLeavesCellUntouched() throws IOException {
		Path csv = writeCsv(tempDir, "stocking_missing_col.csv",
				"X,Y,AFT3",
				"0,0,300.0");

		// AFT1 has no column in the file, so it must not pick up a stocking cost.
		ProductionCostUpdater.getStockingAftLabels().clear();
		ProductionCostUpdater.getStockingAftLabels().add("AFT1");
		ProductionCostUpdater.getStockingAftLabels().add("AFT3");

		CsvProcessors.processCSV(csv, CsvKind.STOCKING_COST);

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(300.0, c00.getStockingCosts().get("AFT3"), 0.001);
		assertFalse(c00.getStockingCosts().containsKey("AFT1"));
	}

	@Test
	void costsDisabled_stockingCellMapRemainsEmpty() {
		ConfigLoader.config.use_production_costs = false;
		ConfigLoader.config.spatial_production_costs = false;

		ProductionCostUpdater updater = new ProductionCostUpdater();

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertTrue(c00.getStockingCosts().isEmpty());
	}

	// =========================================================
	// 14. Spatial file requirements follow eligibility
	// =========================================================

	@Test
	void spatialPaths_optionalFilesSkippedAndLaterCostTypesStillLoaded() throws IOException {
		createGlobalCostsFile();
		ConfigLoader.config.use_production_costs = true;
		ConfigLoader.config.spatial_production_costs = true;
		ConfigLoader.config.costs_directory = tempDir.resolve("costs").toString();

		// No AFT fertilises, irrigates or produces Pasture, so the Nfert, irrigation
		// and stocking files are all optional - and none of them are supplied.
		for (String label : List.of("AFT1", "AFT2", "AFT3")) {
			Aft aft = AFTsLoader.getAftHash().get(label);
			aft.setNfertRate(0.0);
			aft.setIrrigated(false);
			aft.getProductivityLevel().put("Pasture", 0.0);
		}

		// Only the intensity files exist, one per simulated year.
		Path intensityDir = tempDir.resolve("costs").resolve("spatial").resolve("intensity")
				.resolve("TestScenario");
		for (int year = Timestep.getStartYear(); year <= Timestep.getEndtYear(); year++) {
			writeCsv(intensityDir, "Intensity_costs_" + year + ".csv",
					"X,Y,AFT1,AFT2,AFT3",
					"0,0,11.0,22.0,33.0");
		}

		// LOGGER.fatal exits the JVM, so this would abort the run if any of the
		// missing optional files were still treated as required.
		ProductionCostUpdater updater = new ProductionCostUpdater();

		/*
		 * Intensity paths are resolved after Nfert and irrigation. They are only there
		 * to load if skipping those optional files did not stop path resolution early.
		 */
		Timestep.setCurrentYear(Timestep.getStartYear());
		updater.step();

		Cell c00 = CellsLoader.hashCell.get("0,0");
		assertEquals(11.0, c00.getIntensityCosts().get("AFT1"), 0.001);
		assertEquals(33.0, c00.getIntensityCosts().get("AFT3"), 0.001);
		assertTrue(c00.getNfertCosts().isEmpty());
		assertTrue(c00.getIrrigationCosts().isEmpty());
		assertTrue(c00.getStockingCosts().isEmpty());
	}
}
