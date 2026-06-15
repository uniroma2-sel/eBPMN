package it.uniroma2.sel.ebpmn.generators;

/**
 * Generates random samples from an Exponential(λ) distribution.
 *
 * <p>The inverse-transform method is used:
 * {@code X = −ln(1 − U) / λ}, where {@code U ~ Uniform(0,1)}.
 * Mean of the distribution is {@code 1/λ}.
 *
 * <p>Typical uses: inter-arrival times (Poisson arrivals), MTTR (repair times).
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 * @see RandomGenerator
 */
public class ExponentialGenerator implements RandomVariableGenerator {
    /** Rate parameter (λ = 1/mean). */
    private final double lambda;

    /**
     * Creates an ExponentialGenerator with rate {@code lambda}.
     *
     * @param lambda rate parameter λ (= 1 / mean); must be positive
     */
    public ExponentialGenerator(double lambda) {
        this.lambda = lambda;
    }

    /**
     * Returns a random variate from Exponential(λ).
     *
     * @return a non-negative exponentially distributed random value
     */
    @Override
    public double get() {
    	/*
    	 * Generation of a random number according to an Exponential (lambda)
    	 * probability distribution
    	 */
        return -Math.log(1 - RandomGenerator.nextDouble()) / lambda;
    }
}
