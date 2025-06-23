package de.cesr.crafty.gui.utils.analysis;

import java.nio.file.Path;
import java.util.List;

import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.gui.utils.graphical.CSVTableView;
import javafx.scene.Node;

public class CsvToHtml {
	public static String convertCsvToHtml(List<List<String>> rows) {
		StringBuilder html = new StringBuilder();

		// Basic HTML and CSS
		html.append("<!DOCTYPE html>").append("<html>").append("<head>").append("<meta charset=\"UTF-8\">")
				.append("<style>").append("table { border-collapse: collapse; width: 100%; margin: 16px 0; }")
				.append("th, td { border: 1px solid #ccc; padding: 8px; }")
				.append("th { background-color: #1f7b64ff; font-weight: bold; }")
				// Alternate row shading
				.append("tr:nth-child(even) { background-color: #cbf2e8ff; }").append("</style>").append("</head>")
				.append("<body>");

		html.append("<table>");

		// Go row by row
		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
			html.append("<tr>");

			List<String> row = rows.get(rowIndex);
			for (int colIndex = 0; colIndex < row.size(); colIndex++) {
				String cellValue = escapeHtml(row.get(colIndex));

				if (rowIndex == 0) {
					// Treat the first row as column headers
					html.append("<th>").append(cellValue).append("</th>");
				} else if (colIndex == 0) {
					// Treat the first column of each row as a row label
					html.append("<th>").append(cellValue).append("</th>");
				} else {
					html.append("<td>").append(cellValue).append("</td>");
				}
			}

			html.append("</tr>");
		}

		html.append("</table>");
		html.append("</body></html>");

		return html.toString();
	}

	// Optional helper method to escape <, >, & and so on
	private static String escapeHtml(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static Node tabeWeb(Path path) {
//		String htmlString = CsvToHtml.convertCsvToHtml(CsvTools.readCsvFile(path));
//		WebView webView = new WebView();
//		WebEngine engine = webView.getEngine();
//		engine.loadContent(htmlString);

		List<List<String>> data = CsvTools.readCsvFileWithoutZeros(CsvTools.readCsvFile(path));
		return CSVTableView.createTableFromRows(data);
	}

}
