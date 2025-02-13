package de.cesr.crafty.controller.fxml;

import java.util.HashMap;
import java.util.function.Consumer;

import de.cesr.crafty.dataLoader.DemandModel;
import de.cesr.crafty.utils.file.SaveAs;
import de.cesr.crafty.utils.graphical.LineChartTools;
import de.cesr.crafty.utils.graphical.MousePressed;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.layout.Pane;

public class ServicesController {
	@FXML
	private LineChart<Number, Number> demandsChart;
	
	
	
	private static ServicesController instance;

	public ServicesController() {
		instance = this;
	}

	public static ServicesController getInstance() {
		return instance;
	}
	
	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());
		
		new LineChartTools().lineChart((Pane) demandsChart.getParent(), demandsChart,
				DemandModel.serialisationWorldDemand());
		String ItemName = "Save as CSV";
		Consumer<String> action = x -> {
			SaveAs.exportLineChartDataToCSV(demandsChart);
		};
		HashMap<String, Consumer<String>> othersMenuItems = new HashMap<>();
		othersMenuItems.put(ItemName, action);
		MousePressed.mouseControle((Pane) demandsChart.getParent(), demandsChart, othersMenuItems);
	}
	
	public LineChart<Number, Number> getDemandsChart() {
		return demandsChart;
	}


}
