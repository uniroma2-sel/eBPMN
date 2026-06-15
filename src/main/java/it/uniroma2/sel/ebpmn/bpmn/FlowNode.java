package it.uniroma2.sel.ebpmn.bpmn;

import java.util.ArrayList;

import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.BPMNException;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.LogData;
import it.uniroma2.sel.ebpmn.logger.Logger;


/**
 * Abstract base class for every local BPMN element that participates in sequence flow.
 *
 * <p>A {@code FlowNode} extends {@link Node} and adds the concept of outgoing sequence-flow
 * edges: each concrete subclass (e.g. {@code Task}, {@code Start}, {@code End}, gateway
 * classes) holds an ordered list of successor {@code FlowNode} references and implements
 * the {@code handleEvent} dispatch methods to define its simulation behaviour.
 *
 * <p>When the simulation engine dequeues an event whose target is a {@code FlowNode},
 * it calls the appropriate {@code receiveEvent} overload, which places the event on the
 * local {@link ExecutionEngine} event queue via {@code scheduleLocalEvent}.  The engine
 * then calls the corresponding {@code handleEvent} on the same node to execute the BPMN
 * semantics (e.g. consuming a token, starting service, routing via a gateway).
 *
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Node
 * @see Participant
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 */
public abstract class FlowNode extends Node {

    /** Ordered list of successor nodes reachable via outgoing sequence-flow edges. */
    protected ArrayList<FlowNode> nodes;

    /**
     * Reference to the singleton {@link ExecutionEngine}.
     * Stored as a field so every subclass can schedule events without repeated getInstance() calls.
     */
	protected ExecutionEngine engine = ExecutionEngine.getInstance();

    /**
     * Constructs a {@code FlowNode}, registers it with its owning {@link Participant},
     * and initialises the empty outgoing-edge list.
     *
     * @param name        human-readable identifier for this node
     * @param participant the {@link Participant} that owns this node; the node is automatically
     *                    appended to the participant's process flow list
     */
    public FlowNode(String name, Participant participant) {
		super(name, participant);
    	nodes = new ArrayList<>();
		participant.addFlowNode(this);
	}

	/**
	 * Adds a successor node to the outgoing sequence-flow edge list.
	 * For nodes with a single outgoing edge (most BPMN elements) this is called once;
	 * for gateways with multiple branches it may be called once per branch.
	 *
	 * @param n the successor {@link FlowNode} to connect
	 * @throws BPMNException if the connection violates a structural constraint
	 */
	//creates an outgoing edge with routing probability equal to 1
	public void addOutGoingEdge(FlowNode n) throws BPMNException {this.nodes.add(n);}

	/**
	 * Returns the first (and typically only) successor node on the unique outgoing edge.
	 * Subclasses that support multiple outgoing edges (e.g. diverging gateways) override
	 * this method to select the appropriate target.
	 *
	 * @return the next {@link FlowNode} in the sequence flow
	 * @throws NodeNotFoundException if no outgoing edge has been defined
	 */
	//gets the next node connected to the unique outgoing edge
	//It is overridden in flow node that can have multiple outgoing edges
	public FlowNode getNextNode() throws NodeNotFoundException {return this.nodes.get(0);}


	/**
	 * Handles an incoming control-flow token. Concrete subclasses implement the BPMN
	 * token-consumption and firing rules appropriate to their node type.
	 *
	 * @param incomingToken the token event to process
	 * @throws UnexpectedEvent if this node does not accept {@link IncomingToken} events
	 */
	public abstract void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent;

	/**
	 * Handles an incoming message-flow event (delivered locally or via HLA).
	 * Concrete subclasses implement the BPMN message-catching semantics.
	 *
	 * @param incomingMessage the message event to process
	 * @throws UnexpectedEvent if this node does not accept {@link IncomingMessage} events
	 */
	public abstract void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent;

	/**
	 * Handles the completion of a task's service (i.e. a token that has finished being
	 * served by a {@code Resource}).  Typically implemented only by {@code Task} nodes.
	 *
	 * @param servedToken the service-completion event to process
	 * @throws UnexpectedEvent if this node does not accept {@link TokenServiceCompleted} events
	 */
	public abstract void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent;

	/**
	 * Handles the signal to start a participant's process (fires tokens from the
	 * {@code Start} node). Typically implemented only by the {@code Start} node class.
	 *
	 * @param startEvent the start-process event to process
	 * @throws UnexpectedEvent if this node does not accept {@link StartProcess} events
	 */
	public abstract void handleEvent(StartProcess startEvent) throws UnexpectedEvent;

	/*
	 * Implementation of the Event Receivers.
	 * The received events are scheduled as "local events" on the Execution Engine (i.e., they are
	 * added to the local events queue managed by the engine).
	 */

	/**
	 * {@inheritDoc}
	 * <p>Schedules the message event on the local {@link ExecutionEngine} event queue so
	 * that {@link #handleEvent(IncomingMessage)} will be invoked at the correct simulation time.
	 */
	@Override
	public void receiveEvent(IncomingMessage e) throws UnexpectedEvent {
		engine.scheduleLocalEvent(e);
	}

	/**
	 * {@inheritDoc}
	 * <p>Schedules the token event on the local {@link ExecutionEngine} event queue so
	 * that {@link #handleEvent(IncomingToken)} will be invoked at the correct simulation time.
	 */
	@Override
	public void receiveEvent(IncomingToken e) throws UnexpectedEvent {
		engine.scheduleLocalEvent(e);
	}

	/**
	 * {@inheritDoc}
	 * <p>Schedules the service-completion event on the local {@link ExecutionEngine} event queue
	 * so that {@link #handleEvent(TokenServiceCompleted)} will be invoked at the correct
	 * simulation time.
	 */
	@Override
	public void receiveEvent(TokenServiceCompleted e) throws UnexpectedEvent {
		engine.scheduleLocalEvent(e);
	}

	/**
	 * {@inheritDoc}
	 * <p>Schedules the start-process event on the local {@link ExecutionEngine} event queue
	 * so that {@link #handleEvent(StartProcess)} will be invoked at the correct simulation time.
	 */
	@Override
	public void receiveEvent(StartProcess startProcess) throws UnexpectedEvent {
		engine.scheduleLocalEvent(startProcess);
	}

	/**
	 * Writes a structured log entry through the {@link ExecutionEngine}'s logger for
	 * the owning participant.
	 *
	 * @param ld the log data record to write
	 */
	protected void writeLog(LogData ld) {
		Logger l = engine.getLogger(this.getParticipant().getName());
		if (l != null)
			l.write(ld);
	}
}
