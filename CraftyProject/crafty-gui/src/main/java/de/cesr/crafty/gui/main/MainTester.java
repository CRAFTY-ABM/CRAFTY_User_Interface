package de.cesr.crafty.gui.main;

import java.io.InputStream;

import de.cesr.crafty.gui.utils.graphical.Tools;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class MainTester extends Application {

	@Override
	public void start(Stage primaryStage) {
		Label label = new Label("Hello, JavaFX!");
		Scene scene = new Scene(FxMain.anchor, 300, 200);
		FxMain.anchor.setCenter(label);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Minimal JavaFX App");
		primaryStage.show();
		
	}

	public static void main(String[] args) {

		launch(args);
	}

}
