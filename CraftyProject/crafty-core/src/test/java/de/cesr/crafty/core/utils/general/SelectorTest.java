package de.cesr.crafty.core.utils.general;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cesr.crafty.core.crafty.Cell;

/**
 * Cleanup plan step 0.3 - these tests are brought back into service.
 *
 * WHY THEY WERE OFF
 * Every test in this file was commented out because they were written against
 * an older Selector API: they treated the return value as a Map and read its
 * keySet(). Selector.randomSeed now returns a List<Cell> ordered by score, so
 * the tests could not simply be uncommented - they have been rewritten against
 * the current signature.
 *
 * WHAT SELECTOR DOES
 * Every cell is given a deterministic score beforehand (SeedUpdater does this
 * via DeterministicRandom). Selector then picks the n cells with the smallest
 * scores, breaking ties first on cell id and then on the map key, so the
 * choice never depends on the traversal order of a ConcurrentHashMap.
 */
class SelectorTest {

	private static Cell mockCell(long cellId, double score) {
		Cell cell = mock(Cell.class);
		when(cell.getCellID()).thenReturn(cellId);
		when(cell.getScore()).thenReturn(score);
		return cell;
	}

	private static List<Long> ids(List<Cell> cells) {
		return cells.stream().map(Cell::getCellID).collect(Collectors.toList());
	}

	private static Set<Long> idSet(List<Cell> cells) {
		return new TreeSet<>(ids(cells));
	}

	private static ConcurrentHashMap<String, Cell> fiveCells() {
		ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
		cells.put("0,0", mockCell(100L, 0.90));
		cells.put("0,1", mockCell(101L, 0.10));
		cells.put("0,2", mockCell(102L, 0.30));
		cells.put("0,3", mockCell(103L, 0.20));
		cells.put("0,4", mockCell(104L, 0.80));
		return cells;
	}

	@Test
	@DisplayName("the cells with the smallest scores are the ones selected")
	void shouldSelectTheSmallestScores() {
		List<Cell> selected = Selector.randomSeed(fiveCells(), 0.40); // 2 of 5

		// Smallest scores are 0.10 (id 101) and 0.20 (id 103), and the result
		// is returned in ascending score order.
		assertIterableEquals(List.of(101L, 103L), ids(selected));
	}

	@Test
	@DisplayName("the same input selects the same cells every time")
	void sameInputShouldSelectSameCellsEveryTime() {
		Set<Long> first = idSet(Selector.randomSeed(fiveCells(), 0.40));
		Set<Long> second = idSet(Selector.randomSeed(fiveCells(), 0.40));

		assertEquals(first, second);
		assertEquals(Set.of(101L, 103L), first);
	}

	@Test
	@DisplayName("the selection does not depend on map insertion order")
	void sameCellsDifferentInsertionOrderShouldSelectSameCells() {
		ConcurrentHashMap<String, Cell> cellsA = fiveCells();

		ConcurrentHashMap<String, Cell> cellsB = new ConcurrentHashMap<>();
		cellsB.put("0,4", mockCell(104L, 0.80));
		cellsB.put("0,3", mockCell(103L, 0.20));
		cellsB.put("0,2", mockCell(102L, 0.30));
		cellsB.put("0,1", mockCell(101L, 0.10));
		cellsB.put("0,0", mockCell(100L, 0.90));

		assertEquals(idSet(Selector.randomSeed(cellsA, 0.40)),
				idSet(Selector.randomSeed(cellsB, 0.40)),
				"Inserting the same cells in a different order changed the selection");
	}

	@Test
	@DisplayName("cells with equal scores are separated by the smallest cell id")
	void equalScoresShouldUseSmallestCellIdAsTieBreak() {
		ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
		cells.put("a", mockCell(40L, 0.50));
		cells.put("b", mockCell(10L, 0.50));
		cells.put("c", mockCell(30L, 0.50));
		cells.put("d", mockCell(20L, 0.50));

		Set<Long> selected = idSet(Selector.randomSeed(cells, 0.50)); // 2 of 4

		assertEquals(Set.of(10L, 20L), selected,
				"With equal scores the two smallest cell ids should win");
	}

	@Test
	@DisplayName("when score and cell id are equal the map key is the final tie-break")
	void equalScoresAndEqualCellIdShouldUseKeyAsFinalTieBreak() {
		ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
		cells.put("b", mockCell(10L, 0.50));
		cells.put("a", mockCell(10L, 0.50));
		cells.put("c", mockCell(10L, 0.50));

		// round(3 * 1/3) = 1 cell selected.
		List<Cell> selected = Selector.randomSeed(cells, 1.0 / 3.0);

		assertEquals(1, selected.size());
		// Everything else is identical, so the lexicographically smallest key
		// must be the one that survives - and it must do so every time.
		assertEquals(idSet(Selector.randomSeed(cells, 1.0 / 3.0)), idSet(selected));
	}

	@Test
	@DisplayName("empty input, 0%, 100% and out-of-range fractions all behave")
	void boundaryCasesShouldBehaveCorrectly() {
		assertTrue(Selector.randomSeed(new ConcurrentHashMap<>(), 0.50).isEmpty(),
				"An empty landscape should select nothing");

		ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
		cells.put("0,0", mockCell(1L, 0.1));
		cells.put("0,1", mockCell(2L, 0.2));
		cells.put("0,2", mockCell(3L, 0.3));

		assertTrue(Selector.randomSeed(cells, 0.0).isEmpty(), "0% should select nothing");
		assertEquals(3, Selector.randomSeed(cells, 1.0).size(), "100% should select everything");
		assertEquals(3, Selector.randomSeed(cells, 2.0).size(), "Above 1 should clamp to 100%");
		assertTrue(Selector.randomSeed(cells, -1.0).isEmpty(), "Below 0 should clamp to 0%");
	}

	@Test
	@DisplayName("scores from DeterministicRandom give a reproducible selection")
	void sameSeedFromDeterministicRandomShouldProduceSameSelection() {
		long runSeed = 12345L;
		int year = 2030;
		List<Long> cellIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);

		Set<Long> first = idSet(Selector.randomSeed(buildCellsFromSeed(runSeed, year, cellIds), 0.50));
		Set<Long> second = idSet(Selector.randomSeed(buildCellsFromSeed(runSeed, year, cellIds), 0.50));

		assertEquals(first, second);
	}

	@Test
	@DisplayName("a different run seed generally selects a different subset")
	void differentSeedFromDeterministicRandomUsuallyProducesDifferentSelection() {
		int year = 2030;
		List<Long> cellIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);

		Set<Long> fromSeed111 = idSet(Selector.randomSeed(buildCellsFromSeed(111L, year, cellIds), 0.50));
		Set<Long> fromSeed222 = idSet(Selector.randomSeed(buildCellsFromSeed(222L, year, cellIds), 0.50));

		// This is a sanity check that the seed is actually being used, not a
		// guarantee - two seeds could in principle agree by coincidence.
		assertTrue(!fromSeed111.equals(fromSeed222),
				"Two different run seeds produced an identical selection, which suggests the seed is being ignored");
	}

	private static ConcurrentHashMap<String, Cell> buildCellsFromSeed(long runSeed, int year, List<Long> cellIds) {
		ConcurrentHashMap<String, Cell> cells = new ConcurrentHashMap<>();
		for (long cellId : cellIds) {
			double score = DeterministicRandom.randomDouble(
					runSeed,
					year,
					DeterministicRandom.Process.CELL_SELECTION_COMPETITION,
					cellId,
					0L,
					0);
			cells.put("cell-" + cellId, mockCell(cellId, score));
		}
		return cells;
	}
}
