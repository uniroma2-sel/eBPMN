package it.uniroma2.sel.ebpmn.resources;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Series composition of resources.
 *
 * <p>A Subsystem is unavailable if ANY of its child components is unavailable
 * (i.e., it applies a series / AND reliability model).
 *
 * <p>Availability is derived entirely from children via {@link #onChildFailure}
 * and {@link #onChildRepair}; the Subsystem does NOT schedule its own
 * failure/repair events.
 *
 * <pre>
 *   Subsystem feedingStation = new Subsystem("FeedingStation", p1);
 *   feedingStation.addComponent(pickTool);      // Broker
 *   feedingStation.addComponent(conveyorBelt);  // Performer
 * </pre>
 *
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Resource
 * @see Broker
 * @see Performer
 */
public class Subsystem extends Resource {

    private final List<Resource> components = new ArrayList<>();
    private int failedCount = 0;   // number of children that are currently failed

    /**
     * Subsystem constructor. Failure/queue policies default to {@link TokenOnFailure#DELAY} and
     * {@link QueueOnFailure#KEEP}; override after construction with
     * {@link #setTokenOnFailure} / {@link #setQueueOnFailure} if needed.
     *
     * @param name           resource name
     * @param role           resource log - used for logging purpose
     */
    public Subsystem(String name, String role) {
        super(name, role);
    }

    /**
     * Adds a child resource to this Subsystem and registers this Subsystem
     * as the child's parent (so failure/repair notifications flow upward).
     *
     * @return {@code this} for fluent chaining
     */
    public Subsystem addComponent(Resource r) {
        components.add(r);
        r.addParent(this);
        return this;
    }

    @Override
    protected void onChildFailure(Resource child) {
        failedCount++;
        if (failedCount == 1) {
            // Subsystem just became unavailable — propagate upward
            System.out.println(
                    "[Subsystem " + getName() + "] failed (child=" + child.getName() + ")");
            notifyFailure();
        }
    }

    @Override
    protected void onChildRepair(Resource child) {
        if (failedCount > 0) failedCount--;
        if (failedCount == 0) {
            // All children are back — Subsystem becomes available again
            System.out.println(
                    "[Subsystem " + getName() + "] repaired (child=" + child.getName() + ")");
            notifyRepair();
        }
    }
}
