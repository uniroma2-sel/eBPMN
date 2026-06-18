package it.uniroma2.sel.ebpmn.bpmn.tasks;

import java.util.ArrayList;
import java.util.TreeSet;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.Event;
import it.uniroma2.sel.ebpmn.events.IncomingMessage;
import it.uniroma2.sel.ebpmn.events.IncomingToken;
import it.uniroma2.sel.ebpmn.events.TokenServiceCompleted;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;
import it.uniroma2.sel.ebpmn.resources.Resource;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;

/**
 * BPMN Receive Task — requires both an available resource AND an incoming message
 * before it can service a token.
 *
 * <p>A ReceiveTask extends {@link Task} with a secondary synchronisation
 * condition: a token is only dequeued and served when a resource is free
 * <em>and</em> an {@link it.uniroma2.sel.ebpmn.events.IncomingMessage} is
 * available in the internal message queue.  Messages are enqueued by
 * {@link #handleEvent(it.uniroma2.sel.ebpmn.events.IncomingMessage)}.
 *
 * <p>This models a task that performs work in response to an external trigger
 * (e.g., processing a received command).
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Task
 * @see it.uniroma2.sel.ebpmn.events.IncomingMessage
 */
public class ReceiveTask extends Task{

	/** Queue of messages waiting to be paired with an incoming token. */
	private TreeSet<Event> messageQueue;
	
	public ReceiveTask(String name, Participant p, RandomVariableGenerator gen) {
		super(name, p, gen);
		messageQueue = new TreeSet<>();
	}
	
	public boolean isMessageQueueEmpty() {
		return this.messageQueue.isEmpty();
	}


	@Override
	public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
		System.out.println(incomingMessage.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING MESSAGE");
		ProcessMessage(incomingMessage);
	}

