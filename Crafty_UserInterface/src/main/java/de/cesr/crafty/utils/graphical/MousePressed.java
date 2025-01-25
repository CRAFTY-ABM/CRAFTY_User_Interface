package de.cesr.crafty.utils.graphical;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.cesr.crafty.utils.file.SaveAs;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MousePressed {

	/**
	 * @author Mohamed Byari
	 *
	 */

	public static void mouseControle(Pane pane, Node node) {
		mouseControle(pane, node, null, "titel");
	}
	public static void mouseControle(Pane pane, Node node, String titel) {
		mouseControle(pane, node, null, titel);
	}
	public static void mouseControle(Pane box, Node node, HashMap<String, Consumer<String>> othersMenuItems) {
		mouseControle(box, node, othersMenuItems, "titel");
	}

	public static void mouseControle(Pane box, Node node, HashMap<String, Consumer<String>> othersMenuItems,
			String titel) {
		HashMap<String, Consumer<String>> hashAction = new HashMap<>();
		if (othersMenuItems != null) {
			othersMenuItems.forEach((name, action) -> {
				hashAction.put(name, action);
			});
		}

		hashAction.put("Save as PNG", (x) -> {
			SaveAs.png(titel, node);

		});
//		hashAction.put("Close", (x) -> {
//			Parent m = node.getParent();
//			((Pane) m).getChildren().remove(node);
//		});
		hashAction.put("Detach", (x) -> {
			List<Integer> findpath = Tools.findIndexPath(node, box);
			Tools.reInsertChildAtIndexPath(new Separator(), box, findpath);
			NewWindow win = new NewWindow();
			win.creatwindows("", node);
			win.setOnCloseRequest(event -> {
				Tools.reInsertChildAtIndexPath(node, box, findpath);
			});
		});
		MousePressed.smartMenu(node, hashAction);
	}

	static void smartMenu(Node node, HashMap<String, Consumer<String>> hash) {
		ContextMenu cm = new ContextMenu();

		MenuItem[] item = new MenuItem[hash.size()];
		AtomicInteger i = new AtomicInteger();
		hash.forEach((k, v) -> {
			item[i.get()] = new MenuItem(k);
			cm.getItems().add(item[i.get()]);
			item[i.get()].setOnAction(e -> {
				v.accept(k);
			});
			i.getAndIncrement();
		});

		node.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			if (cm.isShowing()) {
				cm.hide();
			}
			if (e.isSecondaryButtonDown()) {
				cm.show((Stage) node.getScene().getWindow(), e.getScreenX(), e.getScreenY());
				e.consume();
			}
		});
	}

}
