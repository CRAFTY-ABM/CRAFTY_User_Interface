package de.cesr.crafty.core.updaters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.utils.general.DeterministicRandom;

/**
 * Cleanup plan step 0.3 - these tests are brought back into service.
 *
 * WHY THEY WERE OFF
 * Every test in this file was commented out. The reason was not flakiness or
 * static-state coupling, as first assumed: the tests had simply been written
 * against an older version of SeedUpdater and were left behind when it
 * changed. In particular the tie-break used to be keyed on Cell.getCellID(),
 * whereas the current code derives it from the cell's x/y coordinates via
 * DeterministicRandom.stableCellId. Uncommenting them as they stood would not
 * have compiled or passed, so they have been rewritten against the code as it
 * is now.
 *
 * WHY THEY MATTER
 * SeedUpdater decides WHICH cells get to act each year. If its choice is not
 * reproducible then nothing downstream can be either, no matter how careful
 * the competition code is. These tests are the floor that the golden-run test
 * stands on.
 */
class SeedUpdaterTest {

	/**
	 * The comparator in SeedUpdater keys its tie-break on the cell's
	 * coordinates, so the mocks must supply x and y as well as utility.
	 */
	private static Cell cell(int x, int y, double utility) {
		Cell c = mock(Cell.class);
		when(c.getX()).thenReturn(x);
		when(c.getY()).thenReturn(y);
		when(c.getCurrentUtility()).thenReturn(utility);
		return c;
	}

	/** Identifies cells in assertions by coordinate, which is what the code sorts on. */
	private static List<String> coords(List<Cell> cells) {
		return cells.stream().map(c -> c.getX() + "," + c.getY()).collect(Collectors.toList());
	}

	@Test
	@DisplayName("same cells, same seed and year: the selection never varies")
	void bottomPercent_sameInputSameSeedSameYear_isStable() {
		long runSeed = 12345L;
		int year = 2030;

		List<Cell> cells = List.of(
				cell(0, 0, 5.0),
				cell(1, 0, 2.0),
				cell(2, 0, 3.0),
				cell(3, 0, 1.0),
				cell(4, 0, 4.0));

		List<String> expected = coords(SeedUpdater.bottomPercent(cells, 0.4, runSeed, year));

		for (int i = 0; i < 50; i++) {
			assertIterableEquals(expected, coords(SeedUpdater.bottomPercent(cells, 0.4, runSeed, year)),
					"Repeating the same selection gave a different answer on attempt " + i);
		}
	}

	@Test
	@DisplayName("the selection does not depend on the order the cells arrive in")
	void bottomPercent_isIndependentOfInputOrder() {
		long runSeed = 98765L;
		int year = 2040;

		List<Cell> cells = new ArrayList<>(List.of(
				cell(0, 0, 5.0),
				cell(1, 0, 2.0),
				cell(2, 0, 2.0),
				cell(3, 0, 1.0),
				cell(4, 0, 4.0),
				cell(5, 0, 3.0)));

		List<String> expected = coords(SeedUpdater.bottomPercent(cells, 0.5, runSeed, year));

		List<Cell> shuffled = new ArrayList<>(cells);
		Collections.shuffle(shuffled, new Random(42));

		assertIterableEquals(expected, coords(SeedUpdater.bottomPercent(shuffled, 0.5, runSeed, year)),
				"Shuffling the input changed which cells were selected");
	}

	@Test
	@DisplayName("cells with identical utility are separated by a reproducible tie-break")
	void bottomPercent_equalUtilities_usesDeterministicTieBreak() {
		long runSeed = 555L;
		int year = 2035;

		List<Cell> cells = List.of(
				cell(0, 0, 7.0),
				cell(1, 0, 7.0),
				cell(2, 0, 7.0),
				cell(3, 0, 7.0),
				cell(4, 0, 7.0));

		// Mirror of the production comparator: hashed tie-break on the cell's
		// stable coordinate id, then the id itself as a final fallback.
		List<String> expected = cells.stream()
				.sorted(Comparator
						.comparingLong((Cell c) -> DeterministicRandom.randomLong(
								runSeed,
								year,
								DeterministicRandom.Process.TIE_BREAK,
								DeterministicRandom.stableCellId(c.getX(), c.getY()),
								0L,
								0))
						.thenComparingLong(c -> DeterministicRandom.stableCellId(c.getX(), c.getY())))
				.limit(2)
				.map(c -> c.getX() + "," + c.getY())
				.collect(Collectors.toList());

		assertIterableEquals(expected, coords(SeedUpdater.bottomPercent(cells, 0.4, runSeed, year)));
	}

