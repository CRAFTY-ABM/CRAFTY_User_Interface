package de.cesr.crafty.gui.utils.graphical;

import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

import de.cesr.crafty.gui.main.FxMain;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Mohamed Byari
 *
 */

public class WarningWindowes {
	static String p = "";

	public static String alterErrorNotFileFound(String message, String path) {
		p = path;
		Alert alert = new Alert(AlertType.WARNING);
		ButtonType selectfile = new ButtonType("Select a new file", ButtonBar.ButtonData.OK_DONE);
		alert.setTitle("Error Dialog");
		alert.setHeaderText(message + " \n" + path);
		System.out.println(message + " \n" + path);
		alert.getButtonTypes().setAll(selectfile, ButtonType.NO);
		alert.showAndWait().ifPresent(response -> {
			if (response == selectfile) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Select a CSV file");
				File selectedFile = fileChooser.showOpenDialog(FxMain.primaryStage);
				if (selectedFile != null) {
					p = selectedFile.getAbsolutePath();
				}
			}
		});
		return p;
	}

	public static void showWarningMessage(String message, String okbuttonName, Consumer<String> Retry,
			String cancelbuttonName, Consumer<String> continuAnyWay) {
		showWarningMessage(message, okbuttonName, Retry, cancelbuttonName, continuAnyWay, new ArrayList<String>());
	}

	public static void showWarningMessage(String message, String okbuttonName, Consumer<String> okbuttonConsumer,
			String cancelbuttonName, Consumer<String> continuAnyWay, List<String> listWarning) {
		ButtonType okButtonType = new ButtonType(okbuttonName, ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(cancelbuttonName, ButtonBar.ButtonData.CANCEL_CLOSE);
		ButtonType customButtonType = new ButtonType("Close CRAFTY");

		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Warning");
		alert.setHeaderText(message);
		String str = "";
		for (int i = 0; i < listWarning.size(); i++) {
			str = str + listWarning.get(i) + "\n";
		}
		alert.setContentText(str);

		// Add custom buttons
		alert.getButtonTypes().setAll(okButtonType, cancelButtonType, customButtonType);

		// Handle button actions
		alert.setOnCloseRequest(event -> {
			ButtonType result = alert.getResult();
			if (result == okButtonType) {
				okbuttonConsumer.accept("");
			} else if (result == cancelButtonType) {
				continuAnyWay.accept("");
			} else if (result == customButtonType) {
				Platform.exit();
			}
		});

		alert.showAndWait();
	}

	public static void showWarningMessage(String message, String okButtonName, Consumer<String> okButtonConsumer) {
		ButtonType okButtonType = new ButtonType(okButtonName, ButtonBar.ButtonData.OK_DONE);
		ButtonType customButtonType = new ButtonType("Close CRAFTY");

		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Warning");
		alert.setHeaderText(message);

		// Add custom buttons
		alert.getButtonTypes().setAll(okButtonType, customButtonType);

		// Handle button actions
		alert.setOnCloseRequest(event -> {
			ButtonType result = alert.getResult();
			if (result == okButtonType) {
				okButtonConsumer.accept("");
			} else if (result == customButtonType) {
				Platform.exit();
			}
		});

		alert.showAndWait();
	}

	public static void showWaitingDialog(Consumer<String> action) {
		Stage waitingDialog = new Stage();
		waitingDialog.initModality(Modality.NONE);
		waitingDialog.initStyle(StageStyle.UNDECORATED);
		// loadingAlert.initModality(Modality.NONE);
		Label label = new Label("Please wait...");
		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setCenterShape(true);
		VBox root = new VBox();
		root.setAlignment(Pos.CENTER);
		root.setSpacing(10);
		root.getChildren().addAll(label, progressIndicator);

		Scene scene = new Scene(root, 200, 100);
		waitingDialog.setScene(scene);
		waitingDialog.show();

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				Thread.sleep(5);
				// method.accept("");
				return null;
			}

			@Override
			protected void succeeded() {
				super.succeeded();
				waitingDialog.close();
			}
		};

		task.setOnSucceeded(wse -> {
			action.accept("");
		});// here

		Thread thread = new Thread(task);
		thread.start();
		thread.setPriority(Thread.MAX_PRIORITY);

	}

}
