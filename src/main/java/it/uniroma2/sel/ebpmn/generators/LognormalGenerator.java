package it.uniroma2.sel.ebpmn.generators;

/**
 * Generates random samples from a Lognormal distribution parameterised by the
 * mean and standard deviation of the lognormal variable itself (i.e. in the
 * original domain), not by the parameters of the underlying normal distribution.
 *
 * <p>If {@code Y ~ N(μ, σ)} then {@code X = e^Y} follows a lognormal
 * distribution with mean {@code m} and standard deviation {@code s} in the
 * original domain.  The log-space parameters are derived internally as:
 * <pre>
 *   φ     = sqrt(s² + m²)
 *   μ     = ln(m² / φ)
 *   σ     = sqrt(ln(φ² / m²))
 * </pre>
 * and the variate is then generated as {@code X = exp(μ + σ · Z)},
 * where {@code Z ~ N(0,1)}.
 *
 * <p>Typical uses: service times, component MTTF in reliability models.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 * @see RandomGenerator
 */
public class LognormalGenerator implements RandomVariableGenerator {

    /** Mean of the underlying normal distribution (log-space), derived from constructor arguments. */
    private final double mu;
    /** Standard deviation of the underlying normal distribution (log-space), derived from constructor arguments. */
    private final double sigma;

    /**
     * Creates a LognormalGenerator parameterised by the mean and standard
     * deviation of the lognormal variable in the original domain.
     *
     * @param mean mean of the lognormal variable (original domain); must be positive
     * @param std  standard deviation of the lognormal variable (original domain); must be non-negative
     */
    public LognormalGenerator(double mean, double std) {
        double phi = Math.sqrt(std * std + mean * mean);
        this.mu    = Math.log(mean * mean / phi);
        this.sigma = Math.sqrt(Math.log(phi * phi / (mean * mean)));
    }

    /**
     * Returns a random variate from the lognormal distribution.
     *
     * @return a positive lognormally distributed random value
     */
    @Override
    public double get() {
        return Math.exp(mu + sigma * RandomGenerator.nextGaussian());
    }
}