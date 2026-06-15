package it.uniroma2.sel.ebpmn.resources;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.SwitchCompletedEvent;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;
import it.uniroma2.sel.ebpmn.resources.policies.StandbyMode;

/**
 * Parallel / redundant composition of resources.
 *
 * <p>A Broker is unavailable only if ALL of its alternative child resources are
 * unavailable (i.e., it applies a parallel / OR reliability model).
 *
 * <p>Only one alternative is active at a time.  If the active alternative fails and
 * others are available, the Broker switches to the next available one.
 * If a {@code switchTimeGenerator} is provided, the switch introduces a delay
 * during which the Broker is temporarily unavailable.
 *
 * <p>The {@link StandbyMode} controls whether non-active alternatives accumulate
 * ageing and can fail while waiting ({@link StandbyMode#HOT}) or whether they
 * are powered off and cannot fail until switched in ({@link StandbyMode#COLD}).
 *
 * <p>Availability is derived from children via {@link #onChildFailure} and
 * {@link #onChildRepair}; the Broker does NOT schedule its own failure events.
 *
 * <pre>
 *   Broker pickTool = new Broker("PickTool", p1, StandbyMode.HOT,
 *                                new ExponentialGenerator(1.0/60));
 *   pickTool.addAlternative(pickingUnitA);   // primary
 *   pickTool.addAlternative(pickingUnitB);   // standby
 * </pre>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Resource
 * @see Subsystem
 * @see it.uniroma2.sel.ebpmn.events.SwitchCompletedEvent
 * @see StandbyMode
 */
public class Broker extends Resource {

    private final List<Resource> alternatives = new ArrayList<>();
    private int availableCount = 0;           // number of alternatives currently up
    private Resource currentResource = null;  // the active alternative
    private final RandomVariableGenerator switchTimeGenerator; // null = instantaneous
    private final StandbyMode standbyMode;    // HOT or COLD

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Full constructor: standby mode + switch time generator.
     *
     * @param name                resource name
     * @param participant         owning participant (for logical grouping)
     * @param standbyMode         HOT or COLD standby for non-active alternatives
     * @param switchTimeGenerator distribution of switchover delay; {@code null} = instantaneous
     */
    public Broker(String name, Participant participant,
                  StandbyMode standbyMode,
                  RandomVariableGenerator switchTimeGenerator) {
        super(name, participant.getName());
        this.standbyMode         = standbyMode;
        this.switchTimeGenerator = switchTimeGenerator;
    }

    /**
     * StandbyMode only, instantaneous switching.
     *
     * @param name        resource name
     * @param participant owning participant
     * @param standbyMode HOT or COLD standby for non-active alternatives
     */
    public Broker(String name, Participant participant, StandbyMode standbyMode) {
        this(name, participant, standbyMode, null);
    }

    /**
     * Switch time only, HOT standby (backward-compatible with original API).
     *
     * @param name                resource name
     * @param participant         owning participant
     * @param switchTimeGenerator distribution of switchover delay; {@code null} = instantaneous
     */
    public Broker(String name, Participant participant,
                  RandomVariableGenerator switchTimeGenerator) {
        this(name, participant, StandbyMode.HOT, switchTimeGenerator);
    }

    /**
     * Convenience constructor: HOT standby, instantaneous switching.
     *
     * @param name        resource name
     * @param participant owning participant
     */
    public Broker(String name, Participant participant) {
        this(name, participant, StandbyMode.HOT, null);
    }

    // -----------------------------------------------------------------------
    // Alternative management
    // -----------------------------------------------------------------------

    /**
     * Adds an alternative child resource to this Broker and registers this
     * Broker as the child's parent.
     *
     * The first alternative added becomes the default active one.
     * Subsequent alternatives are placed in the configured standby mode:
     * HOT (failure clocks keep running) or COLD (failure clocks suspended).
     *
     * @param r the alternative resource to add
     * @return {@code this} for fluent chaining
     */
    public Broker addAlternative(Resource r) {
        alternatives.add(r);
        availableCount++;
        r.addParent(this);
        if (currentResource == null) {
            // First alternative — this is the active one; no standby call needed
            currentResource = r;
        } else {
            // Subsequent alternatives — put in the configured standby mode
            r.setStandby(standbyMode);
        }
        return this;
    }

    // -----------------------------------------------------------------------
    // Child failure / repair callbacks
    // -----------------------------------------------------------------------

    @Override
    protected void onChildFailure(Resource child) {
        availableCount--;
        System.out.println("[Broker " + getName() + "] child " + child.getName()
                + " failed; availableCount=" + availableCount);

        if (availableCount == 0) {
            // All alternatives down — Broker itself fails
            currentResource = null;
            System.out.println("[Broker " + getName() + "] ALL alternatives failed → BROKER FAILED");
            notifyFailure();

        } else if (child == currentResource) {
            // Active resource failed; switch to the next available alternative
            Resource next = selectNext();
            if (next == null) {
                // Guard: shouldn't happen if availableCount > 0
                notifyFailure();
                return;
            }
            if (switchTimeGenerator != null) {
                // Temporarily unavailable during switchover
                System.out.println("[Broker " + getName() + "] switching to " + next.getName()
                        + " (with delay)");
                notifyFailure();   // propagate temporary unavailability
                double switchTime = ExecutionEngine.getInstance().getSimulationTime()
                        + switchTimeGenerator.get();
                ExecutionEngine.getInstance().scheduleLocalEvent(
                        new SwitchCompletedEvent(switchTime, this, next));
            } else {
                // Instantaneous switch — activate the next alternative right away
                activateAlternative(next);
                System.out.println("[Broker " + getName() + "] switched instantly to "
                        + next.getName());
            }
        }
        // If a non-active alternative failed, the Broker stays up — do nothing.
    }

    @Override
    protected void onChildRepair(Resource child) {
        availableCount++;
        System.out.println("[Broker " + getName() + "] child " + child.getName()
                + " repaired; availableCount=" + availableCount);

        if (availableCount == 1) {
            // Was completely failed; now recovering on this child
            activateAlternative(child);
            System.out.println("[Broker " + getName() + "] REPAIRED on " + child.getName());
            notifyRepair();
        } else {
            // Broker was already up — a standby alternative repaired.
            // Put it back in standby (it's not the active one).
            if (child != currentResource) {
                child.setStandby(standbyMode);
            }
        }
    }

    /**
     * Called by {@link SwitchCompletedEvent} when the switchover delay completes.
     * Activates the new alternative and restores Broker availability.
     */
    @Override
    public void handleSwitchCompletedEvent(Resource next, double time) {
        activateAlternative(next);
        System.out.println(time + ") [Broker " + getName() + "] switch completed → "
                + next.getName());
        notifyRepair();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the currently active alternative resource.
     *
     * @return the active alternative, or {@code null} if all alternatives have failed
     */
    public Resource getCurrentResource() {
        return currentResource;
    }

    /**
     * Returns the standby mode configured for this Broker.
     *
     * @return {@link StandbyMode#HOT} or {@link StandbyMode#COLD}
     */
    public StandbyMode getStandbyMode() {
        return standbyMode;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Sets {@code next} as the active alternative and wakes up its failure cycle
     * (no-op for HOT standby; resumes MTTF clock for COLD standby).
     */
    private void activateAlternative(Resource next) {
        currentResource = next;
        next.setStandby(StandbyMode.HOT);
    }

    /** Selects the next available alternative that is not the current one. */
    private Resource selectNext() {
        for (Resource r : alternatives) {
            if (r.isAvailable() && r != currentResource) return r;
        }
        return null;
    }
}
