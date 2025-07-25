package de.cesr.crafty.gui.utils.graphical;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import javax.swing.filechooser.FileSystemView;

public class FileTreeView {

	public static TreeView<Path> build(Path rootDir, String fileTypes, int expandDegreel) {
		return build(rootDir, fileTypes, null, expandDegreel);
	}

	/** Build a TreeView that lists folders plus *.csv files only. */
	public static TreeView<Path> build(Path rootDir, String fileTypes, String fileExcluded, int expandDegreel) {
		TreeItem<Path> root = createNode(rootDir, fileTypes, fileExcluded);
		TreeView<Path> tree = new TreeView<>(root);
		tree.setShowRoot(true);

		tree.setCellFactory(_ -> new TextFieldTreeCell<>() {
			@Override
			public void updateItem(Path item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getFileName() == null ? item.toString() : item.getFileName().toString());
					javax.swing.Icon swingIcon = FileSystemView.getFileSystemView().getSystemIcon(item.toFile());
					setGraphic(convertSwingIconToImageView(swingIcon));
				}
			}
		});

		expand(root, expandDegreel);
		return tree;
	}

	public static void expand(TreeItem<?> item, int degree) {
		if (item == null)
			return;

		if (degree < 0 || item == null) {
			expandAll(item);
			return;
		} // openAll
		item.setExpanded(true); // open the current branch
		if (degree == 0)
			return; // stop here
		for (TreeItem<?> child : item.getChildren()) {
			expand(child, degree - 1); // recurse one level shallower
		}
	}

	private static void expandAll(TreeItem<?> item) {
		item.setExpanded(true); // open the current branch
		for (TreeItem<?> child : item.getChildren()) {
			expandAll(child); // recurse
		}
	}

	// ---------- internals
	// --------------------------------------------------------------

	private static boolean show(Path p, String fileType, String fileExcluded) {
		if (fileType == null || fileType.isEmpty()) {
			return Files.isDirectory(p);
		}

		if (fileExcluded != null && p.getFileName().toString().toLowerCase().contains(fileExcluded.toLowerCase())) {
			return false;
		}
		return Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(fileType);
	}

	private static TreeItem<Path> createNode(final Path path, String fileType, String fileExcluded) {
		return new TreeItem<>(path) {
			private boolean childrenLoaded, leafComputed, isLeafCache;

			@Override
			public ObservableList<TreeItem<Path>> getChildren() {
				if (!childrenLoaded) {
					childrenLoaded = true;
					super.getChildren().setAll(buildChildren(this));
				}
				return super.getChildren();
			}

			@Override
			public boolean isLeaf() {
				if (!leafComputed) {
					leafComputed = true;
					isLeafCache = Files.isRegularFile(path);
				}
				return isLeafCache;
			}

			private ObservableList<TreeItem<Path>> buildChildren(TreeItem<Path> parent) {
				Path dir = parent.getValue();
				if (Files.isDirectory(dir)) {
					ObservableList<TreeItem<Path>> kids = FXCollections.observableArrayList();
					try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
						for (Path child : stream) {
							if (show(child, fileType, fileExcluded)) { // <-- filter happens here
								kids.add(createNode(child, fileType, fileExcluded));
							}
						}
					} catch (IOException ignored) {
					}
					kids.sort(Comparator.comparing((TreeItem<Path> ti) -> Files.isRegularFile(ti.getValue()))
							.thenComparing(ti -> ti.getValue().getFileName().toString().toLowerCase()));
					return kids;
				}
				return FXCollections.emptyObservableList();
			}
		};
	}

	private static ImageView convertSwingIconToImageView(javax.swing.Icon swingIcon) {
		if (swingIcon instanceof javax.swing.ImageIcon) {
			BufferedImage bufferedImage = new BufferedImage(swingIcon.getIconWidth(), swingIcon.getIconHeight(),
					BufferedImage.TYPE_INT_ARGB);
			swingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
			Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
			return new ImageView(fxImage);
		}
		return null;
	}
}
