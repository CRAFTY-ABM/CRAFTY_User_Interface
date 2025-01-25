package de.cesr.crafty.utils.graphical;


import java.util.function.Consumer;

import de.cesr.crafty.utils.camera.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;

/**
 * @author Mohamed Byari
 *
 */

public class  NewWindow extends Stage {


	
	public  void creatwindows(String name,double Width ,double Height , Node... nodes) {
				
		Scene scene;
		StackPane rootPane = new StackPane();
		
		rootPane.getChildren().addAll(nodes);
		
		scene = new Scene(rootPane, Screen.getPrimary().getBounds().getWidth()* Width,
				Screen.getPrimary().getBounds().getHeight() * Height);
	
		setTitle(name);
		setScene(scene);
		setAlwaysOnTop(true);
		show();
	}
	
	public  void creatwindows(String name, Node... nodes) {
		Scene scene;
		StackPane rootPane = new StackPane();
		rootPane.getChildren().addAll(nodes);
		scene = new Scene(rootPane);
	
		setTitle(name);
		setScene(scene);
		setAlwaysOnTop(true);
		show();
	}
	
	public  void creatwindows(String name ,Node nodes,Consumer<Stage> action) {
		action.accept(this);
		creatwindows(name, nodes);
			}

	
	public SubScene subSceneWithCamera(BorderPane rootPane, Node... nodes) {
		
		Group root = new Group();
		 SubScene subScene;
		 Camera camera = new Camera();
		
		

		subScene = new SubScene(root,Screen.getPrimary().getBounds().getWidth()*0.5,Screen.getPrimary().getBounds().getHeight()*.8);
		subScene.setFocusTraversable(true);
		//subScene.widthProperty().bind(rootPane.widthProperty());
		//subScene.heightProperty().bind(rootPane.heightProperty());
		subScene.setCamera(camera);
		root.getChildren().addAll(nodes);
	//   camera.defaultcamera(root,subScene);
	//	camera.adjustCamera(root,subScene);
		
    
	    

		return subScene;
	}
}
