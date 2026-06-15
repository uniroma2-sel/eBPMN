package it.uniroma2.sel.ebpmn.engine;

import it.uniroma2.sel.ebpmn.hla.HlaAdapter;

/**
 * HLA-based time manager for distributed simulation federates.
 *
 * <p>Implements the {@link TimeManager} contract using the HLA Time Management
 * service.  Each call to {@link #advanceTo(double)} issues a Time Advance
 * Request (TAR) to the RTI and blocks until a Time Advance Grant (TAG) is
 * received, ensuring conservative event ordering across federates (lookahead
 * protocol).
 *
 * <p>The {@link it.uniroma2.sel.ebpmn.hla.HlaAdapter} abstraction decouples
 * this class from any specific RTI vendor implementation.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see TimeManager
 * @see LocalTimeManager
 * @see it.uniroma2.sel.ebpmn.hla.HlaAdapter
 */
public class DistributedTimeManager extends TimeManager {


    /** The RTI adapter used to issue TAR/TAG calls. */
    private final HlaAdapter hla;

    /**
     * Creates a DistributedTimeManager backed by the given HLA adapter.
     *
     * @param hla       the RTI adapter
     * @param precision number of decimal places for time values
     */
    public DistributedTimeManager(HlaAdapter hla, int precision) {
        super(precision);
        this.hla = hla;
    }

    /**
     * Advances the federate's logical time to {@code nextTime} by issuing an
     * HLA Time Advance Request and blocking until the grant is received.
     *
     * @param nextTime the requested logical time
     * @return the granted logical time (may be less than or equal to {@code nextTime})
     */
    @Override
    public double advanceTo(double nextTime) {
        try {
            hla.requestTimeAdvance(nextTime);
            hla.waitForTimeAdvanceGrant();
            currentTime = nextTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentTime;
    }
}