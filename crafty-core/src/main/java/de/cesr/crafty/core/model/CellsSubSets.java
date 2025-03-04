package de.cesr.crafty.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.cesr.crafty.core.dataLoader.ProjectLoader;



public class CellsSubSets {

	public static void actionInNeighboorSameLabel(Cell c) {
		if (c.getOwner() != null) {
			AtomicInteger sum = new AtomicInteger();
			neighborhoodOnAction(c, vc -> {
				if (vc.owner != null)
					if (vc.owner.getLabel().equals(c.owner.getLabel())) {
						sum.getAndIncrement();
					}
			});
			c.owner.setGiveInMean(c.owner.getGiveInMean() + sum.get());

//			owner.productivityLevel.forEach((n, v) -> {
//				owner.productivityLevel.put(n, v +  (sum.get()));
//			});
		}
	}

	static void neighborhoodOnAction(Cell c, Consumer<Cell> action) {
		getMooreNeighborhood(c).forEach(c0 -> {
			action.accept(c0);
		});
	}

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
		for (int i = (c.x - 1); i <= c.x + 1; i++) {
			for (int j = (c.y - 1); j <= (c.y) + 1; j++) {
				if (ProjectLoader.cellsSet.getCell(i, j) != null) {
					neighborhood.add(ProjectLoader.cellsSet.getCell(i, j));
				}
			}
		}
		neighborhood.remove(ProjectLoader.cellsSet.getCell(c.x, c.y));

		return neighborhood;
	}

	static Collection<Aft> detectExtendedNeighboringAFTs(Cell c, int r) {
		Set<Aft> neighborhoodAFts = Collections.synchronizedSet(new HashSet<>());
		Set<Cell> neighborhoodCells = getExtendedMooreNeighborhood(c, r);
		neighborhoodCells.forEach(vc -> {
			if (vc.getOwner() != null && vc.getOwner().isInteract())
				neighborhoodAFts.add(vc.getOwner());
		});
		return neighborhoodAFts;
	}

	public static Set<Cell> getExtendedMooreNeighborhood(Cell c, int r) {
		Set<Cell> neighborhood = Collections.synchronizedSet(new HashSet<>());
		for (int i = c.x - r; i <= c.x + r; i++) {
			for (int j = c.y - r; j <= c.y + r; j++) {
				if (i == c.x && j == c.y) {
					continue;
				}
				Cell cell = ProjectLoader.cellsSet.getCell(i, j);
				if (cell != null) {
					neighborhood.add(cell);
				}
			}
		}
		return neighborhood;
	}

//	public static void selectZone(Cell patch, String zonetype) {
//		CellsLoader.hashCell.values().forEach(p -> {
//			if (p.getCurrentRegion().equals(zonetype)) {
//				if (p.getCurrentRegion().equals(patch.getCurrentRegion())) {
//					p.ColorP(p.color);
//					RegionController.patchsInRergion.add(p);
//				}
//			}
//		});
//		RegionController.patchsInRergion.forEach(p -> {
//			p.ColorP(Color.GREY);
//		});
//		CellsSet.gc.drawImage(CellsSet.writableImage, 0, 0);
//	}

}
