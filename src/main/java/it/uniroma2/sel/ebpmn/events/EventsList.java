package it.uniroma2.sel.ebpmn.events;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Future Event List (FEL) — the central priority queue of the DES engine.
 *
 * <p>Maintains a time-ordered list of {@link Event} instances to be processed
 * by {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}.  Events are sorted
 * in non-decreasing order of logical time after every insertion; ties are
 * broken by insertion order (stable sort).
 *
 * <p>The list supports cancellation via {@link #removeEvent(Event)}, which is
 * used by {@link it.uniroma2.sel.ebpmn.resources.Performer} to cancel pending
 * service-completion events when a failure occurs mid-service.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Event
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 */
public class EventsList {
	/** The underlying sorted event buffer. */
	private ArrayList<Event> events;

	/** Creates an empty future event list. */
	public EventsList() {
		events = new ArrayList<>();
	}

	/**
	 * Returns the next event (lowest timestamp) without removing it.
	 *
	 * @return the imminent event
	 */
	public Event getNextEvent() {
		return events.get(0);
	}

	/**
	 * Removes and returns the next event (lowest timestamp).
	 *
	 * @return the removed imminent event
	 */
	public Event getAndRemoveNextEvent(){
		Event e = events.get(0);
		events.remove(0);
		return e;
	}

	/**
	 * Removes a specific event from the list (used for cancellation).
	 *
	 * @param e the event to remove
	 */
	public void removeEvent(Event e){
		events.remove(e);
	}

	/**
	 * Inserts an event into the list and re-sorts by logical time.
	 *
	 * @param e the event to add
	 */
	public void addEvent(Event e) {
		events.add(e);
		events.sort(Comparator.comparingDouble(Event::getTime));
	}

	/**
	 * Returns {@code true} if the list contains no pending events.
	 *
	 * @return {@code true} when the FEL is empty
	 */
	public boolean isEmpty() {
		return events.isEmpty();
	}

	/**
	 * Returns the raw event list (for inspection / debugging).
	 *
	 * @return the underlying {@link ArrayList}
	 */
	public ArrayList<Event> getList(){
		return this.events;
	}
}
