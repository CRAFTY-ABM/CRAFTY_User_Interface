package de.cesr.crafty.controller.fxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.AftCategorised;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.ProjectLoader;
import de.cesr.crafty.main.FxMain;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.utils.graphical.MousePressed;
import de.cesr.crafty.utils.graphical.PieChartTools;
import de.cesr.crafty.utils.graphical.Tools;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

public class AFTsMapsController {
	@FXML
	private PieChart pieAFTsDistrebution;
	@FXML
	private PieChart pieAFTsCategories;
	@FXML
	private Button aftColors;
	@FXML
	private Button CategoriesColor;
	@FXML
	private GridPane grid;
	@FXML
	private VBox toplevel;
	@FXML
	private VBox box;

	ArrayList<PieChart> pieCharts;

	public void initialize() {
		System.out.println("Initialize " + getClass().getSimpleName());
		CellsSet.plotCells();
		updatePieAFTsDistrebution(pieAFTsDistrebution);
		updateCategoryPie(pieAFTsCategories);
//		pieAFTsCategories.setLegendSide(Side.LEFT);
		pieCharts = new ArrayList<>();

		initilasePieChart();
		initializeGridpane(3);
		forceResising();
	}
	private void forceResising() {
		box.setMaxWidth(Screen.getPrimary().getBounds().getWidth() / (FxMain.graphicScaleX * 2));
		box.setMinWidth(Screen.getPrimary().getBounds().getWidth() / (FxMain.graphicScaleX * 2));
	}

	void initilasePieChart() {
		if (AftCategorised.aftCategories.size() > 0) {
			AftCategorised.aftCategories.keySet().forEach(ca -> {
				if (ca.equals("Uncategorized")) {
					return;
				}
				ConcurrentHashMap<String, Integer> map = countIntesityInCategories(ca);
				ConcurrentHashMap<String, Double> convertedMap = new ConcurrentHashMap<>(map.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())));
				HashMap<String, Color> color = new HashMap<>();
				map.keySet().forEach(n -> {
					color.put(n, AftCategorised.categoriesColor.get(ca).interpolate(Color.WHITE, Math.random()));
				});
				if (map.size() > 1) {
					PieChart p = new PieChart();
					pieCharts.add(p);
					new PieChartTools().updateChart(convertedMap, color, p, false);
					p.setTitle(ca);
					p.setLegendVisible(false);
//					p.setMaxHeight(0);
//					p.setMaxWidth(0);

				}

			});
		}
	}

	ConcurrentHashMap<String, Integer> countIntesityInCategories(String category) {
		ConcurrentHashMap<String, Integer> hashAgentNbr = new ConcurrentHashMap<>();
		CellsLoader.hashCell.values().forEach(c -> {
			if (c.getOwner() != null && c.getOwner().getCategory() != null
					&& c.getOwner().getCategory().getName().equals(category))
				hashAgentNbr.merge(c.getOwner().getCategory().getIntensity(), 1, Integer::sum);
		});
		return hashAgentNbr;

	}

	void initializeGridpane(int colmunNBR) {
		int j = 0, k = 0;
		for (int m = 0; m < pieCharts.size(); m++) {
			grid.add(Tools.hBox(pieCharts.get(m)), j++, k);
			if (j % colmunNBR == 0) {
				k++;
				j = 0;
			}
		}
	}

	@FXML
	public void aftColorsAction() {
		CellsSet.colorMap("AFT");
	}

	@FXML
	public void CategoriesColorsAction() {
		CellsSet.colorMap("Categories");
	}

	private void updatePieAFTsDistrebution(PieChart chart) {
		ConcurrentHashMap<String, Double> convertedMap = new ConcurrentHashMap<>(AFTsLoader.hashAgentNbr.entrySet()
				.stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())));
		HashMap<String, Color> color = new HashMap<>();
		AFTsLoader.getAftHash().forEach((name, a) -> {
			color.put(name, a.getColor());
		});

		new PieChartTools().updateChart(convertedMap, color, chart, false);
		chart.setLegendSide(Side.LEFT);
		// * add menu to PiChart*//
		HashMap<String, Consumer<String>> newItemMenu = new HashMap<>();
		Consumer<String> reset = x -> {
			ProjectLoader.cellsLoader.AFtsSet.agentsColorinitialisation();
			ProjectLoader.cellsLoader.AFtsSet.forEach((a) -> {
				color.put(a.getLabel(), a.getColor());
			});
			new PieChartTools().updateChart(convertedMap, color, chart, false);
			CellsSet.colorMap("AFT");
		};

		newItemMenu.put("Reset Colors", reset);

		chart.setOnMouseDragged(event -> {
			chart.setPrefHeight(event.getY());
		});
		HashMap<String, Consumer<String>> hashm = new HashMap<>();

		newItemMenu.forEach((name, action) -> {
			hashm.put(name, action);
		});
		MousePressed.mouseControle((Pane) chart.getParent(), chart, hashm);
	}

	private void updateCategoryPie(PieChart chart) {
		ConcurrentHashMap<String, Double> convertedMap = new ConcurrentHashMap<>(hashCategoryNbr().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())));
		new PieChartTools().updateChart(convertedMap, AftCategorised.categoriesColor, chart, false);
		chart.setLegendSide(Side.LEFT);
		MousePressed.mouseControle((Pane) chart.getParent(), chart);
	}

	public static ConcurrentHashMap<String, Integer> hashCategoryNbr() {
		ConcurrentHashMap<String, Integer> hashAgentNbr = new ConcurrentHashMap<>();
		String Uncategorized = "Uncategorized";

		CellsLoader.hashCell.values().forEach(c -> {
			if (c.getOwner() != null && c.getOwner().getCategory() != null)
				hashAgentNbr.merge(c.getOwner().getCategory().getName(), 1, Integer::sum);
			else {
				hashAgentNbr.merge(Uncategorized, 1, Integer::sum);
			}
		});
		return hashAgentNbr;
	}

}
