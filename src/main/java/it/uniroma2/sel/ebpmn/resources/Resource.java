package it.uniroma2.sel.ebpmn.resources;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.ResourceFailureEvent;
import it.uniroma2.sel.ebpmn.events.ResourceRepairEvent;
import it.uniroma2.sel.ebpmn.events.SwitchCompletedEvent;
import it.uniroma2.sel.ebpmn.events.TokenServiceCompleted;
import it.uniroma2.sel.ebpmn.events.IncomingToken;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;

/**
 * Abstract base class for all eBPMN resource types.
 *
 * <p>A {@code Resource} represents an entity that can execute {@link Task}s within a
 * BPMN process.  It maintains two orthogonal availability flags:
 * <ul>
 *   <li><em>busy / free</em> — whether the resource is currently serving a token
 *       (managed by {@link #setBusy()} and {@link #free()}).</li>
 *   <li><em>available / failed</em> — whether the resource is operationally up
 *       (managed by {@link #notifyFailure()} and {@link #notifyRepair()}).</li>
 * </ul>
 * {@link #isAvailable()} returns {@code true} only when both flags are satisfied.
 *
 * <p><strong>Composite design pattern:</strong> {@code Resource} is the Component role.
 * {@link Performer} is the leaf (atomic resource).
 * {@link Subsystem} and {@link Broker} are composites that aggregate children and
 * implement {@link #onChildFailure(Resource)} / {@link #onChildRepair(Resource)} to
 * derive their own availability from the children's state.</p>
 *
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Performer
 * @see Subsystem
 * @see Broker
 */
public class Resource {

    /** Human-readable name of this resource. */
    private String name;

    /** Role of this resource within its owning organisation or participant. */
    private String role;

    /** Busy/free flag; {@code true} means the resource is idle and may accept a token. */
    private boolean isFree;

    /** Tasks that reference this resource and are notified on repair. */
    private ArrayList<Task> tasks;

    // -----------------------------------------------------------------------
    // Failure / composite availability state
    // -----------------------------------------------------------------------

    /**
     * Failure-level availability flag.
     * true  = resource is not failed (or all required children are up).
     * false = resource has failed (or at least one required child is down).
     *
     * Managed exclusively by notifyFailure() / notifyRepair().
     * Combined with isAvailable (busy flag) in isAvailabe().
     */
    protected boolean available = true;

    /**
     * Composite resources that contain this resource.
     * Used to propagate failure/repair notifications upward.
     */
    protected final List<Resource> parents = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Legacy reliability generators (kept for backward compat; see Performer)
    // -----------------------------------------------------------------------

    /** Mean Time To Failure generator; {@code null} if this resource never fails (legacy path). */
    private RandomVariableGenerator mttfGenerator;

    /** Mean Time To Repair generator; paired with {@link #mttfGenerator} (legacy path). */
    private RandomVariableGenerator mttrGenerator;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Constructs a plain resource without failure modelling.
     *
     * @param n  resource name
     * @param r  role of this resource within its participant
     */
    public Resource(String n, String r) {
        this.name = n;
        this.role = r;
        isFree = true;
        tasks = new ArrayList<>();
    }

    /**
     * Constructs a resource with legacy MTTF/MTTR failure modelling.
     * Prefer using {@link Performer} for atomic resources with failure behaviour.
     *
     * @param n     resource name
     * @param r     role of this resource within its participant
     * @param mttf  Mean Time To Failure generator ({@code null} disables failures)
     * @param mttr  Mean Time To Repair generator
     */
    public Resource(String n, String r, RandomVariableGenerator mttf, RandomVariableGenerator mttr) {
        this(n, r);
        this.mttfGenerator = mttf;
        this.mttrGenerator = mttr;
    }

    // -----------------------------------------------------------------------
    // Basic accessors (backward-compatible)
    // -----------------------------------------------------------------------

    /**
     * Returns the human-readable name of this resource.
     *
     * @return resource name
     */
    public String getName() { return name; }

    /**
     * Returns the role label of this resource within its owning participant.
     *
     * @return role string
     */
    public String getRole() { return role; }

    /**
     * Returns {@code true} only when the resource is BOTH operationally up
     * (not failed) AND not currently serving a token (not busy).
     *
     * @return {@code true} if the resource can accept new work
     */
    public boolean isAvailable() {
        return this.isFree && this.available;
    }

    /** Alias without the typo — preferred in new code. */
    //public boolean isAvailable() { return isAvailabe(); }

    public void setBusy() { this.isFree = false; }
    public void free()    { this.isFree = true;  }

    public void addTask(Task t) { this.tasks.add(t); }
    public ArrayList<Task> getTasks() { return this.tasks; }

    // -----------------------------------------------------------------------
    // Legacy MTTF/MTTR accessors
    // -----------------------------------------------------------------------

    public boolean hasMttf() { return mttfGenerator != null; }
    public RandomVariableGenerator getMttfGenerator() { return mttfGenerator; }
    public RandomVariableGenerator getMttrGenerator() { return mttrGenerator; }

    // -----------------------------------------------------------------------
    // Composite / failure hierarchy
    // -----------------------------------------------------------------------

