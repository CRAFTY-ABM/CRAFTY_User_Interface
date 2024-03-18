package fxmlControllers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import UtilitiesFx.filesTools.FileReder;
import UtilitiesFx.filesTools.PathTools;
import UtilitiesFx.filesTools.SaveAs;
import UtilitiesFx.graphicalTools.LineChartTools;
import UtilitiesFx.graphicalTools.MousePressed;
import UtilitiesFx.graphicalTools.NewWindow;
import UtilitiesFx.graphicalTools.Tools;
import dataLoader.CellsLoader;
import dataLoader.Paths;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;

import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import model.CellsSet;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;

public class OutPuterController {
	CellsLoader M;

	@FXML
	private Button selectoutPut;
	@FXML
	private ChoiceBox<String> yearChoice;
	@FXML
	private GridPane gridChart;
	@FXML
	private ScrollPane scroll;
	@FXML
	private Button selecserivce;

	String outputpath = "";
	RadioButton[] radioColor;
	NewWindow colorbox = new NewWindow();

	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		M = TabPaneController.M;
		radioColor = new RadioButton[CellsSet.getServicesNames().size() + 1];

		yearChoice.setValue(Paths.getCurrentYear() + "");

		for (int i = 0; i < radioColor.length; i++) {
			if (i < CellsSet.getServicesNames().size()) {
				radioColor[i] = new RadioButton(CellsSet.getServicesNames().get(i));

			} else if (i == CellsSet.getServicesNames().size()) {
				radioColor[i] = new RadioButton("Agent");
			}

			int k = i;
			radioColor[i].setOnAction(e -> {
				for (int j = 0; j < radioColor.length; j++) {
					if (k != j) {
						radioColor[j].setSelected(false);
					}
				}
				if (k < CellsSet.getServicesNames().size()) {
					CellsSet.colorMap(CellsSet.getServicesNames().get(k));
				} else if (k == CellsSet.getServicesNames().size()) {
					CellsSet.colorMap("FR");
				}
			});
		}

		scroll.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.8);
		gridChart.prefWidthProperty().bind(scroll.widthProperty());
	}

	@FXML
	public void selectoutPut() {
		File selectedDirectory = PathTools.selectFolder(Paths.getProjectPath() + "\\output");

		if (selectedDirectory != null) {
			outputpath = selectedDirectory.getAbsolutePath();

			ArrayList<String> yearList = new ArrayList<>();
			PathTools.findAllFiles(outputpath).forEach(str -> {
				String tmp = new File(str).getName();
				if (tmp.contains("-Cell-"))
					yearList.add(tmp.replace(".csv", "").replace("-Cell-", "").replace(Paths.getScenario(), ""));
			});
			yearChoice.getItems().addAll(yearList);
			newOutPut(yearChoice.getValue());
			Graphs(gridChart);
		}
	}

	@FXML
	void selecserivce() {
		if (!colorbox.isShowing()) {
			VBox g = new VBox();
			g.getChildren().addAll(radioColor);
			colorbox.creatwindows("Display Services and AFT distribution", g);
		}

	}

	@FXML
	public void yearChoice() {
		Paths.setCurrentYear((int) Tools.sToD(yearChoice.getValue()));

		if (outputpath.length() > 0) {
			newOutPut(Paths.getCurrentYear() + "");
		}
	}

	void newOutPut(String year) {
		M.servicesAndOwneroutPut(year, outputpath);

		for (int i = 0; i < radioColor.length; i++) {
			if (radioColor[i].isSelected()) {
				radioColor[i].fire();
				radioColor[i].setSelected(true);
			}
		}
	}

	void Graphs(GridPane gridPane) {
		gridPane.getChildren().clear();
		ArrayList<LineChart<Number, Number>> lineChart = new ArrayList<>();
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		HashMap<String, ArrayList<String>> reder = FileReder
				.ReadAsaHash(PathTools.fileFilter(outputpath, "-AggregateServiceDemand.csv").get(0));

		ArrayList<HashMap<String, double[]>> has = new ArrayList<>();
		CellsSet.getServicesNames().forEach(servicename -> {
			HashMap<String, double[]> ha = new HashMap<>();
			reder.forEach((name, value) -> {
				double[] tmp = new double[value.size()];
				for (int i = 0; i < value.size(); i++) {
					tmp[i] = Tools.sToD(value.get(i));
				}
				if (name.contains(servicename)) {
					ha.put(name, tmp);
				}
			});
			has.add(ha);
			lineChart.add(
					new LineChart<>(new NumberAxis(Paths.getStartYear(), Paths.getEndtYear(), 5), new NumberAxis()));
		});
		// this is for creating the chart for AFTs aggregationComoposition  // will move tho output analyse
		//has.add(updatComposition(outputpath, "-AggregateAFTComposition.csv"));
		//lineChart.add(new LineChart<>(new NumberAxis(Paths.getStartYear(), Paths.getEndtYear(), 5), new NumberAxis()));
		int j = 0, k = 0;
		for (int i = 0; i < has.size(); i++) {

			HashMap<String, double[]> data = has.get(i);
			LineChart<Number, Number> Ch = lineChart.get(i);

			new LineChartTools().lineChart(M, (Pane) Ch.getParent(), Ch, data);
			// this for coloring the Chart by the AFTs color after the creation of the chart
//			if (i == has.size() - 1) {
//				Ch.setCreateSymbols(false);
//				for (int k2 = 0; k2 < Ch.getData().size(); k2++) {
//					Manager a = M.AFtsSet.getAftHash().get(Ch.getData().get(k2).getName());
//					Ch.getData().get(k2).getNode().lookup(".chart-series-line")
//							.setStyle("-fx-stroke: " + ColorsTools.getStringColor(a.getColor()) + ";");
//				}
//
//				new LineChartTools().labelcolor(M, Ch);
//
//			}
			gridPane.add(Tools.vBox(Ch), j++, k);
			MousePressed.mouseControle((Pane) Ch.getParent(), Ch);
			if (j % 3 == 0) {
				k++;
				j = 0;
			}

			//////
			String ItemName = "Save as CSV";
			Consumer<String> action = x -> {
				SaveAs.exportLineChartDataToCSV(Ch);
			};
			HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
			othersMenuItems.put(ItemName, action);
			MousePressed.mouseControle((Pane)Ch.getParent(), Ch,othersMenuItems);
			//////
		}
	}

	HashMap<String, double[]> updatComposition(String path, String nameFile) {
		HashMap<String, ArrayList<String>> reder = FileReder.ReadAsaHash(PathTools.fileFilter(path, nameFile).get(0));
		HashMap<String, double[]> has = new HashMap<>();

		reder.forEach((name, value) -> {
			double[] tmp = new double[value.size()];
			for (int i = 0; i < value.size(); i++) {
				tmp[i] = Tools.sToD(value.get(i));
			}
			has.put(name, tmp);

		});
		return has;
	}
}
