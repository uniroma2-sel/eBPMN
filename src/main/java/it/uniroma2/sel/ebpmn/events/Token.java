package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * Abstract base class representing a BPMN control-flow token travelling through
 * the process graph.
 *
 * <p>A token carries a unique identifier and an optional reference to the
 * {@link it.uniroma2.sel.ebpmn.resources.Resource} that is (or will be) executing
 * the activity associated with this token.  Concrete subclasses ({@link IncomingToken},
 * {@link TokenServiceCompleted}) specialise the token for different lifecycle phases.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see IncomingToken
 * @see TokenServiceCompleted
 * @see it.uniroma2.sel.ebpmn.resources.Resource
 */
public abstract class Token extends Event{
	/** Unique identifier that tracks this token across its entire lifecycle in the process. */
	private String tokenId;
	/** The {@link it.uniroma2.sel.ebpmn.resources.Resource} responsible for executing
	 *  the activity this token is waiting for; {@code null} if no resource is required. */
	private Resource resource;  //potential resource responsible for the actual
								//execution of the activity

	/**
	 * Constructs a token without a start-timestamp.
	 *
	 * @param id     unique token identifier
	 * @param time   logical simulation time at which this token event is scheduled
	 * @param entity the {@link FlowNode} that will handle this token
	 */
	public Token(String id, double time, FlowNode entity) {
		super(time, entity);
		tokenId=id;

	}

	/**
	 * Constructs a token with a start-timestamp for response-time measurement.
	 *
	 * @param id        unique token identifier
	 * @param time      logical simulation time at which this token event is scheduled
	 * @param entity    the {@link FlowNode} that will handle this token
	 * @param startTime simulation time at which this token was originally created
	 */
	public Token(String id, double time, FlowNode entity, double startTime) {
		super(time, entity, startTime);
		tokenId=id;
	}

	/**
	 * Sets the unique identifier of this token.
	 *
	 * @param id the new token identifier
	 */
	public void setTokenId(String id) {
		this.tokenId=id;
	}

	/**
	 * Returns the unique identifier of this token.
	 *
	 * @return token identifier string
	 */
	public String getTokenId() {
		return this.tokenId;
	}

	/**
	 * Associates a {@link it.uniroma2.sel.ebpmn.resources.Resource} with this token,
	 * recording which resource is handling its execution.
	 *
	 * @param r the resource assigned to serve this token
	 */
	public void setResource(Resource r) {
		this.resource = r;
	}

	/**
	 * Returns the {@link it.uniroma2.sel.ebpmn.resources.Resource} assigned to this
	 * token, or {@code null} if none has been assigned.
	 *
	 * @return the assigned resource, or {@code null}
	 */
	public Resource getResource() {
		return this.resource;
	}
}
