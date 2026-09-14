package de.cesr.crafty.core.crafty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cesr.crafty.core.cli.Config;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.output.Tracker;
import de.cesr.crafty.core.updaters.LandMaskUpdater;
import de.cesr.crafty.core.updaters.Timestep;

/**
 * Tests for twinned-AFT competition, which had no test cover of any kind.
 *
 * WHAT TWINNING IS
 * An AFT may nominate a "twin" - a closely related type it can switch to
 * cheaply, for example irrig/rainfed versions of the same crop & intensity.
 * Once per tick, cells whose owner has a twin get an extra chance to switch
 * to it, separately from ordinary competition.
 *
 * THE RULE BEING TESTED
 * Competitiveness.evaluateTwinCompetition applies one rule: switch when the
 * twin's utility is positive AND beats the current owner's utility by more
 * than the switching cost. The cost is the twin's Twin_cost when
 * use_twinned_cost is on, and zero when it is off.
 *
 * WORTH KNOWING WHEN READING THESE TESTS
 * That single rule is a recent simplification. Previously, when
 * use_twinned_cost was OFF, the twin decision went through the same give-in
 * threshold machinery as ordinary competition, so a twin had to beat the
 * owner by a behavioural margin rather than merely beat it. With the current
 * rule a twin switches on any improvement at all when the cost flag is off,
 * which makes twin switching considerably more eager. These tests describe
 * the rule as it now stands; if the give-in behaviour was meant to be kept,
 * twinSwitches_onAnyImprovement_whenCostIsDisabled is the test that should
 * fail and the one to change.
 *
 * WHY THESE TESTS MATTER
 * The twin path was refactored to evaluate a whole batch before applying any
 * of it, which is what makes the pass reproducible. That refactor relies on
 * evaluateTwinCompetition being free of side effects - it must decide without
 * touching the cell. evaluateDoesNotTouchTheCell below is the test that holds
 * that property in place.
 */
class TwinCompetitionTest {

	private static final String SERVICE = "S1";

	private Config configBackup;
	private Map<String, Aft> aftHashBackup;
	private Map<String, ConcurrentHashMap<String, Boolean>> restrictionsBackup;
	private List<String> servicesBackup;
	private int currentYearBackup;

	@BeforeEach
	void setUp() {
		configBackup = ConfigLoader.config;
		aftHashBackup = new LinkedHashMap<>(AFTsLoader.getAftHash());
		restrictionsBackup = new LinkedHashMap<>(LandMaskUpdater.restrictions);
		servicesBackup = new ArrayList<>(ServiceSet.getServicesList());
		currentYearBackup = Timestep.getCurrentYear();

		Config config = new Config();
		// Plain marginal-utility mode, so utility is simply
		// marginal[service] * competitiveness(aft, service).
		config.use_explicit_price_utility = false;
		config.use_price_only_utility = false;
		config.use_cell_level_taxes = false;
		config.use_twinned_afts = true;
		config.use_twinned_cost = false;
		config.logger_info = false;
		config.logger_warn = false;
		config.logger_trace = false;
		ConfigLoader.config = config;

		ServiceSet.getServicesList().clear();
		ServiceSet.getServicesList().add(SERVICE);

		AFTsLoader.getAftHash().clear();
		LandMaskUpdater.restrictions.clear();

		Timestep.setCurrentYear(2020);
		Listener.landUseChangeCounter.set(0);
		Listener.newAftsInLandNbr = new ConcurrentHashMap<>();
	}

	@AfterEach
	void tearDown() {
		ConfigLoader.config = configBackup;
		AFTsLoader.getAftHash().clear();
		AFTsLoader.getAftHash().putAll(aftHashBackup);
		LandMaskUpdater.restrictions.clear();
		LandMaskUpdater.restrictions.putAll(restrictionsBackup);
		ServiceSet.getServicesList().clear();
		ServiceSet.getServicesList().addAll(servicesBackup);
		Timestep.setCurrentYear(currentYearBackup);
	}

	// =================================================================
	// Cases where no switch should even be considered
	// =================================================================

