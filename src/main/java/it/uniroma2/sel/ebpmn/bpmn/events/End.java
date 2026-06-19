package it.uniroma2.sel.ebpmn.bpmn.events;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.BPMNException;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * BPMN End Event node.
 *
 * <p>Consumes tokens that reach the terminal point of a
 * {@link it.uniroma2.sel.ebpmn.bpmn.Participant}'s process flow.  For each consumed
 * token the node records the end-to-end sojourn time (from the token's birth timestamp
 * to the current simulation time) and incrementally updates the running average service
 * time across all completed cases.
 *
 * <p>An End node has no outgoing edges; attempting to add one raises a
 * {@link it.uniroma2.sel.ebpmn.exceptions.BPMNException}.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.bpmn.events.Start
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class End extends FlowNode{

	/** Running count of tokens (process instances) that have reached this End node. */
	private int processedToken;
	/** Cumulative running average of end-to-end sojourn times across all completed tokens. */
	private double avgServiceTime;
	private final List<double[]> tokenRecords = new ArrayList<>();

	/**
	 * Constructs an End event node, initialising its counters to zero.
	 *
	 * @param name logical name of this node
	 * @param p    the {@link Participant} that owns this node
	 */
	public End(String name, Participant p) {
		super(name, p);
		processedToken = 0;
		avgServiceTime=0;
	}


	/**
	 * Returns the total number of tokens that have been consumed by this End node
	 * during the simulation run.
	 *
	 * @return completed token (case) count
	 */
	public int getProcessedToken() {
		return processedToken;
	}

	/**
	 * Returns the running average end-to-end sojourn time computed over all tokens
	 * that have reached this End node so far.
	 *
	 * @return average sojourn time in simulation time units
	 */
	public double getAvgServiceTime() {
		return avgServiceTime;
	}

	/** Returns all token records for this run: each entry is {completionTime, serviceTime}. */
	public List<double[]> getTokenRecords() {
		return tokenRecords;
	}

	/**
	 * Not supported — an End node cannot have outgoing sequence-flow edges.
	 *
	 * @param n the node that was attempted to be attached
	 * @throws it.uniroma2.sel.ebpmn.exceptions.BPMNException always, because End is terminal
	 */
	@Override
	public void addOutGoingEdge(FlowNode n) throws BPMNException{
		throw new BPMNException(this.getName() + " cannot have an outogoing edge");
	}

	/**
	 * Not supported — an End node has no successor.
	 *
	 * @return never returns normally
	 * @throws NodeNotFoundException always
	 */
	@Override
	public FlowNode getNextNode() throws NodeNotFoundException {
		throw new NodeNotFoundException("Unable to find a flow node after " + this.getName());
	}

	/**
	 * Handles an incoming token by computing the sojourn time, updating the running
	 * average, incrementing the processed-token counter, and logging the completion.
	 *
	 * @param incomingToken the token arriving at this End node
	 * @throws UnexpectedEvent never thrown by this implementation; declared for interface
	 *                         compatibility
	 */
	@Override
	public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent {
		//compute of the mean service time
		double serviceTime = incomingToken.getTime()-incomingToken.getStartTimestamp();
		tokenRecords.add(new double[]{Double.parseDouble(incomingToken.getTokenId()), incomingToken.getTime(), serviceTime });
		avgServiceTime = ((avgServiceTime*processedToken)+serviceTime)/(processedToken+1);
		processedToken++;

		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Token " + incomingToken.getTokenId() + " execution completed in " + serviceTime);
	}

	/**
	 * Not supported — a service-completion event cannot be directed to an End node.
	 *
	 * @param servedToken the unexpected service-completion event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		this.unexpectedEvent(servedToken);
	}

	/**
	 * Not supported — an End node does not accept incoming messages.
	 *
	 * @param incomingMessage the unexpected message event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
		this.unexpectedEvent(incomingMessage);
	}

	/**
	 * Not supported — a process-start event cannot be directed to an End node.
	 *
	 * @param startEvent the unexpected start event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(StartProcess startEvent) throws UnexpectedEvent {
		this.unexpectedEvent(startEvent);
	}

	/**
	 * Throws an {@link UnexpectedEvent} describing the event type that was incorrectly
	 * delivered to this node.
	 *
	 * @param e the event that could not be handled
	 * @throws UnexpectedEvent always, with a descriptive message
	 */
	@Override
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}

}
