package de.cesr.crafty.core.utils.file;


import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class DirectoryWatcher {

	/**
	 * Waits until a directory named {@code prefix + "_" + year} appears under
	 * {@code parentDir}, or throws a TimeoutException if not found before
	 * {@code timeout}.
	 *
	 * @param timeout how long to wait in total
	 * @return Path to the created directory
	 * @throws IOException          if I/O fails
	 * @throws TimeoutException     if the folder wasn't created in time
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	public static void waitForYearFolder(Path path) {
		final Instant deadline = Instant.now().plus(Duration.ofMinutes(30));
		try {
			Path parentDir = path.getParent();
			if (!Files.isDirectory(parentDir)) {
				waitForYearFolder(parentDir);
//				throw new IOException("Parent directory does not exist or is not a directory: " + parentDir);
			}

			final Path targetPath = parentDir.resolve(path);

			try (WatchService ws = parentDir.getFileSystem().newWatchService()) {
				parentDir.register(ws, ENTRY_CREATE, OVERFLOW);
				String s = (existsAsFileOrDir(targetPath) ? "Find the file/directory : " : "Wiating for file: ")
						+ targetPath;
				System.out.print(s);
				while (true) {
					long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
					System.out.print(".");
					// 2) check file
					if (existsAsFileOrDir(targetPath)) {
						System.out.println("Found");
						return;
					}
					// 2) Timeout check
					if (remainingMs <= 0) {
						throw new TimeoutException("Timed out waiting for folder: " + targetPath);
					}
//				 4) Wait for events (use a short poll to re-check periodically on filesystems)
					ws.poll(Math.min(remainingMs, 1000L), TimeUnit.MILLISECONDS);
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static boolean existsAsFileOrDir(Path p) {
		return (Files.isRegularFile(p) || Files.isDirectory(p));
	}

	// --- Example usage ---
	public static void main(String[] args) {
		try {
			Path parent = Paths.get("C:\\Users\\byari-m\\Documents\\Data\\PLUM\\PLUM_output\\run2\\crafty\\x\\y");

			waitForYearFolder(parent);
			System.out.println("----");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
