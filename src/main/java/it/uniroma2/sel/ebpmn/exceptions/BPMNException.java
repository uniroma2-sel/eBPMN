package it.uniroma2.sel.ebpmn.exceptions;

/**
 * Base checked exception for eBPMN modelling errors.
 *
 * <p>Thrown when a structural constraint of the BPMN model is violated at
 * runtime, for example when the sum of routing probabilities in an
 * {@link it.uniroma2.sel.ebpmn.bpmn.gateways.ExclusiveDivergingGateway}
 * exceeds 1.0.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see UnexpectedEvent
 */
public class BPMNException extends Exception{

	/** Creates a BPMNException with no detail message. */
	public BPMNException() {
		super();
	}

	/**
	 * Creates a BPMNException with the given detail message.
	 *
	 * @param message description of the modelling error
	 */
	public BPMNException(String message) {
		super(message);
	}

}
