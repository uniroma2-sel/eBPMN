package it.uniroma2.sel.ebpmn.generators;

/**
 * Generates random samples from a Normal(μ, σ) distribution.
 *
 * <p>Uses the Box-Muller transform via {@link RandomGenerator#nextGaussian()}:
 * {@code X = μ + σ · Z}, where {@code Z ~ N(0,1)}.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 * @see RandomGenerator
 */
public class NormalGenerator implements RandomVariableGenerator{
    /** Mean of the normal distribution. */
    private double mu;
    /** Standard deviation of the normal distribution. */
    private double sigma;

    /**
     * Creates a NormalGenerator with the given parameters.
     *
     * @param mu    mean of the normal distribution
     * @param sigma standard deviation (must be non-negative)
     */
    public NormalGenerator(double mu, double sigma) {
        this.mu = mu;
        this.sigma = sigma;
    }

    /**
     * Returns a random variate from Normal(μ, σ).
     *
     * @return a normally distributed random value
     */
    @Override
    public double get() {
    	/*
    	 * Generation of a random number according to an Normal (mu, sigma) probability
    	 * distribution
    	*/
    	double d = RandomGenerator.nextGaussian();
    	return mu + sigma * d;
    }
}
