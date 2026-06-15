package it.uniroma2.sel.ebpmn.bpmn;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * Abstract base class for every addressable entity in the eBPMN framework.
 *
 * <p>A {@code Node} represents any element that can receive simulation events.
 * There are two concrete subtypes:
 * <ul>
 *   <li>{@link FlowNode} — a local BPMN element that schedules events on the
 *       local {@code ExecutionEngine}.</li>
 *   <li>{@code RemoteNode} — a proxy that forwards events to a remote
 *       {@link Participant} via the HLA RTI.</li>
 * </ul>
 *
 * <p>Both subtypes share this common identity ({@code name}, {@code participant})
 * and the polymorphic {@code receiveEvent} dispatch interface, so the application
 * model code does not need to distinguish between local and distributed targets.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see FlowNode
 * @see RemoteNode
 * @see Participant
 */
public abstract class Node {

    /** Human-readable identifier for this node within the collaboration. */
    private String name;

    /** The {@link Participant} that owns this node. {@code null} for {@code RemoteNode}. */
    private Participant participant;

    /**
     * Creates a node with both a name and an owning participant.
     *
     * @param n the node name
     * @param p the {@link Participant} that contains this node
     */
    public Node(String n, Participant p){
        this.name = n;
        this.participant = p;
    }

    /**
     * Creates a node with only a name (no owning participant).
     * Used when the participant is assigned later, e.g. for {@code RemoteNode}.
     *
     * @param n the node name
     */
    public Node(String n){
        this.name = n;
    }

    /**
     * Returns the name of this node.
     *
     * @return the node name
     */
    public String getName() {return name;}

    /**
     * Returns the {@link Participant} that owns this node, or {@code null} if not set.
     *
     * @return the owning participant
     */
    public Participant getParticipant() {return participant;}

    /**
     * Sets the owning participant. Called during model construction when the
     * participant is not known at node creation time.
     *
     * @param p the {@link Participant} to assign
     */
    public void setParticipant(Participant p) {this.participant=p;}

    /**
     * Handles an incoming control-flow token.
     *
     * <p>A {@link FlowNode} schedules a local event on the {@code ExecutionEngine};
     * a {@code RemoteNode} proxy sends the corresponding HLA interaction to the RTI,
     * which maps it to a local event on the target federate's engine.
     *
     * @param e the incoming token event
     * @throws UnexpectedEvent if this node does not accept {@link IncomingToken} events
     */
    public abstract void receiveEvent(IncomingToken e) throws UnexpectedEvent;

    /**
     * Handles an incoming message-flow event (local or HLA-delivered).
     *
     * @param e the incoming message event
     * @throws UnexpectedEvent if this node does not accept {@link IncomingMessage} events
     */
    public abstract void receiveEvent(IncomingMessage e) throws UnexpectedEvent;

    /**
     * Handles the completion of a task's service (token released from a {@code Task}).
     *
     * @param e the service-completion event
     * @throws UnexpectedEvent if this node does not accept {@link TokenServiceCompleted} events
     */
    public abstract void receiveEvent(TokenServiceCompleted e) throws UnexpectedEvent;

    /**
     * Handles the signal to start a participant's process (fires the {@code Start} event).
     *
     * @param startProcess the start-process event
     * @throws UnexpectedEvent if this node does not accept {@link StartProcess} events
     */
    public abstract void receiveEvent(StartProcess startProcess) throws UnexpectedEvent;

    /**
     * Called when an event type is received that this node cannot handle.
     * Concrete implementations should throw {@link UnexpectedEvent} with a descriptive message.
     *
     * @param e the unexpected event
     * @throws UnexpectedEvent always
     */
    protected abstract void unexpectedEvent(Event e) throws UnexpectedEvent;

}


