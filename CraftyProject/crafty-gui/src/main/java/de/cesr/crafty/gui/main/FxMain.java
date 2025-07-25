package de.cesr.crafty.gui.main;

import java.io.InputStream;
import java.net.URL;
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

	public static Stage primaryStage;
	public static Scene scene;
	public static VBox topLevelBox = new VBox();
	public static BorderPane anchor = new BorderPane();
	private static ImageView logo;
	public static double graphicScaleX = Screen.getPrimary().getBounds().getWidth() / 2560;
	public static double graphicScaleY = Screen.getPrimary().getBounds().getHeight() / 1440;

	@Override
	public void start(Stage primaryStage) throws Exception {

		System.out.println("--Starting CRAFTY execution--");
		FxMain.primaryStage = primaryStage;
		URL fxml = FxMain.class.getResource("/fxmlControllers/MenuBar.fxml");
		topLevelBox.getChildren().add(FXMLLoader.load(fxml));
		topLevelBox.getChildren().add(anchor);
		addLogo();
		scene = new Scene(new Group(topLevelBox));
		scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
		primaryStage.setTitle(" CRAFTY User Interface ");
		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();
		primaryStage.setOnCloseRequest(_ -> Platform.exit());
	}

	private void addLogo() {
		InputStream imageStream = getClass().getResourceAsStream("/graphic/CRAFTY_logo_modern3.png");
		logo = Tools.logo(imageStream, 1);
		// TreeView<Path> tree =
		// FileTreeView.build(Paths.get("C:\\Users\\byari-m\\Desktop\\CRAFTY_DATA\\CRAFTY-EU-5km-data\\output\\RCP2_6-SSP1\\runrun"),".csv","-Cell-",1);
		anchor.setCenter(logo);
	}

	private static boolean useOnlyOneTime = true;

	public static void scaler() {
		if (useOnlyOneTime) {
			anchor.getChildren().remove(logo);
			double scale = Math.min(graphicScaleX, graphicScaleY);
			Scale scaleTransform = new Scale(scale, scale, 0, 0);
			anchor.getTransforms().add(scaleTransform);
			useOnlyOneTime = false;
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
