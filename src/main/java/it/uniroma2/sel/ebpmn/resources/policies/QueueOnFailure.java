package it.uniroma2.sel.ebpmn.resources.policies;

/**
 * Defines what happens to the waiting queue of a failed resource on repair.
 *
 * <ul>
 *   <li>{@link #KEEP}  – tokens that accumulated while the resource was down are
 *       retained and served in arrival order once the resource repairs.</li>
 *   <li>{@link #FLUSH} – the entire waiting queue is discarded on repair;
 *       service restarts with an empty queue.</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see TokenOnFailure
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 */
public enum QueueOnFailure {
    /** Waiting tokens are kept and served in order once the resource repairs. */
    KEEP,
    /** Waiting queue is discarded on repair; service restarts empty. */
    FLUSH
}
