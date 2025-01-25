package de.cesr.crafty.controller.fxml;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.dataLoader.AFTsLoader;
import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.dataLoader.DemandModel;
import de.cesr.crafty.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.dataLoader.PathsLoader;
import de.cesr.crafty.dataLoader.ServiceSet;
import de.cesr.crafty.main.ConfigLoader;
import de.cesr.crafty.main.FxMain;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.model.ModelRunner;
import de.cesr.crafty.model.RegionClassifier;
import de.cesr.crafty.utils.file.PathTools;
import de.cesr.crafty.utils.graphical.ColorsTools;
import de.cesr.crafty.utils.graphical.LineChartTools;
import de.cesr.crafty.utils.graphical.Tools;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;

public class TabPaneController {

	@FXML
	private ChoiceBox<String> scenarioschoice;
	@FXML
	private ChoiceBox<String> yearchoice;
	@FXML
	private TabPane tabpane;
	@FXML
	private VBox mapBox;
	@FXML
	private Tab dataPane;
	@FXML
	CheckBox regionalBox;
//	@FXML
//	private TextArea consoleArea;

	public static CellsLoader cellsLoader = new CellsLoader();

	private boolean isNotInitialsation = false;

	private static TabPaneController instance;

	public TabPaneController() {
		instance = this;
	}

	public static TabPaneController getInstance() {
		return instance;
	}

	public TabPane getTabpane() {
		return tabpane;
	}

	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		mapBox.getChildren().add(FxMain.subScene);
		PathTools.writePathRecentProject("RecentProject.txt", "\n" + PathsLoader.getProjectPath());
		scenarioschoice.getItems().addAll(PathsLoader.getScenariosList());
		scenarioschoice.setValue(PathsLoader.getScenario());
		ArrayList<String> listYears = new ArrayList<>();
		for (int i = PathsLoader.getStartYear(); i < PathsLoader.getEndtYear(); i++) {
			listYears.add(i + "");
		}
		yearchoice.getItems().addAll(listYears);
		yearchoice.setValue(listYears.get(0));
		isNotInitialsation = true;
		tabpane.setPrefWidth(Screen.getPrimary().getBounds().getWidth() / 1.5);
//	    FxMain.subScene.setWidth(Screen.getPrimary().getBounds().getWidth()-tabpane.getWidth());
//		mapTitelPane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
//          if (isNowExpanded) {
//        	  FxMain.subScene.setWidth(Screen.getPrimary().getBounds().getWidth()-tabpane.getWidth());
//          } else {
//        	  FxMain.subScene.setWidth(0);
//          }
//      });
//		 GraphicConsol.start(consoleArea);

		regionalBox.setSelected(RegionClassifier.regionalization);
		// regionalBox.setDisable(ServiceSet.isRegionalServicesExisted());
	}

	@FXML
	public void regionalization() {
		RegionClassifier.regionalization = regionalBox.isSelected();
		ConfigLoader.config.regionalization = regionalBox.isSelected();
		RegionClassifier.initialation();
		ModelRunner.setup();
		AFTsLoader.hashAgentNbrRegions();

		AtomicInteger nbr = new AtomicInteger();
		RegionClassifier.regions.values().forEach(R -> {
			Color color = ColorsTools.colorlist(nbr.getAndIncrement());
			R.getCells().values().forEach(c -> {
				c.ColorP(color);
			});
		});
		CellsSet.gc.drawImage(CellsSet.writableImage, 0, 0);
		// regionalBox.setSelected(CellsLoader.regionsNamesSet.size() > 1);
	}

	@FXML
	public void scenarioschoice() {
		if (isNotInitialsation) {
			cellsLoader.loadMap();
			PathsLoader.setScenario(scenarioschoice.getValue());
			// DemandModel.updateDemand();// =
			// CsvTools.csvReader(Path.fileFilter(Path.scenario, "demand").get(0));
			ServiceSet.initialseServices();
			RegionClassifier.serviceupdater();
			ModelRunner.listner.initializeListeners();
			LineChart<Number, Number> chart = SpatialDataController.getInstance().getDemandsChart();
			new LineChartTools().lineChart((Pane) chart.getParent(), chart, DemandModel.serialisationWorldDemand());
			cellsLoader.AFtsSet.updateAFTsForsenario();
			yearchoice();
			MaskRestrictionDataLoader.allMaskAndRistrictionUpdate();
			MasksPaneController.getInstance().clear(new ActionEvent());
			MasksPaneController.initialiseMask();
		}
	}

	@FXML
	public void yearchoice() {
		if (isNotInitialsation) {
			if (yearchoice.getValue() != null) {
				PathsLoader.setCurrentYear((int) Tools.sToD(yearchoice.getValue()));
				cellsLoader.updateCapitals(PathsLoader.getCurrentYear());
				AFTsLoader.updateAFTs();
				if (dataPane.isSelected()) {
					for (int i = 0; i < CellsLoader.getCapitalsList().size() + 1; i++) {
						if (SpatialDataController.radioColor[i].isSelected()) {
							if (i < CellsLoader.getCapitalsList().size()) {
								CellsSet.colorMap(CellsLoader.getCapitalsList().get(i));
								SpatialDataController.getInstance().histogrameCapitals(
										PathsLoader.getCurrentYear() + "", CellsLoader.getCapitalsList().get(i));
							} else {
								CellsSet.colorMap("AFT");
							}
						}
					}
				}
			}
		}
	}
}
