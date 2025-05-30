package de.cesr.crafty.core.dataLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.cesr.crafty.core.crafty.Cell;
import de.cesr.crafty.core.dataLoader.afts.AFTsLoader;
import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.dataLoader.serivces.ServiceSet;
import de.cesr.crafty.core.modelRunner.ModelRunner;
import de.cesr.crafty.core.updaters.CapitalUpdater;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.general.Utils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.AddCellToColumnException;
import tech.tablesaw.io.csv.CsvReadOptions;

public class CsvProcessors {
	private static final CustomLogger LOGGER = new CustomLogger(CsvProcessors.class);

	public static HashMap<String, ArrayList<Double>> ReadAsaHashDouble(Path filePath) {
		HashMap<String, ArrayList<String>> str = ReadAsaHash(filePath);
		HashMap<String, ArrayList<Double>> dou = new HashMap<>();
		str.forEach((k, list) -> {
			ArrayList<Double> tmp = new ArrayList<>();
			list.forEach(s -> {
				tmp.add(Utils.sToD(s));
			});
			dou.put(k, tmp);
		});
		return dou;
	}

	public static HashMap<String, ArrayList<String>> ReadAsaHash(Path filePath) {
		return ReadAsaHash(filePath, false);
	}

	public static HashMap<String, ArrayList<String>> ReadAsaHash(Path filePath, boolean ignoreIfFileNotExists) {
		LOGGER.info("Reading : " + filePath);
		HashMap<String, ArrayList<String>> hash = new HashMap<>();
		Table T = null;
		try {
			CsvReadOptions options = CsvReadOptions.builder(filePath.toFile()).separator(',').build();
			T = Table.read().usingOptions(options);
			LOGGER.trace(T.print());
		} catch (AddCellToColumnException s) {

			LOGGER.error(s.getMessage());
			/* correctAddCellToColumnException(T, filePath, s); */} catch (Exception e) {
			if (ignoreIfFileNotExists) {
				LOGGER.error(e.getMessage() + " \n     return null");
				return null;
			} else {
				e.printStackTrace();
				LOGGER.error(e.getMessage() + " \n     return null");
				// filePath = WarningWindowes.alterErrorNotFileFound("The file path could not be
				// found:", filePath);
				// T = Table.read().csv(filePath);
			}
		}
		List<String> columnNames = T.columnNames();

		for (Iterator<String> iterator = columnNames.iterator(); iterator.hasNext();) {
			String name = String.valueOf(iterator.next());
			ArrayList<String> tmp = new ArrayList<String>();
			for (int i = 0; i < T.column(name).size(); i++) {
				Object s = T.column(name).get(i);
				if (s instanceof Double) {
					tmp.add(String.valueOf(s));
				} else {
					tmp.add(T.column(name).getString(i));
				}
			}
			hash.put(name, tmp);
		}

		return hash;
	}

//	public static void processCSV(CellsLoader cells, Path filePath, String type) {
//		LOGGER.info("Importing data for " + type + " from : " + filePath + "...");
//		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//		ConcurrentHashMap<String, Integer> indexof = new ConcurrentHashMap<>();
//		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
//			String[] line1 = br.readLine().split(",");
//			for (int i = 0; i < line1.length; i++) {
//				indexof.put(line1[i].toUpperCase(), i);
//			}
//			String line;
//			while ((line = br.readLine()) != null) {
//				final String data = line;
//
//				executor.submit(() -> {
//
//					switch (type) {
//					case "Capitals":
//						associateCapitalsToCells(indexof, data);
//						break;
//					case "Services":
//						associateOutPutServicesToCells(cells, indexof, data);
//						break;
//					case "Baseline":
//						createCells(cells, indexof, data);
//						break;
//					}
//				});
//			}
//		} catch (IOException e) {
//
//			LOGGER.error(e.getMessage());
//		} finally {
//			executor.shutdown();
//			try {
//				// Wait for all tasks to finish
//				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
//					executor.shutdownNow();
//				}
//			} catch (InterruptedException e) {
//				executor.shutdownNow();
//				LOGGER.error(e.getMessage());
//				Thread.currentThread().interrupt();
//			}
//		}
//	}

