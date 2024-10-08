package UtilitiesFx.filesTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dataLoader.PathsLoader;
import main.FxMain;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/**
 * @author Mohamed Byari
 *
 */

public class PathTools {

	private static final Logger LOGGER = LogManager.getLogger(PathTools.class);
	
	
	
	
    public static String[] aggregateArrays(String[] firstArray, String...secondArray) {
        String[] result = new String[firstArray.length + secondArray.length];
        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }
	
	public static Set<Path> listSubdirectories(Path directoryPath)  {
        // Use try-with-resources to ensure the stream is closed properly
        try (Stream<Path> paths = Files.list(directoryPath)) {
            return paths.filter(Files::isDirectory) // Filter to include only directories
                        .collect(Collectors.toSet()); // Collect results into a set to eliminate duplicates
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }
	public static Optional<Path> findFolder(Set<Path> paths, String FolderName) {
        return paths.stream()
                    .filter(path -> path.getFileName().toString().equals(FolderName))
                    .findFirst(); // returns an Optional<Path>
    }
	

	public static String asFolder(String input) {
		return File.separator + input + File.separator;
	}

	static void creatListPaths(final File folder, ArrayList<Path> Listpathe) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				creatListPaths(fileEntry, Listpathe);
			} else {
				Listpathe.add(fileEntry.toPath());
			}
		}
	}

	public static ArrayList<Path> fileFilter(String... condition) {
		return fileFilter(false, condition);
	}

	public static ArrayList<Path> fileFilter(boolean ignoreIfFileNotExists, String... condition) {

		ArrayList<Path> turn = new ArrayList<>();
		PathsLoader.getAllfilesPathInData().forEach(e -> {
			boolean testCodition = true;
			for (int j = 0; j < condition.length; j++) {
				if (!e.toString().contains(condition[j])) {
					testCodition = false;
					break;
				}
			}
			if (testCodition)
				turn.add(e);
		});
		String str = "";
		for (int j = 0; j < condition.length; j++) {
			str = condition[j] + " " + str;
		}

		if (turn.size() == 0) {
			if (ignoreIfFileNotExists) {
				LOGGER.warn(" File ignored because there is no file in its path with all these key worlds : "
						+ Arrays.toString(condition));
				return null;
			}
			return null;
//			return fileFilter(ignoreIfFileNotExists,
//					WarningWindowes.alterErrorNotFileFound("The file path could not be found:", str));
		} else {
			return turn;
		}
	}

	public static ArrayList<Path> findAllFiles(Path path) {
		ArrayList<Path> Listpathe = new ArrayList<>();
		final File folder = path.toFile();
		creatListPaths(folder, Listpathe);
		return Listpathe;
	}

	public static String read(String filePath) {
		Scanner scanner;
		String line = "";
		try {
			scanner = new Scanner(new File(filePath));

			while (scanner.hasNextLine()) {
				line = line + "\n" + scanner.nextLine();
			}
		} catch (FileNotFoundException e) {
		}
		return line;
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

	public static File selecFile(String projectPath) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select Project");
		File initialDirectory = new File(projectPath);
		chooser.setInitialDirectory(initialDirectory);
		File selectedDirectory = chooser.showOpenDialog(FxMain.primaryStage);
		return selectedDirectory;
	}

	static public void writeFile(String path, String text, boolean keepTxt) {
		File file = new File(path);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, keepTxt))) {
			writer.write(text);
		} catch (IOException ex) {
			LOGGER.error("Error writing to file: " + ex.getMessage());
		}
	}

	static public void writePathRecentProject(String path, String text) {
		String paths = PathTools.read(path);
		if (!paths.contains(text)) {
			File file = new File(path);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
				writer.write(text);
			} catch (IOException ex) {
				LOGGER.error("Error writing to file: " + ex.getMessage());
			}
		}
	}

	public static List<File> detectFolders(String folderPath) {
		List<File> filePaths = new ArrayList<>();
		File folder = new File(folderPath);

		// Check if the folder exists and is a directory
		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles();

			// Iterate over the files in the folder
			for (File file : files) {
				// Check if it is a directory
				if (file.isDirectory()) {
					filePaths.add(file);
				}
			}
		} else {
			LOGGER.error("Folder not found: " + folderPath);
		}
		return filePaths;
	}

	public static String makeDirectory(String dir) {
		File directory = new File(dir);
		if (!directory.exists()) {
			directory.mkdir();
		}
		return dir;
	}

}
