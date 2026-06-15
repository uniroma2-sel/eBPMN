package it.uniroma2.sel.ebpmn.generators;

import java.util.Random;

/**
 * Shared seeded pseudorandom number generator (wraps {@link java.util.Random}).
 *
 * <p>eBPMN maintains a single global {@code RandomGenerator} instance so that every
 * distribution-specific generator ({@link ExponentialGenerator}, {@link NormalGenerator},
 * etc.) draws from the same underlying uniform stream. This design guarantees that
 * simulation runs are fully reproducible when the same seed is supplied via
 * {@link #init(long)}.</p>
 *
 * <p>If no explicit seed is provided before the first variate is requested, the class
 * performs lazy initialization using the current wall-clock time (modulo 10⁶) as the
 * seed.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 */
public class RandomGenerator {

    /** The underlying {@link java.util.Random} instance; {@code null} until initialized. */
    private static Random random;

    /** The seed used to initialize {@link #random}. */
    private static long seed;

    /**
     * Initializes the generator with a user-supplied seed, enabling fully reproducible
     * simulation runs.
     *
     * <p>Calling this method resets the generator: all previously drawn variates are
     * discarded and the stream restarts from the new seed.</p>
     *
     * @param customSeed the seed value to use; any {@code long} is accepted
     */
    public static void init(long customSeed) {
        seed = customSeed;
        random = new Random(seed);
    }

    /**
     * Initializes the generator using the current system time as the seed.
     *
     * <p>The seed is computed as {@code System.currentTimeMillis() % 1_000_000}, producing
     * a non-deterministic but compact value. Use {@link #init(long)} instead when
     * reproducibility is required.</p>
     */
    //Initialization with no user seed. Current time in milliseconds are used
    public static void init() {
        seed = System.currentTimeMillis() % 1_000_000L;
        random = new Random(seed);
    }

    /**
     * Returns the next pseudorandom {@code double} value uniformly distributed in
     * {@code [0.0, 1.0)}.
     *
     * <p>Triggers lazy initialization (via {@link #init()}) if the generator has not
     * been initialized yet.</p>
     *
     * @return a uniformly distributed value in {@code [0.0, 1.0)}
     */
    public static double nextDouble() {
        ensureInitialized();
        return random.nextDouble();
    }

    /**
     * Returns the next pseudorandom value drawn from the standard normal distribution
     * N(0, 1), using the Box–Muller algorithm implemented by {@link java.util.Random}.
     *
     * <p>Triggers lazy initialization (via {@link #init()}) if the generator has not
     * been initialized yet.</p>
     *
     * @return a standard-normal variate with mean 0 and standard deviation 1
     */
    public static double nextGaussian() {
        ensureInitialized();
        return random.nextGaussian();
    }

    /**
     * Ensures the generator is initialized before use.
     *
     * <p>If {@link #random} is {@code null} (i.e., neither {@link #init(long)} nor
     * {@link #init()} has been called), this method performs lazy initialization
     * with a time-based seed.</p>
     */
    private static void ensureInitialized() {
        if (random == null) {
            init(); // lazy initialization with no user-defined seed
        }
    }
}
