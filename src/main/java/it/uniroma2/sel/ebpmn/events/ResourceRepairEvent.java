package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * Fired when a {@link it.uniroma2.sel.ebpmn.resources.Performer} completes repair
 * and becomes operationally available again.
 *
 * <p>Scheduled by {@code Performer} at MTTR-drawn intervals immediately after a
 * failure is processed. When it fires, {@link Resource#handleRepairEvent(double)} is
 * called directly on the owning resource, bypassing the
 * {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode} handler chain. Recovery is then propagated
 * upward via {@code Resource#onChildRepair(Resource)}, and waiting tokens are re-activated
 * through the associated {@link it.uniroma2.sel.ebpmn.bpmn.tasks.Task} queues.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 * @see ResourceFailureEvent
 * @see Resource
 */
public class ResourceRepairEvent extends Event {

    private final Resource resource;

    public ResourceRepairEvent(double time, Resource resource) {
        super(time);          // no FlowNode handlerEntity
        this.resource = resource;
    }

    @Override
    public void processByHandler() throws UnexpectedEvent {
        resource.handleRepairEvent(this.getTime());
    }

    public Resource getResource() {
        return resource;
    }
}
