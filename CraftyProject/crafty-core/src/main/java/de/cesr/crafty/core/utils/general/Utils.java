package de.cesr.crafty.core.utils.general;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;

import de.cesr.crafty.core.crafty.Cell;

public class Utils {

//	public static double sToD(String str) {
//		if (str == null)
//			return 0;
//		try {
//			return Double.parseDouble(str);
//		} catch (NumberFormatException e) {
//			return 0;
//		}
//	}
	public static int indexof(String s, String[] tmp) {
		return Arrays.asList(tmp).indexOf(s);
	}

	public static double sToD(String s) {
		return (s == null || s.isEmpty()) || !NumberUtils.isParsable(s) ? 0d : JavaDoubleParser.parseDouble(s);
	}

	public static int sToI(String s) {
		return (int) sToD(s);
	}

	public static List<ConcurrentHashMap<String, Cell>> splitIntoSubsets(ConcurrentHashMap<String, Cell> cellsHash,
			int n) {
		// Create a list to hold the n subsets
		List<ConcurrentHashMap<String, Cell>> subsets = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			subsets.add(new ConcurrentHashMap<>());
		}

		// Distribute keys randomly across the n subsets
		cellsHash.keySet()/* */ .parallelStream().forEach(key -> {
			int subsetIndex = ThreadLocalRandom.current().nextInt(n);
			subsets.get(subsetIndex).put(key, cellsHash.get(key));
		});
		return subsets;
	}

	public static List<Map<String, Cell>> partitionMap(Map<String, Cell> originalMap, int numberOfPartitions) {
		List<Map<String, Cell>> partitions = new ArrayList<>();
		int size = originalMap.size() / numberOfPartitions;
		Iterator<Map.Entry<String, Cell>> iterator = originalMap.entrySet().iterator();
		for (int i = 0; i < numberOfPartitions; i++) {
			Map<String, Cell> part = new HashMap<>();
			for (int j = 0; j < size && iterator.hasNext(); j++) {
				Map.Entry<String, Cell> entry = iterator.next();
				part.put(entry.getKey(), entry.getValue());
			}
			partitions.add(part);
		}
		return partitions;
	}
	
	public static boolean checkDirectFiles(Path dir, String condition) {
		try (Stream<Path> stream = Files.list(dir)) {
			List<Path> list = stream.filter(Files::isRegularFile).collect(Collectors.toList());
			return list.stream().filter(p -> p.getFileName().toString().contains(condition)).findAny().isPresent();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
