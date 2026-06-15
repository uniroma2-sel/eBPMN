package it.uniroma2.sel.ebpmn.bpmn;

import java.util.ArrayList;

/**
 * Top-level container for a BPMN Collaboration model.
 *
 * <p>A Collaboration groups all {@link Participant} instances that take part in
 * a process interaction.  It corresponds to the outermost {@code collaboration}
 * element of a BPMN 2.0 diagram.  Participants may be local (each running their
 * own process flow) or remote proxies (mapped to {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode}
 * in distributed simulation).
 *
 * <p>The {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine} drives the simulation
 * by iterating over the participants registered in a Collaboration.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Participant
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 */
public class Collaboration {

	/** Logical name of this collaboration (typically the top-level system name). */
	String name;
	/** Ordered list of participants registered in this collaboration. */
	ArrayList<Participant> participants;

	/**
	 * Creates a new, empty Collaboration with the given name.
	 *
	 * @param name logical name of the collaboration
	 */
	public Collaboration(String name) {
		this.name = name;
		participants = new ArrayList<>();
	}

	/**
	 * Adds a participant to this collaboration.
	 *
	 * @param p the participant to add
	 */
	public void addParticipant(Participant p) {
		participants.add(p);
	}

	/**
	 * Returns the ordered list of all participants in this collaboration.
	 *
	 * @return list of participants
	 */
	public ArrayList<Participant> getParticipants(){
		return participants;
	}

	/**
	 * Looks up a participant by name.
	 *
	 * @param name the participant name to search for
	 * @return the matching {@link Participant}, or {@code null} if not found
	 */
	public Participant getParticipantByName(String name) {
		for (Participant p : participants) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
}
