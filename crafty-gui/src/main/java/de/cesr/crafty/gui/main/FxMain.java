package de.cesr.crafty.gui.main;

import java.io.InputStream;
import java.net.URL;

import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.gui.utils.camera.Camera;
import de.cesr.crafty.gui.utils.graphical.Tools;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;

/*
 * @author Mohamed Byari
 *
 */

public class FxMain extends Application {

	private static final CustomLogger LOGGER = new CustomLogger(FxMain.class);

	public static Stage primaryStage;
	public static Scene scene;
	public static VBox topLevelBox = new VBox();
	public static BorderPane anchor = new BorderPane();
	public static Camera camera = new Camera();
	public static double graphicScaleX, graphicScaleY;
	public static final double defaultWidth = 2800, defaultHeight = 1550;

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOGGER.info("--Starting CRAFTY execution--");
		FxMain.primaryStage = primaryStage;
		graphicScaleX = Screen.getPrimary().getBounds().getWidth() / defaultWidth;
		graphicScaleY = Screen.getPrimary().getBounds().getHeight() / defaultHeight;

		URL fxml = FxMain.class.getResource("/fxmlControllers/MenuBar.fxml");
		topLevelBox.getChildren().add(FXMLLoader.load(fxml));
		topLevelBox.getChildren().add(anchor);
		scaler();
		addLogo();

		scene = new Scene(new Group(topLevelBox));
		scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
		primaryStage.setTitle(" CRAFTY User Interface ");
		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();
		primaryStage.setOnCloseRequest(event -> Platform.exit());
	}

	private void addLogo() {
		InputStream imageStream = getClass().getResourceAsStream("/graphic/CRAFTY_logo_modern3.png");
		ImageView imageView = Tools.logo(imageStream, 0.3);
		anchor.setCenter(imageView);
	}

	private static void scaler() {
		Scale scaleTransform = new Scale(graphicScaleX, graphicScaleX, 0, 0);
		anchor.getTransforms().add(scaleTransform);
	}

	public static void main(String[] args) {
		launch(args);
	}

}
