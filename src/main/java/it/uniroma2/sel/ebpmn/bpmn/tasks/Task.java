package it.uniroma2.sel.ebpmn.bpmn.tasks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TreeSet;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;
import it.uniroma2.sel.ebpmn.resources.Resource;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;

/**
 * BPMN Service Task node.
 *
 * <p>Represents a unit of work that requires a {@link Resource} to execute.
 * When an {@link it.uniroma2.sel.ebpmn.events.IncomingToken} arrives, the task
 * claims an available resource (round-robin among all registered resources),
 * samples a service duration from the configured {@link RandomVariableGenerator},
 * and schedules a {@link it.uniroma2.sel.ebpmn.events.TokenServiceCompleted} event.
 * The token is held until that event fires, at which point the resource is
 * released and the token is forwarded to the next {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode}.
 *
 * <p>If no resource is available at arrival time the token is placed in an
 * internal waiting queue ({@code tokenQueue}) and processed as soon as a
 * resource becomes free. If a resource fails mid-service the
 * {@code TokenServiceCompleted} event is cancelled; resumption is governed by
 * the resource's failure/repair policy.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 * @see Resource
 * @see it.uniroma2.sel.ebpmn.events.TokenServiceCompleted
 * @see SendTask
 * @see ReceiveTask
 */
public class Task extends FlowNode{

	/** Stochastic service-time generator sampled once per token arrival. */
	protected RandomVariableGenerator generator;

	/** Pool of resources that can execute this task; claimed round-robin. */
	protected ArrayList<Resource> resources;

	/** Index of the last resource used; advances round-robin on each claim. */
	protected int lastResourceIndex;

	/** Wall-clock start time of the simulation run; used for log timestamp computation. */
	protected LocalDateTime initialTime;

	/** Formatter applied to physical timestamps written to the event log. */
	protected DateTimeFormatter dateTimeFormat;

	/** Service duration sampled from {@link #generator} for the current token. */
	protected double serviceTime;

	/** FIFO queue of tokens waiting for an available resource. */
	protected TreeSet<Event> tokenQueue;

	/**
	 * Constructs a Task with the given name, owning participant, and service-time generator.
	 *
	 * @param name the unique node name within the participant's process
	 * @param p    the {@link Participant} that owns this node
	 * @param gen  the {@link RandomVariableGenerator} used to sample service durations
	 */
	public Task(String name, Participant p, RandomVariableGenerator gen) {
		super(name, p);
		this.generator = gen;
		resources = new ArrayList<>();
		lastResourceIndex=-1;
		tokenQueue = new TreeSet<>();
	}

	/**
	 * Handles an incoming control-flow token, delegating to {@link #processToken}.
	 *
	 * @param incomingToken the token event arriving at this task
	 * @throws UnexpectedEvent never thrown by this overload; declared for interface compliance
	 */
	@Override
	public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent{
		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN, Token ID " + incomingToken.getTokenId());
		processToken(incomingToken);
	}

	/**
	 * Handles a service-completion notification, delegating to {@link #completeTokenService}.
	 *
	 * @param servedToken the event signalling that service for a token has finished
	 * @throws UnexpectedEvent never thrown by this overload; declared for interface compliance
	 */
	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		System.out.println(servedToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received SERVICE COMPLETE, Token ID " + servedToken.getTokenId());
		completeTokenService(servedToken);
	}

