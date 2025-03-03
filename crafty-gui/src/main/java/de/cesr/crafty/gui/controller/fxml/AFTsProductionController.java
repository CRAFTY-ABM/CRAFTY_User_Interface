package de.cesr.crafty.gui.controller.fxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.cesr.crafty.gui.utils.analysis.AftAnalyzer;
import de.cesr.crafty.gui.utils.graphical.CSVTableView;
import de.cesr.crafty.core.dataLoader.AFTsLoader;
import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.utils.general.Utils;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class AFTsProductionController {
	@FXML
	private BarChart<String, Number> histogramePlevel;
	@FXML
	VBox boxCharts;

	Button productionFire = new Button();
	Button sensitivtyFire = new Button();
	RadioButton plotInitialDistrebution = new RadioButton("  Distribution map ");
	RadioButton plotOptimalLandon = new RadioButton("Cumulative expected service productivity");

	private static AFTsProductionController instance;

	public AFTsProductionController() {
		instance = this;
	}

	public static AFTsProductionController getInstance() {
		return instance;
	}

	public BarChart<String, Number> getHistogramePlevel() {
		return histogramePlevel;
	}

	public VBox getBoxCharts() {
		return boxCharts;
	}

	static public LineChart<Number, Number> productivitySampleChart(String aftLabel) {
		Map<String, ArrayList<Double>> data = AftAnalyzer.productivitySample(1000, 100,
				AFTsLoader.getAftHash().get(aftLabel));
		LineChart<Number, Number> chart = AftAnalyzer.generateChart(aftLabel, data);
		return chart;
	}

	static public LineChart<Number, Number> productivitySampleChartIntensity(String aftLabel) {
		String thisAFT = aftLabel.replace("Int", "").replace("VExt", "").replace("Ext", "");
		List<Aft> aList = new ArrayList<>();

		AFTsLoader.getAftHash().forEach((label, agent) -> {
			if (thisAFT.equals(label.replace("Int", "").replace("VExt", "").replace("Ext", "")))
				aList.add(agent);
		});
		if (aList.size() > 1) {
			Map<String, ArrayList<Double>> data = AftAnalyzer.productivitySample(1000, 100, aList.toArray(new Aft[0]));
			LineChart<Number, Number> chart = AftAnalyzer.generateChart(thisAFT, data);
			return chart;
		}
		return null;

	}

	static void updateProduction(Aft newAFT, TableView<ObservableList<String>> tabV) {
		String[][] tab = CSVTableView.tableViewToArray(tabV);
		for (int i = 0; i < tab[0].length; i++) {
			newAFT.getProductivityLevel().put(tab[0][i], Utils.sToD(tab[1][i]));
		}
	}
}
