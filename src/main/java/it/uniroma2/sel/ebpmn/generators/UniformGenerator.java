package it.uniroma2.sel.ebpmn.generators;

/**
 * Generates random samples from a Uniform(0, 1) distribution.
 *
 * <p>Returns values in the half-open interval {@code [0.0, 1.0)} using
 * {@link RandomGenerator#nextDouble()}.  Suitable for routing decisions,
 * probability checks, and as a source for other transformations.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 * @see RandomGenerator
 */
public class UniformGenerator implements RandomVariableGenerator{
    /**
     * Returns a uniform pseudo-random number in {@code [0.0, 1.0)}.
     *
     * @return a double drawn from Uniform(0, 1)
     */
    @Override
    public double get() {
        return RandomGenerator.nextDouble();
    }
}