	/*
	@Override
	public void handleEvent(Event e) throws UnexpectedEvent{
		/*
		 * A task is associated to a pool of resources which are in charge of
		 * executing or overseeing the relevant activity. Resources might be responsible
		 * of more than one task.
 		 *
		 * *
		if(e instanceof IncomingToken) {
			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN, Token ID " + ((IncomingToken)e).getTokenId());
			processToken((IncomingToken)e);
		}

		else if(e instanceof TokenServiceCompleted) {
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ ": Received SERVICE COMPLETE, Token ID " + ((TokenServiceCompleted)e).getTokenId());
        	completeTokenService((TokenServiceCompleted)e);
		}
		
		else if(e instanceof IncomingMessage) {			
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+ ": Received INCOMING MESSAGE");
			ProcessMessage((IncomingMessage)e);
		}

		else throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getSimpleName().toUpperCase() + " event type");
	}
	*/
	
	
	@Override
	protected void processToken(IncomingToken e) throws UnexpectedEvent{
		initialTime = engine.getInitialTime();
		dateTimeFormat = engine.getDateTimeFormat(); 
		IncomingMessage message;

		/*
		 * Record the token's arrival time at this task on first visit only;
		 * preserved for RESTART policy to restore queue position after mid-service failure.
		 */
		if (e.getTaskArrivalTime() == -1.0) e.setTaskArrivalTime(e.getTime());

		/* if at least on resource is available and an incoming message is available, 
		 * the token is processed. Otherwise it is enqueued.
		 */

		if (checkForAvailableResource() && !this.messageQueue.isEmpty()) {
			
			//a message is consumed
			message = (IncomingMessage)messageQueue.pollFirst(); //message consumed
			
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available message ");
			
			//find the next to be used according to a round robin policy
			Resource r = this.getAvailableResource();

			//DEBUG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available resource " + r.getName() );
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Starting processing of Token ID " + e.getTokenId());

			//a SERVICE_COMPLETE event is scheduled on this entity
			serviceTime = generator.get();
			TokenServiceCompleted serviceCompleteEvent = new TokenServiceCompleted(e.getTokenId(),
					e.getTime()+serviceTime, this, e.getStartTimestamp());
			serviceCompleteEvent.setResource(r);
			//mark the resource as busy - if overriden (e.g., performer)  also stores the token reference
			r.onServiceStarted(e, e.getTime(), serviceTime, serviceCompleteEvent, this);
            try {
                this.receiveEvent(serviceCompleteEvent);
            } catch (UnexpectedEvent ex) {
                throw new RuntimeException(ex);
            }

			
			//Writing the log entry
			log(e.getTokenId(), e.getTime(), serviceTime, message.getSourceEntity(), message.getMessagePayload());
	        
			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Scheduled SERVICE COMPLETE event for Token ID " + serviceCompleteEvent.getTokenId() + " at time "
				+ serviceCompleteEvent.getTime()
				+ " for " + serviceCompleteEvent.getHandlerEntity().getName());
		} else {
			//the token is enqueued
			this.tokenQueue.add(e);
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + " Token ID " + e.getTokenId() + " enqueued");
		}
	}
	
	private void ProcessMessage(IncomingMessage message)  throws UnexpectedEvent{
		/*
		 * If the token queue is not empty, the message is consumed
		 * and the first token in the queue is processed.
		 * Otherwise the message is enqueued.
		 *
		 */
		messageQueue.add(message);
		if(!tokenQueue.isEmpty() && checkForAvailableResource()) {
			IncomingToken dequeuedToken = (IncomingToken)tokenQueue.pollFirst();
			//update the time of the dequeued token to now before processing it
			dequeuedToken.setTime(engine.getSimulationTime());
			this.processToken(dequeuedToken);	
		} else {
			System.out.println(this.engine.getSimulationTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + " Token queue is empty or no available resource found. The message is enqueued");
		}
	}

	@Override
	public boolean hasPendingTokens() {
		/* A pending token is a token waiting in the queue that becomes eligible for processing
		 * when the associated resource is available.
		 * In a Receive Task, the availability of a message must also be verified.
		 */
		return super.hasPendingTokens() && !isMessageQueueEmpty();
	}
	
	protected void log(String tokenId, double tokenLogicalTime, double serviceTime, String remoteEntity, String messagePayload) {

		LogData ld = new LogData(); 
		
		//token ID corresponds to the case ID
		ld.setCaseId(tokenId);  
		
		/*
		 * As the service is a double, its value is divided into seconds and nanoseconds
		 * to appropriately increase the (long) physical time value to be logged
		 */
		long seconds, serviceSeconds;
		long nanos;
		seconds = (long) tokenLogicalTime;
        nanos = (long) ((tokenLogicalTime - seconds) * 1_000_000_000);
        
        //The start time is the initial time + the current token logical time
        ld.setStartTimestamp(initialTime.plusSeconds(seconds).plusNanos(nanos).format(dateTimeFormat));
        
        //the completion time is obtained adding the service time
        serviceSeconds = (long) serviceTime;
        seconds +=  serviceSeconds;
        nanos += (long) ((serviceTime - serviceSeconds) * 1_000_000_000);
        ld.setCompleteTimestamp(initialTime.plusSeconds(seconds).plusNanos(nanos).format(dateTimeFormat));
        
        /*if more than one resource is required, the resource list is
         * logged as a string in which the various value are separated by
         * a ':' character
         */
        String res = "";
        String roles = "";
        if (resources.size()>0) {
        	res = resources.get(0).getName();
        	roles = resources.get(0).getRole();
        	if (resources.size()>1) {
        		for (int i = 1; i < resources.size(); i++) {
	        		res += ":" + resources.get(i).getName();
	        		roles += ":" + resources.get(i).getRole();
	        	}		
        	}
        }
        ld.setActivity(this.getName());
        ld.setResources(res);
        ld.setRoles(roles);
        ld.setLocalEntity(this.getParticipant().getName());
        ld.setRemoteEntity(remoteEntity);
        ld.setCommType(CommunicationKind.receive);
        ld.setData(messagePayload);
		
        this.writeLog(ld);
	}

}
