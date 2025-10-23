package de.cesr.crafty.core.utils.general;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.utils.analysis.CustomLogger;

public class Selector {

//	public static String[][] seedMap;

	private static final CustomLogger LOGGER = new CustomLogger(Selector.class);

	// Fast 64-bit mixer (SplitMix64). Good distribution, deterministic.
	static long mix64(long z) {
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}

	// Deterministic score per key using seed + key hash; ties break on key
	static long score(String key, long seed) {
		// String.hashCode() is cheap; mix64 fixes its weaknesses well enough
		return mix64(seed ^ key.hashCode());
	}

	// Holds a key with its score; comparable so tie-breaks are deterministic.
	static final class KeyScore {
		final String key;
		final long score;

		KeyScore(String key, long score) {
			this.key = key;
			this.score = score;
		}
	}

	/**
	 * Deterministic random subset: pick the N keys with the smallest
	 * score(seed,key). Complexity: O(M log N), Memory: O(N). Works great when
	 * percentage << 100%.
	 */
	public static ConcurrentHashMap<String, Cell> randomSeed(ConcurrentHashMap<String, Cell> cellsHash,
			double percentage, long seedID) {

		int size = cellsHash.size();
		if (size == 0)
			return new ConcurrentHashMap<>();

		double p = Math.max(0.0, Math.min(1.0, percentage));
		int n = (int) Math.round(size * p);
		if (n <= 0)
			return new ConcurrentHashMap<>();
		if (n >= size)
			return new ConcurrentHashMap<>(cellsHash);

		// Max-heap of the current N *smallest* scores so far.
		// Comparator puts the *largest* (worst) score at the top for easy eviction.
		PriorityQueue<KeyScore> heap = new PriorityQueue<>(n, (a, b) -> {
			int c = Long.compare(b.score, a.score); // reverse order: largest first
			return (c != 0) ? c : b.key.compareTo(a.key); // deterministic tie-break
		});

		// Snapshot keys once (avoids iteration races). Order of iteration doesn’t
		// affect determinism.

		for (String k : cellsHash.keySet()) {
			long s = score(k, seedID);
			if (heap.size() < n) {
				heap.offer(new KeyScore(k, s));
			} else if (s < heap.peek().score || (s == heap.peek().score && k.compareTo(heap.peek().key) < 0)) {
				heap.poll();
				heap.offer(new KeyScore(k, s));
			}
		}

		// Build the result
		ConcurrentHashMap<String, Cell> subset = new ConcurrentHashMap<>(Math.max(16, n * 2));
		for (KeyScore ks : heap) {
			subset.put(ks.key, cellsHash.get(ks.key));
		}
//		seedMap = new String[CellsLoader.hashCell.size() + 1][1];
//		seedMap[0][0] = "ID,X,Y,seed";
//		AtomicInteger i = new AtomicInteger(1);
//		CellsLoader.hashCell.values().forEach(c -> {
//			seedMap[i.getAndIncrement()][0] = c.getID() +","+ c.getX()+"," + c.getY()+"," + (subset.containsValue(c) ? "1" : "0");
//		});
		
		LOGGER.info("seedID =" + seedID + " percentage= " + percentage*100 + "%  seed size " + subset.size());
		return subset;
	}

}