	@Test
	@DisplayName("a NaN utility is treated as the worst possible, not as a winner")
	void bottomPercent_nanUtilitiesAreTreatedAsWorst() {
		long runSeed = 222L;
		int year = 2032;

		List<Cell> cells = List.of(
				cell(0, 0, 1.0),
				cell(1, 0, 2.0),
				cell(2, 0, Double.NaN),
				cell(3, 0, 0.5));

		List<String> selected = coords(SeedUpdater.bottomPercent(cells, 0.25, runSeed, year));

		// Bottom 25% of 4 cells is 1 cell: the genuine lowest utility, 0.5.
		// A NaN must not be allowed to sort to the front.
		assertEquals(List.of("3,0"), selected);
	}

	@Test
	@DisplayName("the k lowest-utility cells are the ones chosen")
	void bottomPercent_selectsCorrectBottomK() {
		long runSeed = 444L;
		int year = 2045;

		List<Cell> cells = List.of(
				cell(0, 0, 9.0),
				cell(1, 0, 1.0),
				cell(2, 0, 7.0),
				cell(3, 0, 2.0),
				cell(4, 0, 3.0),
				cell(5, 0, 8.0));

		// Bottom 3 utilities are 1.0, 2.0, 3.0 - the cells at x = 1, 3 and 4.
		assertIterableEquals(List.of("1,0", "3,0", "4,0"),
				coords(SeedUpdater.bottomPercent(cells, 0.5, runSeed, year)));
	}

	@Test
	@DisplayName("the requested fraction is clamped to a sensible range")
	void bottomPercent_clampsThePercentage() {
		long runSeed = 1L;
		int year = 2000;

		List<Cell> cells = List.of(
				cell(0, 0, 1.0),
				cell(1, 0, 2.0),
				cell(2, 0, 3.0),
				cell(3, 0, 4.0));

		assertTrue(SeedUpdater.bottomPercent(cells, 0.0, runSeed, year).isEmpty(),
				"Asking for 0% should select nothing");
		assertTrue(SeedUpdater.bottomPercent(cells, -1.0, runSeed, year).isEmpty(),
				"A negative fraction should be clamped to 0%");
		assertEquals(4, SeedUpdater.bottomPercent(cells, 1.0, runSeed, year).size(),
				"Asking for 100% should select every cell");
		assertEquals(4, SeedUpdater.bottomPercent(cells, 5.0, runSeed, year).size(),
				"A fraction above 1 should be clamped to 100%");
		assertTrue(SeedUpdater.bottomPercent(List.of(), 0.5, runSeed, year).isEmpty(),
				"An empty input should give an empty selection");
	}

	@Test
	@DisplayName("changing the year changes which tied cells are picked")
	void bottomPercent_differentYearCanReorderTiedCells() {
		long runSeed = 99L;

		List<Cell> cells = new ArrayList<>();
		for (int x = 0; x < 12; x++) {
			cells.add(cell(x, 0, 5.0)); // all tied, so only the tie-break decides
		}

		List<String> year2000 = coords(SeedUpdater.bottomPercent(cells, 0.25, runSeed, 2000));
		List<String> year2001 = coords(SeedUpdater.bottomPercent(cells, 0.25, runSeed, 2001));

		// Each year must be self-consistent...
		assertIterableEquals(year2000, coords(SeedUpdater.bottomPercent(cells, 0.25, runSeed, 2000)));
		// ...but the year is part of the key, so tied cells rotate between years
		// rather than the same handful always being picked.
		assertTrue(!year2000.equals(year2001),
				"The year is part of the random key, so a different year should reshuffle tied cells");
	}
}
