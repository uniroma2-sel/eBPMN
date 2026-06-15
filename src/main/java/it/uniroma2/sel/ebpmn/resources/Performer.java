package it.uniroma2.sel.ebpmn.resources;

import java.util.ArrayList;
import java.util.List;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.IncomingToken;
import it.uniroma2.sel.ebpmn.events.ResourceFailureEvent;
import it.uniroma2.sel.ebpmn.events.ResourceRepairEvent;
import it.uniroma2.sel.ebpmn.events.TokenServiceCompleted;
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
    //private final RandomVariableGenerator serviceTimeGenerator;
    private final RandomVariableGenerator mttfGenerator;
    private final RandomVariableGenerator mttrGenerator;
    private final TokenOnFailure tokenOnFailure;
    private final QueueOnFailure queueOnFailure;

    // -----------------------------------------------------------------------
    // In-service state (set by onServiceStarted; relevant only for direct use)
    // -----------------------------------------------------------------------
    private IncomingToken      tokenInService      = null;
    private double             serviceStartTime    = 0.0;
    private double             remainingServiceTime= 0.0;
    private TokenServiceCompleted pendingCompletion= null;

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
     * Full constructor with MTTF/MTTR and failure policies.
     *
     * @param name               resource name
     * @param participant        owning participant (for logical grouping / logging)
     * @param mttfGenerator      Mean Time To Failure generator (null = no failures)
     * @param mttrGenerator      Mean Time To Repair generator
     * @param tokenOnFailure     what to do with the token in service on failure
     * @param queueOnFailure     what to do with waiting tokens on repair
     */
    public Performer(String name, Participant participant,
                     //RandomVariableGenerator serviceTimeGen,
                     RandomVariableGenerator mttfGenerator,
                     RandomVariableGenerator mttrGenerator,
                     TokenOnFailure tokenOnFailure,
                     QueueOnFailure queueOnFailure) {
        super(name, participant.getName());
        this.participant       = participant;
        //this.serviceTimeGenerator = serviceTimeGen;
        this.mttfGenerator     = mttfGenerator;
        this.mttrGenerator     = mttrGenerator;
        this.tokenOnFailure    = tokenOnFailure;
        this.queueOnFailure    = queueOnFailure;
        // Schedule first failure immediately from simulation time 0.
        // The engine must already be initialized when Performer is created.
        scheduleNextFailure(0.0);
        ExecutionEngine.getInstance().registerPerformer(this);
    }

    /**
     * Convenience constructor without failure model (no-failure Performer).
     */
    public Performer(String name, Participant participant){
                     //RandomVariableGenerator serviceTimeGen) {
        this(name, participant, null, null,
             TokenOnFailure.DELAY, QueueOnFailure.KEEP);
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
    // Service-start hook (called by Task when this Performer is a direct resource)
    // -----------------------------------------------------------------------

    @Override
    public void onServiceStarted(IncomingToken token, double startTime, double serviceDuration,
                                 TokenServiceCompleted pending, Task handlerTask) {
        super.onServiceStarted(token, startTime, serviceDuration, pending, handlerTask); // setBusy()
        this.tokenInService       = token;
        this.serviceStartTime     = startTime;
        this.remainingServiceTime = serviceDuration;
        this.pendingCompletion    = pending;
        // Failure may already be scheduled from the constructor; nothing extra needed.
    }

    @Override
    public void onServiceCompleted(double time) {
        tokenInService       = null;
        pendingCompletion    = null;
        serviceStartTime     = 0.0;
        remainingServiceTime = 0.0;
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

        // Handle the token currently in service (only if Performer is direct resource)
        if (tokenInService != null && pendingCompletion != null) {
            double elapsed = time - serviceStartTime;
            remainingServiceTime = Math.max(0.0, pendingCompletion.getTime() - time);

            switch (tokenOnFailure) {
                case DELAY:
                    // Cancel the scheduled completion; service resumes on repair
                    pendingCompletion.cancel();
                    pendingCompletion = null;
                    System.out.println(time + ") Performer " + getName()
                            + ": DELAY — token " + tokenInService.getTokenId()
                            + " paused, remaining=" + String.format("%.2f", remainingServiceTime));
                    break;

                case DISCARD:
                    pendingCompletion.cancel();
                    pendingCompletion = null;
                    tokenInService = null;
                    System.out.println(time + ") Performer " + getName()
                            + ": DISCARD — token lost");
                    break;

                case RESTART:
                    pendingCompletion.cancel();
                    pendingCompletion = null;
                    // Re-enqueue the interrupted token at the head of the Task's queue
                    List<Task> ts = findAssociatedTasks();
                    if (!ts.isEmpty()) {
                        ts.get(0).requeueToken(tokenInService);
                    }
                    tokenInService = null;
                    System.out.println(time + ") Performer " + getName()
                            + ": RESTART — token re-queued");
                    break;
            }
        }

        // Propagate failure up the parent/task chain
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

        // Apply QueueOnFailure policy (flush Task queues if needed)
        if (queueOnFailure == QueueOnFailure.FLUSH) {
            for (Task task : findAssociatedTasks()) {
                task.flushQueue();
                System.out.println(time + ") Performer " + getName()
                        + ": FLUSH — queue cleared for task " + task.getName());
            }
        }

        // Schedule next failure BEFORE notifying repair (so failure is in queue)
        scheduleNextFailure(time);

        if (tokenOnFailure == TokenOnFailure.DELAY && tokenInService != null) {
            // Resume in-progress service: schedule a new TokenServiceCompleted
            // at repairTime + remaining, mark this resource busy again.
            List<Task> ts = findAssociatedTasks();
            if (!ts.isEmpty()) {
                Task task = ts.get(0);
                setBusy(); // resource is in use for the resumed service
                TokenServiceCompleted resumeCompletion = new TokenServiceCompleted(
                        tokenInService.getTokenId(),
                        time + remainingServiceTime,
                        task,
                        tokenInService.getStartTimestamp());
                resumeCompletion.setResource(this);
                ExecutionEngine.getInstance().scheduleLocalEvent(resumeCompletion);
                System.out.println(time + ") Performer " + getName()
                        + ": DELAY resume — token " + tokenInService.getTokenId()
                        + " completes at " + String.format("%.2f", time + remainingServiceTime));
                tokenInService = null;

                // Only notify Resource parents (not tasks — we're busy with resumed service)
                this.available = true;
                for (Resource parent : parents) parent.onChildRepair(this);
            }
        } else {
            // No token to resume: propagate repair and let tasks process their queues
            tokenInService = null;
            notifyRepair();     // sets available=true, notifies parents and tasks
        }
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
