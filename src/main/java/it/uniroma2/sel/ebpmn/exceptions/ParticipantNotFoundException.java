package it.uniroma2.sel.ebpmn.exceptions;

/**
 * Unchecked exception thrown when a named {@link it.uniroma2.sel.ebpmn.bpmn.Participant}
 * cannot be located in the {@link it.uniroma2.sel.ebpmn.bpmn.Collaboration}.
 *
 * <p>Raised by {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine#getNode(String, String)}
 * when the participant name is not registered in the model.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see NodeNotFoundException
 */
public class ParticipantNotFoundException extends RuntimeException {

    /**
     * Creates a ParticipantNotFoundException for the given participant name.
     *
     * @param participantName the name of the participant that was not found
     */
    public ParticipantNotFoundException(String participantName) {
        super("Participant not found: " + participantName);
    }
}
