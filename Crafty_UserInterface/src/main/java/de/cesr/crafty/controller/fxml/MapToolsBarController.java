package de.cesr.crafty.controller.fxml;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import de.cesr.crafty.main.FxMain;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.utils.file.SaveAs;
import de.cesr.crafty.utils.graphical.ColorsTools;
import de.cesr.crafty.utils.graphical.NewWindow;
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
		FxMain.camera.cameraMousControl(CellsSet.subScene, "pointer");
	}

	// Event Listener on Button[#handButton].onAction
	@FXML
	public void handleHandAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.OPEN_HAND);
		FxMain.camera.cameraMousControl(CellsSet.subScene, "hand");
	}

	// Event Listener on Button[#zoomButton].onAction
	@FXML
	public void zoomAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		FxMain.camera.cameraMousControl(CellsSet.subScene, "zoom");

	}

	// Event Listener on Button[#zoomInButton].onAction
	@FXML
	public void handleZoomInAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		// FxMain.camera.cameraMousControl(FxMain.subScene,"zoom");
		// FxMain.camera.newzoom(FxMain.subScene);
		FxMain.camera.zoom(+100);
	}

	// Event Listener on Button[#zoomOutButton].onAction
	@FXML
	public void handleZoomOutAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.CROSSHAIR);
		FxMain.camera.zoom(-100);
		// FxMain.camera.newzoom(FxMain.subScene);
	}

	// Event Listener on Button[#earthButton].onAction
	@FXML
	public void handleearthAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		FxMain.camera.defaultcamera(CellsSet.getCanvas(), CellsSet.subScene);
		CellsSet.colorMap("AFT");
	}

	// Event Listener on Button[#eyeButton].onAction
	@FXML
	public void gisAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		CellsSet.colorMap("Region_Code");
		System.out.println();
	}

	@FXML
	public void handlePNGAction(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		SaveAs.png("", CellsSet.getCanvas());
	}

	@FXML
	public void colorPallet(ActionEvent event) {
		FxMain.scene.setCursor(Cursor.DEFAULT);
		NewWindow winColor = new NewWindow();
		ColorsTools.windowzpalette(winColor);
	}
}
