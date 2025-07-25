package de.cesr.crafty.gui.canvasFx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;

public class MapPane extends Pane {

	static double scale = 1;
	static double offsetX, offsetY;

	private final Rectangle marquee = new Rectangle();
	private double dragStartX, dragStartY; // screen coords

	private void initMarquee() {
		marquee.setFill(Color.web("#4A90E4", 0.2)); // semi-transparent blue
		marquee.setStroke(Color.web("#4A90E4"));
		marquee.getStrokeDashArray().setAll(6.0, 6.0);
		marquee.setVisible(false);
		getChildren().add(marquee); // above the canvas
	}

	public enum MouseMode {
		SELECT, PAN, ZOOM
	}

	public static MouseMode mouseMode = MouseMode.PAN;

	public MapPane() {
		getChildren().add(CellsCanvas.getCanvas());

		setOnScroll(e -> {
			double factor = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
			scale *= factor;
			redraw();
			e.consume();
		});

		final Delta drag = new Delta();
		wireMouseHandlers(drag);
		initMarquee();

		widthProperty().addListener(_ -> resizeCanvas());
		heightProperty().addListener(_ -> resizeCanvas());

	}

	private void resizeCanvas() {
		CellsCanvas.getCanvas().setWidth(getWidth());
		CellsCanvas.getCanvas().setHeight(getHeight());
		redraw();
	}

	private static void redraw() {
		GraphicsContext g = CellsCanvas.getCanvas().getGraphicsContext2D();
		g.setTransform(new Affine());
		g.clearRect(0, 0, CellsCanvas.getCanvas().getWidth(), CellsCanvas.getCanvas().getHeight());
		g.translate(offsetX, offsetY);
		g.scale(scale, scale);
		CellsCanvas.colorMap();

	}

	public static void fitMapInWindow() {
		double mapW = CellsCanvas.maxX - CellsCanvas.minX; // world units (cells)
		double mapH = CellsCanvas.maxY - CellsCanvas.minY;

		// leave a 10-pixel margin
		double sx = (CellsCanvas.getCanvas().getWidth() - 20) / mapW;
		double sy = (CellsCanvas.getCanvas().getHeight() - 20) / mapH;

		scale = Math.floor(Math.min(sx, sy)); // integer pixels per cell
		if (scale < 1)
			scale = 1; // never < 1 px

		// centre the map
		offsetX = (CellsCanvas.getCanvas().getWidth() - mapW * scale) / 2.0;
		offsetY = (CellsCanvas.getCanvas().getHeight() - mapH * scale) / 2.0;
		redraw();
	}

	public static void zoom(int direction) { // +1 or −1
		int newScale = (int) Math.max(1, scale + direction);
		if (newScale != scale) {
			scale = newScale;
			redraw();
		}
	}

	private void wireMouseHandlers(final Delta drag) {

		setOnMousePressed(e -> {
			if (mouseMode == MouseMode.PAN && e.getButton() == MouseButton.PRIMARY) {
				drag.x = e.getX();
				drag.y = e.getY();
			} // * Zoom-mode mouse handlers
			else if (mouseMode == MouseMode.ZOOM && e.getButton() == MouseButton.PRIMARY) {
				dragStartX = e.getX();
				dragStartY = e.getY();
				marquee.setX(dragStartX);
				marquee.setY(dragStartY);
				marquee.setWidth(0);
				marquee.setHeight(0);
				marquee.setVisible(true);
			}
		});

		setOnMouseDragged(e -> {
			if (mouseMode == MouseMode.PAN && e.getButton() == MouseButton.PRIMARY) {
				offsetX += e.getX() - drag.x;
				offsetY += e.getY() - drag.y;
				drag.x = e.getX();
				drag.y = e.getY();
				redraw();
			}
//			 *  Zoom-mode mouse handlers
			if (mouseMode == MouseMode.ZOOM && marquee.isVisible()) {
				double w = e.getX() - dragStartX;
				double h = e.getY() - dragStartY;
				marquee.setX(w >= 0 ? dragStartX : e.getX());
				marquee.setY(h >= 0 ? dragStartY : e.getY());
				marquee.setWidth(Math.abs(w));
				marquee.setHeight(Math.abs(h));
			}
		});

		setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				CellsCanvas.MapControlerBymouse();
			}
		});
		setOnMouseReleased(_ -> {
			if (mouseMode == MouseMode.ZOOM && marquee.isVisible()) {
				marquee.setVisible(false);
				// Ignore microscopic drags (e.g. accidental clicks)
				if (marquee.getWidth() < 4 || marquee.getHeight() < 4) {
					return;
				}
				zoomToMarquee();
			}
		});

	}

	/** Compute new scale/offset so the marquee fills the canvas. */
	private void zoomToMarquee() {

		// --- screen → world conversion -------------------------------
		double screenMinX = marquee.getX();
		double screenMinY = marquee.getY();
		double screenMaxX = screenMinX + marquee.getWidth();
		double screenMaxY = screenMinY + marquee.getHeight();

		double worldMinX = (screenMinX - offsetX) / scale + CellsCanvas.minX;
		double worldMinY = (screenMinY - offsetY) / scale + CellsCanvas.minY;
		double worldMaxX = (screenMaxX - offsetX) / scale + CellsCanvas.minX;
		double worldMaxY = (screenMaxY - offsetY) / scale + CellsCanvas.minY;

		double worldW = worldMaxX - worldMinX;
		double worldH = worldMaxY - worldMinY;

		// --- choose the largest integer scale that fits ---------------
		double newScale = Math.floor(Math.min((getWidth() - 20) / worldW, (getHeight() - 20) / worldH));
		if (newScale < 1)
			newScale = 1; // never < 1 px / cell

		// --- centre the selected area in the window -------------------
		double newOffsetX = (getWidth() - worldW * newScale) / 2.0 - (worldMinX - CellsCanvas.minX) * newScale;
		double newOffsetY = (getHeight() - worldH * newScale) / 2.0 - (worldMinY - CellsCanvas.minY) * newScale;

		scale = newScale;
		offsetX = Math.round(newOffsetX);
		offsetY = Math.round(newOffsetY);

		redraw();
	}

	private static class Delta {
		double x, y;
	}
}
