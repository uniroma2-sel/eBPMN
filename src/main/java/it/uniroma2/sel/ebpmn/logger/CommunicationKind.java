package it.uniroma2.sel.ebpmn.logger;

/**
 * Classifies the inter-participant communication direction recorded in a log entry.
 *
 * <p>Used by {@link LogData} to annotate each activity execution with its
 * message-flow role for process mining and conformance checking tools.
 * The {@link #ND} constant indicates that no inter-participant communication
 * is associated with the activity; it renders as an empty string in CSV output.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see LogData
 * @see it.uniroma2.sel.ebpmn.logger.CSVLogger
 */
public enum CommunicationKind {
	/** The activity sends a message to another participant. */
	send,
	/** The activity receives a message from another participant. */
	receive,
	/** The activity both sends and receives (e.g., a synchronous exchange). */
	send_receive,
	/** No inter-participant communication (renders as empty string in logs). */
	ND;

	/** Returns the constant name, or an empty string for {@link #ND}. */
	@Override
    public String toString() {
        return this == ND ? "" : this.name();
    }
}
