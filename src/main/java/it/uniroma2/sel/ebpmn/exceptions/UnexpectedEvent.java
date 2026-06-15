package it.uniroma2.sel.ebpmn.exceptions;

/**
 * Checked exception thrown when a {@link it.uniroma2.sel.ebpmn.bpmn.Node}
 * receives an event type it is not designed to handle.
 *
 * <p>Each concrete node implements the visitor callbacks
 * ({@code handleEvent(IncomingToken)}, {@code handleEvent(IncomingMessage)},
 * etc.) and delegates to {@code unexpectedEvent(Event)} for any callback that
 * should never be invoked for that node type.  That method throws this
 * exception.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see BPMNException
 */
public class UnexpectedEvent extends Exception{

	/** Creates an UnexpectedEvent with no detail message. */
	public UnexpectedEvent() {
		super();
	}

	/**
	 * Creates an UnexpectedEvent with the given detail message.
	 *
	 * @param message description of the unexpected event
	 */
	public UnexpectedEvent(String message) {
		super(message);
	}

}
