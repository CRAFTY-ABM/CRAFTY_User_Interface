package UtilitiesFx.graphicalTools;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageExporter {

	static WritableImage createImage(Node node) {
		// Set up snapshot parameters with a transparent background
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.WHITE);

		// Take a snapshot of the node
		return node.snapshot(parameters, null);
	}

	public static void NodeToImage(Node node, String path) {
//		Pane pane = (Pane) node.getParent();
//		if (node.getParent() == null) {
//			pane = new Pane();
//			pane.getChildren().add(node);
//		}

		// Take a snapshot without showing the stage
		WritableImage image = createImage(node);
		File file = new File(path);
		// Save the image to a file
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
			System.out.println("Image saved to " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Normally you would call primaryStage.show() to display the UI, but it's not
		// necessary here
	}
}
