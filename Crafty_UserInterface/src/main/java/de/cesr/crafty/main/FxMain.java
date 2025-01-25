package de.cesr.crafty.main;

import java.io.InputStream;

import de.cesr.crafty.utils.analysis.CustomLogger;
import de.cesr.crafty.utils.camera.Camera;
import de.cesr.crafty.utils.graphical.Tools;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/*
 * @author Mohamed Byari
 *
 */

public class FxMain extends Application {
	public static Camera camera = new Camera();
	public static SubScene subScene;
	public static Group root = new Group();
	public static ImageView imageView = new ImageView();
	public static Stage primaryStage;
	public static AnchorPane anchor = new AnchorPane();
	public static Scene scene = new Scene(anchor);
	private static final CustomLogger LOGGER = new CustomLogger(FxMain.class);

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOGGER.info("--Starting CRAFTY execution--");
		double w = Screen.getPrimary().getBounds().getWidth();
		double h = Screen.getPrimary().getBounds().getHeight();
		FxMain.primaryStage = primaryStage;
		subScene = new SubScene(root, w * .45, h * .95);
		InputStream imageStream = getClass().getResourceAsStream("/graphic/craftylogo.png");
		imageView = Tools.logo(imageStream, w / 3, h / 3, 0.65);
		anchor.getChildren().add(imageView);

		primaryStage.setTitle(" CRAFTY User Interface ");

		VBox vbox = new VBox();

		vbox.getChildren().add(FXMLLoader.load(getClass().getResource("/fxmlControllers/MenuBar.fxml")));

		vbox.getChildren().add(anchor);
		scene = new Scene(vbox, w * .8, h * .8);
		scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();
		primaryStage.setOnCloseRequest(event -> Platform.exit());
	}

	public static void main(String[] args) {
		launch(args);
	}

}
