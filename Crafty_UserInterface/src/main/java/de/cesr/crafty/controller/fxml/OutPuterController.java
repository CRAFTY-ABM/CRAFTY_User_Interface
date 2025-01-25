package de.cesr.crafty.controller.fxml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.main.ConfigLoader;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.model.Manager;
import de.cesr.crafty.utils.analysis.CustomLogger;
import de.cesr.crafty.utils.file.PathTools;
import de.cesr.crafty.utils.file.ReaderFile;
import de.cesr.crafty.utils.file.SaveAs;
import de.cesr.crafty.utils.graphical.ColorsTools;
import de.cesr.crafty.utils.graphical.ImageExporter;
import de.cesr.crafty.utils.graphical.ImagesToPDF;
import de.cesr.crafty.utils.graphical.LineChartTools;
import de.cesr.crafty.utils.graphical.MousePressed;
import de.cesr.crafty.utils.graphical.SankeyPlotGraph;
import de.cesr.crafty.utils.graphical.Tools;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;

public class OutPuterController {

	@FXML
	private Button saveAllFilAsPNG;
	@FXML
	private ChoiceBox<String> yearChoice;
	@FXML
	private ChoiceBox<String> sankeyBox;
	@FXML
	private GridPane gridChart;
	@FXML
	private ScrollPane scroll;
	@FXML
	private Button selecserivce;
	@FXML
	private VBox borderPane;
	@FXML
	private ChoiceBox<String> regionsBox;
	@FXML
	private Tab regionTab;
	@FXML
	private ScrollPane scrollRegions;
	@FXML
	private GridPane regionalGridChart;

	ArrayList<CheckBox> radioListOfAFTs = new ArrayList<>();
	private static final CustomLogger LOGGER = new CustomLogger(OutPuterController.class);

	public static boolean isCurrentResult = false;
	public static Path outputpath;

