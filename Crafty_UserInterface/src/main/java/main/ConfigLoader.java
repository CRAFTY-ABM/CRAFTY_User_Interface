package main;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {

	public static Config config ;//= ConfigLoader.loadConfig("/config.yaml");

	public static void init(String resourcePath) {
		config = loadConfig(resourcePath);
	}

	public static Config loadConfig(String resourcePath) {
		// Load resource as a stream from the classpath
		InputStream inputStream = null;
		if (Files.exists(Paths.get(resourcePath))) {
			try {// Load from absolute file path
				inputStream = new FileInputStream(resourcePath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Load from classpath
			inputStream = ConfigLoader.class.getResourceAsStream(resourcePath);
			if (inputStream == null) {
				throw new IllegalArgumentException("Resource not found: " + resourcePath);
			}
		}

		// Use Constructor with LoaderOptions
		Constructor constructor = new Constructor(Config.class, new LoaderOptions());
		Yaml yaml = new Yaml(constructor);

		return yaml.load(inputStream);
	}
}
