package de.cesr.crafty.core.utils.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.utils.analysis.CustomLogger;

/**
 * @author Mohamed Byari
 *
 */

public class PathTools {

	private static final CustomLogger LOGGER = new CustomLogger(PathTools.class);

	public static String[] aggregateArrays(String[] firstArray, String... secondArray) {
		String[] result = new String[firstArray.length + secondArray.length];
		System.arraycopy(firstArray, 0, result, 0, firstArray.length);
		System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
		return result;
	}

	public static Set<Path> listSubdirectories(Path directoryPath) {// used for Plum coupling
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

	public static String asFolder(String input) {
		return File.separator + input + File.separator;
	}

	static void creatListPaths(final File folder, ArrayList<Path> Listpathe) {
		try {
			for (final File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					creatListPaths(fileEntry, Listpathe);
				} else {
					Listpathe.add(fileEntry.toPath());
				}
			}
		} catch (NullPointerException e) {
			LOGGER.fatal(" \n Fatal error. Project folder Path not fund " + folder);
		}
	}

	public static ArrayList<Path> fileFilter(String... condition) {
		return fileFilter(false, condition);
	}

	public static ArrayList<Path> fileFilter(boolean ignoreIfFileNotExists, String... condition) {
		return fileFilter(ProjectLoader.getAllfilesPathInData(), ignoreIfFileNotExists, condition);
	}

	public static ArrayList<Path> fileFilter(ArrayList<Path> getAllfilesPathInFolder, String... condition) {
		return fileFilter(getAllfilesPathInFolder, false, condition);
	}

	public static ArrayList<Path> fileFilter(ArrayList<Path> getAllfilesPathInFolder, boolean ignoreIfFileNotExists,
			String... condition) {

		ArrayList<Path> turn = new ArrayList<>();
		getAllfilesPathInFolder.forEach(e -> {
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
		} else {
			return turn;
		}
	}

	public static ArrayList<Path> findAllFilePaths(Path path) {
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
			LOGGER.warn("Folder not found: " + folderPath);
		}
		return filePaths;
	}

	public static List<Path> getAllFolders(String rootFolder) {
		Path root = Paths.get(rootFolder);
		try {
			if (!Files.isDirectory(root)) {
				throw new IllegalArgumentException("Provided path is not a directory: " + rootFolder);
			}
			try (Stream<Path> stream = Files.walk(root)) {
				return stream.filter(Files::isDirectory) // Include only directories
						.filter(path -> !path.equals(root)) // Exclude the root folder itself
						.collect(Collectors.toList());
			}
		} catch (IOException e) {
			return null;
		}
	}

	public static String makeDirectory(String dir) {
		if (dir == null) {
			return null;
		}
		File directory = new File(dir);
		if (!directory.exists()) {
			// Use mkdirs() if you want to create all parent dirs automatically
			boolean created = directory.mkdirs();
			if (!created) {
				return null;
			}
		}
		return dir;
	}

}
