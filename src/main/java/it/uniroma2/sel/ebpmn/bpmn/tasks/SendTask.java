package it.uniroma2.sel.ebpmn.bpmn.tasks;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Node;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.IncomingMessage;
import it.uniroma2.sel.ebpmn.events.IncomingToken;
import it.uniroma2.sel.ebpmn.events.TokenServiceCompleted;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * BPMN Send Task — performs a service activity and sends a message on completion.
 *
 * <p>A SendTask extends {@link Task} by adding a message flow: when service
 * completes for a token, an {@link it.uniroma2.sel.ebpmn.events.IncomingMessage}
 * is dispatched to the configured {@code messageTargetNode} with timestamp
 * {@code tokenArrivalTime + serviceTime}.  The target may be a local node or a
 * {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode} for distributed simulation.
 *
 * <p>Resource management and token queueing are inherited from {@link Task}.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see Task
 * @see it.uniroma2.sel.ebpmn.events.IncomingMessage
 */
public class SendTask extends Task{

	/** The message-flow target node (local or remote). */
	private Node messageTargetNode;
	/** Payload data carried by the outgoing message. */
	private String messageData;


	public SendTask(String name, Participant p, RandomVariableGenerator gen) {
		super(name, p, gen);
	}

	public void addMessageFlow(Node targetNode, String data) {
		this.messageTargetNode = targetNode;
		this.messageData = data;
	}

	public void addMessageFlow(FlowNode targetNode, String data) {
		this.messageTargetNode = targetNode;
		this.messageData = data;
	}

	@Override
	protected void processToken(IncomingToken t) throws UnexpectedEvent{
		initialTime = engine.getInitialTime();
		dateTimeFormat = engine.getDateTimeFormat();

		if (checkForAvailableResource()) {
			/* if at least on resource is available and the token queue is
			 * empty, the token is processed otherwise it is enqueued.
			 *
			 */
			//find the next to be used according to a round robin policy
			Resource r = this.getAvailableResource();

			//DEBUG
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available resource " + r.getName() );
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Starting processing of Token ID " + t.getTokenId());


			//mark the resource as busy
			r.setBusy();

			//a SERVICE_COMPLETE event is scheduled on this entity
			serviceTime = generator.get();
			TokenServiceCompleted serviceCompleteEvent = new TokenServiceCompleted(t.getTokenId(),
					t.getTime()+serviceTime, this, t.getStartTimestamp());
			serviceCompleteEvent.setResource(r);
			this.receiveEvent(serviceCompleteEvent);

			//Writing the log entry
			log(t.getTokenId(), t.getTime(), serviceTime);

			//LOG
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Scheduled SERVICE COMPLETE event for Token ID " + serviceCompleteEvent.getTokenId() + " at time "
					+ serviceCompleteEvent.getTime()
					+ " for " + serviceCompleteEvent.getHandlerEntity().getName());

			/* A message is sent with timestamp equal to current logical time + service time.
			 * To send a message to the target node a corresponding INCOMING MESSAGE EVENT
			 * is scheduled.
			 */
			IncomingMessage message = new IncomingMessage(t.getTime()+serviceTime,this.getName(), messageTargetNode, messageData);
			//engine.getEventsList().addEvent(message);
			try {
				messageTargetNode.receiveEvent(message);
			} catch (UnexpectedEvent ex) {
				throw new RuntimeException(ex);
			}

			//LOG
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+ ": Scheduled INCOMING MESSAGE event at time "
					+ message.getTime() + " for " + message.getTargetEntity().getName());

		} else {
			//the token is enqueued
			this.tokenQueue.add(t);
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Token ID " + t.getTokenId() + " enqueued");
		}
	}

	@Override
	protected void log(String tokenId, double tokenLogicalTime, double serviceTime) {
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
        ld.setRemoteEntity(this.messageTargetNode.getParticipant().getName());
        ld.setCommType(CommunicationKind.send);
        ld.setData(this.messageData);
		
        this.writeLog(ld);
	}
}
