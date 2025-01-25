package de.cesr.crafty.main;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {
	public static String configPath = "/config.yaml";
	public static Config config = ConfigLoader.loadConfig();

	public static void init() {
		config = loadConfig();
	}

	public static Config loadConfig() {
		// Load resource as a stream from the classpath
		InputStream inputStream = null;
		if (Files.exists(Paths.get(configPath))) {
			try {// Load from absolute file path
				inputStream = new FileInputStream(configPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Load from classpath
			inputStream = ConfigLoader.class.getResourceAsStream(configPath);
			if (inputStream == null) {
				throw new IllegalArgumentException("Resource not found: " + configPath);
			}
		}

		// Use Constructor with LoaderOptions
		Constructor constructor = new Constructor(Config.class, new LoaderOptions());
		Yaml yaml = new Yaml(constructor);

		return yaml.load(inputStream);
	}
}
