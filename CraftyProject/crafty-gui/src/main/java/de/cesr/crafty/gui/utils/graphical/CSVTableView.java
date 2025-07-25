package de.cesr.crafty.gui.utils.graphical;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.general.Utils;
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
				row.set(colIndex, Utils.sToD(event.getNewValue()) + "");
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

	public static TableView<ObservableList<String>> createTableFromRows( List<List<String>> rows) {
		TableView<ObservableList<String>> tableView = new TableView<>();
		createTableFromRows(tableView,rows);
		return tableView;
	}

	public static void createTableFromRows(TableView<ObservableList<String>> tableView ,List<List<String>> rows) {
		tableView.getColumns().clear();

		// If there's no data at all, return an empty table
		if (rows == null || rows.isEmpty()) {
			return;
		}

		// The first row is column headers
		List<String> headerRow = rows.get(0);

		// The data rows are everything after the first row
		List<List<String>> dataRows = rows.size() > 1 ? rows.subList(1, rows.size()) : new ArrayList<>(); // empty if
																											// there's
																											// only 1
																											// row

		// Determine how many columns we need (widest row among *all* rows—headers +
		// data)
		int maxColumns = headerRow.size();
		for (List<String> row : dataRows) {
			maxColumns = Math.max(maxColumns, row.size());
		}

		// Create columns
		for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
			final int index = colIndex;
			String columnHeader;

			// If header row has a column for this index, use it; otherwise use "Column X"
			if (colIndex < headerRow.size()) {
				columnHeader = headerRow.get(colIndex);
				// If the header cell is empty, consider giving it a default name
				if (columnHeader == null || columnHeader.trim().isEmpty()) {
					columnHeader = "Column " + (colIndex + 1);
				}
			} else {
				columnHeader = "Column " + (colIndex + 1);
			}

			TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnHeader);

			// Use a cellValueFactory that grabs the right index from each row's
			// ObservableList
			column.setCellValueFactory(cellData -> {
				ObservableList<String> rowData = cellData.getValue();
				// Return empty string if this row doesn't have data for this column
				if (index < rowData.size()) {
					return new SimpleStringProperty(rowData.get(index));
				} else {
					return new SimpleStringProperty("");
				}
			});

			// Make cells editable as text fields (optional)
			column.setCellFactory(TextFieldTableCell.forTableColumn());

			tableView.getColumns().add(column);
		}

		// Build an ObservableList of ObservableList<String> for the data rows
		ObservableList<ObservableList<String>> tableData = FXCollections.observableArrayList();
		for (List<String> row : dataRows) {
			tableData.add(FXCollections.observableArrayList(row));
		}
		tableView.setItems(tableData);

		// Set a constrained resize policy so columns expand/shrink to fill available
		// width
		tableView.setColumnResizePolicy(param -> TableView.UNCONSTRAINED_RESIZE_POLICY.equals(param.getTable().getColumnResizePolicy()));

		// Optionally let the table be editable
		tableView.setEditable(true);

		// Optionally let the table adapt to its content height
		// (makes the table's preferred height just large enough to show all rows)
		tableView.setFixedCellSize(25); // or some other row height
		tableView.prefHeightProperty()
				.bind(tableView.fixedCellSizeProperty().multiply(FXCollections.observableArrayList(tableData).size())
						.add(tableView.getInsets().getTop() + tableView.getInsets().getBottom()).add(30) // a bit of																								// header
																											// row
				);
	}

}