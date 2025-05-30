package de.cesr.crafty.core.utils.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.general.Utils;

public class CsvTools {
	private static final CustomLogger LOGGER = new CustomLogger(CsvTools.class);

	/**
	 * @author Mohamed Byari
	 *
	 */

	public static String[][] csvReader(Path filePath) {
		LOGGER.info("Read as a table file: " + filePath);
		List<String[]> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(","); // Assumes CSV uses comma as delimiter
				lines.add(values);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[][] array = new String[lines.size()][];
		for (int i = 0; i < lines.size(); i++) {
			array[i] = lines.get(i);
		}
		return array;
	}

	public static void writeCSVfile(String[][] tabl, Path filePath) {
		LOGGER.info("writing CSV file: " + filePath);
		File file = filePath.toFile();
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
			BufferedWriter bw = new BufferedWriter(fw);

			for (int i = 0; i < tabl.length; i++) {
				for (int j = 0; j < tabl[0].length - 1; j++) {
					bw.write(tabl[i][j] != null ? tabl[i][j] + "," : ",");
				}
				bw.write(tabl[i][tabl[0].length - 1] != null ? tabl[i][tabl[0].length - 1] : "");
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
		}
	}

	public static List<File> detectFiles(Path folderPath) {
		List<File> filePaths = new ArrayList<>();
		File folder = folderPath.toFile();
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("Input path is not a directory.");
		}
		File[] files = folder.listFiles();
		if (files == null) {
			throw new RuntimeException("Error occurred while retrieving files.");
		}
		for (File file : files) {
			if (file.isFile()) {
				filePaths.add(file);
			}
		}
		return filePaths;
	}

	public static void exportToCSV(String filePath) {
		LOGGER.info("Processing data to write a csv file...");
		List<String> serviceImmutableList = Collections.unmodifiableList(ServiceSet.getServicesList());
		// Process the cells in parallel to transform each Cell into a CSV string
		Set<String> csvLines = CellsLoader.hashCell.values().stream()/* .parallelStream() */ .map(c -> {
			String servicesFlattened = flattenHashMap(c, serviceImmutableList);

			return String.join(",", String.valueOf(c.getID()), String.valueOf(c.getX()), String.valueOf(c.getY()),
					c.getOwner() != null ? c.getOwner().getLabel() : "null", String.valueOf(c.getUtilityValue()),
					servicesFlattened);
		}).collect(Collectors.toSet());

		LOGGER.info("Writing processed lines to the CSV file : " + filePath);
		// Write the processed lines to the CSV file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write("ID,X,Y,Agent,Utility," + String.join(",", serviceImmutableList) + "\n"); // CSV header
			for (String line : csvLines) {
				writer.write(line + "\n");
			}

		} catch (IOException e) {
			LOGGER.error("Unable to export file: " + filePath + "\n" + e.getMessage());
		}
	}

	private static String flattenHashMap(Cell c, List<String> serviceImmutableList) {
		List<String> service = Collections.synchronizedList(new ArrayList<>());
		serviceImmutableList.forEach(ServiceName -> {
			if (c.getCurrentProductivity().get(ServiceName) != null) {
				service.add(String.valueOf(c.getCurrentProductivity().get(ServiceName)));
			} else {
				service.add("0");
			}
		});

		return String.join(",", service);
	}

	public static List<List<String>> readCsvFile(Path csvPath) {
		List<List<String>> rows = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(csvPath)) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(",");
				rows.add(Arrays.asList(columns));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rows;
	}

	public static List<List<String>> readCsvFileWithoutZeros(List<List<String>> data) {
		List<List<String>> rows = new ArrayList<>();
		rows.add(data.get(0));
		// delete list ==0;
		// delet row=0;
		data.forEach(list -> {
			boolean isnull = true;
			for (String str : list) {
				if (Utils.sToD(str) != 0) {
					isnull = false;
					break;
				}
			}
			if (!isnull) {
				rows.add(list);
			}
		});

		List<List<String>> ret = new ArrayList<>();
		ret.add(new ArrayList<>());
		for (int i = 0; i < rows.size(); i++) {
			ret.get(0).add(rows.get(i).get(0));
		}
		for (int i = 1; i < rows.iterator().next().size(); i++) {
			boolean isnull = true;
			for (List<String> list : rows) {
				if (Utils.sToD(list.get(i)) != 0) {
					isnull = false;
					break;
				}
			}
			if (!isnull) {
				ret.add(new ArrayList<>());
				for (List<String> list : rows) {
					ret.get(ret.size() - 1).add(list.get(i));
				}
			}
		}
		ret.get(0).set(0, "Service/Capital");
		return ret;
	}
	
	 public static void writeCSVfile(Map<String, ArrayList<Double>> dataInput, Path filePathOutput){
	        // Extract headers in insertion order
	        List<String> headers = new ArrayList<>(dataInput.keySet());

	        // Determine the maximum number of rows
	        int maxRows = 0;
	        for (ArrayList<Double> column : dataInput.values()) {
	            if (column.size() > maxRows) {
	                maxRows = column.size();
	            }
	        }

	        // Open writer in try-with-resources to ensure closure
	        try (BufferedWriter writer = Files.newBufferedWriter(filePathOutput)) {
	            // Write header line
	            writer.write(String.join(",", headers));
	            writer.newLine();

	            // Write each row
	            for (int row = 0; row < maxRows; row++) {
	                for (int col = 0; col < headers.size(); col++) {
	                    String header = headers.get(col);
	                    ArrayList<Double> columnData = dataInput.get(header);
	                    String cell = "";
	                    if (row < columnData.size()) {
	                        cell = columnData.get(row).toString();
	                    }
	                    writer.write(cell);
	                    if (col < headers.size() - 1) {
	                        writer.write(",");
	                    }
	                }
	                writer.newLine();
	            }
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	
	
	

}
