package de.cesr.crafty.core.utils.general;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;

import de.cesr.crafty.core.cli.ConfigLoader;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChartExporter {

	/**
	 * Creates a line chart from the given data and saves it as PNG.
	 *
	 * @param hashData   Map of series name -> list of Y-values
	 * @param chartTitle Title of the chart
	 * @param pngPath    Output path for the PNG file
	 */
	public static void createAndSaveChartAsPNG(Map<String, ArrayList<Double>> hashData, int startYear,
			String chartTitle, String pngPath) {
		createAndSaveChartAsPNG(hashData, null, startYear, chartTitle, pngPath);
	}

	public static void createAndSaveChartAsPNG(Map<String, ArrayList<Double>> hashData, Map<String, Color> hashColors,
			int startYear, String chartTitle, String pngPath) {

		XYChart chart = new XYChartBuilder().width(800).height(600).title(chartTitle)/**/.theme(ChartTheme.Matlab)
				.build();
		chartStyle(chart);
		chart.getStyler().setYAxisMin(0.0);
		// 3) Add Series
		for (Map.Entry<String, ArrayList<Double>> entry : hashData.entrySet()) {
			String seriesName = entry.getKey();
			ArrayList<Double> yData = entry.getValue();
			List<Integer> xData = new ArrayList<>();
			for (int i = 0; i < yData.size(); i++) {
				xData.add(i + startYear);
			}
			// Add to chart
			if (yData.size() != 0) {
				chart.addSeries(seriesName, xData, yData);
			}
		}
		if (hashColors != null) {
			addColors(chart, hashColors);
		}
		// 4) Save
		try {
			if (ConfigLoader.config.generate_charts_plots_PNG) {
				BitmapEncoder.saveBitmap(chart, pngPath + ".PNG", BitmapFormat.PNG);
			}
			if (ConfigLoader.config.generate_charts_plots_PDF) {
				VectorGraphicsEncoder.saveVectorGraphic(chart, pngPath.replace("png", "pdf"), VectorGraphicsFormat.PDF);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void chartStyle(XYChart chart) {
		chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
		chart.getStyler().setMarkerSize(4);
		chart.getStyler().setXAxisDecimalPattern("####");

		// Customize Chart
	    // chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
	}

	private static void addColors(XYChart chart, Map<String, Color> hashColors) {

		chart.getSeriesMap().forEach((name, series) -> {
			if (hashColors.containsKey(name)) {
				series.setLineColor(hashColors.get(name));
				series.setMarkerColor(hashColors.get(name));
			}
		});
	}

}
