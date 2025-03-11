package de.cesr.crafty.gui.plumLinking;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import ac.ed.lurg.ModelConfig;
import ac.ed.lurg.ModelMain;
import de.cesr.crafty.core.plumLinking.PlumToCrafty;
import de.cesr.crafty.gui.utils.graphical.GraphicConsol;
import de.cesr.crafty.gui.utils.graphical.Tools;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
//import utils.analysis.CustomLogger;


public class PlumController {
	@FXML
	private VBox box;
	@FXML
	private ScrollPane scroll;
//	private static final CustomLogger LOGGER = new CustomLogger(PlumController.class);

	boolean isPlumInitialized = false;
	boolean isATickFinished = true;
	AtomicInteger tick = new AtomicInteger(1);

	PlumToCrafty PtoC = new PlumToCrafty();

	// List<Map<String, String>> bio_crop_demand;

	public void initialize() {
		System.out.println("--------------------------------------------------");
		scroll.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.85);
		PtoC.initialize();
	}

	@FXML
	void plumInitialsation() {
		if (!isPlumInitialized) {
			progressBarFunction(true, " Initialisation ", x -> {
				ModelMain.main(new String[] {});
			}, y -> {
				isPlumInitialized = true;
			});
		}
	}

	@FXML
	void initCoupling() {
		plumInitialsation();
	}

	@FXML
	public void oneTick() {
		if (isPlumInitialized && isATickFinished) {
			isATickFinished = false;
			String year = (ModelConfig.BASE_YEAR + ModelMain.newStartYear) + "";

			Consumer<String> runNTick = x -> {
				ModelMain.theModel.runNTick(tick.getAndIncrement());
			};

			Consumer<String> Build_demands_for_Crafty = y -> {
				isATickFinished = true;
				Button btn = Tools.button("Build demands for Crafty", "");
				btn.setOnAction(e -> {
					CreateTextAreaAndGraphicConsole();
				});
				box.getChildren().addAll(Tools.hBox(btn, Tools.text("    for " + 2020, Color.BLUE)));
			};

			progressBarFunction(true, "Running PLUM for: " + year, runNTick, Build_demands_for_Crafty);
		}
	}


	void CreateTextAreaAndGraphicConsole() {
		TextArea console = new TextArea();
		box.getChildren().addAll(console);
		GraphicConsol.start(console);
	}

	void progressBarFunction(boolean useGraphicalConsol, String titel, Consumer<String> actoin,
			Consumer<String> succeeded) {
		box.getChildren().forEach(e -> {
			if (e.getClass().getSimpleName().equals("TitledPane")) {
				((TitledPane) e).setExpanded(false);
			}
		});
		ProgressBar progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.MAX_VALUE);
		TextArea console = new TextArea();
		box.getChildren().addAll(Tools.T(titel, true, progressBar, useGraphicalConsol ? console : new Separator()));
		if (useGraphicalConsol)
			GraphicConsol.start(console);
		progressBar.setVisible(true); // Show the progress bar when task starts
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				actoin.accept("");
				return null;
			}

			@Override
			protected void succeeded() {
				super.succeeded();
				succeeded.accept("");
				progressBar.setVisible(false);
				GraphicConsol.restoreOutput();
			}

			@Override
			protected void failed() {
				super.failed();
				progressBar.setVisible(false); // Hide on failure as well
			}
		};
		new Thread(task).start();
	}

}