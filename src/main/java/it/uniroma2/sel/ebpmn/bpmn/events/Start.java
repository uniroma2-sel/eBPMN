package it.uniroma2.sel.ebpmn.bpmn.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.logger.CommunicationKind;
import it.uniroma2.sel.ebpmn.logger.LogData;
import it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator;

/**
 * BPMN Start Event node.
 *
 * <p>Acts as the token generator for a {@link it.uniroma2.sel.ebpmn.bpmn.Participant}'s
 * process. At simulation start the {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}
 * fires one {@link it.uniroma2.sel.ebpmn.events.StartProcess} event per configured token;
 * each such event causes {@code Start} to mint a fresh {@link it.uniroma2.sel.ebpmn.events.IncomingToken}
 * and inject it into the downstream flow.  Inter-arrival times between consecutive token
 * injections are sampled from a {@link it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator}.
 *
 * {@code it.uniroma2.sel.syssl.System} process.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.bpmn.events.End
 * @see it.uniroma2.sel.ebpmn.generators.RandomVariableGenerator
 * @see it.uniroma2.sel.ebpmn.engine.ExecutionEngine
 */
public class Start extends FlowNode{
	//The Start node acts as a Token generator.

	/** Number of tokens to be generated and injected into the process at start-up. */
	private int nTokens;
	/*
	 * A random variable generator is used to generate a sequence of
	 * interarrival times distributed according to a given probability distribution
	 */
	/** Stochastic inter-arrival time generator; each call to {@code get()} returns the
	 *  next sampled inter-arrival delay in simulation time units. */
	private RandomVariableGenerator generator;
	/** Monotonically increasing counter used to assign a unique string ID to each minted token. */
	private int tokenID=1;
	/** Flag controlling whether token-generation events are written to the process log. */
	private boolean logEnabled = false;

	/**
	 * Constructs a Start event with logging disabled.
	 *
	 * @param name    logical name of this node (used in log output and model identification)
	 * @param p       the {@link Participant} that owns this node
	 * @param gen     random variable generator for inter-arrival times
	 * @param nTokens number of tokens to generate when the simulation starts
	 */
	public Start(String name, Participant p, RandomVariableGenerator gen, int nTokens) {
		super(name, p);
		//this.interArrivalTime = interArrivalTime;
		this.generator = gen;
		this.nTokens = nTokens;
	}

	/**
	 * Constructs a Start event with an explicit logging flag.
	 *
	 * @param name      logical name of this node
	 * @param p         the {@link Participant} that owns this node
	 * @param gen       random variable generator for inter-arrival times
	 * @param nTokens   number of tokens to generate when the simulation starts
	 * @param enableLog {@code true} to write a log entry for each generated token
	 */
	public Start(String name, Participant p, RandomVariableGenerator gen, int nTokens, boolean enableLog) {
		super(name, p);
		this.generator = gen;
		this.nTokens = nTokens;
		this.logEnabled = enableLog;

	}

	/**
	 * Enables or disables process-log recording for token-generation events.
	 *
	 * @param enable {@code true} to enable logging, {@code false} to disable
	 */
	public void setEnableLog(boolean enable) {
		this.logEnabled=enable;
	}

	/**
	 * Returns the total number of tokens this node will generate at simulation start.
	 *
	 * @return the configured token count
	 */
	public int getnTokens() {
		return nTokens;
	}

	/**
	 * Samples and returns the next inter-arrival time from the configured generator.
	 *
	 * @return next inter-arrival delay in simulation time units
	 */
	public double getNextInterarrivalTime() {
		return generator.get();
	}

	/**
	 * Registers {@code n} as the single downstream flow node.
	 *
	 * @param n the {@link FlowNode} to attach on the outgoing sequence flow
	 */
	@Override
	public void addOutGoingEdge(FlowNode n) {
		this.nodes.add(n);

	}

	/**
	 * Returns the single outgoing flow node.
	 * A Start node has exactly 1 outgoing edge.
	 *
	 * @return the unique downstream {@link FlowNode}
	 */
	@Override
	//A Start node has exactly 1 outgoing edge
	public FlowNode getNextNode() {
		return this.nodes.get(0);
	}

	/**
	 * Not supported — a Start node does not accept incoming tokens.
	 *
	 * @param incomingToken the unexpected token event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent{
		this.unexpectedEvent(incomingToken);
	}

	/**
	 * Not supported — a Start node does not accept service-completion events.
	 *
	 * @param servedToken the unexpected service-completion event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(TokenServiceCompleted servedToken) throws UnexpectedEvent {
		this.unexpectedEvent(servedToken);
	}

	/**
	 * Not supported — a Start node does not accept incoming messages.
	 *
	 * @param message the unexpected message event
	 * @throws UnexpectedEvent always
	 */
	@Override
	public void handleEvent(IncomingMessage message) throws UnexpectedEvent {
		this.unexpectedEvent(message);
	}

	/**
	 * Handles a {@link StartProcess} event by minting a new token and forwarding it
	 * to the single downstream node.  If logging is enabled, a log entry is written
	 * recording the token generation.
	 *
	 * @param startEvent the start-process trigger carrying the current simulation time
	 * @throws UnexpectedEvent never thrown by this implementation; declared for interface
	 *                         compatibility
	 */
	@Override
	public void handleEvent(StartProcess startEvent) throws UnexpectedEvent {
		IncomingToken token = new IncomingToken(Integer.toString(tokenID),startEvent.getTime(),
				this.getNextNode(),startEvent.getTime());

		//LOG
		System.out.println(startEvent.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Scheduled INCOMING TOKEN, Token ID " + token.getTokenId() + " to " + token.getHandlerEntity().getName());

		this.getNextNode().receiveEvent(token);
		//engine.getEventsList().addEvent(token);
		tokenID++;

		/* the log is written if the feature is enabled otherwise the START node behavior is not tracked.
		* Might be useful depending on the capabilities provided by the log analysis tool
		*/
		if(logEnabled) log(token.getTokenId(), token.getTime());
	}

	/**
	 * Throws an {@link UnexpectedEvent} describing the event type that was incorrectly
	 * delivered to this node.
	 *
	 * @param e the event that could not be handled
	 * @throws UnexpectedEvent always, with a descriptive message
	 */
	@Override
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}


	/*
	@Override
	public void handleEvent(Event e) throws UnexpectedEvent {
		//Start only admits START_PROCESS events
		if(e instanceof StartProcess) {

			IncomingToken token = new IncomingToken(Integer.toString(tokenID),e.getTime(),
	            						this.getNextNode(),e.getTime());
			
			//LOG
			System.out.println(e.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
					+ ": Scheduled INCOMING TOKEN, Token ID " + token.getTokenId() + " to " + token.getTargetEntity().getName());
			engine.getEventsList().addEvent(token);			
			tokenID++;
			
			/* the log is written if the feature is enabled otherwise the
			   START node behavior is not tracked.
			   Might be useful depending on the capabilities provided by the log analysis tool
			  *
			if(logEnabled)
				log(token.getTokenId(), token.getTime());
		}
		else throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getSimpleName().toUpperCase() + " event type");

	}
	*/
	
	/**
	 * Writes a log entry for a token generation event.
	 *
	 * @param tokenId           unique string identifier of the generated token (also used as case ID)
	 * @param tokenLogicalTime  simulation logical time at which the token was generated
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
        ld.setRemoteEntity("");
        ld.setCommType(CommunicationKind.ND);
        ld.setData("");
	
        this.writeLog(ld);
	}



}
