package de.cesr.crafty.gui.utils.analysis;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class MixedLineChart extends Application {

	@Override
	public void start(Stage stage) {

		ArrayList<Double> v1 = NonGraphic.generateNormalData(5, 4, 10, 10);
		ArrayList<Double> v2 = NonGraphic.generateNormalData(20, 2, 10, 10);
		HashMap<String, List<Double>> h = new HashMap<String, List<Double>>();
		HashMap<String, List<Double>> h2 = new HashMap<String, List<Double>>();

		h.put("blue", v1);
		h2.put("red", v2);
		Map<String, Color> colors = new HashMap<>();
		colors.put("blue", Color.BLUE);
		colors.put("red", Color.RED);

		LineChart<Number, Number> chart = MixedLineChart.mixedLineChart("test", h2, h, colors);

		/* ------------ show ------------------------------------------------ */
		stage.setScene(new Scene(chart, 800, 450));
		stage.setTitle("JavaFX 24 – mixed LineChart demo");
		stage.show();
	}

	public static LineChart<Number, Number> mixedLineChart(String titel, Map<String, List<Double>> lineSeries,
			Map<String, List<Double>> dotSeries, Map<String, Color> colours) {
		/* ------------ create & configure the chart ---------------------- */
		LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
		chart.setTitle(titel);
		chart.setCreateSymbols(true); // we will hide them selectively
		chart.setLegendVisible(true);

		List<String> allNames = new ArrayList<>();

		// add line series first – their index drives CSS “seriesN”
		lineSeries.forEach((name, ys) -> {
			chart.getData().add(makeSeries(name, ys));
			allNames.add(name);
		});
		dotSeries.forEach((name, ys) -> {
			chart.getData().add(makeSeries(name, ys));
			allNames.add(name);
		});

		/* ------------ style after first layout pulse -------------------- */
		Platform.runLater(() -> {
			chart.applyCss(); // ensure sub‑nodes exist
			chart.layout();

			for (int i = 0; i < allNames.size(); i++) {
				String name = allNames.get(i);
				boolean lineOnly = lineSeries.containsKey(name);
				String rgba = toRgba(colours.getOrDefault(name, Color.BLACK));

				/* 1) poly-line */
				Node poly = chart.lookup(".chart-series-line.series" + i);
				if (poly != null) {
					poly.setStyle("-fx-stroke:" + (lineOnly ? rgba : "transparent") + ';');
				}

				/* 2) symbols in plot area */
				for (Node dot : chart.lookupAll(".chart-line-symbol.series" + i)) {
					if (lineOnly) {
						dot.setStyle("-fx-background-color: transparent;");
					} else {
						dot.setStyle("-fx-background-color:" + rgba + ", white;" + "-fx-background-insets: 2,3;"
								+ "-fx-background-radius: 5px;");
					}
				}

				Node fill = chart.lookup(".chart-series-area-fill.series" + i);
				if (fill != null ) {
					fill.setStyle("-fx-fill: rgba(" + rgba + ",0.3);");
				}

				/*
				 * 3) legend rows – row itself has no series class, but its *symbol* does
				 */
				for (Node symbol : chart.lookupAll(".chart-legend-item-symbol.series" + i)) {

					Node row = symbol.getParent(); // the enclosing Label
					if (row != null && lineOnly) {
						row.setManaged(false); // collapse the gap
						row.setVisible(false); // hide
					}
				}
			}
		});
		return chart;

	}

	/* helper: build a series, x = list index */
	private static XYChart.Series<Number, Number> makeSeries(String name, List<Double> ys) {

		XYChart.Series<Number, Number> s = new XYChart.Series<>();
		s.setName(name);
		for (int x = 0; x < ys.size(); x++) {
			s.getData().add(new XYChart.Data<>(x, ys.get(x)));
		}
		return s;
	}

	/* helper: convert Color → "rgba(r,g,b,a)" */
	private static String toRgba(Color c) {
		return String.format("rgba(%d,%d,%d,%f)", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
				Math.round(c.getBlue() * 255), c.getOpacity());
	}

	public static void main(String[] args) {
		launch(args);
	}
}
