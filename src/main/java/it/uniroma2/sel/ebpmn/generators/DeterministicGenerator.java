package it.uniroma2.sel.ebpmn.generators;

/**
 * Deterministic (constant) pseudo-random variable generator.
 *
 * <p>Always returns the same fixed value regardless of how many times
 * {@link #get()} is called.  Useful for testing, calibration, and scenarios
 * where a parameter is known precisely (e.g., a fixed service time).
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see RandomVariableGenerator
 */
public class DeterministicGenerator implements RandomVariableGenerator{

	/** The constant value returned by every call to {@link #get()}. */
	private double deterministicValue;

	/**
	 * Creates a generator that always returns {@code value}.
	 *
	 * @param value the fixed value to return
	 */
	public DeterministicGenerator(double value) {
        this.deterministicValue = value;
    }

	/**
	 * Returns the fixed deterministic value.
	 *
	 * @return the constant value supplied at construction
	 */
    @Override
    public double get() {
        return deterministicValue;
    }
}
