package de.cesr.crafty.gui.utils.camera;


import javafx.event.EventHandler;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * @author Mohamed Byari
 *
 */

public class Camera extends PerspectiveCamera {
	public CameraTransformer cameraTransform = new CameraTransformer();
	public double mousePosX;
	public double mousePosY;
	private double mouseOldX;
	private double mouseOldY;
	private double mouseDeltaX;
	private double mouseDeltaY;
	public static String mousType="hand";

	public Camera() {
		cameraTransform.getChildren().add(this);
//		setFarClip(1000000.0);
//		setTranslateZ(-3000);
//		setTranslateX(4000);
//		setTranslateY(3500);
	}

	public void cameraKeyCodeControl(SubScene subScene) {
		subScene.setOnKeyPressed(event -> {
			double change = 100.0;
			// Add shift modifier to simulate "Running Speed"
			if (event.isShiftDown()) {
				change = 500.0;
			}
			// What key did the user press?
			KeyCode keycode = event.getCode();
			// Step 2c: Add Zoom controls
			if (keycode == KeyCode.S) {
				setTranslateZ(getTranslateZ() + change);
			}
			if (keycode == KeyCode.M) {
				setTranslateZ(getTranslateZ() - change);
			}
			// Step 2d: Add Strafe controls
			if (keycode == KeyCode.D) {
				setTranslateX(getTranslateX() - change);
			}
			if (keycode == KeyCode.A) {
				setTranslateX(getTranslateX() + change);
			}
			if (keycode == KeyCode.W) {
				setTranslateY(getTranslateY() + change);
			}
			if (keycode == KeyCode.X) {
				setTranslateY(getTranslateY() - change);
			}
			// default camera
			if (keycode == KeyCode.H) {
				// defaultcamera(4000, 3500, -3000);
			}

		});
	}

	/*
	 * public void defaultcamera(double X, double Y, double Z) {
	 * cameraTransform.t.setX(0); cameraTransform.t.setY(0); setTranslateX(X);
	 * setTranslateY(Y); setTranslateZ(Z); }
	 */

	public void defaultcamera(Canvas canvas,SubScene subScene) { 
		//cameraMousControl(subScene);
	//	cameraKeyCodeControl(subScene);
	//	newzoom(subScene);

		subScene.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {});
		
		cameraTransform.reset();
		cameraTransform.t.setX((canvas.getWidth()-subScene.getWidth())/2);
		cameraTransform.t.setY((canvas.getHeight()-subScene.getHeight())/2);
		double subMax= subScene.getHeight()+ subScene.getWidth();
		double canvasMax= canvas.getHeight()+canvas.getWidth();
		cameraTransform.t.setZ(subMax-canvasMax*1.4);

	}
	
	
	
	

	


	
	private void initialzeMousePosition(SubScene scene) {
		scene.setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});
	}
	


	public void cameraMousControl(SubScene scene, String mousType) {

		initialzeMousePosition(scene);
		scene.setOnMouseDragged((MouseEvent me) -> {
			if (!isHover()) {
				mouseOldX = mousePosX;
				mouseOldY = mousePosY;
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
				mouseDeltaX = (mousePosX - mouseOldX);
				mouseDeltaY = (mousePosY - mouseOldY);

				double modifier = 60.0;
				double modifierFactor = 0.1;

				if (me.isControlDown()) {
					modifier = 0;
				}
				if (me.isShiftDown()) {
					modifier = 100.0;
				}
				if (me.isPrimaryButtonDown() && mousType.equalsIgnoreCase("hand")) {
					cameraTransform.t.setX(cameraTransform.t.getX() - mouseDeltaX * modifierFactor * modifier * 0.3); // -
					cameraTransform.t.setY(cameraTransform.t.getY() - mouseDeltaY * modifierFactor * modifier * 0.3); // -

				} else if (me.isPrimaryButtonDown() && mousType.equalsIgnoreCase("zoom")) {
					
					double newZ = cameraTransform.t.getZ() + (mouseDeltaX - mouseDeltaY) * modifierFactor * modifier;
					cameraTransform.t.setZ(newZ);
				}
				else if (me.isPrimaryButtonDown() && mousType.equalsIgnoreCase("pointer")){
//					cameraTransform.t.setX(cameraTransform.t.getX());
//					cameraTransform.t.setY(cameraTransform.t.getY());
//					cameraTransform.t.setZ(cameraTransform.t.getZ());
					
				}

			}
		});
	}
	
	public void zoom(double value) {
		
		cameraTransform.t.setZ(cameraTransform.t.getZ()+value);

	}
	

	public void newzoom(SubScene scene) {
		scene.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				double deltaY = event.getDeltaY();

				setTranslateZ(getTranslateZ() + deltaY);

				event.consume();
//				System.out.println(
//						cameraTransform.t.getX()+","+
//								cameraTransform.t.getY()+","+
//								getTranslateX()+","+
//								getTranslateY()+","+
//								getTranslateZ());
			}
		});
	}

	public void cameraFocusBox() {
//		cameraTransform.t.setX(0);
//		cameraTransform.t.setY(0);
//		cameraTransform.t.setZ(0);
		cameraTransform.reset();
		setTranslateX(0);
		setTranslateY(0);
		setTranslateZ(-3000);
		
	}
}
