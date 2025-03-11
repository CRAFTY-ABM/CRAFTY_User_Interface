package de.cesr.crafty.gui.controller.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import de.cesr.crafty.core.cli.ConfigLoader;
import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.gui.utils.graphical.ColorsTools;
import de.cesr.crafty.gui.utils.graphical.NewWindow;
import de.cesr.crafty.gui.utils.graphical.Tools;
import de.cesr.crafty.gui.utils.graphical.WarningWindowes;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.core.utils.file.PathTools;
import javafx.application.Platform;
import javafx.event.ActionEvent;

public class MenuBarController {
	@FXML
	private Menu recent;




	public void initialize() {
		updateRecentFilesMenu();
	}

	// Event Listener on MenuItem.onAction
	@FXML
	public void open(ActionEvent event) {
		openProject();

		if (ProjectLoader.getProjectPath()!=null) {
			initialsePAnes();
		}
	}

	@FXML
	public void Exit(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	public void resrart(ActionEvent event) {
		restartApplication();
	}

	@FXML
	public void welcome(ActionEvent event) {
		openWebInBrowser();
	}

	@FXML
	public void colorPallet(ActionEvent event) {
		NewWindow winColor = new NewWindow();
		ColorsTools.windowzpalette(winColor);
	}

	@SuppressWarnings("deprecation")
	private void restartApplication() {
		StringBuilder cmd = new StringBuilder();
		cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java ");
		for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			cmd.append(jvmArg + " ");
		}
		cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
		cmd.append(FxMain.class.getName()).append(" ");

		try {
			Runtime.getRuntime().exec(cmd.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	static void openProject() {
		File selectedDirectory;
		String userDocumentsPath = System.getProperty("user.home") + File.separator + "Documents";
		File documentsDir = new File(userDocumentsPath);

		// Check if the Data directory exists within Documents
		File dataDir = new File(documentsDir, "Data");
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			// If the Data directory does not exist, fall back to the Documents directory
			selectedDirectory = Tools.selectFolder(userDocumentsPath);
		} else {
			// If the Data directory exists, use it as the starting path
			selectedDirectory = Tools.selectFolder(dataDir.getAbsolutePath());
		}

		if (selectedDirectory != null) {
			ConfigLoader.config.project_path = selectedDirectory.getAbsolutePath();
			ProjectLoader.pathInitialisation(Paths.get(ConfigLoader.config.project_path));
			ConfigLoader.config.scenario = ProjectLoader.getScenariosList().get(1);
			ProjectLoader.modelInitialisation();
		}
		System.out.println();
	}

	void initialsePAnes() {
		WarningWindowes.showWaitingDialog(x -> {
			try {
				FxMain.anchor.setCenter(FXMLLoader.load(getClass().getResource("/fxmlControllers/TabPaneFXML.fxml")));
				//FxMain.anchor.getChildren().add(FXMLLoader.load(getClass().getResource("/fxmlControllers/TabPaneFXML.fxml")));
			} catch (IOException en) {
				// TODO Auto-generated catch block
				en.printStackTrace();
			}
		});

	}

	void openWebInBrowser() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

			try {
				Desktop.getDesktop().browse(new URI("https://landchange.imk-ifu.kit.edu/CRAFTY"));
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}

		}
	}

	@SuppressWarnings("resource")
	private void updateRecentFilesMenu() {
		recent.getItems().clear();
		String[] paths = PathTools.read("RecentProject.txt").split("\n");
		for (int i = paths.length - 1; i >= 0; i--) {
			if (!paths[i].equals("")) {
				MenuItem item = new MenuItem(paths[i]);
				int j = i;
				item.setOnAction(event -> {
					ConfigLoader.config.project_path = paths[j];
					ProjectLoader.pathInitialisation(Paths.get(ConfigLoader.config.project_path));
					ConfigLoader.config.scenario = ProjectLoader.getScenariosList().get(1);
					ProjectLoader.modelInitialisation();
					initialsePAnes();
				});
				recent.getItems().add(item);
			}
		}
		MenuItem item = new MenuItem("Clear History");
		item.setOnAction(event -> {
			try {
				new FileWriter("RecentProject.txt", false);
				recent.getItems().clear();
				recent.getItems().add(item);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		recent.getItems().add(new SeparatorMenuItem());
		recent.getItems().add(item);
	}

}
