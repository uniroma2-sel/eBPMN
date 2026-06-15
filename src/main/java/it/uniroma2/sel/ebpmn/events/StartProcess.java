package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * Bootstrapping event that triggers a participant's process at simulation start.
 *
 * <p>The {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine} creates one
 * {@code StartProcess} event per token for each initiating
 * {@link it.uniroma2.sel.ebpmn.bpmn.Participant} during initialisation.  When
 * it fires, {@link it.uniroma2.sel.ebpmn.bpmn.events.Start#handleEvent(StartProcess)}
 * is called, which in turn schedules the first
 * {@link IncomingToken} event into the process flow.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.bpmn.events.Start
 * @see IncomingToken
 */
public class StartProcess extends Event {
	public StartProcess(double time, FlowNode entity) {
		super(time, entity);
	}

	@Override
	public void processByHandler() throws UnexpectedEvent{
		handlerEntity.handleEvent(this);  // double dispatch
	}


}
