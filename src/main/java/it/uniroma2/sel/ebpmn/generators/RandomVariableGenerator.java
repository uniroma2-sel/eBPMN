package it.uniroma2.sel.ebpmn.generators;

/**
 * Single-method interface for random variate generation.
 *
 * <p>Every stochastic parameter in eBPMN — service times, inter-arrival times,
 * Mean Time To Failure (MTTF), and Mean Time To Repair (MTTR) — is typed as
 * {@code RandomVariableGenerator}. This allows callers to remain agnostic of the
 * underlying probability distribution while still obtaining properly distributed
 * samples at runtime.</p>
 *
 * <p>All concrete implementations draw their randomness from the shared
 * {@link RandomGenerator} singleton, ensuring global reproducibility when the
 * simulator is seeded with a fixed value.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomGenerator
 * @see ExponentialGenerator
 * @see NormalGenerator
 * @see LognormalGenerator
 * @see UniformGenerator
 * @see DeterministicGenerator
 */
public interface RandomVariableGenerator {

    /**
     * Returns the next random variate drawn from the generator's underlying
     * probability distribution.
     *
     * @return a non-negative random sample (units are determined by the simulation
     *         context in which the generator is used, e.g. seconds for service times)
     */
    public double get();
}