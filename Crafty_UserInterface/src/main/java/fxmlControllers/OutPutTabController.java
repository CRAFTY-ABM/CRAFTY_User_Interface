package fxmlControllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import model.CellsSet;

import java.io.IOException;

import UtilitiesFx.graphicalTools.NewWindow;
import dataLoader.ServiceSet;
import javafx.event.Event;
import javafx.event.EventHandler;

public class OutPutTabController {
	@FXML
	private TabPane tabpane;
	@FXML
	private Tab addTab;
	@FXML
	private Tab tmp;
	@FXML
	private Button selecserivce;

	NewWindow colorbox = new NewWindow();
	public static RadioButton[] radioColor;

	private static OutPutTabController instance;

	public OutPutTabController() {
		instance = this;
	}

	public static OutPutTabController getInstance() {
		return instance;
	}

	public void initialize() {
		radioColor = new RadioButton[ServiceSet.getServicesList().size() + 1];

		for (int i = 0; i < radioColor.length; i++) {
			if (i < ServiceSet.getServicesList().size()) {
				radioColor[i] = new RadioButton(ServiceSet.getServicesList().get(i));

			} else if (i == ServiceSet.getServicesList().size()) {
				radioColor[i] = new RadioButton("Agent");
			}
			int k = i;
			radioColor[i].setOnAction(e -> {
				for (int j = 0; j < radioColor.length; j++) {
					if (k != j) {
						radioColor[j].setSelected(false);
					}
				}
				if (k < ServiceSet.getServicesList().size()) {
					CellsSet.colorMap(ServiceSet.getServicesList().get(k));
				} else if (k == ServiceSet.getServicesList().size()) {
					CellsSet.colorMap("FR");
				}

			});
		}

		tmp.setClosable(false);
		addTab.setClosable(false);
		addTab.setOnSelectionChanged(new EventHandler<Event>() {
			@Override
			public void handle(Event t) {
				if (addTab.isSelected()) {
					createNewTab("OutPut " + (tabpane.getTabs().size() - 1));
				}
			}
		});
	}

	@FXML
	void selecserivce() {
		if (!colorbox.isShowing()) {
			VBox g = new VBox();
			g.getChildren().addAll(radioColor);
			colorbox.creatwindows("Display Services and AFT distribution", g);
		}

	}

	public void createNewTab(String name) {
		Tab tab = new Tab(name);
		try {
			tab.setContent(FXMLLoader.load(getClass().getResource("/fxmlControllers/OutPuter.fxml")));
		} catch (IOException e) {
		}
		tabpane.getTabs().add(tabpane.getTabs().indexOf(addTab), tab);
		tabpane.getSelectionModel().select(tab);
		tabpane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
		removeTabIfIsEmpty(tab);
	}

	private void removeTabIfIsEmpty(Tab tab) {
		try {
			((VBox) ((VBox) tab.getContent()).getChildren().iterator().next()).getChildren().forEach(s -> {
				if (s instanceof ChoiceBox) {
					if (((ChoiceBox<?>) s).getItems().size() == 0) {
						tabpane.getTabs().remove(tabpane.getTabs().indexOf(addTab) - 1);
					}
				}
			});
		} catch (NullPointerException e) {
			tabpane.getTabs().remove(tabpane.getTabs().indexOf(addTab) - 1);
		}
	}

}
