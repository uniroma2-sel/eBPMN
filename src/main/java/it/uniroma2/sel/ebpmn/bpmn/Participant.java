package it.uniroma2.sel.ebpmn.bpmn;

import java.util.ArrayList;

/**
 * Represents a local BPMN Pool — an autonomous entity with its own process flow.
 *
 * <p>A {@code Participant} owns an ordered list of {@link FlowNode} elements that together
 * define its BPMN process.  The boolean {@code isInitiating} flag distinguishes two roles:
 * <ul>
 *   <li>{@code true} — a <em>local</em> participant whose process is executed by the local
 *       {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine} (i.e. a local federate).</li>
 *   <li>{@code false} — a <em>placeholder</em> for a remote participant whose logic runs
 *       in a different HLA federate; only used to register a name so that
 *       {@link RemoteNode} references can be resolved during model construction.</li>
 * </ul>
 *
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Collaboration
 * @see RemoteNode
 * @see FlowNode
 */
public class Participant {

	/** Human-readable identifier for this participant within the collaboration. */
	private String name;

	/**
	 * {@code true} if this participant is local (its process is executed by the local engine);
	 * {@code false} if it is a placeholder for a remote participant in another HLA federate.
	 */
	private boolean isInitiating;

	/**
	 * Ordered list of {@link FlowNode} elements composing the process executed by this participant.
	 * Nodes are appended in construction order via {@link #addFlowNode(FlowNode)}.
	 */
	ArrayList<FlowNode> process;

	/**
	 * Constructs a participant with the given name and locality flag.
	 *
	 * @param name         human-readable identifier for this participant
	 * @param isInitiating {@code true} for a local participant whose process runs on the local
	 *                     engine; {@code false} for a remote-participant placeholder
	 */
	public Participant(String name, boolean isInitiating) {
		this.name = name;
		this.isInitiating = isInitiating;
		process = new ArrayList<>();
	}

	/**
	 * Returns the name of this participant.
	 *
	 * @return the participant name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns {@code true} if this participant is local (its process is executed by the
	 * local {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}), or {@code false} if it
	 * is a placeholder for a remote participant in another HLA federate.
	 *
	 * @return {@code true} for a local participant; {@code false} for a remote placeholder
	 */
	public boolean isInitiating() {
		return this.isInitiating;
	}

	/**
	 * Updates the name of this participant.
	 *
	 * @param name the new participant name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Appends a {@link FlowNode} to this participant's process flow list.
	 * Called automatically by {@link FlowNode#FlowNode(String, Participant)} during
	 * node construction.
	 *
	 * @param n the flow node to register
	 */
	public void addFlowNode(FlowNode n) {
		process.add(n);
	}

	/**
	 * Returns the {@link FlowNode} at the given position in the process flow list.
	 *
	 * @param i zero-based index into the process flow list
	 * @return the flow node at position {@code i}
	 */
	public FlowNode getFlowNode(int i) {
		return process.get(i);
	}

	/**
	 * Returns the first node in the process flow list, conventionally the {@code Start} node.
	 *
	 * @return the start node of this participant's process
	 */
	public FlowNode getStartNode() {
		return process.get(0);
	}

	/**
	 * Returns the last node in the process flow list, conventionally the {@code End} node.
	 *
	 * @return the end node of this participant's process
	 */
	public FlowNode getEndNode() {
		return process.get(process.size()-1);
	}

	/**
	 * Looks up a flow node by name within this participant's process.
	 *
	 * @param nodeName the name of the node to find
	 * @return the matching {@link FlowNode}, or {@code null} if no node with that name exists
	 */
	public FlowNode getNodeByName(String nodeName) {
		for (FlowNode node : process) {
			if (nodeName.equals(node.getName())) {
				return node;
			}
		}
		return null;
	}

}
