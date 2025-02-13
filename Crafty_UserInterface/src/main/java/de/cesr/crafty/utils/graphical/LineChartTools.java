package de.cesr.crafty.utils.graphical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.ProjectLoader;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * @author Mohamed Byari
 *
 */

public class LineChartTools {
	

	public void lineChart(Pane box, LineChart<Number, Number> lineChart, Map<String, ArrayList<Double>> hash) {
		if (hash == null) {
			return;
		}
		configurexAxis(lineChart, ProjectLoader.getStartYear(), ProjectLoader.getEndtYear());
		lineChart.getData().clear();
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
			if (value != null) {
				for (int j = 0; j < value.size(); j++) {
					series[k.get()].getData().add(new XYChart.Data<>(
							j + ((NumberAxis) lineChart.getXAxis()).getLowerBound(), (Number) (value.get(j))));
				}
				k.getAndIncrement();
			}
		});
//		if (hash.size() > 8) {
//			AtomicInteger K = new AtomicInteger();
//			sortedKeys.forEach((key) -> {
//				ArrayList<Double> value = hash.get(key);
//				if (value != null) {
//					for (int j = 0; j < value.size(); j++) {
//						series[K.get()].getData().add(new XYChart.Data<>(j, +value.get(j)));
//						series[K.get()].getNode().lookup(".chart-series-line").setStyle(
//								"-fx-stroke: " + ColorsTools.getStringColor(ColorsTools.colorlist(K.get())) + ";");
//					}
//					K.getAndIncrement();
//				}
//			});
//			if (TabPaneController.cellsLoader != null)
//				labelcolor( lineChart);
//			lineChart.setCreateSymbols(false);
//		}
		strokeColor(lineChart, hash);
//		if (box != null) {
//			MousePressed.mouseControle(box, lineChart);
//		}
		LineChartTools.addSeriesTooltips(lineChart);
	}
	
	public static void strokeColor(LineChart<Number, Number> lineChart, Map<String, ArrayList<Double>> hash) {
		List<String> sortedKeys = new ArrayList<>(hash.keySet());
		Collections.sort(sortedKeys);
		if (hash.size() > 8) {
			AtomicInteger K = new AtomicInteger();
			sortedKeys.forEach((key) -> {
				ArrayList<Double> value = hash.get(key);
				if (value != null) {
					for (int j = 0; j < value.size(); j++) {
						lineChart.getData().get(K.get()).getData().add(new XYChart.Data<>(j, +value.get(j)));
						lineChart.getData().get(K.get()).getNode().lookup(".chart-series-line").setStyle(
								"-fx-stroke: " + ColorsTools.getStringColor(ColorsTools.colorlist(K.get())) + ";");
					}
					K.getAndIncrement();
				}
			});
			if (ProjectLoader.cellsLoader != null)
				labelcolor( lineChart);
			lineChart.setCreateSymbols(false);
		}
	}

	public static void addSeriesTooltips(LineChart<Number, Number> lineChart) {
		for (XYChart.Series<Number, Number> series : lineChart.getData()) {
			// Building the tooltip text
			String tooltipText = "Series: " + series.getName() + "\nData Points: " + series.getData().size();
			// Set the tooltip for the line
			Tooltip seriesTooltip = new Tooltip(tooltipText);
			seriesTooltip.setShowDelay(Duration.millis(100));
			Tooltip.install(series.getNode(), seriesTooltip);

			// Apply the same tooltip to each data point in the series
			for (XYChart.Data<Number, Number> data : series.getData()) {
				Tooltip dataPointTooltip = new Tooltip(tooltipText);
				dataPointTooltip.setShowDelay(Duration.millis(100));
				if (data.getNode() != null) {
					Tooltip.install(data.getNode(), dataPointTooltip);
					// Optional: Highlight data points when hovered
					data.getNode()
							.setOnMouseEntered(event -> data.getNode().setStyle("-fx-scale-x: 1.5; -fx-scale-y: 1.5;"));
					data.getNode()
							.setOnMouseExited(event -> data.getNode().setStyle("-fx-scale-x: 1; -fx-scale-y: 1;"));
				}
			}
		}
	}

	public static void labelcolor(LineChart<Number, Number> lineChart) {
		int m = 0;
		for (Node item : lineChart.lookupAll("Label.chart-legend-item")) {
			Label label = (Label) item;
			Color co = AFTsLoader.getAftHash().get(label.getText()) != null
					? AFTsLoader.getAftHash().get(label.getText()).getColor()
					: ColorsTools.colorlist(m);
			final Rectangle rectangle = new Rectangle(10, 10, co);
			label.setGraphic(rectangle);
			m++;
		}
	}

	public static void configurexAxis(LineChart<Number, Number> demandsChart, int start, int end) {
		NumberAxis xAxis = ((NumberAxis) demandsChart.getXAxis());
		xAxis.setAutoRanging(false);
		xAxis.setLowerBound(start);
		xAxis.setUpperBound(end);
		xAxis.setTickUnit((end - start) / 10);
		xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis) {
			@Override
			public String toString(Number object) {
				return String.format("%d", object.intValue());
			}
		});
	}
	public static void configurexYxis(LineChart<Number, Number> demandsChart, double start, double end) {
		NumberAxis yAxis = ((NumberAxis) demandsChart.getYAxis());
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(start);
		yAxis.setUpperBound(end);
		yAxis.setTickUnit((end - start) / 10);
		yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
		    @Override
		    public String toString(Number object) {
		        // Use %.3f to format to 3 decimal places
		        return String.format("%.3f", object.doubleValue());
		    }
		});
	}
}
