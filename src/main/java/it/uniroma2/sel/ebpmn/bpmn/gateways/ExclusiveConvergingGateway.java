package it.uniroma2.sel.ebpmn.bpmn.gateways;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * BPMN Exclusive (XOR) Converging Gateway — pass-through token merge.
 *
 * <p>Forwards each arriving token directly to the single outgoing edge without
 * buffering or counting.  In an XOR join, only one branch fires at a time so
 * every token is independent; no synchronisation is needed.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see ExclusiveDivergingGateway
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class ExclusiveConvergingGateway extends FlowNode{

	/**
	 * Creates an ExclusiveConvergingGateway with the given name and owning participant.
	 *
	 * @param name the node name
	 * @param p    the owning participant (pool)
	 */
	public ExclusiveConvergingGateway(String name, Participant p) {
		super(name, p);
	}

	@Override
	public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent {
		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN, Token ID " + incomingToken.getTokenId());
		String id = incomingToken.getTokenId();
		IncomingToken t = new IncomingToken(id, incomingToken.getTime(),
				this.getNextNode(), incomingToken.getStartTimestamp());
		this.getNextNode().receiveEvent(t);

		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - "  + this.getName()
				+ ": Scheduled INCOMING TOKEN event at time "
				+ t.getTime() + " "
				+ "for " + t.getHandlerEntity().getName());
	}

	@Override
	public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
		this.unexpectedEvent(incomingMessage);
	}

	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		this.unexpectedEvent(servedToken);
	}

	@Override
	public void handleEvent(StartProcess startEvent) throws UnexpectedEvent {
		this.unexpectedEvent(startEvent);
	}

	@Override
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}
}
