package de.cesr.crafty.core.utils.general;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.cesr.crafty.core.crafty.Aft;
import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;

public class CellsSubSets {

	static Collection<Aft> detectNeighboringAFTs(Cell c) {
		Set<Aft> neighborhoodAFts = Collections.synchronizedSet(new HashSet<>());
		Set<Cell> neighborhoodCells = getMooreNeighborhood(c);
		neighborhoodCells.forEach(vc -> {
			if (vc.getOwner() != null && vc.getOwner().isInteract())
				neighborhoodAFts.add(vc.getOwner());
		});
		return neighborhoodAFts;
	}

	static Set<Cell> getMooreNeighborhood(Cell c) {
		Set<Cell> neighborhood = Collections.synchronizedSet(new HashSet<>());
		for (int i = (c.getX() - 1); i <= c.getX() + 1; i++) {
			for (int j = (c.getY() - 1); j <= (c.getY()) + 1; j++) {
				if (CellsLoader.getCell(i, j) != null) {
					neighborhood.add(CellsLoader.getCell(i, j));
				}
			}
		}
		neighborhood.remove(CellsLoader.getCell(c.getX(), c.getY()));

		return neighborhood;
	}

	public static Collection<Aft> detectExtendedNeighboringAFTs(Cell c, int raduis) {
		Set<Aft> neighborhoodAFts = Collections.synchronizedSet(new HashSet<>());
		Set<Cell> neighborhoodCells = getExtendedMooreNeighborhood(c, raduis);
		neighborhoodCells.forEach(vc -> {
			if (vc.getOwner() != null && vc.getOwner().isInteract())
				neighborhoodAFts.add(vc.getOwner());
		});
		return neighborhoodAFts;
	}

	public static Set<Cell> getExtendedMooreNeighborhood(Cell c, int r) {
		Set<Cell> neighborhood = Collections.synchronizedSet(new HashSet<>());
		for (int i = c.getX() - r; i <= c.getX() + r; i++) {
			for (int j = c.getY() - r; j <= c.getY() + r; j++) {
				if (i == c.getX() && j == c.getY()) {
					continue;
				}
				Cell cell = CellsLoader.getCell(i, j);
				if (cell != null) {
					neighborhood.add(cell);
				}
			}
		}
		return neighborhood;
	}

//	public static ConcurrentHashMap<String, Cell> randomSeed(ConcurrentHashMap<String, Cell> cellsHash,
//			double percentage) {
//
//		int numberOfElementsToSelect = (int) (cellsHash.size() * (percentage));
//
//		// Use parallel stream for better performance on large maps
//		List<String> keys = new ArrayList<>(cellsHash.keySet());
//		ConcurrentHashMap<String, Cell> randomSubset = new ConcurrentHashMap<>();
//
//		Collections.shuffle(keys, new Random()); // Shuffling the keys for randomness
//		keys.stream()/* .parallelStream() */.unordered() // This improve performance by eliminating the need for
//															// maintaining order
//				.limit(numberOfElementsToSelect).forEach(key -> randomSubset.put(key, cellsHash.get(key)));
//		return randomSubset;
//	}
}