    /**
     * Registers this resource as a child of a composite resource
     * ({@link Subsystem} or {@link Broker}).
     *
     * @param parent the composite resource that will receive
     *               {@link #onChildFailure} / {@link #onChildRepair} notifications
     */
    public void addParent(Resource parent) {
        parents.add(parent);
    }

    /**
     * Called when this resource fails.
     * Sets available=false and propagates to all Resource parents.
     * Does NOT notify Tasks — arriving tokens will see isAvailabe()=false and queue up naturally.
     */
    protected void notifyFailure() {
        available = false;
        for (Resource parent : parents) {
            parent.onChildFailure(this);
        }
    }

    /**
     * Called when this resource repairs.
     * Sets available=true, propagates to Resource parents, then triggers Task queues.
     */
    protected void notifyRepair() {
        available = true;
        for (Resource parent : parents) {
            parent.onChildRepair(this);
        }
        // Notify directly-associated Tasks so they can process their queued tokens.
        double now = ExecutionEngine.getInstance().getSimulationTime();
        for (Task task : tasks) {
            task.onResourceRepaired(this, now);
        }
    }

    /**
     * Called when a child resource has failed.  Override in {@link Subsystem}
     * and {@link Broker} to update composite availability.
     *
     * @param child the child resource that just failed
     */
    protected void onChildFailure(Resource child) {}

    /**
     * Called when a child resource has repaired.  Override in {@link Subsystem}
     * and {@link Broker} to update composite availability.
     *
     * @param child the child resource that just repaired
     */
    protected void onChildRepair(Resource child) {}

    // -----------------------------------------------------------------------
    // Service-start hook (called by Task.processToken when service begins)
    // -----------------------------------------------------------------------

    /**
     * Called by Task immediately before scheduling TokenServiceCompleted.
     * Default: marks this resource as busy.
     * Performer overrides this to also store the token reference and schedule
     * the first failure event.
     *
     * @param token        the token entering service
     * @param startTime    simulation time at which service begins
     * @param serviceDuration  sampled service time
     * @param pending      the TokenServiceCompleted event that will be scheduled
     * @param handlerTask  the Task that owns the pending completion
     */
    public void onServiceStarted(IncomingToken token, double startTime, double serviceDuration,
                                 TokenServiceCompleted pending, Task handlerTask) {
        setBusy();
    }

    /**
     * Called by {@link Task} immediately after {@link #free()} when a
     * {@link it.uniroma2.sel.ebpmn.events.TokenServiceCompleted} event is processed
     * normally (i.e., not cancelled).
     * Default: no-op.
     * {@link Performer} overrides this to clear all in-service state fields so that
     * a late {@link it.uniroma2.sel.ebpmn.events.ResourceFailureEvent} cannot operate
     * on a stale token reference.
     *
     * @param time simulation time at which service completion is processed
     */
    public void onServiceCompleted(double time) {}

    // -----------------------------------------------------------------------
    // Resource-event dispatch hooks (called by the new event types)
    // -----------------------------------------------------------------------

    /**
     * Called when a {@link it.uniroma2.sel.ebpmn.events.ResourceFailureEvent} fires.
     * Override in {@link Performer} to implement failure behaviour.
     *
     * @param time logical simulation time of the failure
     */
    public void handleFailureEvent(double time) {}

    /**
     * Called when a {@link it.uniroma2.sel.ebpmn.events.ResourceRepairEvent} fires.
     * Override in {@link Performer} to implement repair behaviour.
     *
     * @param time logical simulation time of the repair
     */
    public void handleRepairEvent(double time) {}

    /**
     * Override in Broker to handle a SwitchCompletedEvent.
     *
     * @param next  the alternative resource that has just become active
     * @param time  event timestamp
     */
    public void handleSwitchCompletedEvent(Resource next, double time) {}

    /**
     * Called by {@link Broker} when this resource is placed into or taken out of
     * standby mode.  The base implementation is a no-op; {@link Performer} overrides
     * this to suspend or resume its failure/repair cycle for COLD standby.
     *
     * @param mode  {@link it.uniroma2.sel.ebpmn.resources.policies.StandbyMode#HOT}
     *              or {@link it.uniroma2.sel.ebpmn.resources.policies.StandbyMode#COLD}
     */
    public void setStandby(it.uniroma2.sel.ebpmn.resources.policies.StandbyMode mode) {}

    // -----------------------------------------------------------------------
    // Utility: walk up the parent chain to find the Tasks that use this
    // resource hierarchy (needed for nested Performers in Subsystem/Broker).
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link Task}s directly or indirectly associated with this resource.
     * If this resource has tasks of its own, those are returned.
     * Otherwise the search recurses into parent composites.
     *
     * @return a non-null (possibly empty) list of associated tasks
     */
    protected List<Task> findAssociatedTasks() {
        if (!tasks.isEmpty()) return new ArrayList<>(tasks);
        List<Task> result = new ArrayList<>();
        for (Resource parent : parents) {
            result.addAll(parent.findAssociatedTasks());
        }
        return result;
    }
}
