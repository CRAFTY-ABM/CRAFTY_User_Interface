package de.cesr.crafty.gui.utils.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.gui.utils.graphical.LineChartTools;
import de.cesr.crafty.gui.utils.graphical.MousePressed;
import de.cesr.crafty.gui.utils.graphical.NewWindow;
import de.cesr.crafty.gui.utils.graphical.SaveAs;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Pane;

public class CapitalsAnalyzer {

//	public static void main(String[] args) {
//		MainHeadless.modelInitialisation();
////		generateGrapheData();
//
//	}

	static ConcurrentHashMap<String, Double> mapToValues(int year) {
		ProjectLoader.setCurrentYear(year);
		ProjectLoader.cellsSet.updateCapitals(year);
		ConcurrentHashMap<String, Double> capiHash = new ConcurrentHashMap<>();

		CellsLoader.hashCell.values().forEach(c -> {
			c.getCapitals().forEach((cn, cv) -> {
				capiHash.merge(cn, cv, Double::sum);
			});
		});
		CellsLoader.getCapitalsList().forEach(cn -> {
			capiHash.put(cn, capiHash.get(cn) / CellsLoader.getNbrOfCells());
		});

		return capiHash;
	}

	static Map<String, ArrayList<Double>> generateGrapheData() {
		Map<String, ArrayList<Double>> hash = new ConcurrentHashMap<>();

		CellsLoader.getCapitalsList().forEach(nc -> {
			hash.put(nc, new ArrayList<Double>());
		});

		for (int i = ProjectLoader.getStartYear(); i < ProjectLoader.getEndtYear(); i++) {
			ConcurrentHashMap<String, Double> h = mapToValues(i);
			h.forEach((cn, cv) -> {
				hash.get(cn).add(cv);
			});
		}
		return hash;
	}

	public static void generateGrapheDataByScenarios() {
		Map<String, Map<String, ArrayList<Double>>> hash = new ConcurrentHashMap<>();
		ProjectLoader.getScenariosList().forEach(scenario -> {
			if (!scenario.equals("Baseline")) {
				ProjectLoader.setScenario(scenario);
				hash.put(scenario, generateGrapheData());
			}
		});

		CellsLoader.getCapitalsList().forEach(cn -> {
			Map<String, ArrayList<Double>> h = new ConcurrentHashMap<>();
			ProjectLoader.getScenariosList().forEach(scenario -> {
				if (!scenario.equals("Baseline")) {
					h.put(scenario, hash.get(scenario).get(cn));
				}
			});
			generateChart(h, cn);
		});
	}

	public static void generateChart(Map<String, ArrayList<Double>> data, String titel) {
		LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
		chart.setTitle(titel);
		lineChart(chart, data, titel);

		LineChartTools.configurexAxis(chart, ProjectLoader.getStartYear(), ProjectLoader.getEndtYear());
		double minY = getMinimumValue(data);
		double maxY = getMaximumValue(data);
		LineChartTools.configurexYxis(chart, minY, maxY);
		String ItemName = "Save as CSV";
		Consumer<String> action = x -> {
			SaveAs.exportLineChartDataToCSV(chart);
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle((Pane) chart.getParent(), chart, othersMenuItems);
		NewWindow win = new NewWindow();
		win.creatwindows("", chart);
	}

	public static void lineChart(LineChart<Number, Number> lineChart, Map<String, ArrayList<Double>> hash,
			String titel) {

		Series<Number, Number>[] series = new XYChart.Series[hash.size()];

		AtomicInteger i = new AtomicInteger();
		List<String> sortedKeys = new ArrayList<>(hash.keySet());
		Collections.sort(sortedKeys);
		for (String key : sortedKeys) {
			ArrayList<Double> value = hash.get(key);
			if (value != null) {
				series[i.get()] = new XYChart.Series<Number, Number>();
				series[i.get()].setName(key);
				lineChart.getData().add(series[i.get()]);
				i.getAndIncrement();
			}
		}

		AtomicInteger k = new AtomicInteger();
		sortedKeys.forEach((key) -> {
			ArrayList<Double> value = hash.get(key);
			for (int j = 0; j < value.size(); j++) {
				series[k.get()].getData().add(new XYChart.Data<>(j + ProjectLoader.getStartYear(), value.get(j)));
			}
			k.getAndIncrement();
		});

		MousePressed.mouseControle((Pane) lineChart.getParent(), lineChart, titel);

		LineChartTools.addSeriesTooltips(lineChart);
	}

	public static double getMinimumValue(Map<String, ArrayList<Double>> hash) {
		double min = Double.MAX_VALUE;

		for (Map.Entry<String, ArrayList<Double>> entry : hash.entrySet()) {
			for (double val : entry.getValue()) {
				if (val < min) {
					min = val;
				}
			}
		}
		return min;
	}

	public static double getMaximumValue(Map<String, ArrayList<Double>> hash) {
		double max = Double.MIN_VALUE;

		for (Map.Entry<String, ArrayList<Double>> entry : hash.entrySet()) {
			for (double val : entry.getValue()) {
				if (val > max) {
					max = val;
				}
			}
		}
		return max;
	}
}