	@Test
	@DisplayName("an owner with no twin is left alone")
	void noDecision_whenOwnerHasNoTwin() {
		Aft owner = interactingAft("Owner", null);
		Cell cell = cellOwnedBy(owner, 1.0);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	@Test
	@DisplayName("a cell with no owner at all is left alone")
	void noDecision_whenCellHasNoOwner() {
		Cell cell = mock(Cell.class);
		when(cell.getOwner()).thenReturn(null);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	@Test
	@DisplayName("a twin label that does not resolve to a known AFT is ignored")
	void noDecision_whenTwinLabelDoesNotResolve() {
		Aft owner = interactingAft("Owner", "GhostAft");
		register(owner); // the twin is deliberately never registered
		Cell cell = cellOwnedBy(owner, 1.0);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	@Test
	@DisplayName("a twin that is not an interacting AFT is ignored")
	void noDecision_whenTwinIsNotInteracting() {
		Aft owner = interactingAft("Owner", "Abandoned");
		Aft abandoned = new Aft("Abandoned"); // the built-in non-interacting manager
		register(owner, abandoned);
		Cell cell = cellOwnedBy(owner, 1.0);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	// =================================================================
	// The switching rule itself
	// =================================================================

	@Test
	@DisplayName("with cost disabled, the twin takes over on any improvement")
	void twinSwitches_onAnyImprovement_whenCostIsDisabled() {
		ConfigLoader.config.use_twinned_cost = false;

		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		// owner utility 5.0, twin utility 6.0 -> a modest improvement
		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(6.0).when(cell).competitiveness(twin, SERVICE);

		Competitiveness.CompetitionDecision decision =
				Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0));

		assertNotNull(decision, "A twin that beats the owner should take over when there is no switching cost");
		assertSame(twin, decision.newOwner());
		assertSame(cell, decision.cell());
	}

	@Test
	@DisplayName("a twin that does not beat the owner is refused")
	void noSwitch_whenTwinDoesNotBeatOwner() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		// owner 6.0 vs twin 5.0
		Cell cell = cellOwnedBy(owner, 6.0);
		doReturn(5.0).when(cell).competitiveness(twin, SERVICE);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	@Test
	@DisplayName("a twin with zero or negative utility never takes over, even against a worse owner")
	void noSwitch_whenTwinUtilityIsNotPositive() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		// The owner is doing badly (-5), but the twin is no better than zero,
		// so switching would be pointless.
		Cell cell = cellOwnedBy(owner, -5.0);
		doReturn(0.0).when(cell).competitiveness(twin, SERVICE);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)));
	}

	@Test
	@DisplayName("with cost enabled, a gain smaller than the switching cost is refused")
	void switchingCost_blocksAMarginalGain() {
		ConfigLoader.config.use_twinned_cost = true;

		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		twin.setTwinCost(3.0);
		register(owner, twin);

		// Gain is 6 - 5 = 1, which does not cover the cost of 3.
		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(6.0).when(cell).competitiveness(twin, SERVICE);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)),
				"A gain smaller than Twin_cost should not be worth switching for");
	}

	@Test
	@DisplayName("with cost enabled, a gain larger than the switching cost is accepted")
	void switchingCost_allowsAWorthwhileGain() {
		ConfigLoader.config.use_twinned_cost = true;

		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		twin.setTwinCost(3.0);
		register(owner, twin);

		// Gain is 10 - 5 = 5, comfortably above the cost of 3.
		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(10.0).when(cell).competitiveness(twin, SERVICE);

		Competitiveness.CompetitionDecision decision =
				Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0));

		assertNotNull(decision);
		assertSame(twin, decision.newOwner());
	}

	@Test
	@DisplayName("the same gain can be accepted or refused depending only on Twin_cost")
	void switchingCost_isWhatDecidesABorderlineCase() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(7.0).when(cell).competitiveness(twin, SERVICE); // gain of 2

		ConfigLoader.config.use_twinned_cost = true;
		twin.setTwinCost(1.0);
		assertNotNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)),
				"A gain of 2 should beat a cost of 1");

		twin.setTwinCost(2.5);
		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)),
				"The same gain of 2 should not beat a cost of 2.5");
	}

	// =================================================================
	// Constraints shared with ordinary competition
	// =================================================================

	@Test
	@DisplayName("a mask that forbids the transition also blocks a twin switch")
	void noSwitch_whenAMaskForbidsTheTransition() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(10.0).when(cell).competitiveness(twin, SERVICE); // would easily win
		when(cell.getMaskType()).thenReturn("Protected");

		ConcurrentHashMap<String, Boolean> mask = new ConcurrentHashMap<>();
		mask.put("Owner_Twin", false); // this transition is not allowed here
		LandMaskUpdater.restrictions.put("Protected", mask);

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)),
				"Protected land should not switch to a twin any more than to any other AFT");
	}

	@Test
	@DisplayName("an owner still inside its minimum life cycle cannot be replaced by its twin")
	void noSwitch_whileOwnerIsWithinItsMinimumLifeCycle() {
		Aft owner = interactingAft("Owner", "Twin");
		owner.setMin_life_cycle(5);
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(10.0).when(cell).competitiveness(twin, SERVICE); // would easily win
		when(cell.getOwnerLifeCounter()).thenReturn(2); // only 2 years in, needs 5

		assertNull(Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0)),
				"An owner part-way through its minimum life cycle should be left in place");
	}

	// =================================================================
	// The property the batching refactor depends on
	// =================================================================

	@Test
	@DisplayName("evaluating a twin switch does not change the cell")
	void evaluateDoesNotTouchTheCell() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);

		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(10.0).when(cell).competitiveness(twin, SERVICE);

		Competitiveness.CompetitionDecision decision =
				Competitiveness.evaluateTwinCompetition(cell, runnerWithMarginal(1.0));

		// The decision says the twin should take over...
		assertNotNull(decision);
		assertSame(twin, decision.newOwner());

		// ...but nothing about the cell has actually moved yet. The twinned
		// pass evaluates a whole batch before applying any of it, and that is
		// only safe while this method stays free of side effects.
		assertSame(owner, cell.getOwner(), "evaluate must not change the owner");
		assertEquals(5.0, cell.getCurrentUtility(), 1e-12, "evaluate must not change the utility");
		assertEquals(0, Listener.landUseChangeCounter.get(),
				"evaluate must not count a land-use change that has not happened yet");
	}

	@Test
	@DisplayName("applying the decision is what actually hands the cell to the twin")
	void applyingTheDecisionChangesTheOwner() {
		Aft owner = interactingAft("Owner", "Twin");
		Aft twin = interactingAft("Twin", "Owner");
		register(owner, twin);
		setUpSankeyData("Twin", 2020);

		Cell cell = cellOwnedBy(owner, 5.0);
		doReturn(10.0).when(cell).competitiveness(twin, SERVICE);

		// twinCompetition is the immediate "decide and apply" form.
		Competitiveness.twinCompetition(cell, runnerWithMarginal(1.0));

		assertSame(twin, cell.getOwner(), "the twin should now own the cell");
		assertEquals(1, Listener.landUseChangeCounter.get(), "the change should be counted exactly once");
	}

	// =================================================================
	// Fixture helpers
	// =================================================================

	private static Aft interactingAft(String label, String twinLabel) {
		Aft a = new Aft(label);
		a.setType(de.cesr.crafty.core.crafty.ManagerTypes.AFT);
		a.setCategory(new AftCategory("Uncategorized"));
		a.setTwinLabel(twinLabel);
		a.setMin_life_cycle(0);
		a.setMax_life_cycle(Integer.MAX_VALUE);
		a.getProductivityLevel().put(SERVICE, 1.0);
		a.getSensByService().put(SERVICE, new ConcurrentHashMap<>());
		return a;
	}

	private static void register(Aft... afts) {
		for (Aft a : afts) {
			AFTsLoader.getAftHash().put(a.getLabel(), a);
		}
	}

	/**
	 * A cell owned by the given AFT with a known current utility. It is a spy
	 * on a real Cell so that competitiveness(...) can be stubbed per AFT while
	 * everything else behaves normally.
	 */
	private static Cell cellOwnedBy(Aft owner, double currentUtility) {
		Cell cell = spy(new Cell(0, 0));
		cell.setOwner(owner);
		cell.setCurrentUtility(currentUtility);
		cell.setOwnerLifeCounter(10); // comfortably past any minimum life cycle
		return cell;
	}

	/** A runner whose only relevant behaviour is the marginal utility per service. */
	private static RegionalModelRunner runnerWithMarginal(double marginal) {
		RegionalModelRunner runner = mock(RegionalModelRunner.class);
		when(runner.getMarginal()).thenReturn(new ConcurrentHashMap<>(Map.of(SERVICE, marginal)));
		return runner;
	}

	/** takeOverAcell writes into the land-use transition tables, which must exist. */
	private static void setUpSankeyData(String newOwnerLabel, int year) {
		Map<Integer, Map<String, Integer>> byYear = new HashMap<>();
		byYear.put(year, new ConcurrentHashMap<>());
		Tracker.sankeydata.put(newOwnerLabel, byYear);
	}

	@SuppressWarnings("unused")
	private static void setStatic(Class<?> type, String field, Object value) throws Exception {
		Field f = type.getDeclaredField(field);
		f.setAccessible(true);
		f.set(null, value);
	}
}
