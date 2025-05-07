package de.cesr.crafty.gui.canvasFx;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.cesr.crafty.gui.utils.graphical.ColorsTools;
import de.cesr.crafty.gui.utils.graphical.NewWindow;
import de.cesr.crafty.gui.utils.graphical.SaveAs;
import de.cesr.crafty.gui.utils.graphical.Tools;
import de.cesr.crafty.gui.controller.fxml.RegionController;
import de.cesr.crafty.gui.main.FxMain;
import de.cesr.crafty.core.utils.analysis.CustomLogger;
import de.cesr.crafty.core.dataLoader.AftCategorised;
import de.cesr.crafty.core.dataLoader.CellsLoader;
import de.cesr.crafty.core.dataLoader.MaskRestrictionDataLoader;
import de.cesr.crafty.core.dataLoader.ServiceSet;
import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.model.Cell;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Mohamed Byari
 *
 */

public class CellsCanvas {
	private static final CustomLogger LOGGER = new CustomLogger(CellsCanvas.class);
	public static boolean isPlotedMap = false;
	private static Canvas canvas;
	public static GraphicsContext gc;
	public static PixelWriter pixelWriter;
	public static WritableImage writableImage;

	private static String colortype = "AFT";
	// private static CellsLoader cellsSet;

	public static Group root = new Group();
	public static SubScene subScene = new SubScene(root, FxMain.defaultWidth / 2, FxMain.defaultHeight);

	public static void plotCells() {
		isPlotedMap = true;

		LOGGER.info(
				"matrix size: " + (CellsLoader.maxX - CellsLoader.minX) + "," + (CellsLoader.maxY - CellsLoader.minY));
		canvas = new Canvas((CellsLoader.maxX - CellsLoader.minX) * Cell.getSize(),
				(CellsLoader.maxY - CellsLoader.minY) * Cell.getSize());
		gc = canvas.getGraphicsContext2D();
		writableImage = new WritableImage(CellsLoader.maxX, CellsLoader.maxY);
		pixelWriter = writableImage.getPixelWriter();
		root.getChildren().clear();
		root.getChildren().add(canvas);

		subScene.setCamera(FxMain.camera);
		FxMain.camera.defaultcamera(canvas, subScene);
		LOGGER.info("Number of cells = " + CellsLoader.hashCell.size());
		MapControlerBymouse();
	}

	public static void ColorP(Cell c, Color color) {
		pixelWriter.setColor(c.getX(), c.getY(), color);
	}

	public static void ColorP(Cell c, String color) {
		ColorP(c, Color.web(color));
	}

	public void ColorP(Cell c) {
		ColorP(c, c.getColor());
	}

	public static ConcurrentHashMap<String, Cell> getSubset(ConcurrentHashMap<String, Cell> cellsHash,
			double percentage) {

		int numberOfElementsToSelect = (int) (cellsHash.size() * (percentage));
		ConcurrentHashMap<String, Cell> subset = new ConcurrentHashMap<>();
		cellsHash.keySet().parallelStream().unordered().limit(numberOfElementsToSelect)
				.forEach(key -> subset.put(key, cellsHash.get(key)));
		return subset;
	}

	public static void colorMap(String str) {
		colortype = str;
		colorMap();
	}

	public static void showOnlyOneAFT(Aft a) {
		CellsLoader.hashCell.values().parallelStream().forEach(cell -> {
			if (cell.getOwner() == null || !cell.getOwner().getLabel().equals(a.getLabel())) {
				ColorP(cell, Color.GRAY);
			} else {
				ColorP(cell, a.getColor());
			}
		});
		gc.drawImage(writableImage, 0, 0);
	}