	public void initialize() {
		System.out.println("Initialize " + getClass().getSimpleName());

		selectoutPut();
		scroll.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.8);
		gridChart.prefWidthProperty().bind(scroll.widthProperty());
		scrollRegions.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.8);
		regionalGridChart.prefWidthProperty().bind(scrollRegions.widthProperty());
		isCurrentResult = false;
		initialiseregionBox();
		if (regionsBox.getItems().size() > 0) {
			regionsBox.setValue(regionsBox.getItems().get(0));
		}
	}

	void initialiseregionBox() {
		List<File> folders = PathTools.detectFolders(outputpath.toString());
		folders.forEach(e -> {
			if (e.getName().contains("region_")) {
				regionsBox.getItems().addAll(e.getName());
			}
		});
		if (regionsBox.getItems().size() == 0) {
			regionTab.setDisable(true);
			regionTab.getTooltip().setText("Output Files by regions are Not Available For This Simulation ");
		}
	}

	@FXML
	public void regionsBoxAction() {
		Graphs(regionalGridChart, regionsBox.getValue() + "-AggregateServiceDemand.csv",
				regionsBox.getValue() + "-AggregateAFTComposition.csv");

	}

	HashMap<Manager, HashMap<Manager, Integer>> stateToHashSankey(String lastYear) {
		HashMap<String, String> copyfirstYearHash = new HashMap<>();
		CellsLoader.hashCell.forEach((coor, c) -> {
			if (c.getOwner() != null)
				copyfirstYearHash.put(coor, c.getOwner().getLabel());
		});
		// Find file in the correct folder and update hashCell

		yearChoice.getItems().stream().filter(str -> str.contains(lastYear)).findFirst().ifPresent(this::newOutPut);

		HashMap<String, Integer> h = new HashMap<>();
		copyfirstYearHash.forEach((coor, label) -> {
			Manager owner = CellsLoader.hashCell.get(coor).getOwner();
			if (owner != null) {
				h.merge(label + "," + owner.getLabel(), 1, Integer::sum);
			}
		});

		copyfirstYearHash.clear();
		HashMap<Manager, HashMap<Manager, Integer>> hash = new HashMap<>();

		AFTsLoader.getActivateAFTsHash().keySet().forEach(label -> {
			HashMap<Manager, Integer> h1 = new HashMap<>();
			h.forEach((k, v) -> {
				String[] vect = k.split(",");
				if (vect.length == 2) {
					if (vect[0].equals(label)) {
						Manager reciver = AFTsLoader.getActivateAFTsHash().get(vect[1]);
						if (reciver != null)
							h1.put(reciver, v);
					}
				}
			});
			hash.put(AFTsLoader.getActivateAFTsHash().get(label), h1);
		});
		return hash;
	}

	public void selectoutPut() {
		if (!isCurrentResult) {
			File selectedDirectory = PathTools.selectFolder(PathsLoader.getProjectPath() + File.separator + "output");
			if (selectedDirectory != null) {
				outputpath = Paths.get(selectedDirectory.getAbsolutePath());
			} else {
				outputpath = null;
			}
		} else {
			if (ConfigLoader.config.output_folder_name != null) {
				outputpath = Paths.get(ConfigLoader.config.output_folder_name);
				isCurrentResult = false;
			}
		}
		if (outputpath != null) {
			ArrayList<String> yearList = new ArrayList<>();
			PathTools.findAllFiles(outputpath).forEach(str -> {
				File file = str.toFile();
				String tmp = new File(file.getParent()).getName() + File.separator + file.getName();

				if (tmp.contains("-Cell-"))
					yearList.add(tmp);
			});
			LOGGER.info("output files List: " + yearList);
			yearChoice.getItems().addAll(yearList);
			yearChoice.setValue(yearList.get(0));
			sankeyBox.getItems().addAll(yearList);
			sankeyBox.setValue(yearList.get(yearList.size() - 1));
			OutPutTabController.radioColor[OutPutTabController.radioColor.length - 1].setSelected(true);
			// newOutPut(yearChoice.getValue());
			Graphs(gridChart, "Total-AggregateServiceDemand.csv", "Total-AggregateAFTComposition.csv");
		}
	}

	@FXML
	public void saveAllFilAsPNGAction() {
		for (int i = 0; i < OutPutTabController.radioColor.length; i++) {
			int ii = i;
			if (OutPutTabController.radioColor[i].getText().contains("Agent")) {
				String newfolder = PathTools
						.makeDirectory(outputpath + File.separator + OutPutTabController.radioColor[ii].getText());
				yearChoice.getItems().forEach(filepath -> {
					TabPaneController.cellsLoader.servicesAndOwneroutPut(filepath, outputpath.toString());
					OutPutTabController.radioColor[ii].fire();
					String fileyear = new File(filepath).getName().replace(".csv", "").replace("-Cell-", "");
					for (String scenario : PathsLoader.getScenariosList()) {
						fileyear = fileyear.replace(scenario, "");
					}
					ImageExporter.NodeToImage(CellsSet.getCanvas(), newfolder + File.separator + fileyear + ".PNG");
				});
			}
		}
		String newfolder = PathTools.makeDirectory(outputpath + File.separator + "Charts");

		// First, create a snapshot of the children with their positions
		List<Node> children = new ArrayList<>(gridChart.getChildren());
		List<Integer> rowIndexes = new ArrayList<>();
		List<Integer> colIndexes = new ArrayList<>();

		for (Node child : children) {
			rowIndexes.add(GridPane.getRowIndex(child));
			colIndexes.add(GridPane.getColumnIndex(child));
		}

		// Clear children to prevent ConcurrentModificationException
		gridChart.getChildren().clear();

		// Process each child, create an image, and then re-add to the original position
		for (int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			VBox container = (VBox) child;
			@SuppressWarnings("unchecked")
			LineChart<Number, Number> ch = (LineChart<Number, Number>) container.getChildren().iterator().next();
			double w = ch.getWidth();
			double h = ch.getHeight();
			ch.setPrefSize(1000, 1000);

			Group rootPane = new Group();
			rootPane.getChildren().add(child); // Temporarily add to another group

			ImageExporter.NodeToImage(rootPane, newfolder + File.separator + ch.getTitle() + ".PNG");

			// Now re-add the child to the grid at its original position
			GridPane.setRowIndex(child, rowIndexes.get(i));
			GridPane.setColumnIndex(child, colIndexes.get(i));
			gridChart.getChildren().add(child);
			ch.setPrefSize(w, h);
		}

//		gridChart.getChildren().forEach(chart -> {
//			VBox container = (VBox) chart;
//			@SuppressWarnings("unchecked")
//			LineChart<Number, Number> ch = (LineChart<Number, Number>) container.getChildren().iterator().next();
//			ch.setPrefSize(1000, 1000);
//			StackPane rootPane = new StackPane();
//			rootPane.getChildren().add(chart);
//			ImageExporter.NodeToImage(rootPane, newfolder + File.separator  + ch.getTitle() + ".PNG");
//
//		});
		List<File> foders = PathTools.detectFolders(outputpath.toString());
		for (File folder : foders) {
			ImagesToPDF.createPDFWithImages(folder.getAbsolutePath(), folder.getName() + ".pdf", 4, 4);
		}
	}

	@FXML
	public void yearChoice() {
		if (Files.exists(outputpath)) {
			newOutPut(yearChoice.getValue());
		}
	}

	@FXML
	public void sankeyPlot() {
		if (Files.exists(outputpath)) {
			Text txt2 = Tools.text(new File(yearChoice.getValue()).getName(), Color.BLUE);
			HashMap<Manager, HashMap<Manager, Integer>> h = stateToHashSankey(sankeyBox.getValue());
			Set<Manager> setManagers = new HashSet<>();
			VBox boxOfAftRadios = new VBox();

			AFTsLoader.getActivateAFTsHash().keySet().forEach(n -> {
				CheckBox radio = new CheckBox(n);
				radioListOfAFTs.add(radio);
				boxOfAftRadios.getChildren().add(radio);
				radio.setSelected(true);
				setManagers.add(AFTsLoader.getActivateAFTsHash().get(radio.getText()));
				radio.setOnAction(e -> {
					if (radio.isSelected()) {
						setManagers.add(AFTsLoader.getActivateAFTsHash().get(radio.getText()));
					} else {
						if (setManagers.size() > 1) {
							setManagers.remove(AFTsLoader.getActivateAFTsHash().get(radio.getText()));
						} else {
							radio.setSelected(true);
						}
					}
					updateSankeyPlot(txt2, boxOfAftRadios, h, setManagers);
				});
			});
			updateSankeyPlot(txt2, boxOfAftRadios, h, setManagers);
		}
	}

	private void updateSankeyPlot(Text txt2, VBox boxOfAftRadios, HashMap<Manager, HashMap<Manager, Integer>> h,
			Set<Manager> setManagers) {
		Text txt = new Text("Create a Sankey diagram for  ");
		Text txt3 = new Text("  To  ");
		Text txt4 = Tools.text(new File(sankeyBox.getValue()).getName(), Color.RED);
		SankeyPlotGraph.AFtsToSankeyPlot(h, setManagers);
		MousePressed.mouseControle(borderPane, SankeyPlotGraph.sankey);
		borderPane.getChildren().clear();
		borderPane.getChildren().addAll(Tools.hBox(txt, txt2, txt3, txt4),
				Tools.hBox(boxOfAftRadios, SankeyPlotGraph.sankey));
		SankeyPlotGraph.sankey.setPrefWidth(scroll.getPrefWidth());
	}

	void newOutPut(String year) {
		TabPaneController.cellsLoader.servicesAndOwneroutPut(year, outputpath.toString());
		for (int i = 0; i < OutPutTabController.radioColor.length; i++) {
			if (OutPutTabController.radioColor[i].isSelected()) {
				OutPutTabController.radioColor[i].fire();
				OutPutTabController.radioColor[i].setSelected(true);
			}
		}

	}

	void Graphs(GridPane gridPane, String serviceDemand, String aftComposition) {
		gridPane.getChildren().clear();
		ArrayList<LineChart<Number, Number>> lineChart = new ArrayList<>();
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		ArrayList<Path> servicespath = PathTools.fileFilter(outputpath.toString(), serviceDemand);
		HashMap<String, ArrayList<String>> reder = ReaderFile.ReadAsaHash(servicespath.get(0));

		ArrayList<HashMap<String, ArrayList<Double>>> has = new ArrayList<>();
		ServiceSet.getServicesList().forEach(serviceName -> {
			HashMap<String, ArrayList<Double>> ha = new HashMap<>();
			reder.forEach((name, value) -> {
				ArrayList<Double> tmp = new ArrayList<>();
				for (int i = 0; i < value.size(); i++) {
					tmp.add(Tools.sToD(value.get(i)));
				}
				if (name.contains(serviceName)) {
					ha.put(name, tmp);
				}
			});
			if (ha != null) {
				has.add(ha);
			}
			LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
			chart.setTitle(serviceName);
			lineChart.add(chart);
		});

		has.add(updatComposition(outputpath.toString(), aftComposition));
		LineChart<Number, Number> chart = new LineChart<>(
				new NumberAxis(PathsLoader.getStartYear(), PathsLoader.getEndtYear(), 5), new NumberAxis());
		chart.setTitle("Land use trends");
		lineChart.add(chart);
		int j = 0, k = 0;
		for (int i = 0; i < has.size(); i++) {
			LineChart<Number, Number> Ch = lineChart.get(i);
			new LineChartTools().lineChart((Pane) Ch.getParent(), Ch, has.get(i));
			// this for coloring the Chart by the AFTs color after the creation of the chart
			if (i == has.size() - 1) {
				coloringChartByAFts(Ch);
			}
			gridPane.add(Tools.vBox(Ch), j++, k);
			MousePressed.mouseControle((Pane) Ch.getParent(), Ch);
			if (j % 3 == 0) {
				k++;
				j = 0;
			}

			String ItemName = "Save as CSV";
			Consumer<String> action = x -> {
				SaveAs.exportLineChartDataToCSV(Ch);
			};
			HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
			othersMenuItems.put(ItemName, action);
			MousePressed.mouseControle((Pane) Ch.getParent(), Ch, othersMenuItems);
		}
	}

	private void coloringChartByAFts(LineChart<Number, Number> Ch) {
		Ch.setCreateSymbols(false);
		for (int k2 = 0; k2 < Ch.getData().size(); k2++) {
			Manager a = AFTsLoader.getAftHash().get(Ch.getData().get(k2).getName());
			Ch.getData().get(k2).getNode().lookup(".chart-series-line")
					.setStyle("-fx-stroke: " + ColorsTools.getStringColor(a.getColor()) + ";");
		}
		LineChartTools.labelcolor(Ch);
	}

	HashMap<String, ArrayList<Double>> updatComposition(String path, String nameFile) {

		HashMap<String, ArrayList<String>> reder = ReaderFile.ReadAsaHash(PathTools.fileFilter(path, nameFile).get(0));
		HashMap<String, ArrayList<Double>> has = new HashMap<>();

		reder.forEach((name, value) -> {
			if (AFTsLoader.getAftHash().keySet().contains(name)) {
				ArrayList<Double> tmp = new ArrayList<>();
				for (int i = 0; i < value.size(); i++) {
					tmp.add(Tools.sToD(value.get(i)));
				}
				has.put(name, tmp);
			}
		});
		return has;
	}

	// Detect if there is regional output
	// if yes create a set of countries or click on region to acces
}
