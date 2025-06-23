package de.cesr.crafty.gui.utils.graphical;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class ShadedLineChartApp extends Application {

	@Override
	public void start(Stage stage) {
		stage.setTitle("Multi-Series Shaded Line Chart");

		// === example data; replace with your own maps ===
		Map<String, List<Double>> data = Map.of("red", List.of(0.0, 10.0, 25.0, 40.0, 30.0, 20.0), "BLUE",
				List.of(5.0, 15.0, 20.0, 35.0, 25.0, 10.0), "BLACK", List.of(2.0, 12.0, 18.0, 28.0, 22.0, 8.0));
		Map<String, Color> colors = Map.of("red", Color.RED, "BLUE", Color.BLUE, "BLACK", Color.BLACK);
		// =================================================

		AreaChart<Number, Number> chart = createShadedAreaChart(data, colors);
		Scene scene = new Scene(chart, 800, 400);
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * Builds an AreaChart (i.e. a LineChart + fill) with: • multiple series from
	 * seriesData (key → list of Y values at X = 0,1,2,…) • no symbols (data points)
	 * • each series stroked & filled in its supplied Color (with 30% opacity on
	 * fill)
	 */
	public static AreaChart<Number, Number> createShadedAreaChart(Map<String, List<Double>> seriesData,
			Map<String, Color> seriesColors) {
		// 1) set up axes
		NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("X");
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Y");

		// 2) make an AreaChart (it draws the line + an under-curve polygon)
		AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
		chart.setCreateSymbols(false); // disable point markers

		// 3) populate series
		seriesData.forEach((name, values) -> {
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			series.setName(name);
			for (int i = 0; i < values.size(); i++) {
				series.getData().add(new XYChart.Data<>(i, values.get(i)));
			}
			chart.getData().add(series);
		});

		// 4) defer styling until after the chart is laid out
		Platform.runLater(() -> {
			chart.applyCss();
			chart.layout();
			for (int i = 0; i < chart.getData().size(); i++) {
				String seriesClass = ".series" + i;
				XYChart.Series<Number, Number> series = chart.getData().get(i);
				Color color = Color.YELLOW;// seriesColors.get(series.getName());
				if (color == null)
					continue;

				int r = (int) (color.getRed() * 255);
				int g = (int) (color.getGreen() * 255);
				int b = (int) (color.getBlue() * 255);
				String rgb = r + "," + g + "," + b;

				// fill under the line (30% opacity)
//				Node fill = chart.lookup(".chart-series-area-fill" + seriesClass);
//				if (fill != null) {
//					fill.setStyle("-fx-fill: rgba(" + rgb + ",0.3);");
//				}

//                // the line itself (full opacity)
//				Node line = chart.lookup(".chart-series-line" + seriesClass);
//				if (line != null) {
//					line.setStyle("-fx-stroke: rgba(" + rgb + ",1);" + "-fx-stroke-width: 2px;");
//				}

				/* 1) poly-line */
//				Node poly = chart.lookup(".chart-series-line.series" + i);
//				if (poly != null) {
//					poly.setStyle("-fx-stroke:" + rgb + ';');
//				}

				/* 2) symbols in plot area */
//				for (Node dot : chart.lookupAll(".chart-line-symbol.series" + i)) {
//					dot.setStyle("-fx-background-color: red;");
//					dot.setStyle("-fx-background-color:" + rgb + ", white;" + "-fx-background-insets: 2,3;"
//							+ "-fx-background-radius: 5px;");
//
//				}

			}
		});

		return chart;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
