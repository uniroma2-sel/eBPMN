package it.uniroma2.sel.ebpmn.resources.policies;

/**
 * Standby mode for redundant resource groups managed by a
 * {@link it.uniroma2.sel.ebpmn.resources.Broker}.
 *
 * <ul>
 *   <li>{@link #HOT}  — alternative units accumulate ageing and can fail while
 *       in standby (warm/hot standby).  All MTTF clocks run simultaneously.</li>
 *   <li>{@link #COLD} — alternative units are powered off while in standby and
 *       cannot fail until they are switched in (cold standby).  Only the active
 *       unit's MTTF clock runs.</li>
 * </ul>
 *
 * <p>The eBPMN {@link it.uniroma2.sel.ebpmn.resources.Broker} implementation honours both
 * modes: COLD standby cancels pending
 * {@link it.uniroma2.sel.ebpmn.events.ResourceFailureEvent}s for inactive
 * alternatives, while HOT standby leaves all MTTF clocks running.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.resources.Broker
 * @see it.uniroma2.sel.ebpmn.resources.Performer
 */
public enum StandbyMode {
    /** All MTTF clocks run; non-active units can fail during standby. */
    HOT,
    /** MTTF clock suspended for non-active units; failures only occur when active. */
    COLD
}
