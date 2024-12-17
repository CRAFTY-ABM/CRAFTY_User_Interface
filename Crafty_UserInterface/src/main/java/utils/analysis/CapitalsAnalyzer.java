package utils.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import dataLoader.AFTsLoader;
import dataLoader.CellsLoader;
import dataLoader.PathsLoader;
import fxmlControllers.SpatialDataController;
import fxmlControllers.TabPaneController;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Pane;
import main.MainHeadless;
import model.Cell;
import model.CellsSet;
import model.Manager;
import utils.graphicalTools.LineChartTools;
import utils.graphicalTools.MousePressed;
import utils.graphicalTools.NewWindow;
import utils.graphicalTools.Tools;

public class CapitalsAnalyzer {

	public static void main(String[] args) {
		MainHeadless.modelInitialisation();
//		generateGrapheData();

	}

	static ConcurrentHashMap<String, Double> mapToValues(int year) {
		PathsLoader.setCurrentYear(year);
		TabPaneController.cellsLoader.updateCapitals(year);
		ConcurrentHashMap<String, Double> capiHash = new ConcurrentHashMap<>();

		CellsLoader.hashCell.values().forEach(c -> {
			c.getCapitals().forEach((cn, cv) -> {
				capiHash.merge(cn, cv, Double::sum);
			});
		});
		return capiHash;
	}

	static Map<String, ArrayList<Double>> generateGrapheData() {
		Map<String, ArrayList<Double>> hash = new ConcurrentHashMap<>();

		CellsLoader.getCapitalsList().forEach(nc -> {
			hash.put(nc, new ArrayList<Double>());
		});

		for (int i = PathsLoader.getStartYear(); i < PathsLoader.getEndtYear(); i++) {
			ConcurrentHashMap<String, Double> h = mapToValues(i);
			h.forEach((cn, cv) -> {
				hash.get(cn).add(cv);
			});
		}
		return hash;
	}

	public static void generateGrapheDataByScenarios() {
		Map<String, Map<String, ArrayList<Double>>> hash = new ConcurrentHashMap<>();
		PathsLoader.getScenariosList().forEach(scenario -> {
			if (!scenario.equals("Baseline")) {
				PathsLoader.setScenario(scenario);
				hash.put(scenario, generateGrapheData());
			}
		});

		CellsLoader.getCapitalsList().forEach(cn -> {
			Map<String, ArrayList<Double>> h = new ConcurrentHashMap<>();
			PathsLoader.getScenariosList().forEach(scenario -> {
				if (!scenario.equals("Baseline")) {
					h.put(scenario, hash.get(scenario).get(cn));
				}
			});
			generateChart(h,cn);
		});
	}

	public static void generateChart(Map<String, ArrayList<Double>> data, String titel) {
		LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
		chart.setTitle(titel);
		lineChart(chart, data, titel);
		LineChartTools.configurexAxis(chart,PathsLoader.getStartYear(), PathsLoader.getEndtYear());
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
				series[k.get()].getData().add(new XYChart.Data<>(j+PathsLoader.getStartYear(), value.get(j)));
			}
			k.getAndIncrement();

		});

		MousePressed.mouseControle((Pane) lineChart.getParent(), lineChart, titel);

		LineChartTools.addSeriesTooltips(lineChart);
	}
}