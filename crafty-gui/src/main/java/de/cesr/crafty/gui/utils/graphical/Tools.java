package de.cesr.crafty.gui.utils.graphical;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;

import java.util.stream.Collectors;

import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.gui.utils.graphical.Tools;
import javafx.stage.DirectoryChooser;

/**
 * @author Mohamed Byari
 *
 */

public class Tools {
	private static final CustomLogger LOGGER = new CustomLogger(Tools.class);

	public static VBox vBox(Node... children) {
		VBox vbox = new VBox();
		vbox.getChildren().addAll(children);

		return vbox;
	}

	public static HBox hBox(Node... children) {
		HBox h = new HBox();
		h.getChildren().addAll(children);
		return h;
	}

	public static Button button(String string, String color) {
		Button button = new Button(string);
		if (!"".equals(color))
			button.setStyle(" -fx-base: #" + color + ";");
		return button;
	}

	public static Slider slider(double a, double b, double d) {
		Slider slider = new Slider(a, b, d);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		return slider;
	}

	public static ChoiceBox<String> choiceBox(List<String> list) {
		ChoiceBox<String> choice = new ChoiceBox<>();
		choiceBox(choice, list);
		return choice;
	}

	public static void choiceBox(ChoiceBox<String> choice, List<String> list) {
		if (list.size() == 0) {
			list.add("Empty");
		}
		choice.getItems().addAll(list);
		choice.setValue(list.get(0));

	}

	public static List<String> getKeysInSortedOrder(HashMap<String, Integer> map) {
		return map.entrySet().stream().sorted((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()))
				.map(entry -> entry.getKey()).collect(Collectors.toList());
	}

	public static TitledPane T(String name, boolean isopen, Node... children) {
		TitledPane spatial = new TitledPane(name, vBox(children));
		spatial.setExpanded(isopen);
		// spatial.setStyle(" -fx-base: #ffffff;");
		// Tools.mouseControle(spatial, "");
		return spatial;
	}

	public static GridPane grid(double hGap, double vGap) {
		GridPane gridSensitivityChart = new GridPane();
		gridSensitivityChart.setHgap(hGap);
		gridSensitivityChart.setVgap(vGap);
		return gridSensitivityChart;
	}

	public static Text text(String txt, Color color) {
		Text t1 = new Text(txt);
		t1.setFill(color);
		return t1;
	}
	public static void reInsertChildAtIndexPath(Node child, Parent rootParent, List<Integer> indexPath) {
		Parent currentParent = rootParent;
		// Traverse down the hierarchy using the index path
		for (int i = 0; i < indexPath.size() - 1; i++) {
			// Get the next parent in the path
			Node nextParent = currentParent.getChildrenUnmodifiable().get(indexPath.get(i));
			if (nextParent instanceof Parent) {
				currentParent = (Parent) nextParent;
			} else {
				throw new IllegalArgumentException("Index path is invalid. Node at index is not a Parent.");
			}
		}
		// The last index is where the child should be inserted
		int insertIndex = indexPath.get(indexPath.size() - 1);

		if (currentParent instanceof Pane) {
			((Pane) currentParent).getChildren().set(insertIndex, child);
		} else if (currentParent instanceof Group) {
			((Group) currentParent).getChildren().set(insertIndex, child);
		} else {
			LOGGER.error("The parent is neither a Pane nor a Group, cannot modify children.");
		}
	}

	public static List<Integer> findIndexPath(Node child, Parent parent) {
		List<Integer> indexPath = new ArrayList<>();
		Node current = child;
		// Traverse up the parent hierarchy from the child to the specified parent
		int n = 10;
		while (current != null && current != parent && n < 100) {
			n++;
			Parent currentParent = current.getParent();
			// If the current node has a parent, find the index of the current node in its
			// parent
			if (currentParent != null) {
				int index = currentParent.getChildrenUnmodifiable().indexOf(current);
				indexPath.add(index);
				current = currentParent;
			}
		}
		// Reverse the list since we built it from child to parent
		Collections.reverse(indexPath);
		return indexPath;
	}

	

	public static ImageView logo(InputStream imageStream, double scale) {
		ImageView imageView = new ImageView();
		Image image = new Image(imageStream);
		imageView.setImage(image);
		Scale scaleTransform = new Scale(scale, scale, Screen.getPrimary().getBounds().getWidth(),
				Screen.getPrimary().getBounds().getWidth());
		imageView.getTransforms().add(scaleTransform);

		return imageView;
	}

	public static  GridPane initializeGridpane(int colmunNBR, List<Node> nodes) {
		GridPane grid = new GridPane();
		int j = 0, k = 0;
		for (int m = 0; m < nodes.size(); m++) {
			grid.add(Tools.hBox(nodes.get(m)), j++, k);
			if (j % colmunNBR == 0) {
				k++;
				j = 0;
			}
		}
		return grid;
	}
	public static File selectFolder(String projectPath) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select Project");
		File initialDirectory = new File(projectPath);
		if (initialDirectory.exists())
			chooser.setInitialDirectory(initialDirectory);
		File selectedDirectory = chooser.showDialog(FxMain.primaryStage);
		return selectedDirectory;
	}
}
