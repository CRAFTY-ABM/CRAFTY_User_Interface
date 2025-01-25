package de.cesr.crafty.controller.fxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.DemandModel;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.model.RegionClassifier;
import de.cesr.crafty.utils.file.SaveAs;
import de.cesr.crafty.utils.graphical.LineChartTools;
import de.cesr.crafty.utils.graphical.MousePressed;
import de.cesr.crafty.utils.graphical.PieChartTools;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SpatialDataController {

	public static RadioButton[] radioColor;
	@FXML
	private VBox vboxForSliderColors;
	@FXML
	private BarChart<String, Number> histogramCapitals;
	@FXML
	private PieChart pieChartColor;
	@FXML
	private LineChart<Number, Number> demandsChart;

	private boolean isNotInitialsation = false;

	private static SpatialDataController instance;

	public SpatialDataController() {
		instance = this;
	}

	public static SpatialDataController getInstance() {
		return instance;
	}

	public LineChart<Number, Number> getDemandsChart() {
		return demandsChart;
	}

	public BarChart<String, Number> getHistogramCapitals() {
		return histogramCapitals;
	}

	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		CellsLoader.loadCapitalsList();
		ServiceSet.loadServiceList();

		TabPaneController.cellsLoader.loadMap();
		CellsSet.setCellsSet(TabPaneController.cellsLoader);
		CellsSet.plotCells();
		RegionClassifier.initialation();
		RegionClassifier.serviceupdater();
		new LineChartTools().lineChart((Pane) demandsChart.getParent(), demandsChart,
				DemandModel.serialisationWorldDemand());
		String ItemName = "Save as CSV";
		Consumer<String> action = x -> {
			SaveAs.exportLineChartDataToCSV(demandsChart);
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle((Pane) demandsChart.getParent(), demandsChart, othersMenuItems);

		updatePieChartColorAFts(pieChartColor);
		mapColorAndCapitalHistogrameInitialisation();
		radioColor[0].fire();
		isNotInitialsation = true;
		((CategoryAxis) histogramCapitals.getXAxis()).setCategories(FXCollections.observableArrayList(
				IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(Collectors.toList())));
	}

	private void mapColorAndCapitalHistogrameInitialisation() {
		radioColor = new RadioButton[CellsLoader.getCapitalsList().size() + 1];
		for (int i = 0; i < CellsLoader.getCapitalsList().size(); i++) {
			radioColor[i] = new RadioButton(CellsLoader.getCapitalsList().get(i));
			vboxForSliderColors.getChildren().add(radioColor[i]);
			int k = i;
			radioColor[k].setOnAction(e -> {
				for (int j = 0; j < CellsLoader.getCapitalsList().size() + 1; j++) {
					if (j != k) {
						if (radioColor[j] != null) {
							radioColor[j].setSelected(false);
						}
					}
				}
				if (isNotInitialsation) {
					histogramCapitals.getData().clear();
					if (!PathsLoader.getScenario().equalsIgnoreCase("Baseline")) {
						histogrameCapitals(PathsLoader.getCurrentYear() + "", CellsLoader.getCapitalsList().get(k));
					}
					CellsSet.colorMap(CellsLoader.getCapitalsList().get(k));
				}
			});
		}
		radioColor[CellsLoader.getCapitalsList().size()] = new RadioButton("AFTs Distribution");
		radioColor[CellsLoader.getCapitalsList().size()].setSelected(true);
		vboxForSliderColors.getChildren().add(radioColor[CellsLoader.getCapitalsList().size()]);
		radioColor[CellsLoader.getCapitalsList().size()].setOnAction(e -> {
			for (int j = 0; j < CellsLoader.getCapitalsList().size() + 1; j++) {
				if (j != CellsLoader.getCapitalsList().size()) {
					if (radioColor[j] != null) {
						radioColor[j].setSelected(false);
					}
				}
			}
			histogramCapitals.getData().clear();
			CellsSet.colorMap("AFT");
		});
	}

	void histogrameCapitals(String year, String capitalName) {
		Set<Double> dset = CellsLoader.hashCell.values().stream().map(c -> c.getCapitals().get(capitalName))
				.collect(Collectors.toSet());
		XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
		List<Integer> numbersInInterval = countNumbersInIntervals(dset, 100);
		dataSeries.setName(capitalName + "_" + year + "_" + PathsLoader.getScenario());
		for (int i = 0; i < numbersInInterval.size(); i++) {
			Integer v = numbersInInterval.get(i);
			dataSeries.getData().add(new XYChart.Data<>((i) + "", v));
		}
		histogramCapitals.getData().add(dataSeries); 
		String ItemName = "Clear Histogram";
		Consumer<String> action = x -> {
			histogramCapitals.getData().clear();
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle((Pane) histogramCapitals.getParent(), histogramCapitals, othersMenuItems);
 	}

	public static List<Integer> countNumbersInIntervals(Set<Double> numbers, int intervalNBR) {
		int[] counts = new int[intervalNBR + 1];
		for (Double number : numbers) {
			if (number != null && number >= 0.0 && number <= 1.0) {
				int index = (int) (number * intervalNBR);
				counts[index]++;
			}
		}
		OptionalInt max = Arrays.stream(counts).max();
		List<Integer> result = new ArrayList<>();
		for (int count : counts) {
			result.add((count * 100) / max.getAsInt());
		}
		return result;
	}

	private void updatePieChartColorAFts(PieChart chart) {
		ConcurrentHashMap<String, Double> convertedMap = new ConcurrentHashMap<>(AFTsLoader.hashAgentNbr.entrySet()
				.stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())));
		HashMap<String, Color> color = new HashMap<>();
		AFTsLoader.getAftHash().forEach((name, a) -> {
			color.put(name, a.getColor());
		});

		new PieChartTools().updateChart(convertedMap, color, chart);
		chart.setLegendSide(Side.LEFT);
		// * add menu to PiChart*//
		HashMap<String, Consumer<String>> newItemMenu = new HashMap<>();
		Consumer<String> reset = x -> {
			TabPaneController.cellsLoader.AFtsSet.agentsColorinitialisation();
			TabPaneController.cellsLoader.AFtsSet.forEach((a) -> {
				color.put(a.getLabel(), a.getColor());
			});
			new PieChartTools().updateChart(convertedMap, color, chart);
			CellsSet.colorMap("AFT");
		};

		Consumer<String> saveInPutData = x -> {
			TabPaneController.cellsLoader.AFtsSet.updateColorsInputData();
		};

		newItemMenu.put("Reset Colors", reset);
		newItemMenu.put("Save new Colors to Input Data", saveInPutData);

		chart.setOnMouseDragged(event -> {
			chart.setPrefHeight(event.getY());
		});
		HashMap<String, Consumer<String>> hashm = new HashMap<>();

		newItemMenu.forEach((name, action) -> {
			hashm.put(name, action);
		});
		MousePressed.mouseControle((Pane) chart.getParent(), chart, hashm);
	}

}
