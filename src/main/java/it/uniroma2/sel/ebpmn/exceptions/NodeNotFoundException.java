package it.uniroma2.sel.ebpmn.exceptions;

/**
 * Unchecked exception thrown when a named {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode}
 * cannot be located within a {@link it.uniroma2.sel.ebpmn.bpmn.Participant}.
 *
 * <p>Raised by {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine#getNode(String, String)}
 * when the node name is not registered under the given participant.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see ParticipantNotFoundException
 */
public class NodeNotFoundException extends RuntimeException {

    /**
     * Creates a NodeNotFoundException for the given node name.
     *
     * @param nodeName the name of the node that was not found
     */
    public NodeNotFoundException(String nodeName) {
        super("Node not found: " + nodeName);
    }
}