	/**
	 * Rejects message-flow events; a plain Task does not participate in message flows.
	 *
	 * @param incomingMessage the unexpected message event
	 * @throws UnexpectedEvent always thrown
	 */
	@Override
	public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
		this.unexpectedEvent(incomingMessage);
	}

	/**
	 * Rejects process-start events; a Task is never the entry point of a process.
	 *
	 * @param startEvent the unexpected start event
	 * @throws UnexpectedEvent always thrown
	 */
	@Override
	public void handleEvent(StartProcess startEvent) throws UnexpectedEvent {
		this.unexpectedEvent(startEvent);
	}

	/**
	 * Raises an {@link UnexpectedEvent} for any event type not handled by this node.
	 *
	 * @param e the unrecognised event
	 * @throws UnexpectedEvent always thrown with a diagnostic message
	 */
	@Override
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}

	/**
	 * Associates a {@link Resource} with this task so it can serve tokens.
	 *
	 * <p>The resource is appended to the internal pool and back-registered on the
	 * resource via {@code r.addTask(this)}, establishing the bidirectional link
	 * needed for cross-task queue inspection after repair events.
	 *
	 * @param r the resource to register
	 */
	public void addResource(Resource r) {
		this.resources.add(r);
		r.addTask(this);
	}

	/**
	 * Returns the list of resources registered with this task.
	 *
	 * @return the mutable resource pool; never {@code null}
	 */
	public ArrayList<Resource> getResources() {
		return this.resources;
	}





	/**
	 * Core token-processing logic.
	 *
	 * <p>A task is associated to a pool of resources which are in charge of
	 * executing or overseeing the relevant activity. Resources might be responsible
	 * for more than one task.
	 *
	 * <p>If an available resource exists and the token queue is empty, service
	 * begins immediately: a service duration is sampled from {@link #generator},
	 * a {@link it.uniroma2.sel.ebpmn.events.TokenServiceCompleted} event is scheduled,
	 * and a log entry is written. Otherwise the token is placed in {@link #tokenQueue}.
	 *
	 * @param t the incoming control-flow token to process
	 * @throws UnexpectedEvent propagated from nested scheduling calls
	 */
	protected void processToken(IncomingToken t) throws UnexpectedEvent{
		initialTime = engine.getInitialTime();
		dateTimeFormat = engine.getDateTimeFormat();


		/*
		 * Record the token's arrival time at this task on first visit only;
		 * preserved for RESTART policy to restore queue position after mid-service failure.
		 */
		if (t.getTaskArrivalTime() == -1.0) t.setTaskArrivalTime(t.getTime());

		if (checkForAvailableResource()) {
			/* if at least one resource is available and the token queue is
			 * empty, the token is processed otherwise it is enqueued.
			 */
			//find the next to be used according to a round robin policy
			Resource r = this.getAvailableResource();

			//LOG
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found available resource " + r.getName() );
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Starting processing of Token ID " + t.getTokenId());

			//sample service time
			serviceTime = generator.get();

			//build the completion event BEFORE notifying the resource (Performer needs the reference)
			TokenServiceCompleted serviceCompleteEvent = new TokenServiceCompleted(t.getTokenId(),
					t.getTime() + serviceTime, this, t.getStartTimestamp());
			serviceCompleteEvent.setResource(r);

			/*
			 * Notify the resource that service is starting.
			 * Default (Resource): just calls setBusy().
			 * Performer override: stores token/completion references and may
			 * schedule the first failure event when used as a direct resource.
			 */
			r.onServiceStarted(t, t.getTime(), serviceTime, serviceCompleteEvent, this);

			//schedule the completion event into the engine queue
			this.receiveEvent(serviceCompleteEvent);

			//PB***Writing the log entry
			//log(t.getTokenId(), t.getTime(), serviceTime);

			//LOG
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Scheduled SERVICE COMPLETE event for Token ID " + serviceCompleteEvent.getTokenId() + " at time "
				+ serviceCompleteEvent.getTime()
				+ " for " + serviceCompleteEvent.getHandlerEntity().getName());
		} else {
			//the token is enqueued
			this.tokenQueue.add(t);
			System.out.println(t.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Token ID " + t.getTokenId() + " enqueued");
		}
	}
	
	protected void completeTokenService(TokenServiceCompleted token) throws UnexpectedEvent {

		/*
		 * A Performer may cancel the TokenServiceCompleted event when it fails
		 * mid-service.  The Performer is responsible for rescheduling a new
		 * completion event after repair (DELAY policy).  Just skip the stale event.
		 */
		if (token.isCancelled()) {
			System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - "
					+ this.getName() + ": Skipping cancelled ServiceCompleted for Token ID "
					+ token.getTokenId() + " (resource failed mid-service)");
			return;
		}

		//LOG
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Token ID " + token.getTokenId() + " Service completed");
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Resource " + token.getResource().getName() + " is now free");

		//the involved resource is marked as available
		Resource r = token.getResource();
		//PB***Writing the log entry
		log(token.getTokenId(), r.getServiceStartTime(), token.getTime() - r.getServiceStartTime());
		r.free();
		r.onServiceCompleted(token.getTime());

		/*
		 * To route the token towards the next node, a new INCOMING_TOKEN event is created and dispatched to the target entity
		 * */
        FlowNode targetEntity = null;
        try {
            targetEntity = this.getNextNode();
        } catch (NodeNotFoundException e) {
            e.printStackTrace();
        }
        IncomingToken incomingTokenEvent = new IncomingToken(token.getTokenId(), token.getTime(),
				targetEntity, token.getStartTimestamp());
		targetEntity.receiveEvent(incomingTokenEvent);
        //l.addEvent(incomingTokenEvent);


		//LOG
		System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
			+ ": Scheduled INCOMING TOKEN event at time "
			+ incomingTokenEvent.getTime() + " "
			+ "for " + incomingTokenEvent.getHandlerEntity().getName());


		//check for enqueued tokens waiting for a free resource
		if(hasPendingTokens() && checkForAvailableResource()) {
			IncomingToken dequeuedToken = (IncomingToken)tokenQueue.pollFirst();
			//update the time of the dequeued token to now before processing it
			
			System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": Found Token ID " + dequeuedToken.getTokenId() + " in queue with availabe resources" );

			dequeuedToken.setTime(engine.getSimulationTime());
			processToken(dequeuedToken);
		}
		else {
			/*
			 * If the resource is also assigned to other tasks, the same check must
			 * be performed for all. The first token waiting in the queue of a task
			 * is processed.
			 * */
			
			System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName() + ": No token in queue or available resources found. Inspecting other nodes served by resource " + r.getName() );
			
			ArrayList<Task> tasks = r.getTasks();
			//tasks.remove(this);
			for(Task t : tasks) {
				if(t.hasPendingTokens() && t.checkForAvailableResource()){
					IncomingToken iToken = (IncomingToken)t.tokenQueue.pollFirst();
					iToken.setTime(token.getTime());

					System.out.println(token.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
							+ ": Resource " + r.getName() + " find pending Token ID " + iToken.getTokenId()
							+ " still waiting in queue at " + t.getName());

					t.processToken(iToken);
					break;
				}
			}

		}

	}
	
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
        ld.setRemoteEntity("");
        ld.setCommType(CommunicationKind.ND);
        ld.setData("");
	
        this.writeLog(ld);
	}

	protected boolean checkForAvailableResource() {
		boolean available=false;
		for(Resource r : this.resources) {
			available = available || r.isAvailable();
			if (available)
				break;

		}
		return available;
	}

	protected Resource getAvailableResource() {
		//an available resource is identified according to a round robin discipline
		do
			lastResourceIndex = (lastResourceIndex + 1)%this.resources.size();
		while(!resources.get(lastResourceIndex).isAvailable());
		return resources.get(lastResourceIndex);
	}

	/* A pending token is a token waiting in the queue that becomes eligible for processing
	 * when the associated resource is available.*/
	protected boolean hasPendingTokens() {
		return !tokenQueue.isEmpty();
	}

	// -----------------------------------------------------------------------
	// Resource-failure / repair integration
	// -----------------------------------------------------------------------

	/**
	 * Called by {@code Resource#notifyFailure()} when a resource that serves
	 * this Task fails. Delegates policy logic to
	 * {@link Resource#applyTokenOnFailure(double)} and re-enqueues the token
	 * for the {@link TokenOnFailure#RESTART} policy.
	 *
	 * @param r    the resource that just failed
	 * @param time current simulation time (failure timestamp)
	 */
	public void onResourceFailed(Resource r, double time) {
		System.out.println(time + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": resource " + r.getName() + " failed — applying TokenOnFailure policy");

		// Apply policy first: cancels pendingCompletion, updates remainingServiceTime.
		// For RESTART, tokenInService is intentionally left non-null after this call.
		r.applyTokenOnFailure(time);

		// After applyTokenOnFailure(), retrieve and re-enqueue the token if RESTART.
		if (r.getTokenOnFailure() == TokenOnFailure.RESTART) {
			IncomingToken t = r.getAndClearTokenInService();
			if (t != null) {
				/*
				 * RESTART: restore the token's original arrival time so it regains
				 * its correct position in the queue ahead of later arrivals.
				 */

				t.setTime(t.getTaskArrivalTime());
				tokenQueue.add(t);
				System.out.println(time + ") " + this.getParticipant().getName() + " - " + this.getName()
						+ ": RESTART — Token ID " + t.getTokenId() + " re-queued with time: " + t.getTime());
			}
		}
	}

	/**
	 * Called by {@code Resource#notifyRepair()} (through the parent chain) when
	 * a resource that serves this Task becomes available again after a failure.
	 *
	 * Checks the waiting queue and processes the next token if possible.
	 *
	 * @param r    the resource that just repaired
	 * @param time current simulation time (repair timestamp)
	 */
	public void onResourceRepaired(Resource r, double time) {
		System.out.println(time + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": resource " + r.getName() + " repaired — checking token queue");
		if (hasPendingTokens() && checkForAvailableResource()) {
			IncomingToken dequeuedToken = (IncomingToken) tokenQueue.pollFirst();
			dequeuedToken.setTime(time);
			try {
				processToken(dequeuedToken);
			} catch (UnexpectedEvent e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Re-inserts a token at the HEAD of the waiting queue.
	 * Used by Performer on RESTART policy: the interrupted token is retried
	 * from scratch when the resource repairs.
	 */
	public void requeueToken(IncomingToken t) {
		// TreeSet has no addFirst; wrap in a new TreeSet won't preserve the head.
		// We simulate "head insertion" by temporarily giving the token a lower time.
		// A simpler approach: convert to LinkedList temporarily.
		// Because TreeSet sorts by time (compareTo), we just re-add it — the token's
		// time is the original arrival time, so it will be served before later arrivals.
		tokenQueue.add(t);
	}

	/**
	 * Discards all tokens currently in the waiting queue.
	 * Used by Performer on QueueOnFailure.FLUSH policy.
	 */
	public void flushQueue() {
		tokenQueue.clear();
	}

}
