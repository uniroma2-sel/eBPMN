package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * Control-flow token travelling along BPMN sequence edges.
 *
 * <p>An IncomingToken represents a process instance (case) moving from one
 * {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode} to the next.  It is the primary
 * event type in the DES: every time a token crosses a sequence-flow edge a new
 * IncomingToken event is scheduled on the destination node.
 *
 * <p>The double-dispatch entry point {@link #processByHandler()} delegates to
 * {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode#handleEvent(IncomingToken)} on the
 * target handler.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Token
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class IncomingToken extends Token {

	public IncomingToken(String id, double time, FlowNode entity) {
		super(id, time, entity);
	}
	public IncomingToken(String id, double time, FlowNode entity, double startTime) {
		super(id, time, entity, startTime);
	}

	@Override
	public void processByHandler() throws UnexpectedEvent {
		handlerEntity.handleEvent(this); // double dispatch
	}
}
