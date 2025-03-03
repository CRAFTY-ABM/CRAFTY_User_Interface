package de.cesr.crafty.gui.controller.fxml;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import de.cesr.crafty.gui.utils.graphical.CSVTableView;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class GlobalViewFXMLController {
	@FXML
	private TableView<ObservableList<String>> TablCapitals;
	@FXML
	private TableView<ObservableList<String>> TablServices;
	@FXML
	private TableView<ObservableList<String>> TabScenarios;
	@FXML
	private TableView<ObservableList<String>> TablAFTs;
	@FXML
	VBox vbox;

	public void initialize() {

		initilaseTabls();

	}

	void initilaseTabls() {
		System.out.println("initialize " + getClass().getSimpleName());
		CSVTableView.updateTableView(CsvTools.csvReader(PathTools.fileFilter(File.separator+"AFTsMetaData.csv").get(0)), null,
				TablAFTs);
		CSVTableView.updateTableView(CsvTools.csvReader(PathTools.fileFilter(File.separator+"Capitals.csv").get(0)), null,
				TablCapitals);
		CSVTableView.updateTableView(CsvTools.csvReader(PathTools.fileFilter(File.separator+"Services.csv").get(0)), null,
				TablServices);
		CSVTableView.updateTableView(CsvTools.csvReader(PathTools.fileFilter(File.separator+"scenarios.csv").get(0)), null,
				TabScenarios);
		Set<TitledPane> panes = new HashSet<>();

		vbox.getChildren().forEach(node -> {
			if (node instanceof TitledPane) {
				panes.add(((TitledPane) node));
			}
		});
	}
	

}
