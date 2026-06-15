package it.uniroma2.sel.ebpmn.resources.policies;

/**
 * Defines what happens to the token currently being served
 * when its resource fails mid-service.
 *
 * <ul>
 *   <li>{@link #DELAY}   – token pauses, preserving elapsed service time;
 *       resumes from where it stopped when the resource repairs.</li>
 *   <li>{@link #DISCARD} – token is lost.</li>
 *   <li>{@link #RESTART} – token is re-queued at the head of the Task queue;
 *       service restarts from scratch when the resource repairs.</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see QueueOnFailure
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 */
public enum TokenOnFailure {
    /** Token pauses; elapsed service time is preserved and resumed on repair. */
    DELAY,
    /** Token is permanently lost when the resource fails. */
    DISCARD,
    /** Token is re-queued; service restarts from scratch on repair. */
    RESTART
}
