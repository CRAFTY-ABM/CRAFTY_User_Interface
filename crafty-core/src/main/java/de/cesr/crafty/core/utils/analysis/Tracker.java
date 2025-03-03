package de.cesr.crafty.core.utils.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.model.RegionClassifier;

public class Tracker {
	private static final CustomLogger LOGGER = new CustomLogger(Tracker.class);

	public static void trackSupply(int year) {
		if (ConfigLoader.config.track_changes) {
			long staetTime = System.currentTimeMillis();
			ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> container = new ConcurrentHashMap<>();
			AFTsLoader.getAftHash().values().forEach(a -> {
				ConcurrentHashMap<String, Double> tmp = new ConcurrentHashMap<>();
				container.put(a.getLabel(), tmp);
			});
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				c.getCurrentProductivity().forEach((s, v) -> {
					if (c.getOwner() != null)
						container.get(c.getOwner().getLabel()).merge(s, v, Double::sum);
				});
			});
			AFTsLoader.hashAgentNbr.forEach((label, a) -> {
				container.get(label).put("AggregateAFT", (double) a);
			});
			writeCSV(container,
					ConfigLoader.config.output_folder_name + File.separator + "SupplyTracker_" + year + ".csv");
			LOGGER.trace("Time taken for trackSupply " + (System.currentTimeMillis() - staetTime) + " ms");

		}
	}

	public static void trackSupply(int year, String regionName) {
		if (ConfigLoader.config.output_folder_name != null && ConfigLoader.config.track_changes) {
			ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> container = new ConcurrentHashMap<>();
			AFTsLoader.getAftHash().values().forEach(a -> {
				ConcurrentHashMap<String, Double> tmp = new ConcurrentHashMap<>();
				container.put(a.getLabel(), tmp);
			});
			RegionClassifier.regions.get(regionName).getCells().values().parallelStream().forEach(c -> {
				c.getCurrentProductivity().forEach((s, v) -> {
					if (c.getOwner() != null)
						container.get(c.getOwner().getLabel()).merge(s, v, Double::sum);
				});
			});
			AFTsLoader.hashAgentNbrRegions.get(regionName).forEach((label, a) -> {
				container.get(label).put("AggregateAFT", (double) a);
			});
			writeCSV(container, ConfigLoader.config.output_folder_name + File.separator + "region_" + regionName
					+ File.separator + "SupplyTracker_" + year + ".csv");
		}
	}

	public static void writeCSV(ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> container,
			String fileName) {
		LOGGER.info("writing CSV file:" + fileName);
		// Collect all possible column headers (keys from nested HashMaps)
		Set<String> columnHeaders = new TreeSet<>();
		for (ConcurrentHashMap<String, Double> nestedMap : container.values()) {
			columnHeaders.addAll(nestedMap.keySet());
		}

		try (FileWriter writer = new FileWriter(fileName)) {
			// Write column headers
			writer.append("ID");
			for (String header : columnHeaders) {
				writer.append(',').append(header);
			}
			writer.append('\n');

			// Write rows
			for (Map.Entry<String, ConcurrentHashMap<String, Double>> entry : container.entrySet()) {
				String key = entry.getKey();
				ConcurrentHashMap<String, Double> valuesMap = entry.getValue();

				writer.append(key);
				for (String header : columnHeaders) {
					writer.append(',');
					if (valuesMap.containsKey(header)) {
						writer.append(valuesMap.get(header).toString());
					} else {
						writer.append("0"); // or writer.append("") for empty value if that's preferred
					}
				}
				writer.append('\n');
			}
		} catch (IOException e) {
			System.err.println("Error writing the CSV file: " + e.getMessage());
		}
	}

}
