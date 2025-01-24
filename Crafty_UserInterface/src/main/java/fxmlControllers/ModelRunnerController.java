package fxmlControllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ibm.icu.impl.data.ResourceReader;

import dataLoader.AFTsLoader;
import dataLoader.PathsLoader;
import dataLoader.ServiceSet;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import main.Config;
import main.ConfigLoader;
import model.CellsSet;
import model.ModelRunner;
import model.RegionClassifier;
import model.Service;
import output.Listener;
import utils.analysis.CustomLogger;
import utils.filesTools.PathTools;
import utils.filesTools.SaveAs;
import utils.graphicalTools.ColorsTools;
import utils.graphicalTools.LineChartTools;
import utils.graphicalTools.MousePressed;
import utils.graphicalTools.NewWindow;
import utils.graphicalTools.Tools;
import javafx.scene.layout.GridPane;

public class ModelRunnerController {
	@FXML
	private VBox vbox;
	@FXML
	private Label tickTxt;
	@FXML
	private Button oneStep;
	@FXML
	private Button run;
	@FXML
	private Button pause;
	@FXML
	private Button stop;
	@FXML
	private GridPane gridPaneLinnChart;
	@FXML
	private ScrollPane scroll;

	public static ModelRunner runner;

	Timeline timeline;

	public static AtomicInteger tick;
	ArrayList<LineChart<Number, Number>> lineChart;

	RadioButton[] radioColor;
	NewWindow colorbox = new NewWindow();

	private boolean startRunin = true;
	private static final CustomLogger LOGGER = new CustomLogger(ModelRunnerController.class);

	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		init();
		tickTxt.setText(tick.toString());
		lineChart = new ArrayList<>();

