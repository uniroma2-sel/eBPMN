package it.uniroma2.sel.ebpmn.bpmn.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeSet;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.BPMNException;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;

/**
 * BPMN Intermediate Message Catch Event — synchronises an incoming token with
 * an incoming message before forwarding the token downstream.
 *
 * <p>Two independent streams converge at this node:
 * <ul>
 *   <li>Tokens arriving via the sequence flow (handled by
 *       {@link #handleEvent(it.uniroma2.sel.ebpmn.events.IncomingToken)}).</li>
 *   <li>Messages arriving from a message flow (handled by
 *       {@link #handleEvent(it.uniroma2.sel.ebpmn.events.IncomingMessage)}).</li>
 * </ul>
 *
 * <p>The node maintains two internal {@link TreeSet}-based queues.  A token is
 * forwarded only when a matching message is available, and vice versa.  The
 * <em>first-available</em> pair is always consumed in logical-time order.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see IntermediateMessageThrowEvent
 * @see it.uniroma2.sel.ebpmn.events.IncomingMessage
 * @see it.uniroma2.sel.ebpmn.events.IncomingToken
 */
public class IntermediateMessageCatchEvent extends FlowNode {

	/** Tokens waiting for a corresponding incoming message. */
	private TreeSet<Event> tokenQueue;
	/** Messages waiting for a corresponding incoming token. */
	private TreeSet<Event> messageQueue;
	/** The next sequence-flow node (outgoing edge). */
	private FlowNode nextEntity;
	
	public IntermediateMessageCatchEvent(String name, Participant p) {
		super(name, p);
		tokenQueue = new TreeSet<>();
		messageQueue = new TreeSet<>();
	}



	@Override
	public void handleEvent(IncomingToken inToken) throws UnexpectedEvent {
		IncomingMessage message;
        try {
            nextEntity = this.getNextNode();
        } catch (NodeNotFoundException e) {
            e.printStackTrace();
        }

        //LOG
		System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN event, Token ID: " + inToken.getTokenId());

		/*
		 * If the message queue is not empty, a message is consumed
		 * If the token queue is emtpy the token is routed
		 * to the next entity. Otherwise, the incoming token is
		 * enqueued and the first token in the queue is forwarded.
		 *
		 * If the message queue is emtpy, the token is enqueued
		 */

		if(!messageQueue.isEmpty()) {
		//A message available. the token is immediately processed
			IncomingToken tokenToBeForwarded;
			message = (IncomingMessage)messageQueue.pollFirst(); //message consumed
			System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available message ");

			if (tokenQueue.isEmpty()) {
			//no tokens are waiting
			//the incoming token is forwarded
				tokenToBeForwarded = inToken;
				System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": no token are waiting in queue. ");
			}
			else {//token queue not emtpy
				//the incoming token is enqueued
				tokenQueue.add(inToken);
				//the first token is pulled from the queue
				tokenToBeForwarded = (IncomingToken)tokenQueue.pollFirst();

				System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - "
					+ this.getName() + ": token queue is not empty. Token ID " + inToken.getTokenId() + "is enqueued, Token ID " + tokenToBeForwarded.getTokenId() + "is processed");

			}

			//the token is forwarded with no delay
            tokenToBeForwarded.setHandlerEntity(nextEntity);
            //engine.getEventsList().addEvent(tokenToBeForwarded);
            nextEntity.receiveEvent(tokenToBeForwarded);

