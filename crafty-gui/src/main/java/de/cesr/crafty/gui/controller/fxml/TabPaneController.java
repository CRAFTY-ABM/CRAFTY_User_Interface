package de.cesr.crafty.gui.controller.fxml;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.DemandModel;
import de.cesr.crafty.core.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.gui.canvasFx.CellsCanvas;
import de.cesr.crafty.gui.utils.graphical.ColorsTools;
import de.cesr.crafty.gui.utils.graphical.LineChartTools;
import de.cesr.crafty.core.model.ModelRunner;
import de.cesr.crafty.core.model.RegionClassifier;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;

public class TabPaneController {

	@FXML
	private HBox topLevelBox;
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

	// public static CellsLoader cellsLoader = new CellsLoader();

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
		mapBox.getChildren().add(CellsCanvas.subScene);

		PathTools.writePathRecentProject("RecentProject.txt", "\n" + ProjectLoader.getProjectPath());
		scenarioschoice.getItems().addAll(ProjectLoader.getScenariosList());
		scenarioschoice.setValue(ProjectLoader.getScenario());
		ArrayList<String> listYears = new ArrayList<>();
		for (int i = ProjectLoader.getStartYear(); i < ProjectLoader.getEndtYear(); i++) {
			listYears.add(i + "");
		}
		yearchoice.getItems().addAll(listYears);
		yearchoice.setValue(listYears.get(0));
		isNotInitialsation = true;
		// tabpane.setPrefWidth(FxMain.topLevelBox.getWidth()/2);
		regionalBox.setSelected(RegionClassifier.regionalization);
		// regionalBox.setDisable(ServiceSet.isRegionalServicesExisted());
		MenuBarController.getInstance().getDataAnalysis().setDisable(false);
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
				CellsCanvas.ColorP(c, color);
			});
		});
		CellsCanvas.gc.drawImage(CellsCanvas.writableImage, 0, 0);
		// regionalBox.setSelected(CellsLoader.regionsNamesSet.size() > 1);
	}

	@FXML
	public void scenarioschoice() {
		if (isNotInitialsation) {
			ProjectLoader.cellsSet.loadMap();
			ProjectLoader.setScenario(scenarioschoice.getValue());
			// DemandModel.updateDemand();// =
			// CsvTools.csvReader(Path.fileFilter(Path.scenario, "demand").get(0));
			ServiceSet.initialseServices();
			RegionClassifier.serviceupdater();
			ModelRunner.listner.initializeListeners();
			LineChart<Number, Number> chart = ServicesController.getInstance().getDemandsChart();
			new LineChartTools().lineChart((Pane) chart.getParent(), chart, DemandModel.serialisationWorldDemand());
			ProjectLoader.cellsSet.AFtsSet.updateAFTsForsenario();
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
				ProjectLoader.setCurrentYear((int) Utils.sToD(yearchoice.getValue()));
				ProjectLoader.cellsSet.updateCapitals(ProjectLoader.getCurrentYear());
				AFTsLoader.updateAFTs();
//				if (dataPane.isSelected()) {
				for (int i = 0; i < CellsLoader.getCapitalsList().size() + 1; i++) {
					if (CapitalsController.radioColor[i].isSelected()) {
						if (i < CellsLoader.getCapitalsList().size()) {
							CellsCanvas.colorMap(CellsLoader.getCapitalsList().get(i));
							CapitalsController.getInstance().updateHistogrameCapitals(ProjectLoader.getCurrentYear(),
									CellsLoader.getCapitalsList().get(i));
						} else {
							CellsCanvas.colorMap("AFT");
						}
					}
				}
//				}
			}
		}
	}
}