	public static void colorMap() {
		if (!isPlotedMap) {
			return;
		}
		LOGGER.info("Changing the map colors...");
		Set<Double> values = Collections.synchronizedSet(new HashSet<>());
		if (colortype.equalsIgnoreCase("Agent") || colortype.equalsIgnoreCase("AFT")) {
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				if (c.getOwner() != null) {
					ColorP(c, c.getOwner().getColor());
				} else {
					ColorP(c, Color.GRAY);
				}
			});
		} else if (CellsLoader.getCapitalsList().contains(colortype)) {
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				ColorP(c, ColorsTools.getColorForValue(c.getCapitals().get(colortype)));
			});

		} else if (ServiceSet.getServicesList().contains(colortype)) {
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				if (c.getCurrentProductivity().get(colortype) != null)
					values.add(c.getCurrentProductivity().get(colortype));
			});
			double max = values.size() > 0 ? Collections.max(values) : 0;

			CellsLoader.hashCell.values().parallelStream().forEach(c -> {

				if (c.getCurrentProductivity().get(colortype) != null) {
					ColorP(c, ColorsTools.getColorForValue(max, c.getCurrentProductivity().get(colortype)));
				} else {
					ColorP(c, ColorsTools.getColorForValue(max, 0));
				}
			});
		} else if (colortype.equalsIgnoreCase("Mask")) {
			ArrayList<String> listOfMasks = new ArrayList<>(MaskRestrictionDataLoader.hashMasksPaths.keySet());
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				if (c.getMaskType() != null) {
					ColorP(c, ColorsTools.colorlist(listOfMasks.indexOf(c.getMaskType())));
				} else {
					ColorP(c, Color.gray(0.75));
				}
			});
		} else if (colortype.equalsIgnoreCase("Categories")) {
			if (AftCategorised.aftCategories.size() != 0) {
				CellsLoader.hashCell.values().parallelStream().forEach(c -> {
					if (c.getOwner() != null)
						ColorP(c, AftCategorised.categoriesColor.get(c.getOwner().getCategory().getName()));
				});
			}
		} else {
			HashMap<String, Color> colorGis = new HashMap<>();

			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				colorGis.put(c.getCurrentRegion()/* .get(colortype) */ , ColorsTools.RandomColor());
			});
			CellsLoader.hashCell.values().parallelStream().forEach(c -> {
				ColorP(c, colorGis.get(c.getCurrentRegion()/* .getGisNameValue().get(colortype) */));
			});
		}
		gc.drawImage(writableImage, 0, 0);
	}

	public static void MapControlerBymouse() {
		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			if (event.getButton() == MouseButton.SECONDARY) {
				// Convert mouse coordinates to "pixel" coordinates
				int pixelX = (int) (event.getX() - (event.getX() % Cell.getSize()));
				int pixelY = (int) (event.getY() - (event.getY() % Cell.getSize()));
				// Convert pixel coordinates to cell coordinates
				int cx = (int) (pixelX / Cell.getSize());
				int cy = (int) (/* maxY - */ pixelY / Cell.getSize());
				if (CellsLoader.hashCell.get(cx + "," + cy) != null) {
					gc.setFill(Color.RED);
					gc.fillRect(pixelX, pixelY, Cell.getSize(), Cell.getSize());
					HashMap<String, Consumer<String>> menu = new HashMap<>();
					menu.put("Print Info into the Console", e -> {
						System.out.println(CellsLoader.hashCell.get(cx + "," + cy));
					});
					menu.put("Save Map as PNG", e -> {
						SaveAs.png("", canvas);
					});
					menu.put("Selecet Region", e -> {
						selectRegion(CellsLoader.hashCell.get(cx + "," + cy));
					});
					menu.put("Clean Regions", e -> {
						box.getChildren().clear();
						RegionController.getRegionCells().clear();
					});
					menu.put("Open Regions Selected", e -> {
						openRegions(CellsLoader.hashCell.get(cx + "," + cy));
					});
					menu.put("Detach", (x) -> {
						try {
							VBox mapBox = (VBox) subScene.getParent();
							VBox parent = (VBox) subScene.getParent().getParent();
							List<Integer> findpath = Tools.findIndexPath(mapBox, parent);
							Tools.reInsertChildAtIndexPath(new Separator(), parent, findpath);
							NewWindow win = new NewWindow();
							win.creatwindows("", mapBox);
							win.setOnCloseRequest(event2 -> {
								parent.getChildren().add(mapBox);
							});
						} catch (ClassCastException d) {
							LOGGER.warn(d.getMessage());
						}
					});

					ContextMenu cm = new ContextMenu();

					MenuItem[] item = new MenuItem[menu.size()];
					AtomicInteger i = new AtomicInteger();
					menu.forEach((k, v) -> {
						item[i.get()] = new MenuItem(k);
						cm.getItems().add(item[i.get()]);
						item[i.get()].setOnAction(e -> {
							v.accept(k);
						});
						i.getAndIncrement();
					});
					cm.show(canvas.getScene().getWindow(), event.getScreenX(), event.getScreenY());
					event.consume();
				}
			}
		});
	}

	static VBox box = new VBox();

	private static void openRegions(Cell c) {
		if (c.getCurrentRegion() != null) {
			Platform.runLater(() -> {
				URL fxml = FxMain.class.getResource("/fxmlControllers/Region.fxml");
				NewWindow win = new NewWindow();
				try {
					box.getChildren().add(FXMLLoader.load(fxml));
					win.creatwindows(c.getCurrentRegion(), box);
				} catch (IOException e) {
				}
			});
		}
	}

	private static void selectRegion(Cell c) {
		CellsLoader.hashCell.values()./* parallelStream(). */forEach(cs -> {
			if (c.getCurrentRegion().equals(cs.getCurrentRegion())) {
				gc.setFill(Color.GRAY);
				gc.fillRect(cs.getX(), cs.getY(), Cell.getSize(), Cell.getSize());
				// initial cells
				Cell newCEll = new Cell(cs.getX(), cs.getY());
				cs.copyCell(newCEll);
				if (cs.getOwner() != null)
					newCEll.setColor(cs.getOwner().getColor());
				else
					newCEll.setColor("#000000");
				RegionController.getRegionCells().put(cs.getX() + "," + cs.getY(), newCEll);
			}
		});
	}

	public static GraphicsContext getGc() {
		return gc;
	}

	public static Canvas getCanvas() {
		return canvas;
	}

	public static void setCanvas(Canvas canvas) {
		CellsCanvas.canvas = canvas;
	}

}
