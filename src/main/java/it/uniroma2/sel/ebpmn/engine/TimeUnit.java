package it.uniroma2.sel.ebpmn.engine;

/**
 * Enumeration of the time units available for expressing simulation time.
 *
 * <p>The chosen constant determines the physical interpretation of every timestamp
 * and duration value used throughout an eBPMN simulation run. It is specified in
 * the JSON configuration file and read by {@link it.uniroma2.sel.ebpmn.configuration.SimulationConfig}.
 * The engine itself is dimensionless — {@code TimeUnit} is used only for labelling
 * output and for converting user-facing values when needed.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.configuration.SimulationConfig
 * @see ExecutionEngine
 */
public enum TimeUnit {

    /** One billionth of a second (10<sup>-9</sup> s). */
    NANOSECONDS,

    /** One millionth of a second (10<sup>-6</sup> s). */
    MICROSECONDS,

    /** One thousandth of a second (10<sup>-3</sup> s). */
    MILLISECONDS,

    /** One second — the SI base unit of time. */
    SECONDS,

    /** Sixty seconds. */
    MINUTES,

    /** Three thousand six hundred seconds. */
    HOURS,

    /** Twenty-four hours. */
    DAYS;
}