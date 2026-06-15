package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * Fired by a {@link it.uniroma2.sel.ebpmn.resources.Broker} when the switchover
 * delay to a backup alternative resource has elapsed.
 *
 * <p>When the active alternative of a {@code Broker} fails and a
 * {@link it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator} switch-time is
 * configured, the {@code Broker} schedules a {@code SwitchCompletedEvent} for
 * {@code currentTime + switchTime}. When it fires,
 * {@link Resource#handleSwitchCompletedEvent(Resource, double)} is called on the
 * {@code Broker}, which activates the next alternative and restores {@code Broker}
 * availability. The event bypasses the {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode}
 * handler chain.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.resources.Broker
 * @see ResourceFailureEvent
 */
public class SwitchCompletedEvent extends Event {

    private final Resource broker;       // the Broker that must handle this event
    private final Resource nextResource; // the alternative that becomes active

    public SwitchCompletedEvent(double time, Resource broker, Resource nextResource) {
        super(time);         // no FlowNode handlerEntity
        this.broker = broker;
        this.nextResource = nextResource;
    }

    @Override
    public void processByHandler() throws UnexpectedEvent {
        broker.handleSwitchCompletedEvent(nextResource, this.getTime());
    }

    public Resource getNextResource() {
        return nextResource;
    }
}
