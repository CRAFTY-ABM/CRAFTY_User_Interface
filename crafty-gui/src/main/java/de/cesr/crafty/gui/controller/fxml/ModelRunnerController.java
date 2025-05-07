package de.cesr.crafty.gui.controller.fxml;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cesr.crafty.core.cli.Config;
import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.gui.canvasFx.CellsCanvas;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.gui.utils.graphical.ColorsTools;
import de.cesr.crafty.gui.utils.graphical.LineChartTools;
import de.cesr.crafty.gui.utils.graphical.MousePressed;
import de.cesr.crafty.gui.utils.graphical.NewWindow;
import de.cesr.crafty.gui.utils.graphical.Tools;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.output.Listener;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.gui.utils.graphical.SaveAs;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.scene.layout.GridPane;

public class ModelRunnerController {
	@FXML
	private VBox TopBox;

	@FXML
	private Label tickTxt;
	@FXML
	private Button oneStep;
	@FXML
	private Button run;
	@FXML
	private Button stop;
	@FXML
	private GridPane gridPaneLinnChart;
	@FXML
	private ScrollPane scroll;

	public String colorDisplay = "AFT";
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
		ConfigLoader.config.output_folder_name = ProjectLoader.getScenario();
		initilaseChart(lineChart);
		initialzeRadioColorBox();
		initializeGridpane(3);
		Tools.forceResisingWidth(TopBox);
		Tools.forceResisingHeight(1, scroll);

	}

	public static void init() {
		runner = new ModelRunner();
		ModelRunner.setup();
		tick = new AtomicInteger(ProjectLoader.getStartYear());
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
		radioColor[radioColor.length - 1] = new RadioButton("AFT");
		for (int i = 0; i < ServiceSet.getServicesList().size(); i++) {
			radioColor[i] = new RadioButton(ServiceSet.getServicesList().get(i));
		}
		for (int i = 0; i < radioColor.length; i++) {
			int m = i;
			radioColor[i].setOnAction(e -> {
				colorDisplay = radioColor[m].getText();
				CellsCanvas.colorMap(radioColor[m].getText());
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
		ProjectLoader.setCurrentYear(tick.get());
		runner.step();
		mapSynchronisation();
		tickTxt.setText(tick.toString());
		updateSupplyDemandLineChart();
		tick.getAndIncrement();
	}

	private void mapSynchronisation() {
		if (Config.mapSynchronisation
				&& ((ProjectLoader.getCurrentYear() - ProjectLoader.getStartYear()) % Config.mapSynchronisationGap == 0
						|| ProjectLoader.getCurrentYear() == ProjectLoader.getEndtYear())) {
			CellsCanvas.colorMap(colorDisplay);
		}
	}

	private void updateSupplyDemandLineChart() {
		if (Config.chartSynchronisation && ((ProjectLoader.getCurrentYear() - ProjectLoader.getStartYear())
				% Config.chartSynchronisationGap == 0
				|| ProjectLoader.getCurrentYear() == ProjectLoader.getEndtYear())) {
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
		if (startRunin || !ConfigLoader.config.generate_output_files) {
			ModelRunner.demandEquilibrium();
			scheduleIteravitveTicks(Duration.millis(1000));
		}
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
		if (ProjectLoader.getCurrentYear() > ProjectLoader.getEndtYear()) {
			// Stop if max iterations reached
			if (ConfigLoader.config.generate_output_files)
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
			System.out.println("Tick=...." + ProjectLoader.getCurrentYear());
			scheduleIteravitveTicks(Duration.millis(delayForNextTick));

		}));
		timeline.play();
	}

	@FXML
	public void stop() {

		tick.set(ProjectLoader.getStartYear());
		ProjectLoader.setCurrentYear(ProjectLoader.getStartYear());
		ProjectLoader.cellsSet.loadMap();
		CellsCanvas.colorMap();
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
					new NumberAxis(ProjectLoader.getStartYear(), ProjectLoader.getEndtYear(), 5), new NumberAxis());
			l.getData().add(s1);
			l.getData().add(s2);
			LineChartTools.configurexAxis(l, ProjectLoader.getStartYear(), ProjectLoader.getEndtYear());
			lineChart.add(l);
			LineChartTools.addSeriesTooltips(l);

			String ItemName = "Save as CSV";
			Consumer<String> action = x -> {
				SaveAs.exportLineChartDataToCSV(l);
			};
			HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
			othersMenuItems.put(ItemName, action);

			MousePressed.mouseControle(TopBox, l, othersMenuItems);
		});
		LineChart<Number, Number> l = new LineChart<>(
				new NumberAxis(ProjectLoader.getStartYear(), ProjectLoader.getEndtYear(), 5), new NumberAxis());
		lineChart.add(l);

		AFTsLoader.getAftHash().forEach((name, a) -> {
			Series<Number, Number> s = new XYChart.Series<Number, Number>();
			s.setName(name);
			l.getData().add(s);
			s.getNode().lookup(".chart-series-line")
					.setStyle("-fx-stroke: " + ColorsTools.getStringColor(Color.web(a.getColor())) + ";");
		});
		l.setCreateSymbols(false);
		LineChartTools.addSeriesTooltips(l);
		MousePressed.mouseControle(TopBox, l);
		LineChartTools.labelcolor(l);
	}

	Alert simulationFolderName() {
		
		if (!ConfigLoader.config.generate_output_files) {
			return null;
		}
		ConfigLoader.config.generate_map_output_files=true;
		Listener.initializeListYears();
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setHeaderText("Please enter OutPut folder name");
		String cofiguration = Listener.exportConfigurationFile();
		cofiguration = cofiguration + " \n " + "Add any comments \n ";
		TextField textField = new TextField();
		textField.setPromptText("Output_Folder_Name (if not specified, a default name will be created)");
		Text txt = new Text(ProjectLoader.getProjectPath() + PathTools.asFolder("output") + ProjectLoader.getScenario()
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
				Listener.outputfolderPath(null, textField.getText());
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
