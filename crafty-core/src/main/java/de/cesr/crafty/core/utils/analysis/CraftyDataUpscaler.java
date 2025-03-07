package de.cesr.crafty.core.utils.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.dataLoader.ProjectLoader;
import de.cesr.crafty.core.dataLoader.ReaderFile;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.core.utils.file.PathTools;
import de.cesr.crafty.core.utils.general.Utils;

public class CraftyDataUpscaler {
	static double scale = 2;
	static String DataFolderPath;

	static void createDataTemplate() {
		copyFolder(ProjectLoader.getProjectPath() + File.separator + "agents",
				DataFolderPath + File.separator + "agents");
		copyFolder(ProjectLoader.getProjectPath() + File.separator + "csv", DataFolderPath + File.separator + "csv");
		copyFolder(ProjectLoader.getProjectPath() + File.separator + "production",
				DataFolderPath + File.separator + "production");
		copyFolder(ProjectLoader.getProjectPath() + File.separator + "services",
				DataFolderPath + File.separator + "services");
		PathTools.makeDirectory(DataFolderPath + File.separator + "output");
	}

	static void upscaleCsvMap(Path pathInput, Path pathoutput) {
		HashMap<String, ArrayList<String>> reader = ReaderFile.ReadAsaHash(pathInput);
		Map<String, HashMap<String, String>> newMap = new HashMap<>();
		String xx = reader.get("X") != null ? "X" : "x";
		String yy = reader.get("Y") != null ? "Y" : "y";

		for (int i = 0; i < reader.get(xx).size(); i++) {
			int x = (int) (Utils.sToD(reader.get(xx).get(i)) / scale);
			int y = (int) (Utils.sToD(reader.get(yy).get(i)) / scale);
			HashMap<String, String> line = new HashMap<>();
			line.put("X", String.valueOf(x));
			line.put("Y", String.valueOf(y));
			for (String colmunName : reader.keySet()) {
				if (!colmunName.equals(xx) && !colmunName.equals(yy) && !colmunName.equals("C0")
						&& !colmunName.equals(""))
					line.put(colmunName, reader.get(colmunName).get(i));
			}
			newMap.put(x + "," + y, line);
		}

		String[][] csv = new String[newMap.size() + 1][newMap.values().iterator().next().size()];

		ArrayList<String> ky = new ArrayList<>(newMap.values().iterator().next().keySet());
		AtomicInteger i = new AtomicInteger(1);
		csv[0][0] = "X";
		csv[0][1] = "Y";
		int k = 2;
		for (String s : ky) {
			if (!s.equals("X") && !s.equals("Y")) {
				csv[0][k++] = s;
			}
		}
		newMap.forEach((coor, line) -> {
			line.forEach((kye, value) -> {
				int index = Utils.indexof(kye, csv[0]);
				if (index != -1) {
					csv[i.get()][index] = value;
				}
			});
			i.getAndIncrement();
		});
		CsvTools.writeCSVfile(csv, pathoutput);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ProjectLoader.modelInitialisation();
		DataFolderPath = PathTools.makeDirectory(ProjectLoader.getProjectPath() + "_upscaled_" + scale);
		createDataTemplate();
		System.out.println(DataFolderPath);
		folderUpscaler(ProjectLoader.getProjectPath() + PathTools.asFolder("worlds"));
		folderUpscaler(ProjectLoader.getProjectPath() + PathTools.asFolder("GIS"));

	}

	static void folderUpscaler(String folderPath) {
		PathTools.makeDirectory(oToUp(folderPath.toString()));
		List<Path> listSubFolders = PathTools.getAllFolders(folderPath);
		listSubFolders.forEach(l -> {
			System.out.println(l);
			PathTools.makeDirectory(oToUp(l.toString()));
		});
		ArrayList<Path> foldersinCapitals = PathTools.fileFilter(folderPath);
		foldersinCapitals.forEach(path -> {
			if (path.toString().contains(".csv") && !path.toString().contains("Restrictions"))
				upscaleCsvMap(path, Paths.get(oToUp(path.toString())));
		});
	}

	static String oToUp(String path) {
		return path.replace(ProjectLoader.getProjectPath().toString(), DataFolderPath);
	}

	public static void copyFolder(String sourcePath, String destinationPath) {
		try {
			Path sourceDirectory = Paths.get(sourcePath);
			Path destinationDirectory = Paths.get(destinationPath);
			// Ensure source exists
			if (!Files.exists(sourceDirectory)) {
				throw new IllegalArgumentException("Source path does not exist: " + sourcePath);
			}
			// Create the destination directory if it does not exist
			if (!Files.exists(destinationDirectory)) {

				Files.createDirectories(destinationDirectory);
			}
			// Walk through the source directory and copy each file/directory
			Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path targetDir = destinationDirectory.resolve(sourceDirectory.relativize(dir));
					if (!Files.exists(targetDir)) {
						Files.createDirectory(targetDir);
					}
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path targetFile = destinationDirectory.resolve(sourceDirectory.relativize(file));
					Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
