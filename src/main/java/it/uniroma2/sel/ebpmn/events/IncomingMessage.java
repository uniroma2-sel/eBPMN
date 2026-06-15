package it.uniroma2.sel.ebpmn.events;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Node;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * Message-flow event carrying a payload from a sender node to a target node.
 *
 * <p>IncomingMessage is produced by {@link it.uniroma2.sel.ebpmn.bpmn.events.IntermediateMessageThrowEvent},
 * {@link it.uniroma2.sel.ebpmn.bpmn.tasks.SendTask}, and
 * {@link it.uniroma2.sel.ebpmn.hla.HlaAdapterImpl} (for remote interactions).
 * It is consumed by {@link it.uniroma2.sel.ebpmn.bpmn.events.IntermediateMessageCatchEvent}
 * and {@link it.uniroma2.sel.ebpmn.bpmn.tasks.ReceiveTask}.
 *
 * <p>When the target is a {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode}, the
 * {@code handlerEntity} field is {@code null} (the event is not enqueued locally
 * but forwarded to the RTI).
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Token
 * @see it.uniroma2.sel.ebpmn.bpmn.events.IntermediateMessageCatchEvent
 */
public class IncomingMessage extends Event{
	/** Application-level payload (e.g., telemetry data, command). */
	private String messagePayload;
	/** Name of the sending node (used for logging). */
	private String sourceEntity;
	/** The target node (local {@link FlowNode} or {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode}). */
	private Node targetEntity;


	/**
	 * Creates an IncomingMessage event.
	 *
	 * <p>When the target is a {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode},
	 * {@code handlerEntity} is set to {@code null} because the message is
	 * forwarded to the RTI rather than enqueued locally.
	 *
	 * @param time         logical timestamp of the message
	 * @param sourceEntity name of the sending node
	 * @param targetEntity the receiving node (local {@link FlowNode} or a
	 *                     {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode})
	 * @param data         application-level payload string
	 */
	public IncomingMessage(double time, String sourceEntity, Node targetEntity, String data) {
		super(time, (FlowNode)(targetEntity instanceof FlowNode ? targetEntity : null));
		this.targetEntity = targetEntity;
		this.messagePayload = data;
		this.sourceEntity = sourceEntity;
	}

	@Override
	public void processByHandler() throws UnexpectedEvent {
		handlerEntity.handleEvent(this); // double dispatch
	}

	/**
	 * Returns the application-level payload carried by this message.
	 *
	 * @return message payload string
	 */
	public String getMessagePayload() {
		return messagePayload;
	}

	/**
	 * Returns the name of the node that sent this message.
	 *
	 * @return source entity name
	 */
	public String getSourceEntity() {
		return sourceEntity;
	}

	/**
	 * Returns the target node of this message (may be a RemoteNode).
	 *
	 * @return the target {@link Node}
	 */
	public Node getTargetEntity(){
		return targetEntity;
	}

}