package it.uniroma2.sel.ebpmn.engine;

/**
 * Time manager for local (single-JVM) discrete event simulation.
 *
 * <p>In a purely local run there is no external synchronisation authority, so time
 * advancement is instantaneous: the method simply returns the requested timestamp,
 * effectively setting the engine clock to the timestamp of the next event in the
 * {@link it.uniroma2.sel.ebpmn.events.EventsList}. No blocking or inter-process
 * communication takes place.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see TimeManager
 * @see DistributedTimeManager
 * @see ExecutionEngine
 */
public class LocalTimeManager extends TimeManager {

    /**
     * Constructs a {@code LocalTimeManager} with the specified decimal precision.
     *
     * @param precision the number of decimal digits used for timestamp rounding,
     *                  as read from {@link it.uniroma2.sel.ebpmn.configuration.SimulationConfig}
     */
    public LocalTimeManager(int precision) {
        super(precision);
    }

    /**
     * Advances virtual simulation time to {@code nextTime} with no blocking or
     * external synchronisation.
     *
     * @param nextTime the target virtual time, equal to the timestamp of the next
     *                 event in the future event list
     * @return {@code nextTime} unchanged
     */
    @Override
    public double advanceTo(double nextTime) {
        return nextTime;
    }
}
