package it.uniroma2.sel.ebpmn.logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract base class for simulation event loggers in the eBPMN framework.
 *
 * <p>Concrete subclasses are responsible for writing simulation output in a
 * specific format (e.g., CSV). Each time a simulation event of interest occurs,
 * the {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine} (or a {@link it.uniroma2.sel.ebpmn.bpmn.Node}) calls {@link #write(LogData)}
 * with a populated {@link LogData} value object. When the simulation ends,
 * {@link #closeLog()} must be called to flush and release the underlying file
 * resource.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see CSVLogger
 * @see LogData
 */
public abstract class Logger {

	/** Buffered writer used by subclasses to append rows to the output file. */
	protected BufferedWriter writer;

	/**
	 * Opens the output file at the given path and initialises the underlying
	 * {@link BufferedWriter}.
	 *
	 * <p>If the file cannot be created or opened, the stack trace is printed
	 * and {@code writer} is left {@code null}; subclasses must therefore guard
	 * against a {@code null} writer in their {@link #write(LogData)} implementation.</p>
	 *
	 * @param fileName path (relative or absolute) of the output file to create or overwrite
	 */
	public Logger(String fileName) {
		try {
			Path path = Paths.get(fileName);
			Files.createDirectories(path.getParent());
			writer = Files.newBufferedWriter(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes a single log entry to the output.
	 *
	 * <p>Implementations must serialise all fields of {@code data} into the
	 * format specific to the concrete logger (e.g., a comma-separated row)
	 * and append them to the underlying writer.</p>
	 *
	 * @param data the log entry to write; must not be {@code null}
	 */
	public abstract void write(LogData data);

	/**
	 * Flushes and closes the underlying writer, finalising the output file.
	 *
	 * <p>This method should be called once at the end of the simulation run.
	 * After this call, further invocations of {@link #write(LogData)} will
	 * produce an {@link IOException}.</p>
	 */
	public void closeLog() {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
