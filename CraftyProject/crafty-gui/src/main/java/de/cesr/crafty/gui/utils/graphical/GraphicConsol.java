package de.cesr.crafty.gui.utils.graphical;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * @author Mohamed Byari
 *
 */

public class GraphicConsol {
	private static PrintStream originalOut = System.out;
	private static PrintStream originalErr = System.err;

	public static void restoreOutput() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	public static void start(TextArea console) {
		console.setEditable(false);
		// Set an uncaught exception handler for the JavaFX Application Thread
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
			appendText(console, "Exception on " + thread.getName() + ": " + throwable + "\n");
		});

		// Redirect output streams to the console
		ConsoleOutputCapturer.captureOutput(console);

	}

	private static void appendText(TextArea textArea, String text) {
		Platform.runLater(() -> textArea.appendText(text));
	}

}

class ConsoleOutputCapturer {
	public static void captureOutput(TextArea console) {
		OutputStream consoleOut = new Console(console);
		System.setOut(new PrintStream(consoleOut, true));
		System.setErr(new PrintStream(consoleOut, true));
	}
}

class Console extends OutputStream {
    private final TextArea output;
    private final StringBuffer buffer = new StringBuffer();

    public Console(TextArea ta) {
        this.output = ta;
    }

    @Override
    public synchronized void write(int b) {
        buffer.append((char) b);  // Buffer the data
        if ((char) b == '\n') {
            flushBuffer();  // Flush when a newline character is received
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        String message = new String(b, off, len, StandardCharsets.UTF_8);
        buffer.append(message);  // Append the message to the buffer
        if (message.contains("\n")) {
            flushBuffer();  // Flush the buffer if newline is present in the message
        }
    }

    private void flushBuffer() {
        String text = buffer.toString();  // Convert buffer to String
        buffer.setLength(0);  // Clear the buffer
        Platform.runLater(() -> output.appendText(text));  // Safely update TextArea on the JavaFX thread
    }
}