            //LOG
			System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ "Scheduled INCOMING TOKEN event at time " + tokenToBeForwarded.getTime() + " for "
				+ tokenToBeForwarded.getHandlerEntity().getName());


			//Writing the log entry
			this.log(tokenToBeForwarded.getTokenId(), tokenToBeForwarded.getTime(), message.getSourceEntity(), message.getMessagePayload());
		}
		else {
		//No messages available. The token is enqueued
			tokenQueue.add(inToken);
			//LOG
			System.out.println(inToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": No message avaialable. TOKEN ID " + inToken.getTokenId() + " is enqueued");
		}
	}

	@Override
	public void handleEvent(IncomingMessage message) throws UnexpectedEvent {

		//LOG
		System.out.println(message.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING MESSAGE");

		/*
		 * If the token queue is not empty, the message is consumed
		 * and the first token in the queue is routed to the next entity.
		 * Otherwise the message is enqueued.
		 *
		 */

		if(!tokenQueue.isEmpty()) {
			IncomingToken token = (IncomingToken)tokenQueue.pollFirst();
			token.setTime(message.getTime());

			//the token is forwarded with no delay
            token.setHandlerEntity(nextEntity);
            //engine.getEventsList().addEvent(token);
            nextEntity.receiveEvent(token);


            //LOG
			System.out.println(message.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() +	" Found Token ID " + token.getTokenId() + " waiting in queue. "
				+ " Forwarded with no delay to " + token.getHandlerEntity().getName());

			//Writing the log entry
			this.log(token.getTokenId(), token.getTime(), message.getSourceEntity(), message.getMessagePayload());


		}else {
			messageQueue.add(message);
			System.out.println(message.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + " Token queue is empty. The message is enqueued");
		}
	}

	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		this.unexpectedEvent(servedToken);
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

	/*@Override
	public void handleEvent(Event e) throws UnexpectedEvent {
		IncomingMessage message;
		
		if(e instanceof IncomingToken) {

			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+ ": Received INCOMING TOKEN event, Token ID: " + ((IncomingToken)e).getTokenId());

			/*
			 * If the message queue is not empty, a message is consumed
			 * If the token queue is emtpy the token is routed
			 * to the next entity. Otherwise, the incoming token is
			 * enqueued and the first token in the queue is forwarded.
			 *
			 * If the message queue is emtpy, the token is enqueued
			 *
			 *

			if(!messageQueue.isEmpty()) {
			//A message available. the token is immediately processed
				IncomingToken token;

				message = (IncomingMessage)messageQueue.pollFirst(); //message consumed
				
				System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available message ");
				
				if (tokenQueue.isEmpty()) {
					//no tokens are waiting
					//the incoming token is forwarded
					token = (IncomingToken)e;
					
					System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": no token are waiting in queue. ");
				}
				else {
					//token queue not emtpy
					tokenQueue.add(e); //the incoming token is enqueued
					//
					//the first token is pulled from the queue
					token = (IncomingToken)tokenQueue.pollFirst();
					
					System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " 
					+ this.getName() + ": token queue is not empty. Token ID " + ((IncomingToken)e).getTokenId() + "is enqueued, Token ID " + token.getTokenId() + "is processed");
					
				}

				//the token is forwarded with no delay
				try {
					token.setTargetEntity(this.getNextNode());
				} catch (BPMNException ex) {ex.printStackTrace();}
				engine.getEventsList().addEvent(token);
				//LOG
				System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() 
				+ "Scheduled INCOMING TOKEN event at time " + token.getTime() + " for " 
				+ token.getTargetEntity().getName());
						
				
				//Writing the log entry
				this.log(token.getTokenId(), token.getTime(), message.getSourceEntity().getParticipant().getName(), message.getMessagePayload());
			}
			else {
			//No messages available. The token is enqueued
				tokenQueue.add(e);
				//LOG
				System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": No message avaialable. TOKEN ID " + ((IncomingToken)e).getTokenId() + " is enqueued");
			}

		}
		else if(e instanceof IncomingMessage) {
			message = (IncomingMessage)e;
			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ ": Received INCOMING MESSAGE");

			/*
			 * If the token queue is not empty, the message is consumed
			 * and the first token in the queue is routed to the next entity.
			 * Otherwise the message is enqueued.
			 *
			 *

			if(!tokenQueue.isEmpty()) {
				IncomingToken token = (IncomingToken)tokenQueue.pollFirst();
				token.setTime(e.getTime());
				//the token is forwarded with no delay
				try {
					token.setTargetEntity(this.getNextNode());
				} catch (BPMNException ex) {ex.printStackTrace();}
				engine.getEventsList().addEvent(token);
				//LOG
				System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+	" Found Token ID " + token.getTokenId() + " waiting in queue. "
				+ "\\ Forwarded with no delay to " + token.getTargetEntity().getName());
				
				//Writing the log entry
				this.log(token.getTokenId(), token.getTime(), message.getSourceEntity().getParticipant().getName(), message.getMessagePayload());
				
				
			}else {
				messageQueue.add(e);
				System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + " Token queue is empty. The message is enqueued");
			}

		}
		else throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getSimpleName().toUpperCase() + " event type");
	}*/
	
private void log(String tokenId, double tokenLogicalTime, String remoteEntity, String messagePayload) {
		
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
        ld.setRemoteEntity(remoteEntity);
        ld.setCommType(CommunicationKind.receive);
        ld.setData(messagePayload);
	
        this.writeLog(ld);
	}
}
