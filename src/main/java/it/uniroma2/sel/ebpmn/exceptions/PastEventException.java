package it.uniroma2.sel.ebpmn.exceptions;

/**
 * Checked exception thrown when the engine encounters an event whose timestamp
 * lies in the past relative to the current simulation time.
 *
 * <p>This is a fatal simulation error indicating a modelling bug (e.g., a
 * zero-delay cycle that generates events with decreasing timestamps).
 * Thrown by {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine#run()} when
 * the next event time is less than the current simulation time.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see BPMNException
 */
public class PastEventException extends Exception{

	/** Creates a PastEventException with no detail message. */
	public PastEventException() {
		super();
	}

	/**
	 * Creates a PastEventException with the given detail message.
	 *
	 * @param message description of the offending event and its timestamp
	 */
	public PastEventException(String message) {
		super(message);
	}

}
