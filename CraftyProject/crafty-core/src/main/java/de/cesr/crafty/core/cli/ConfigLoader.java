package de.cesr.crafty.core.cli;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
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
	    System.out.println(configPath + "->" + (Files.exists(Paths.get(configPath))));
	    InputStream inputStream = null;
	    try {
	        if (Files.exists(Paths.get(configPath))) {
	            // Load from absolute file path
	            inputStream = new FileInputStream(configPath);
	        } else {
	            // Load from classpath
	            inputStream = ConfigLoader.class.getResourceAsStream(configPath);
	        }
	        if (inputStream == null) {
	            System.out.println("Config file not found. Using default config values.");
	            return new Config(); // Return default config
	        }

	        Constructor constructor = new Constructor(Config.class, new LoaderOptions());
	        Yaml yaml = new Yaml(constructor);
	        Config loadedConfig = yaml.load(inputStream);

	        if (loadedConfig == null) {
	            System.out.println("Config file is empty or invalid. Using default config values.");
	            return new Config();
	        }
	        return loadedConfig;
	    } catch (Exception e) {
	        System.out.println("Failed to load config. Using default config values.");
	        e.printStackTrace();
	        return new Config();
	    }
	}


//	public static Config loadConfig() {
//		// Load resource as a stream from the classpath
//		System.out.println(configPath + "->" + (Files.exists(Paths.get(configPath))));
//		InputStream inputStream = null;
//		if (Files.exists(Paths.get(configPath))) {
//			try {
//				// Load from absolute file path
//				inputStream = new FileInputStream(configPath);
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		} else {
//			// Load from classpath
//			inputStream = ConfigLoader.class.getResourceAsStream(configPath);
//			if (inputStream == null) {
//				throw new IllegalArgumentException("Resource not found: " + configPath);
//			}
//		}
//
//		// Use Constructor with LoaderOptions
//		Constructor constructor = new Constructor(Config.class, new LoaderOptions());
//		Yaml yaml = new Yaml(constructor);
//
//		return yaml.load(inputStream);
//	}
}
