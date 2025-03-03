package de.cesr.crafty.core.dataLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.cesr.crafty.core.model.Cell;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.utils.general.Utils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.AddCellToColumnException;
import tech.tablesaw.io.csv.CsvReadOptions;

public class ReaderFile {
	private static final CustomLogger LOGGER = new CustomLogger(ReaderFile.class);

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

	public static void processCSV(CellsLoader cells, Path filePath, String type) {
		LOGGER.info("Importing data for " + type + " from : " + filePath + "...");
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		ConcurrentHashMap<String, Integer> indexof = new ConcurrentHashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
			String[] line1 = br.readLine().split(",");
			for (int i = 0; i < line1.length; i++) {
				indexof.put(line1[i].toUpperCase(), i);
			}

			String line;
			while ((line = br.readLine()) != null) {
				final String data = line;

				executor.submit(() -> {

					switch (type) {
					case "Capitals":
						// System.out.println(type +"-->"+ data);
						associateCapitalsToCells(indexof, data);
						break;
					case "Services":
						associateOutPutServicesToCells(cells, indexof, data);
						break;
					case "Baseline":
						createCells(cells, indexof, data);
						break;

					}
				});
			}
		} catch (IOException e) {

			LOGGER.error(e.getMessage());
		} finally {
			executor.shutdown();
			try {
				// Wait for all tasks to finish
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				LOGGER.error(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

	static void associateCapitalsToCells(ConcurrentHashMap<String, Integer> indexof, String data) {
	    List<String> immutableList = Collections.unmodifiableList(Arrays.asList(data.split(",")));
	    int x = (int) Utils.sToD(immutableList.get(indexof.get("X")));
	    int y = (int) Utils.sToD(immutableList.get(indexof.get("Y")));
	    CellsLoader.getCapitalsList().forEach(capital_name -> {
	        if (indexof.get(capital_name.toUpperCase()) == null) {
	        	ProjectLoader.cellsSet.getCell(x, y).getCapitals().put(capital_name, 0.);
	        }else {
	        double capital_value = Utils.sToD(immutableList.get(indexof.get(capital_name.toUpperCase())));
	        ProjectLoader.cellsSet.getCell(x, y).getCapitals().put(capital_name, capital_value);}
	    });
	}

	static void createCells(CellsLoader cells, ConcurrentHashMap<String, Integer> indexof, String data) {
		List<String> immutableList = Collections.unmodifiableList(Arrays.asList(data.split(",")));
		int x = (int) Utils.sToD(immutableList.get(indexof.get("X")));
		int y = (int) Utils.sToD(immutableList.get(indexof.get("Y")));

		Cell c = new Cell(x, y);

		if (c != null) {
			// if(AFTsLoader.getAftHash().contains(immutableList.get(indexof.get("FR")))){}
			c.setOwner(AFTsLoader.getAftHash().get(immutableList.get(indexof.get("FR"))));

			CellsLoader.hashCell.put(x + "," + y, c);
			c.setID(CellsLoader.hashCell.size());
		}
		CellsLoader.getCapitalsList().forEach(capital_name -> {
			double capital_value = Utils.sToD(immutableList.get(indexof.get(capital_name.toUpperCase())));
			c.getCapitals().put(capital_name, capital_value);//
		});
	}

	static void associateOutPutServicesToCells(CellsLoader cells, ConcurrentHashMap<String, Integer> indexof,
			String data) {
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

}
