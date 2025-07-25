package de.cesr.crafty.gui.utils.analysis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

public class MultiShadowLineChart extends LineChart<Number, Number> {

	private final Map<XYChart.Series<Number, Number>, SeriesData> dataMap = new LinkedHashMap<>();

	public MultiShadowLineChart(String titel,Map<String, List<Double>> points, Map<String, List<Double>> errors,
			Map<String, Color> colours) {
		super(new NumberAxis(), new NumberAxis());

		setAnimated(false);
		setCreateSymbols(false);
		this.setTitle(titel);
//		getXAxis().setLabel("Index");
//		getYAxis().setLabel("Value");

		int colourFallbackIndex = 0;
		List<Color> defaultPalette = List.of(Color.CRIMSON, Color.ROYALBLUE, Color.SEAGREEN, Color.ORANGE,
				Color.MEDIUMPURPLE, Color.CHOCOLATE);
		AtomicInteger count = new AtomicInteger();
		for (String key : points.keySet()) {
			List<Double> ys = points.get(key);
			List<Double> errs = errors.get(key);

			Color colour = colours.getOrDefault(key, defaultPalette.get(colourFallbackIndex++ % defaultPalette.size()));

			SeriesData sd = new SeriesData(ys, errs, colour);
			XYChart.Series<Number, Number> s = new XYChart.Series<>();
			s.setName(key);
			for (int i = 0; i < sd.y.length; i++) {
				s.getData().add(new XYChart.Data<>(i, sd.y[i]));
			}
			getData().add(s);
			dataMap.put(s, sd);

			// configure band visual
			sd.area.setStrokeWidth(0);
			sd.area.setFill(Color.color(colour.getRed(), colour.getGreen(), colour.getBlue(), 0.25));
			// ensure the band lives behind everything else
			getPlotChildren().add(0, sd.area);

			// once the series node exists we can style the line
			final Color lineColour = colour;
			Node poly = this.lookup(".chart-series-line.series" + count.getAndIncrement());
			if (poly != null) {
				poly.setStyle("-fx-stroke:" + toRgb(lineColour) + ';');
			}
		}
		labelcolor(this, colours);
	}

	@Override
	protected void layoutPlotChildren() {
		super.layoutPlotChildren();

		// Rebuild every band so it tracks the data after each layout pass
		for (Map.Entry<XYChart.Series<Number, Number>, SeriesData> e : dataMap.entrySet()) {
			SeriesData sd = e.getValue();

			Path area = sd.area;
			area.getElements().clear();

			if (sd.y.length < 2)
				continue;

			NumberAxis xAxis = (NumberAxis) getXAxis();
			NumberAxis yAxis = (NumberAxis) getYAxis();

			// Lower edge (y - err)
			area.getElements()
					.add(new MoveTo(xAxis.getDisplayPosition(0), yAxis.getDisplayPosition(sd.y[0] - sd.err[0])));
			for (int i = 1; i < sd.y.length; i++) {
				area.getElements()
						.add(new LineTo(xAxis.getDisplayPosition(i), yAxis.getDisplayPosition(sd.y[i] - sd.err[i])));
			}
			// Upper edge (y + err) – walk back
			for (int i = sd.y.length - 1; i >= 0; i--) {
				area.getElements()
						.add(new LineTo(xAxis.getDisplayPosition(i), yAxis.getDisplayPosition(sd.y[i] + sd.err[i])));
			}
			area.getElements().add(new ClosePath());
		}
	}

	private static void labelcolor(LineChart<Number, Number> chart, Map<String, Color> colours) {

		List<Label> legendItems = chart.lookupAll(".chart-legend-item").stream().filter(Label.class::isInstance)
				.map(Label.class::cast).toList();
		int m = 0;
		for (Node item : chart.lookupAll("Label.chart-legend-item")) {
			Label label = (Label) item;
			Color co = colours.get(legendItems.get(m).getText());
			final Rectangle rectangle = new Rectangle(10, 10, co);
			label.setGraphic(rectangle);
			m++;
		}
	}

	private static String toRgb(Color c) {
		return String.format("rgb(%d,%d,%d)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
				(int) (c.getBlue() * 255));

	}

	private static class SeriesData {
		final double[] y;
		final double[] err;
		final Path area = new Path();

		SeriesData(List<Double> y, List<Double> err, Color colour) {
			this.y = y.stream().mapToDouble(Double::doubleValue).toArray();
			this.err = (err == null ? Collections.<Double>nCopies(y.size(), 0.0) : err).stream()
					.mapToDouble(Double::doubleValue).toArray();
		}
	}
}
