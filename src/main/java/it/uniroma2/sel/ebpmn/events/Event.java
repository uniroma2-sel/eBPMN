package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/*
* Implements an abstract event in eBPMN.
* As there are multiple events that trigger different behaviors in the receiving entity a Visitor design pattern
* has been used to handle the double dispatch. The event class acts like a visitor
* */

/**
 * Abstract base class for all simulation events in the eBPMN future event list.
 *
 * <p>Every event carries a logical simulation time and, optionally, a reference to the
 * {@link FlowNode} that must handle it. Subclasses implement
 * {@link #processByHandler()} to perform the double-dispatch call that routes the event
 * to the correct {@code handleEvent} overload on the target node or resource.</p>
 *
 * <p>Events are kept in time order inside {@link EventsList} via the {@link Comparable}
 * implementation, which delegates to {@link Double#compare(double, double)}.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see EventsList
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 */
public abstract class Event implements Comparable<Event>{
	/** Logical simulation time at which this event is scheduled to fire. */
	private double time;
	/** The BPMN {@link FlowNode} responsible for handling this event; may be {@code null}
	 *  for resource-targeted events ({@link ResourceFailureEvent}, {@link ResourceRepairEvent},
	 *  {@link SwitchCompletedEvent}) that bypass the flow-node hierarchy. */
	protected FlowNode handlerEntity;
	/** Simulation time at which the originating token was first created;
	 *  used for end-to-end response-time statistics. Zero if not set. */
	private double startTimestamp;

	/**
	 * Constructs an event targeting a specific {@link FlowNode}.
	 *
	 * @param time   logical simulation time at which this event fires
	 * @param entity the {@link FlowNode} that will handle this event
	 */
	public Event(double time, FlowNode entity) {
		this.time = time;
		this.handlerEntity = entity;
	}

	/**
	 * No-entity constructor for resource-targeted events (ResourceFailureEvent,
	 * ResourceRepairEvent, SwitchCompletedEvent).  These events dispatch directly
	 * to the owning Resource rather than going through the FlowNode hierarchy.
	 *
	 * @param time logical simulation time at which this event fires
	 */
	protected Event(double time) {
		this.time = time;
		this.handlerEntity = null;
	}

	/**
	 * Constructs an event targeting a {@link FlowNode}, also recording the token's
	 * original creation time for response-time measurement.
	 *
	 * @param time      logical simulation time at which this event fires
	 * @param entity    the {@link FlowNode} that will handle this event
	 * @param startTime simulation time at which the token that generated this event was created
	 */
	public Event(double time, FlowNode entity, double startTime) {
		this.time = time;
		this.handlerEntity = entity;
		this.startTimestamp = startTime;
	}

	/**
	 * Returns the simulation time at which the originating token was created.
	 *
	 * @return token creation timestamp, or {@code 0.0} if not set
	 */
	public double getStartTimestamp() {
		return this.startTimestamp;
	}

	/**
	 * Returns the logical simulation time at which this event is scheduled.
	 *
	 * @return scheduled simulation time
	 */
	public double getTime() {
		return time;
	}

	/**
	 * Overrides the scheduled simulation time of this event.
	 *
	 * @param time new logical simulation time
	 */
	public void setTime(double time) {
		this.time = time;
	}

	/**
	 * Returns the {@link FlowNode} that will handle this event, or {@code null} for
	 * resource-targeted events.
	 *
	 * @return handler {@link FlowNode}, or {@code null}
	 */
	public FlowNode getHandlerEntity() {
	//public Node getTargetEntity() {
		return handlerEntity;
	}

	/**
	 * Replaces the handler {@link FlowNode} for this event.
	 *
	 * @param node the new handler {@link FlowNode}
	 */
	public void setHandlerEntity(FlowNode node) {
		this.handlerEntity = node;
	}

	//Manages the processing of an event on a local FlowNode entity
	/**
	 * Dispatches this event to its handler via double dispatch.
	 *
	 * <p>Concrete subclasses call the appropriate {@code handleEvent()} overload on
	 * {@link #handlerEntity} (or directly on the owning resource for infrastructure
	 * events), ensuring that the correct polymorphic method is invoked.</p>
	 *
	 * @throws UnexpectedEvent if the handler does not recognise this event type
	 */
	public abstract void processByHandler() throws UnexpectedEvent;

	//public abstract void deliverTo(Node node) throws UnexpectedEvent;

	/**
	 * Compares this event with another by scheduled simulation time, enabling
	 * time-ordered sorting inside {@link EventsList}.
	 *
	 * @param o the other event to compare against
	 * @return a negative integer, zero, or a positive integer as this event's time
	 *         is less than, equal to, or greater than {@code o}'s time
	 */
	@Override
	public int compareTo(Event o) {
		return Double.compare(this.getTime(), o.getTime());
	}

}

