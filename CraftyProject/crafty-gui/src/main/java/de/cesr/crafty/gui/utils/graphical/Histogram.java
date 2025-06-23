package de.cesr.crafty.gui.utils.graphical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import kotlin.random.Random;

/**
 * @author Mohamed Byari
 *
 */

public class Histogram {

	public static void histo(Pane box, String name, BarChart<String, Number> histogram, Set<Double> values) {
		XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();

		List<Integer> numbersInInterval = countNumbersInIntervals(values, 100);
		dataSeries.setName(name);
		for (int i = 0; i < numbersInInterval.size(); i++) {
			Integer v = numbersInInterval.get(i);
			dataSeries.getData().add(new XYChart.Data<>(String.valueOf(i), v));
		}
		histogram.getData().add(dataSeries);
		String ItemName = "Clear Histogram";
		Consumer<String> action = x -> {
			histogram.getData().clear();
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle(box, histogram, othersMenuItems);

	}

	public static void histo(Pane box, String name, BarChart<String, Number> histogram, Map<String, Double> hash) {
		histogram.getData().clear();

		XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
		dataSeries.setName(name);
		hash.forEach((n, v) -> {
			dataSeries.getData().add(new XYChart.Data<>(n, v));
		});

		Platform.runLater(() -> {
			Color color = ColorsTools.colorlist((int) (Math.random() * 100));
			for (XYChart.Data<String, Number> d : dataSeries.getData()) {
				d.getNode().setStyle("-fx-bar-fill: " + ColorsTools.toHex(color) + ";");
			}
		});

		histogram.getData().add(dataSeries);
		if (box != null)
			MousePressed.mouseControle(box, histogram);
	}

	public static void histo(String name, BarChart<String, Number> histogram, Map<String, Double> hash) {
		histo(null, name, histogram, hash);
	}

	public static List<Integer> countNumbersInIntervals(Set<Double> numbers, int intervalNBR) {
		int[] counts = new int[intervalNBR];

		for (Double number : numbers) {
			if (number >= 0.0 && number <= 1.0) {
				int index = (int) (number * intervalNBR);
				// Handle the edge case where a number is exactly 1.0
				if (index == intervalNBR) {
					index = intervalNBR - 1;
				}
				counts[index]++;
			}
		}

		List<Integer> result = new ArrayList<>();
		for (int count : counts) {
			result.add(count);
		}
		return result;
	}

	public static void mouseHistogrameController(BarChart<String, Number> histogram) {
		String ItemName = "Clear Histogram";
		Consumer<String> action = x -> {
			histogram.getData().clear();
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle((Pane) histogram.getParent(), histogram, othersMenuItems);
	}
}
