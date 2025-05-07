package de.cesr.crafty.gui.controller.fxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.model.Cell;
import de.cesr.crafty.core.model.ManagerTypes;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.gui.utils.graphical.Histogram;
import de.cesr.crafty.gui.utils.graphical.MousePressed;
import de.cesr.crafty.gui.utils.graphical.PieChartTools;
import de.cesr.crafty.gui.utils.graphical.Tools;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;

public class RegionController {
	@FXML
	private AnchorPane root;
	@FXML
	private Canvas canvas;
	@FXML
	private VBox generalVbox;
	@FXML
	private VBox runVbox;

	private static ConcurrentHashMap<String, Cell> RegionCells = new ConcurrentHashMap<>();
	// these will be set each time you call plotCells(...)
	private int lastMinX, lastMinY;
	private int lastPixelSize;

	/** upper‐bounds for display size */
	private static final int MAX_CANVAS_WIDTH = 1000;
	private static final int MAX_CANVAS_HEIGHT = 800;

//
//	public static GraphicsContext gc;
//	public static PixelWriter pixelWriter;
//	public static WritableImage writableImage;
//
	public static ConcurrentHashMap<String, Cell> getRegionCells() {
		return RegionCells;
	}

	public void initialize() {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Color.NAVY);
		gc.fillRect(50, 40, 300, 220);
		plotCells(RegionCells);
		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onCanvasClick);
		fillGeneralVbox();
		fillRunModel();
		runVbox.setMaxWidth(500);
		runVbox.setMinWidth(500);
	}

	public void plotCells(ConcurrentHashMap<String, Cell> data) {
		if (data == null || data.isEmpty())
			return;

		// 1) find min/max
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (Cell c : data.values()) {
			int x = c.getX(), y = c.getY();
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
		}

		// 2) compute grid size
		int gridW = maxX - minX + 1;
		int gridH = maxY - minY + 1;

		// 3) pick pixelSize
		int pixelSize = Math.max(1, Math.min(MAX_CANVAS_WIDTH / gridW, MAX_CANVAS_HEIGHT / gridH));

		// remember for click‐to‐cell mapping
		lastMinX = minX;
		lastMinY = minY;
		lastPixelSize = pixelSize;

		// 4) resize canvas
		canvas.setWidth(gridW * pixelSize);
		canvas.setHeight(gridH * pixelSize);

		// 5) draw cells
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

		if (pixelSize == 1) {
			PixelWriter pw = gc.getPixelWriter();
			for (Cell c : data.values()) {
				int x = c.getX() - minX;
				int y = c.getY() - minY;
				pw.setColor(x, y, Color.web(c.getColor()));
			}
		} else {
			for (Cell c : data.values()) {
				int x = (c.getX() - minX) * pixelSize;
				int y = (c.getY() - minY) * pixelSize;
				gc.setFill(Color.web(c.getColor()));
				gc.fillRect(x, y, pixelSize, pixelSize);
			}
		}
	}

	/**
	 * Mouse‐click handler: converts the click position into your cell coord.
	 */
	private void onCanvasClick(MouseEvent e) {
		double mouseX = e.getX();
		double mouseY = e.getY();

		// which pixel did we click?
		int cellX = (int) mouseX / lastPixelSize + lastMinX;
		int cellY = (int) mouseY / lastPixelSize + lastMinY;

		System.out.println("Clicked cell at X=" + cellX + ", Y=" + cellY);
	}

	private void fillGeneralVbox() {
		Map<String, Double> capitalAverage = new HashMap<>();

		CellsLoader.getCapitalsList().forEach(capitalName -> {
			RegionCells.values().forEach(c -> {
				capitalAverage.merge(capitalName, c.getCapitals().get(capitalName) / RegionCells.size(), Double::sum);
			});
		});
		BarChart<String, Number> histogram = new BarChart<>(new CategoryAxis(), new NumberAxis());
		Histogram.histo("Capital average", histogram, capitalAverage);
		HBox box = new HBox(histogram);
		MousePressed.mouseControle(box, histogram);

		PieChart pieChart = new PieChart();

		Map<String, Integer> hash = hashAgentNbr();
		ConcurrentHashMap<String, Double> convertedMap = new ConcurrentHashMap<>(hash.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())));

		HashMap<String, Color> color = new HashMap<>();
		AFTsLoader.getAftHash().forEach((name, a) -> {
			color.put(name, Color.web(a.getColor()));
		});

		new PieChartTools().updateChart(convertedMap, color, pieChart, false);

		Map<String, Double> productivityData = regionServiceCalculation();
		BarChart<String, Number> PHisto = new BarChart<>(new CategoryAxis(), new NumberAxis());
		Histogram.histo("Supply", PHisto, productivityData);
		HBox Pbox = new HBox(PHisto);
		MousePressed.mouseControle(Pbox, PHisto);

		generalVbox.getChildren().addAll(pieChart, box, Pbox);

	}

	private static Map<String, Integer> hashAgentNbr() {
		Map<String, Integer> hashAgentNbr = new HashMap<>();
		RegionCells.values().forEach(c -> {
			if (c.getOwner() != null)
				hashAgentNbr.merge(c.getOwner().getLabel(), 1, Integer::sum);
		});
		return hashAgentNbr;
	}

	static Map<String, Double> productivityCalculator(Cell c) {
		ConcurrentHashMap<String, Double> services = new ConcurrentHashMap<>();
		for (Aft manager : AFTsLoader.getActivateAFTsHash().values()) {

			if (manager.getType() != ManagerTypes.Abandoned && manager.getSensitivity() != null) {
				ServiceSet.getServicesList().forEach(s -> {
					double product = c.getCapitals().entrySet().stream()
							.mapToDouble(e -> (manager.getSensitivity().get(e.getKey() + "|" + s) != null
									? Math.pow(e.getValue(), manager.getSensitivity().get(e.getKey() + "|" + s))
									: 0))
							.reduce(1.0, (x, y) -> x * y);
					services.put(s, product * manager.getProductivityLevel().get(s));
				});
			}
		}
		return services;
	}

	static Map<String, Double> regionServiceCalculation() {
		ConcurrentHashMap<String, Double> supply = new ConcurrentHashMap<>();

		RegionCells.values().forEach(c -> {
			Map<String, Double> data = productivityCalculator(c);
			ServiceSet.getServicesList().forEach(s -> {
				double d = data.get(s) != null ? data.get(s) : 0;
				supply.merge(s, d, Double::sum);
			});

		});
		return supply;
	}

	private void fillRunModel() {
		GridPane grid = new GridPane();
		int j = 0, k = 0, colmunNBR = 3;
		for (int m = 0; m < ServiceSet.getServicesList().size(); m++) {
			LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
			lineChart.setTitle(ServiceSet.getServicesList().get(m));
			grid.add(Tools.hBox(lineChart), j++, k);
			if (j % colmunNBR == 0) {
				k++;
				j = 0;
			}
		}
		runVbox.getChildren().add(grid);

	}

}