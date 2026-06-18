package it.uniroma2.sel.ebpmn.resources;

import java.util.ArrayList;
import java.util.List;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.ResourceFailureEvent;
import it.uniroma2.sel.ebpmn.events.ResourceRepairEvent;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;
import it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure;
import it.uniroma2.sel.ebpmn.resources.policies.StandbyMode;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Atomic (leaf) resource with MTTF/MTTR failure/repair behaviour.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Resource
 * @see it.uniroma2.sel.ebpmn.events.ResourceFailureEvent
 * @see it.uniroma2.sel.ebpmn.events.ResourceRepairEvent
 * @see it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure
 * @see it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure
 */
public class Performer extends Resource {

    private final Participant participant;
    private final RandomVariableGenerator mttfGenerator;
    private final RandomVariableGenerator mttrGenerator;

    // Prevents double-scheduling of failure events
    private boolean failureScheduled = false;

    // COLD standby: true when this Performer is a non-active alternative in a Broker.
    // While in standby, failure events are cancelled and not rescheduled.
    private boolean inStandby = false;

    // Reference to the pending ResourceFailureEvent so it can be cancelled on cold-standby.
    private ResourceFailureEvent pendingFailureEvent = null;

    private final List<double[]> failureRepairRecords = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Full constructor with MTTF/MTTR generators.
     * Failure/queue policies default to {@link TokenOnFailure#DELAY} and
     * {@link QueueOnFailure#KEEP}; override after construction with
     * {@link #setTokenOnFailure} / {@link #setQueueOnFailure} if needed.
     *
     * @param name           resource name
     * @param participant    owning participant (for logical grouping / logging)
     * @param mttfGenerator  Mean Time To Failure generator (null = no failures)
     * @param mttrGenerator  Mean Time To Repair generator
     */
    public Performer(String name, Participant participant,
                     RandomVariableGenerator mttfGenerator,
                     RandomVariableGenerator mttrGenerator) {
        super(name, participant.getName());
        this.participant   = participant;
        this.mttfGenerator = mttfGenerator;
        this.mttrGenerator = mttrGenerator;
        // Schedule first failure immediately from simulation time 0.
        // The engine must already be initialized when Performer is created.
        scheduleNextFailure(0.0);
        ExecutionEngine.getInstance().registerPerformer(this);
    }

    /**
     * Constructor without failure model (no-failure Performer).
     */
    public Performer(String name, Participant participant) {
        this(name, participant, null, null);
    }

    // -----------------------------------------------------------------------
    // Failure scheduling - if the resource is in cold stand-by schedureNextFailure does nothing
    // -----------------------------------------------------------------------

    private void scheduleNextFailure(double currentTime) {
        if (mttfGenerator != null && !failureScheduled && !inStandby) {
            double failureTime = currentTime + mttfGenerator.get();
            pendingFailureEvent = new ResourceFailureEvent(failureTime, this);
            ExecutionEngine.getInstance().scheduleLocalEvent(pendingFailureEvent);
            failureScheduled = true;
            System.out.println(currentTime + ") Performer " + getName()
                    + ": next failure scheduled at " + String.format("%.2f", failureTime));
        }
    }

    // -----------------------------------------------------------------------
    // ResourceFailureEvent handler
    // -----------------------------------------------------------------------

    @Override
    public void handleFailureEvent(double time) {
        System.out.println(time + ") Performer " + getName() + ": FAILED");
        failureRepairRecords.add(new double[]{ time, Double.NaN });
        failureScheduled = false;

        // Schedule repair
        if (mttrGenerator != null) {
            double repairTime = time + mttrGenerator.get();
            ExecutionEngine.getInstance().scheduleLocalEvent(
                    new ResourceRepairEvent(repairTime, this));
            System.out.println(time + ") Performer " + getName()
                    + ": repair scheduled at " + String.format("%.2f", repairTime));
        }

        // Token handling delegated to Task.onResourceFailed() via notifyFailure()
        notifyFailure();
    }

    // -----------------------------------------------------------------------
    // ResourceRepairEvent handler
    // -----------------------------------------------------------------------

    @Override
    public void handleRepairEvent(double time) {
        System.out.println(time + ") Performer " + getName() + ": REPAIRED");
        if (!failureRepairRecords.isEmpty()) {
            double[] last = failureRepairRecords.get(failureRepairRecords.size() - 1);
            if (Double.isNaN(last[1])) {
                last[1] = time;
            }
        }
        failureScheduled = false;

        // Apply QueueOnFailure policy only when directly registered in a Task.
        // If tasks is empty this Performer is a child of a composite resource
        // and must not touch any queue directly.
        if (queueOnFailure == QueueOnFailure.FLUSH && !getTasks().isEmpty()) {
            for (Task task : getTasks()) {
                task.flushQueue();
                System.out.println(time + ") Performer " + getName()
                        + ": FLUSH — queue cleared for task " + task.getName());
            }
        }

        // Schedule next failure BEFORE notifying repair (so failure is in queue)
        scheduleNextFailure(time);

        // DELAY resumption and repair propagation handled by Resource.notifyRepair()
        notifyRepair();
    }

    // -----------------------------------------------------------------------
    // Standby mode (managed by Broker for redundant resource groups)
    // -----------------------------------------------------------------------

    /**
     * Called by {@link Broker} to place this Performer in or out of standby.
     *
     * <ul>
     *   <li>{@link StandbyMode#COLD}: cancels any pending failure event and
     *       suspends the failure/repair cycle until the Performer is activated.</li>
     *   <li>{@link StandbyMode#HOT}: resumes the failure/repair cycle; schedules
     *       the next failure immediately from the current simulation time.</li>
     * </ul>
     */
    @Override
    public void setStandby(StandbyMode mode) {
        if (mode == StandbyMode.COLD) {
            inStandby = true;
            if (pendingFailureEvent != null) {
                pendingFailureEvent.cancel();
                pendingFailureEvent = null;
            }
            failureScheduled = false;
            System.out.println(ExecutionEngine.getInstance().getSimulationTime()
                    + ") Performer " + getName() + ": placed in COLD standby");
        } else { // HOT — activate
            inStandby = false;
            scheduleNextFailure(ExecutionEngine.getInstance().getSimulationTime());
            System.out.println(ExecutionEngine.getInstance().getSimulationTime()
                    + ") Performer " + getName() + ": activated (HOT/active)");
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public Participant getParticipant() { return participant; }

    /** Returns all failure/repair records: each entry is {failureTime, repairTime}.
     *  repairTime is NaN if the performer has not yet been repaired. */
    public List<double[]> getFailureRepairRecords() {
        return failureRepairRecords;
    }
}
