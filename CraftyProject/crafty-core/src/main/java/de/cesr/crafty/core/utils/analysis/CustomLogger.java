package de.cesr.crafty.core.utils.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import de.cesr.crafty.core.cli.ConfigLoader;

/**
 * @author Mohamed Byari
 *
 */

public class CustomLogger {

	private final Logger logger;

	public CustomLogger(Class<?> c) {
		this.logger = LogManager.getLogger(c);
	}

	public void info(String message) {
		if (ConfigLoader.config.LOGGER_info)
			logger.info(message);
	}

	public void warn(String message) {
		if (ConfigLoader.config.LOGGER_warn)
			logger.warn(message);
	}

	public void error(String message) {
		logger.error(message);
	}

	public void debug(String message) {
		logger.debug(message);
	}

	public void trace(String message) {
		if (ConfigLoader.config.LOGGER_trace)
			logger.trace(message);
	}

	public void fatal(String message) {
		logger.fatal(message);
		System.exit(1);
	}

	public static void configureLogger(Path logFilePath) {
		ensureDirectoryExists(logFilePath);

		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();

		// Create a layout for the log messages
		PatternLayout layout = PatternLayout.newBuilder().withPattern("%d{HH:mm:ss} - %-5level: [%logger{36}] - %msg%n")
				.build();

		// Create the FileAppender

		FileAppender appender = FileAppender.newBuilder().withFileName(logFilePath.toString())
				.setName("DynamicFileAppender").setLayout(layout).withAppend(false) // Append to the file if it exists
				.setConfiguration(config) // Pass the configuration
				.build();

		// Start the appender
		appender.start();

		// Add the appender to the root logger
		config.addAppender(appender);
		config.getRootLogger().addAppender(appender, null, null);

		// Update the logger context
		context.updateLoggers();
	}

	private static void ensureDirectoryExists(Path logFilePath) {
		Path directory = logFilePath.getParent();
		if (directory != null && !Files.exists(directory)) {
			try {
				Files.createDirectories(directory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // Create directories if they don't exist
		}
	}

}
