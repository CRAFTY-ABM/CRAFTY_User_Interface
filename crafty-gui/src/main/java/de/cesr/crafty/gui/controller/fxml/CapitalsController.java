package de.cesr.crafty.gui.controller.fxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.gui.canvasFx.CellsSet;
import de.cesr.crafty.gui.utils.graphical.MousePressed;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class CapitalsController {

	@FXML
	private VBox vboxForSliderColors;
	@FXML
	private BarChart<String, Number> histogramCapitals;
	public static RadioButton[] radioColor;
	private static CapitalsController instance;

	public CapitalsController() {
		instance = this;
	}
	public static CapitalsController getInstance() {
		return instance;
	}

	public BarChart<String, Number> getHistogramCapitals() {
		return histogramCapitals;
	}

	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		mapColorAndCapitalHistogrameInitialisation();
//		((CategoryAxis) histogramCapitals.getXAxis()).setCategories(FXCollections.observableArrayList(
//				IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(Collectors.toList())));
		radioColor[0].fire();
	}

	private void mapColorAndCapitalHistogrameInitialisation() {
		ToggleGroup radiosgroup = new ToggleGroup();

		radioColor = new RadioButton[CellsLoader.getCapitalsList().size()];
		for (int i = 0; i < radioColor.length; i++) {
			int k = i;
			if (k < CellsLoader.getCapitalsList().size()) {
				radioColor[k] = new RadioButton(CellsLoader.getCapitalsList().get(i));
			}
			radioColor[k].setOnAction(e -> {
				updatehistogram(k);
				CellsSet.colorMap(radioColor[k].getText());
			});
			radioColor[k].setToggleGroup(radiosgroup);
			vboxForSliderColors.getChildren().add(radioColor[k]);
		}
	}

	private void updatehistogram(int k) {
		histogramCapitals.getData().clear();
		if (k < CellsLoader.getCapitalsList().size()) {
			if (!ProjectLoader.getScenario().equalsIgnoreCase("Baseline")) {
				histogrameCapitals(ProjectLoader.getCurrentYear() + "", CellsLoader.getCapitalsList().get(k));
			}
		}
	}

	void histogrameCapitals(String year, String capitalName) {
		Set<Double> dset = CellsLoader.hashCell.values().stream().map(c -> c.getCapitals().get(capitalName))
				.collect(Collectors.toSet());
		XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
		List<Integer> numbersInInterval = countNumbersInIntervals(dset, 100);
		dataSeries.setName(capitalName + "_" + year + "_" + ProjectLoader.getScenario());
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

}
