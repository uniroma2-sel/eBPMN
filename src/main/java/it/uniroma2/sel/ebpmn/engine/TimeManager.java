package it.uniroma2.sel.ebpmn.engine;

/**
 * Abstract contract for time advancement in the eBPMN Discrete Event Simulation engine.
 *
 * <p>The {@link ExecutionEngine} delegates all time-advance operations to a concrete
 * {@code TimeManager} subclass, keeping the engine loop independent of whether the
 * simulation is running locally or in an HLA-distributed federation. Two concrete
 * implementations are provided:</p>
 * <ul>
 *   <li>{@link LocalTimeManager} — advances virtual time instantaneously with no
 *       external synchronisation.</li>
 *   <li>{@link DistributedTimeManager} — issues a Time Advance Request (TAR) to the
 *       RTI and blocks until a Time Advance Grant (TAG) is received.</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see LocalTimeManager
 * @see DistributedTimeManager
 * @see ExecutionEngine
 */
public abstract class TimeManager {

    /** Number of decimal digits used when rounding simulation timestamps. */
    private final int precision;    // number of decimal digits

    /** The virtual simulation time after the most recent successful advance. */
    protected double currentTime;

    /**
     * Constructs a {@code TimeManager} with the specified decimal precision.
     *
     * @param precision the number of decimal digits used for timestamp rounding
     */
    public TimeManager(int precision){
        this.precision = precision;
    }

    /**
     * Advances virtual simulation time to the given target timestamp.
     *
     * <p>In a local simulation the advance is immediate; in a distributed simulation
     * the method blocks until the RTI grants the requested time advance.</p>
     *
     * @param nextTime the target virtual time to advance to
     * @return the actual virtual time reached after the advance (may be less than
     *         {@code nextTime} in a distributed scenario if a lower-timestamped
     *         event arrives while waiting for the grant)
     */
    public abstract double advanceTo(double nextTime);

    /*
    public long toLogicalTime(double userTime) {
        long scale = (long) Math.pow(10, precision); // 1 logical time unit = user time unit / 10^decimals
        return Math.round(userTime * scale);
    }

    public double toUserTime(long logicalTime) {
        long scale = (long) Math.pow(10, precision); // 1 logical time unit = user time unit / 10^decimals
        return (double) logicalTime / scale;
    }*/
}


