package it.uniroma2.sel.ebpmn.bpmn.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Node;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.BPMNException;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;

/**
 * BPMN Intermediate Message Throw Event — sends a message and forwards the token.
 *
 * <p>When a token arrives, this node simultaneously:
 * <ol>
 *   <li>Creates an {@link it.uniroma2.sel.ebpmn.events.IncomingMessage} targeted at
 *       {@code targetNode} (which may be local or a
 *       {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode} for distributed simulation) and
 *       dispatches it immediately.</li>
 *   <li>Forwards the original token to the next sequence-flow node with no delay.</li>
 * </ol>
 *
 * <p>Message flow is configured via {@link #addMessageFlow(Node, String)}.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.bpmn.events.IntermediateMessageCatchEvent
 * @see it.uniroma2.sel.ebpmn.events.IncomingMessage
 */
public class IntermediateMessageThrowEvent extends FlowNode{

	/** The next node on the sequence flow (outgoing edge). */
	private FlowNode nextEntity;
	/** The message-flow target node (may be local or remote). */
    private Node targetNode;
	/** Payload data carried by the outgoing message. */
    private String messageData;

	public IntermediateMessageThrowEvent(String name, Participant p) {
		super(name, p);
	}

	public void addMessageFlow(Node targetNode, String data) {
		this.targetNode = targetNode;
		this.messageData = data;
	}

	public void addMessageFlow(FlowNode targetNode, String data) {
		this.targetNode = targetNode;
		this.messageData = data;
	}

	@Override
	public void handleEvent(IncomingToken token) throws UnexpectedEvent {
		//LOG
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN, Token ID: " + token.getTokenId());

		/* To send a message to the target node
		* a corresponding  ICOMING MESSAGE EVENT is scheduled
		*/
		IncomingMessage message = new IncomingMessage(token.getTime(),this.getName(), targetNode, messageData);
		targetNode.receiveEvent(message);

		//LOG
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ message.getTime() + " - " + this.getName()
			+ ": Scheduled INCOMING MESSAGE event at time "
			+ message.getTime() + " "
			+ "for " + message.getTargetEntity().getName());

		// the INCOMING TOKEN event is scheduled to the next entity with no delay
		try {
			nextEntity = this.getNextNode();
			token.setHandlerEntity(nextEntity);
			nextEntity.receiveEvent(token);
			//engine.getEventsList().addEvent(token);
		} catch (NodeNotFoundException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}


		//LOG
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ ": Scheduled INCOMING TOKEN event at time "
			+ token.getTime() + " "
			+ "with handler " + token.getHandlerEntity().getName());

		//writing the log entry
		log(token.getTokenId(), token.getTime());
	}

	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		this.unexpectedEvent(servedToken);
	}

	@Override
	public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
		this.unexpectedEvent(incomingMessage);
	}

	@Override
	public void handleEvent(StartProcess startEvent) throws UnexpectedEvent {
		this.unexpectedEvent(startEvent);
	}

	@Override
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}


	/*
	@Override
	public void handleEvent(Event e) throws UnexpectedEvent {
		//Intermediate Message Throw only admits INCOMING TOKEN events
		if(e instanceof IncomingToken) {

			IncomingToken token = (IncomingToken)e;
			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+ ": Received INCOMING TOKEN, Token ID: " + token.getTokenId());

			/* To send a message to the target node
			 * a corresponding  ICOMING MESSAGE EVENT is scheduled
			 *
			IncomingMessage message = new IncomingMessage(token.getTime(),this, targetNode, messageData);
			engine.getEventsList().addEvent(message);

			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ message.getTime() + " - " + this.getName()
			+ ": Scheduled INCOMING MESSAGE event at time "
			+ message.getTime() + " "
			+ "directed to " + message.getTargetEntity().getName());

			/*
			 * To forward the same INCOMING TOKEN event is scheduled
			 * to the next entity with no delay
			 *
			 *
			try {
				token.setTargetEntity(this.getNextNode());
			} catch (BPMNException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
			engine.getEventsList().addEvent(token);

			//LOG
			System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ ": Scheduled INCOMING TOKEN event at time "
			+ token.getTime() + " "
			+ "with handler " + token.getTargetEntity().getName());
			
			//writing the log entry
			log(token.getTokenId(), token.getTime());


		}
		else throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getSimpleName().toUpperCase() + " event type");

	}
	*/
	
	private void log(String tokenId, double tokenLogicalTime) {
		
		LogData ld = new LogData();
		LocalDateTime initialTime = engine.getInitialTime();
		DateTimeFormatter dateTimeFormat = engine.getDateTimeFormat(); 
		
		//token ID corresponds to the case ID
		ld.setCaseId(tokenId);  
		
		/*
		 * An intermediate throw event does not add any delay in forwarding
		 * the token (i.e., it does not require any time to be computed)
		 * Only the completion time is recorded
		 * */
		
		ld.setStartTimestamp("");
		
		long seconds;
		long nanos;
		seconds = (long) tokenLogicalTime;
        nanos = (long) ((tokenLogicalTime - seconds) * 1_000_000_000);
        
        //The completion time is the initial time + the current token logical time
        ld.setCompleteTimestamp(initialTime.plusSeconds(seconds).plusNanos(nanos).format(dateTimeFormat));
        ld.setActivity(this.getName());
        /*
         * As an intermediate event does not execute any
         * action, it does not requires any resource as well
         * 
         */
        ld.setResources("");
        ld.setRoles("");
        ld.setLocalEntity(this.getParticipant().getName());
        ld.setRemoteEntity(targetNode.getParticipant().getName());
        ld.setCommType(CommunicationKind.send);
        ld.setData(messageData);
	
        this.writeLog(ld);
	}


}
