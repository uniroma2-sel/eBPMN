package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * Event fired when a {@link it.uniroma2.sel.ebpmn.bpmn.tasks.Task} finishes
 * serving a token.
 *
 * <p>Scheduled by {@link it.uniroma2.sel.ebpmn.bpmn.tasks.Task} at
 * {@code arrivalTime + serviceTime} whenever a resource accepts a token.
 * When it fires, the task frees the resource, logs the activity, and routes
 * the token to the next node via a new {@link IncomingToken} event.
 *
 * <p>A TokenServiceCompleted may be cancelled mid-flight when a
 * {@link it.uniroma2.sel.ebpmn.resources.Performer} fails while the token is
 * in service.  The {@link #cancelled} flag is checked by
 * {@code Task.completeTokenService()} to skip stale completions.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Token
 * @see it.uniroma2.sel.ebpmn.bpmn.tasks.Task
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 */
public class TokenServiceCompleted extends Token{

	/**
	 * Set to true by Performer when a failure interrupts an in-progress service.
	 * Task.completeTokenService() checks this flag and skips normal completion
	 * processing when the event is stale.
	 */
	private boolean cancelled = false;

	public TokenServiceCompleted(String id, double time, FlowNode entity) {
		super(id, time, entity);
	}

	public TokenServiceCompleted(String id, double time, FlowNode entity, double startTime) {
		super(id, time, entity, startTime);
	}

	/** Marks this completion event as cancelled (fired due to mid-service failure). */
	public void cancel() {
		this.cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void processByHandler() throws UnexpectedEvent {
		handlerEntity.handleEvent(this);  // double dispatch
	}

}