	public static void processCSV(Path file, CsvKind kind) {
		LOGGER.info("Importing data for " + kind + " from : " + file + "...");
		try (Stream<String> lines = Files.lines(file)) {
			Iterator<String> it = lines.iterator();
			Map<String, Integer> index = buildIndex(it.next());

			/* the remaining lines are processed in parallel */
			StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), true) // true = parallel
					.forEach(line -> kind.apply(line, index));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Map<String, Integer> buildIndex(String headerLine) {
		String[] cols = headerLine.split(",");
		Map<String, Integer> idx = new HashMap<>(cols.length);

		for (int i = 0; i < cols.length; i++) {
			// trim in case the file has “X, Y ,Z” with spaces
			idx.put(cols[i].trim().toUpperCase(), i);
		}
		/*
		 * The parsing thread is the only writer; thereafter the map is read-only, so we
		 * wrap it to make the intention explicit.
		 */
		return Collections.unmodifiableMap(idx);
	}

//	public static void processCSV(CellsLoader cells, Path filePath, String type) {// not multithred
//	    LOGGER.info("Importing data for " + type + " from : " + filePath + "...");
//
//	    ConcurrentHashMap<String, Integer> indexof = new ConcurrentHashMap<>();
//
//	    try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
//	        // Read the first line (column headers)
//	        String[] line1 = br.readLine().split(",");
//	        for (int i = 0; i < line1.length; i++) {
//	            indexof.put(line1[i].toUpperCase(), i);
//	        }
//
//	        // Read each subsequent line and process directly
//	        String line;
//	        while ((line = br.readLine()) != null) {
//	            // No executor; just do the switch logic on the current thread
//	            switch (type) {
//	                case "Capitals":
//	                    associateCapitalsToCells(indexof, line);
//	                    break;
//	                case "Services":
//	                    associateOutPutServicesToCells(cells, indexof, line);
//	                    break;
//	                case "Baseline":
//	                    createCells(cells, indexof, line);
//	                    break;
//	            }
//	        }
//	    } catch (IOException e) {
//	        LOGGER.error(e.getMessage());
//	    }
//	}

	static void associateCapitalsToCells(Map<String, Integer> indexof, String data) {
		List<String> immutableList = Collections.unmodifiableList(Arrays.asList(data.split(",")));
		int x = (int) Utils.sToD(immutableList.get(indexof.get("X")));
		int y = (int) Utils.sToD(immutableList.get(indexof.get("Y")));
		CapitalUpdater.getCapitalsList().forEach(capital_name -> {
			if (indexof.get(capital_name.toUpperCase()) == null) {
				ModelRunner.cellsSet.getCell(x, y).getCapitals().put(capital_name, 0.);
			} else {
				double capital_value = Utils.sToD(immutableList.get(indexof.get(capital_name.toUpperCase())));
				ModelRunner.cellsSet.getCell(x, y).getCapitals().put(capital_name, capital_value);
			}
		});
	}

	static void createCells( Map<String, Integer> indexof, String data) {
		List<String> immutableList = Collections.unmodifiableList(Arrays.asList(data.split(",")));
		int x = (int) Utils.sToD(immutableList.get(indexof.get("X")));
		int y = (int) Utils.sToD(immutableList.get(indexof.get("Y")));

		Cell c = new Cell(x, y);

		if (c != null) {
			// if(AFTsLoader.getAftHash().contains(immutableList.get(indexof.get("FR")))){}
			c.setOwner(AFTsLoader.getAftHash().get(immutableList.get(indexof.get("FR"))));

			CellsLoader.hashCell.put(x + "," + y, c);
			c.setID(immutableList.get(indexof.get("ID")));
		}
//		CellsLoader.getCapitalsList().forEach(capital_name -> {
//			double capital_value = Utils.sToD(immutableList.get(indexof.get(capital_name.toUpperCase())));
//			c.getCapitals().put(capital_name, capital_value);//
//		});
	}

	static void associateOutPutServicesToCells(Map<String, Integer> indexof, String data) {
		List<String> immutableList = Collections.unmodifiableList(Arrays.asList(data.split(",")));
		int x = (int) Utils.sToD(immutableList.get(indexof.get("X")));
		int y = (int) Utils.sToD(immutableList.get(indexof.get("Y")));
		String aft_name = immutableList.get(indexof.get("AGENT"));

		Cell c = CellsLoader.hashCell.get(x + "," + y);

		c.setOwner(AFTsLoader.getAftHash().get(aft_name));
		c.getCurrentProductivity().clear();
		ServiceSet.getServicesList().forEach(service_name -> {
			double service_value = Utils.sToD(immutableList.get(indexof.get(service_name.toUpperCase())));
			c.getCurrentProductivity().put(service_name, service_value);
		});
	}

	public static HashMap<String, Double> readCsvToMatrixMap(Path csvFilePath) {
		// Read the CSV with headers enabled
		CsvReadOptions options = CsvReadOptions.builder(csvFilePath.toFile()).header(true).separator(',').build();
		Table table = null;
		try {
			table = Table.read().usingOptions(options);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String> columnNames = table.columnNames();

		HashMap<String, Double> matrixMap = new HashMap<>();

		for (int rowIndex = 0; rowIndex < table.rowCount(); rowIndex++) {
			String rowLabel = table.getString(rowIndex, 0);

			for (int colIndex = 1; colIndex < table.columnCount(); colIndex++) {
				String colName = columnNames.get(colIndex);
				String cellValue = table.getString(rowIndex, colIndex);
				String key = rowLabel + "|" + colName;
				matrixMap.put(key, Utils.sToD(cellValue));
			}
		}

		return matrixMap;
	}

}
