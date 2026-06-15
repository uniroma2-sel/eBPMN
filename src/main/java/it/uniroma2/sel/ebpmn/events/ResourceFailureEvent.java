package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * Fired when a {@link it.uniroma2.sel.ebpmn.resources.Performer} fails.
 *
 * <p>Scheduled by {@code Performer} at MTTF-drawn intervals. When it fires,
 * {@link Resource#handleFailureEvent(double)} is invoked directly on the owning
 * resource, bypassing the {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode} handler chain.
 * The failure is then propagated upward to parent resources via
 * {@code Resource#onChildFailure(Resource)}.</p>
 *
 * <p>The event carries a {@link #cancelled} flag so that a pending failure can be
 * silently discarded when, for example, a {@link it.uniroma2.sel.ebpmn.resources.Broker}
 * places the resource into {@link it.uniroma2.sel.ebpmn.resources.policies.StandbyMode#COLD}
 * standby before the event fires.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 * @see ResourceRepairEvent
 * @see Resource
 */
public class ResourceFailureEvent extends Event {

    private final Resource resource;
    private boolean cancelled = false;

    public ResourceFailureEvent(double time, Resource resource) {
        super(time);          // no FlowNode handlerEntity
        this.resource = resource;
    }

    /** Marks this event as cancelled so it is silently ignored when it fires. */
    public void cancel() { this.cancelled = true; }

    /** Returns true if this event has been cancelled. */
    public boolean isCancelled() { return cancelled; }

    @Override
    public void processByHandler() throws UnexpectedEvent {
        if (!cancelled) {
            resource.handleFailureEvent(this.getTime());
        }
    }

    public Resource getResource() {
        return resource;
    }
}
