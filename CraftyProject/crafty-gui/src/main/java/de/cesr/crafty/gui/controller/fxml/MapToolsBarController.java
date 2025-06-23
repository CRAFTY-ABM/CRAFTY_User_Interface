package de.cesr.crafty.gui.controller.fxml;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import de.cesr.crafty.gui.canvasFx.CellsCanvas;
import de.cesr.crafty.gui.utils.graphical.ColorsTools;
import de.cesr.crafty.gui.utils.graphical.NewWindow;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.gui.utils.graphical.SaveAs;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

import javafx.util.Duration;

public class MapToolsBarController {

	@FXML
	private Button mousePointer, hand, colorPallet, zoom, zoomIn, zoomOut, earth, gis, png;

	@FXML
	private void initialize() {
		
		hand.getTooltip().setShowDelay(Duration.millis(100));
		zoomIn.getTooltip().setShowDelay(Duration.millis(100));
		zoom.getTooltip().setShowDelay(Duration.millis(100));
		zoomOut.getTooltip().setShowDelay(Duration.millis(100));
		earth.getTooltip().setShowDelay(Duration.millis(100));
		gis.getTooltip().setShowDelay(Duration.millis(100));
		png.getTooltip().setShowDelay(Duration.millis(100));
		colorPallet.getTooltip().setShowDelay(Duration.millis(100));

	}

	// Event Listener on Button[#handButton].onAction
	@FXML
	public void pointer(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		FxMain.camera.cameraMousControl(CellsCanvas.subScene, "pointer");
	}

	// Event Listener on Button[#handButton].onAction
	@FXML
	public void handleHandAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.OPEN_HAND);
		FxMain.camera.cameraMousControl(CellsCanvas.subScene, "hand");
	}

	// Event Listener on Button[#zoomButton].onAction
	@FXML
	public void zoomAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		FxMain.camera.cameraMousControl(CellsCanvas.subScene, "zoom");

	}

	// Event Listener on Button[#zoomInButton].onAction
	@FXML
	public void handleZoomInAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		// MainGUI.camera.cameraMousControl(MainGUI.subScene,"zoom");
		// MainGUI.camera.newzoom(MainGUI.subScene);
		FxMain.camera.zoom(+100);
	}

	// Event Listener on Button[#zoomOutButton].onAction
	@FXML
	public void handleZoomOutAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		FxMain.camera.zoom(-100);
		// MainGUI.camera.newzoom(MainGUI.subScene);
	}

	// Event Listener on Button[#earthButton].onAction
	@FXML
	public void handleearthAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		FxMain.camera.defaultcamera(CellsCanvas.getCanvas(), CellsCanvas.subScene);
		CellsCanvas.colorMap("AFT");
	}

	// Event Listener on Button[#eyeButton].onAction
	@FXML
	public void gisAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		CellsCanvas.colorMap("Region_Code");
		System.out.println();
	}

	@FXML
	public void handlePNGAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		SaveAs.png("", CellsCanvas.getCanvas());
	}

	@FXML
	public void colorPallet(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		NewWindow winColor = new NewWindow();
		ColorsTools.windowzpalette(winColor);
	}
}