		Collections.synchronizedList(lineChart);
		ConfigLoader.config.output_folder_name = PathsLoader.getScenario();
		initilaseChart(lineChart);
		initialzeRadioColorBox();
		scroll.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.8);

		initializeGridpane(2);
		gridPaneLinnChart.prefWidthProperty().bind(scroll.widthProperty());
	}

	public static void init() {
		runner = new ModelRunner();
		ModelRunner.setup();
		tick = new AtomicInteger(PathsLoader.getStartYear());
	}

	void initializeGridpane(int colmunNBR) {
		int j = 0, k = 0;
		for (int m = 0; m < lineChart.size(); m++) {
			gridPaneLinnChart.add(Tools.hBox(lineChart.get(m)), j++, k);
			if (j % colmunNBR == 0) {
				k++;
				j = 0;
			}
		}
	}

	void initialzeRadioColorBox() {
		radioColor = new RadioButton[ServiceSet.getServicesList().size() + 1];
		radioColor[radioColor.length - 1] = new RadioButton("FR");
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			radioColor[i] = new RadioButton(ServiceSet.getServicesList().get(i));
		}
		for (int i = 0; i < radioColor.length; i++) {
			int m = i;
			radioColor[i].setOnAction(e -> {
				runner.colorDisplay = radioColor[m].getText();
				CellsSet.colorMap(radioColor[m].getText());
				for (int I = 0; I < radioColor.length; I++) {
					if (I != m) {
						radioColor[I].setSelected(false);
					}
				}
			});
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
	public void oneStep() {
		LOGGER.info("------------------- Start of Tick  |" + tick.get() + "| -------------------");
		PathsLoader.setCurrentYear(tick.get());
		runner.step();
		tickTxt.setText(tick.toString());
		updateSupplyDemandLineChart();
		tick.getAndIncrement();
	}

	private void updateSupplyDemandLineChart() {
		if (Config.chartSynchronisation
				&& ((PathsLoader.getCurrentYear() - PathsLoader.getStartYear()) % Config.chartSynchronisationGap == 0
						|| PathsLoader.getCurrentYear() == PathsLoader.getEndtYear())) {
			AtomicInteger m = new AtomicInteger();
			ServiceSet.getServicesList().forEach(service -> {
				lineChart.get(m.get()).getData().get(0).getData().add(new XYChart.Data<>(tick.get(),
						ServiceSet.worldService.get(service).getDemands().get(tick.get())));
				lineChart.get(m.get()).getData().get(1).getData()
						.add(new XYChart.Data<>(tick.get(), runner.totalSupply.get(service)));
				m.getAndIncrement();
			});
			ObservableList<Series<Number, Number>> observable = lineChart.get(lineChart.size() - 1).getData();
			List<String> listofNames = observable.stream().map(Series::getName).collect(Collectors.toList());
			AFTsLoader.hashAgentNbr.forEach((name, value) -> {
				observable.get(listofNames.indexOf(name)).getData().add(new XYChart.Data<>(tick.get(), value));
			});
		}
	}

	@FXML
	public void run() {
		run.setDisable(true);
		simulationFolderName();
		if (ConfigLoader.config.export_LOGGER) {
			CustomLogger
					.configureLogger(Paths.get(ConfigLoader.config.output_folder_name + File.separator + "LOGGER.txt"));
		}
		if (startRunin || !ConfigLoader.config.generate_csv_files) {
			demandEquilibrium();
			scheduleIteravitveTicks(Duration.millis(1000));
		}
	}

	public static void demandEquilibrium() {
		if (ConfigLoader.config.initial_demand_supply_equilibrium) {
			RegionalDemandEquilibrium_calculation();
			ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
				RegionalRunner.R.getServicesHash().forEach((ns, s) -> {
					s.getDemands().forEach((year, v) -> {
						s.getDemands().put(year, v / s.getCalibration_Factor());
					});
				});
			});
			initialTotalDSEquilibriumListrner();
			RegionClassifier.aggregateDemandToWorldServiceDemand();
		}
	}

	private static void RegionalDemandEquilibrium_calculation() {
		ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
			RegionalRunner.initialDSEquilibriumFactorCalculation();
		});
	}

	public static void initialTotalDSEquilibriumListrner() {
		ServiceSet.worldService.forEach((serviceName, service) -> {
			ModelRunner.regionsModelRunner.values().forEach(RegionalRunner -> {
				Service s = RegionalRunner.R.getServicesHash().get(serviceName);
				int i = ServiceSet.getServicesList().indexOf(serviceName);
				RegionalRunner.listner.DSEquilibriumListener[i + 1][0] = serviceName;
				RegionalRunner.listner.DSEquilibriumListener[i + 1][1] = s.getCalibration_Factor() + "";
			});
		});
	}

	private void displayRunAsOutput() {
		OutPuterController.isCurrentResult = true;
		OutPutTabController.getInstance().createNewTab("Current simulation");
		TabPane tabpane = TabPaneController.getInstance().getTabpane();
		tabpane.getSelectionModel().select(tabpane.getTabs().size() - 1);
		tabpane.getTabs().stream().filter(tab -> tab.getText().equals("Model OutPut")) // Match tab by name
				.findFirst() // Get the first matching tab (if any)
				.ifPresent(tab -> tabpane.getSelectionModel().select(tab)); // Select the tab if found
	}

	private void scheduleIteravitveTicks(Duration delay) {
		if (PathsLoader.getCurrentYear() > PathsLoader.getEndtYear()) {
			// Stop if max iterations reached
			if (ConfigLoader.config.generate_csv_files)
				displayRunAsOutput();
			return;
		}
		// Stop the old timeline if it's running
		if (timeline != null) {
			timeline.stop();
		}
		// Create a new timeline for the next tick
		timeline = new Timeline(new KeyFrame(delay, event -> {
			long startTime = System.currentTimeMillis();
			// Perform the simulation update
			Platform.runLater(() -> {
				oneStep();
			});
			// Calculate the delay for the next tick to maintain the rhythm
			long delayForNextTick = Math.max(300, (System.currentTimeMillis() - startTime) / 3);
			// Schedule the next tick
			System.out.println("Tick=...." + PathsLoader.getCurrentYear());
			scheduleIteravitveTicks(Duration.millis(delayForNextTick));

		}));
		timeline.play();
	}

	@FXML
	public void pause() {
		timeline.stop();
		run.setDisable(false);
	}

	@FXML
	public void stop() {

		tick.set(PathsLoader.getStartYear());
		PathsLoader.setCurrentYear(PathsLoader.getStartYear());
		TabPaneController.cellsLoader.loadMap();
		CellsSet.colorMap();
		RegionClassifier.serviceupdater();

		try {
			timeline.stop();
		} catch (RuntimeException e) {
		}
		run.setDisable(false);

		gridPaneLinnChart.getChildren().clear();
		lineChart.clear();
		initilaseChart(lineChart);
		int j = 0, k = 0;
		for (int m = 0; m < lineChart.size(); m++) {
			gridPaneLinnChart.add(lineChart.get(m), j++, k);
			if (j % 3 == 0) {
				k++;
				j = 0;
			}
		}
	}

	void initilaseChart(ArrayList<LineChart<Number, Number>> lineChart) {
		ServiceSet.getServicesList().forEach(service -> {
			Series<Number, Number> s1 = new XYChart.Series<Number, Number>();
			Series<Number, Number> s2 = new XYChart.Series<Number, Number>();
			s1.setName("Demand " + service);
			s2.setName("Supply " + service);
			LineChart<Number, Number> l = new LineChart<>(
					new NumberAxis(PathsLoader.getStartYear(), PathsLoader.getEndtYear(), 5), new NumberAxis());
			l.getData().add(s1);
			l.getData().add(s2);
			LineChartTools.configurexAxis(l, PathsLoader.getStartYear(), PathsLoader.getEndtYear());
			lineChart.add(l);
			LineChartTools.addSeriesTooltips(l);

			String ItemName = "Save as CSV";
			Consumer<String> action = x -> {
				SaveAs.exportLineChartDataToCSV(l);
			};
			HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
			othersMenuItems.put(ItemName, action);

			MousePressed.mouseControle(vbox, l, othersMenuItems);
		});
		LineChart<Number, Number> l = new LineChart<>(
				new NumberAxis(PathsLoader.getStartYear(), PathsLoader.getEndtYear(), 5), new NumberAxis());
		lineChart.add(l);

		AFTsLoader.getAftHash().forEach((name, a) -> {
			Series<Number, Number> s = new XYChart.Series<Number, Number>();
			s.setName(name);
			l.getData().add(s);
			s.getNode().lookup(".chart-series-line")
					.setStyle("-fx-stroke: " + ColorsTools.getStringColor(a.getColor()) + ";");
		});
		l.setCreateSymbols(false);
		LineChartTools.addSeriesTooltips(l);
		MousePressed.mouseControle(vbox, l);
		LineChartTools.labelcolor(l);
	}

	public static String exportConfigurationFile() {
		String cofiguration = "";
		try {
			InputStream inputStream = ResourceReader.class.getResourceAsStream(""/* ConfigLoader.configPath */);
			cofiguration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cofiguration;
	}

	Alert simulationFolderName() {
		if (!ConfigLoader.config.generate_csv_files) {
			return null;
		}
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setHeaderText("Please enter OutPut folder name");
		String cofiguration = exportConfigurationFile();
		cofiguration = cofiguration + " \n " + "Add any comments \n ";
		TextField textField = new TextField();
		textField.setPromptText("Output_Folder_Name (if not specified, a default name will be created)");
		Text txt = new Text(PathsLoader.getProjectPath() + PathTools.asFolder("output") + PathsLoader.getScenario()
				+ File.separator + "...");
		TextArea textArea = new TextArea();
		textArea.setText(cofiguration);
		VBox v = new VBox(txt, textField, textArea);
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.setContent(v);
		Window window = alert.getDialogPane().getScene().getWindow();
		((Stage) window).setAlwaysOnTop(true);

		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				Listener.outputfolderPath(textField.getText());
				PathTools.writeFile(ConfigLoader.config.output_folder_name + File.separator + "readme.txt",
						textArea.getText(), false);
				startRunin = true;
			} else if (response == ButtonType.CANCEL) {
				startRunin = false;
				stop();
			}
		});
		return alert;
	}

}
