package de.cesr.crafty.utils.graphical;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cesr.crafty.utils.file.CsvTools;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

/**
 * @author Mohamed Byari
 *
 */

public final class CSVTableView extends TableView<String> {

	public static TableView<ObservableList<String>> newtable(String[][] data, Consumer<String> action) {
		TableView<ObservableList<String>> tableView = new TableView<>();
		tableView.setEditable(true);

		editable(data, action, tableView);
		// Populate the TableView with data
		for (int i = 1; i < data.length; i++) {
			tableView.getItems().add(FXCollections.observableArrayList(data[i]));
		}

		// double height = 25 * (data.length + 1);
		// double width = 100 * data[0].length;
		// tableView.setPrefHeight(height);
		// tableView.setPrefWidth(width);
		return tableView;
	}

	public static TableView<ObservableList<String>> newtable(String[][] data) {
		return newtable(data, null);
	}

	public static TableView<ObservableList<String>> newtable(Path file) {
		return newtable(CsvTools.csvReader(file));
	}

	public static void updateTableView(String[][] data, Consumer<String> action,
			TableView<ObservableList<String>> tableView) {

		if (data == null || data.length == 0 || data[0].length == 0) {
			return;
		}

		ObservableList<ObservableList<String>> dataObservable = FXCollections.observableArrayList();
		for (int i = 1; i < data.length; i++) { // start from 1 to skip the first row
			dataObservable.add(FXCollections.observableArrayList(data[i]));
		}
		tableView.setItems(dataObservable);
		tableView.getColumns().clear();
		editable(data, action, tableView);
	}

	static void editable(String[][] data, Consumer<String> action, TableView<ObservableList<String>> tableView) {
		// Create columns and set them to be editable
		for (int i = 0; i < data[0].length; i++) {
			final int colIndex = i;
			TableColumn<ObservableList<String>, String> column = new TableColumn<>(data[0][i]);
			column.setCellValueFactory(param -> {
				ObservableList<String> rowData = param.getValue();
				if (colIndex < rowData.size()) {
					return new SimpleStringProperty(rowData.get(colIndex));
				} else {
					return new SimpleStringProperty("");
				}
			});
			if (action != null) {
				column.setCellFactory(TextFieldTableCell.forTableColumn());
			}
			column.setOnEditCommit(event -> {
				// Update the data array when user edits a cell
				ObservableList<String> row = event.getRowValue();
				row.set(colIndex, Tools.sToD(event.getNewValue()) + "");
				action.accept("");
			});

			tableView.getColumns().add(column);
		}
	}

	public static String[][] tableViewToArray(TableView<ObservableList<String>> tableView) {
		int numRows = tableView.getItems().size() + 1;
		int numCols = tableView.getColumns().size();
		String[][] array = new String[numRows][numCols];
		array[0] = tableView.getColumns().stream().map(TableColumn::getText).toArray(String[]::new);
		for (int i = 1; i < numRows; i++) {
			array[i] = tableView.getItems().get(i - 1).stream().collect(Collectors.toList()).toArray(new String[0]);
		}
		return array;
	}

	public static Map<String, String[]> tableViewToMap(TableView<ObservableList<String>> tableView) {
		Map<String, String[]> map = new HashMap<>();

		for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
			List<String> columnData = new ArrayList<>();
			for (ObservableList<String> row : tableView.getItems()) {
				columnData.add(row.get(col.getParentColumn().getColumns().indexOf(col)));
			}
			map.put(col.getText(), columnData.toArray(new String[0]));
		}

		return map;
	}

}