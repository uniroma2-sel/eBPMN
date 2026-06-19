package it.uniroma2.sel.ebpmn.engine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.resources.Performer;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.events.Event;
import it.uniroma2.sel.ebpmn.events.EventsList;
import it.uniroma2.sel.ebpmn.events.StartProcess;
import it.uniroma2.sel.ebpmn.exceptions.PastEventException;
import it.uniroma2.sel.ebpmn.exceptions.ParticipantNotFoundException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException;
import it.uniroma2.sel.ebpmn.generators.RandomGenerator;
import it.uniroma2.sel.ebpmn.hla.HlaAdapter;
import it.uniroma2.sel.ebpmn.hla.HlaFactory;
import it.uniroma2.sel.ebpmn.logger.Logger;


/**
 * Singleton simulation engine — the central event-processing loop for eBPMN.
 *
 * <p>The ExecutionEngine owns the {@link EventsList} (future event list) and
 * drives the main simulation loop.  It supports both local and distributed
 * (HLA) execution modes, selected transparently via {@link SimulationConfig}.
 *
 * <p><b>Lifecycle:</b>
 * <ol>
 *   <li>Call {@link #initialize(SimulationConfig)} once to create the singleton
 *       and set up the time manager and (for distributed runs) the HLA
 *       infrastructure.</li>
 *   <li>Register the {@link Collaboration} model with {@link #setModel(Collaboration)}.</li>
 *   <li>Optionally set per-participant loggers with {@link #setLogger(String, Logger)}.</li>
 *   <li>Call {@link #run()} to start the simulation.</li>
 * </ol>
 *
 * <p><b>HLA transparency:</b>
 * From the model code perspective, local and distributed simulation are
 * identical: sending a message to a
 * {@link it.uniroma2.sel.ebpmn.bpmn.RemoteNode} automatically routes it via
 * the {@link it.uniroma2.sel.ebpmn.hla.HlaAdapter}.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see EventsList
 * @see SimulationConfig
 * @see TimeManager
 * @see it.uniroma2.sel.ebpmn.hla.HlaAdapter
 */
public class ExecutionEngine {

	/** Current logical simulation time. */
	private double simulationTime = 0.0;
	/** The future event list (FEL). */
	private EventsList eventsList;
	/** The BPMN collaboration model being simulated. */
	private Collaboration model;
	/** Singleton instance (volatile for safe publication). */
	private static volatile ExecutionEngine instance;
	/** Delegates time-advance semantics (local or HLA). */
	private TimeManager timeManager;
	/** RTI adapter; {@code null} for local simulation. */
	private HlaAdapter adapter = null;

	/** Per-participant event log writers. */
	private HashMap<String, Logger> logs;
	/** All Performers registered during model construction (for run report). */
	private final List<Performer> registeredPerformers = new ArrayList<>();
	/** Physical start time anchored to wall-clock for log timestamps. */
	private LocalDateTime initialTime;
	/** Format pattern for log timestamps. */
	private DateTimeFormatter dateTimeFormat;
	/** Loaded simulation configuration. */
	private SimulationConfig config;
	
	private ExecutionEngine(SimulationConfig config){
		eventsList = new EventsList();	
		logs = new HashMap<String, Logger>();
		// default physical time format
        dateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
        initialTime = LocalDateTime.now(); // default initial time
		this.config = config;
		long seed = config.getSeed();
		//Initialization of Random Genetors
		if (seed==-1) {
			RandomGenerator.init();
		} else {
			RandomGenerator.init(seed);
		}

		//Distributed/Local specific setup
		if (config.getSimulationType() == SimulationConfig.SimulationType.DISTRIBUTED) {
			//Initialization of the HLA distributed infrastructure
            try {
                this.adapter = HlaFactory.create(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
			//the distributed time manager interacts with the RTI (via the adapter) to handle the time advance requests
            this.timeManager = new DistributedTimeManager(adapter, config.getPrecision());
			System.out.println("[ENGINE] Distributed Time Manger initialized.");
		} else {
			this.timeManager = new LocalTimeManager(config.getPrecision());
			System.out.println("[ENGINE] Local Time Manger initialized.");
		}
		ExecutionEngine.instance = this;
	}

	/**
	 * Creates and returns the singleton engine.  Must be called exactly once
	 * before any other engine operation.
	 *
	 * @param config simulation configuration
	 * @return the newly created engine instance
	 * @throws IllegalStateException if the engine has already been initialised
	 */
	public static synchronized ExecutionEngine initialize(SimulationConfig config) {
		if (instance != null) {
			throw new IllegalStateException("[ENGINE] ExecutionEngine already initialized.");
		}
		instance = new ExecutionEngine(config);
		return instance;
	}

	/**
	 * Returns the HLA adapter, or {@code null} for local simulation.
	 *
	 * @return the {@link it.uniroma2.sel.ebpmn.hla.HlaAdapter}, possibly {@code null}
	 */
	public HlaAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Returns the HLA federation name from the configuration.
	 *
	 * @return federation name string
	 */
	public String getFederationName(){
		return config.getFederationName();
	}

	/**
	 * Returns the singleton engine instance.
	 *
	 * @return the engine
	 * @throws IllegalStateException if {@link #initialize(SimulationConfig)} has not been called
	 */
	public static ExecutionEngine getInstance() {
		if (instance == null)
			throw new IllegalStateException("Engine not initialized yet");
		return instance;
	}

	/**
	 * Returns the HLA lookahead value from the configuration.
	 *
	 * @return lookahead in simulation time units
	 */
	public double getLookahead(){return config.getLookahead();}

	/**
	 * Registers the BPMN collaboration model to be simulated.
	 *
	 * @param model the collaboration model
	 */
	public void setModel(Collaboration model){
		this.model = model;
	}

	/**
	 * Registers a Performer so its failure/repair data is included in the run report.
	 * Called automatically by the Performer constructor.
	 */
	public void registerPerformer(Performer p) {
		registeredPerformers.add(p);
	}

	/**
	 * Registers an event log writer for the named participant.
	 *
	 * @param participantName participant name (used as map key)
	 * @param log             the logger to register
	 */
	public void setLogger(String participantName, Logger log) {
		logs.put(participantName, log);
	}

	/**
	 * Returns the event log writer for the named participant.
	 *
	 * @param participantName participant name
	 * @return the associated {@link Logger}, or {@code null} if not registered
	 */
	public Logger getLogger(String participantName) {
		return logs.get(participantName);
	}

	/**
	 * Returns the physical wall-clock start time used as the epoch for log timestamps.
	 *
	 * @return initial physical time
	 */
	public LocalDateTime getInitialTime() {
		return initialTime;
	}

	/**
	 * Returns the current simulation logical time.
	 *
	 * @return simulation time in the configured {@link TimeUnit}
	 */
	public double getSimulationTime() {
		return simulationTime;
	}

	/**
	 * Overrides the physical epoch for log timestamps.
	 *
	 * @param initialTimeAsString date-time string matching the current formatter pattern
	 */
	public void setInitialTime(String initialTimeAsString) {
		this.initialTime = LocalDateTime.parse(initialTimeAsString, dateTimeFormat);
	}

	/**
	 * Returns the log timestamp formatter.
	 *
	 * @return the active {@link DateTimeFormatter}
	 */
	public DateTimeFormatter getDateTimeFormat() {
		return dateTimeFormat;
	}

	/**
	 * Overrides the log timestamp format pattern.
	 *
	 * @param formatString a {@link DateTimeFormatter} pattern string
	 */
	public void setFormatter(String formatString) {
		this.dateTimeFormat = DateTimeFormatter.ofPattern(formatString);
	}

	/**
	 * Inserts an event into the future event list.
	 *
	 * @param e the event to schedule
	 */
	public void scheduleLocalEvent(Event e){
		eventsList.addEvent(e);
	}

	/**
	 * Looks up a {@link FlowNode} by participant and node name.
	 *
	 * @param participantName name of the owning participant
	 * @param nodeName        name of the node within that participant
	 * @return the matching flow node
	 * @throws it.uniroma2.sel.ebpmn.exceptions.ParticipantNotFoundException if the participant is not found
	 * @throws it.uniroma2.sel.ebpmn.exceptions.NodeNotFoundException        if the node is not found
	 */
	public FlowNode getNode(String participantName, String nodeName) {
		Participant participant = model.getParticipantByName(participantName);

		if (participant == null) {
			throw new ParticipantNotFoundException(participantName);
		}

		FlowNode node = participant.getNodeByName(nodeName);

		if (node == null) {
			throw new NodeNotFoundException(nodeName);
		}

		return node;
	}

	/**
	 * Runs the simulation main loop until the event list is empty or the
	 * configured simulation length is exceeded.
	 *
	 * <p>For each iteration: advances logical time to the next event timestamp
	 * (via {@link TimeManager#advanceTo(double)}), dequeues and processes the
	 * imminent event.  In distributed mode, time advance may yield a grant
	 * earlier than requested due to conservative synchronisation.
	 *
	 * <p>On completion, all loggers are closed and (for distributed runs) the
	 * federate resigns from the federation.  Summary statistics are printed to
	 * standard output.
	 *
	 * @throws it.uniroma2.sel.ebpmn.exceptions.PastEventException if an event with a
	 *         past timestamp is encountered (indicates a modelling error)
	 * @throws it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent    if a node receives
	 *         an event type it cannot handle
	 */
	public void run() throws PastEventException, UnexpectedEvent {
		System.out.println("[ENGINE] " + config.getSimulationType() + " SIMULATION START ");
		init();
		double nextEventTimestamp;
		Event e; //next event to be processed
		//Simulation main loop
		while(!eventsList.isEmpty() && simulationTime < config.getSimulationLength()) {
			nextEventTimestamp = eventsList.getNextEvent().getTime();
			if(nextEventTimestamp<simulationTime)
				throw new PastEventException("Past Event: "  + eventsList.getNextEvent().getClass() + " timestamp: " + nextEventTimestamp);
			//time advance
			System.out.print("[ENGINE] - Current time: " + simulationTime);
			if(config.getSimulationType().equals(SimulationConfig.SimulationType.DISTRIBUTED))
				System.out.print(" RTI Logical Time: " + adapter.queryRTILogicalTime());
			System.out.println(" advancing to time: " + nextEventTimestamp);
			timeManager.advanceTo(nextEventTimestamp);


			/*
			 * In a distributed simulation, events with timestamps lower than the quested nextEventTimestamp
			 * might be received (and added at the top of the event queue) while the federate waits for a time advance grant.
			 * As a result:
			 *    i)  the granted time advance may be lowe than requested one;
			 *    ii) the event to be processed (the first element in the queue) must be identified after
			 *        receiving the time advance grant.
			 */
			e = eventsList.getNextEvent();
			simulationTime = e.getTime();
			System.out.println("[ENGINE] - Advanced to time: " + simulationTime);
			simulationTime = e.getTime();
			eventsList.removeEvent(e);
			e.processByHandler();	//An event knows its handler entity: FlowNode handlerEntity attribute of Event Class
		}


		System.out.println("\n" +"[ENGINE] SIMULATION END");
		writeRunReport();
		logs.forEach((key,log)->log.closeLog());

		//in a distributed simulation the federate must resign and possibly destroy the federation execution
		if(config.getSimulationType()== SimulationConfig.SimulationType.DISTRIBUTED)
			adapter.resign();

		System.out.println("\n\n" +"****************************************\n" + "RESULTS");
		for (Participant participant : model.getParticipants()) {
            System.out.println("Process " + participant.getName());
            End end= (End) participant.getEndNode();
            System.out.println("Processed tokens : " + end.getProcessedToken());
			int digits= config.getPrecision();
			System.out.printf("Mean process completion time: %." + digits + "f %s \n",
					end.getAvgServiceTime(),
					config.getTimeUnit().name().toLowerCase());
		}

	}

	/**
	 * Writes a structured run report to run_report.txt.
	 * Reports token completion times and service times per participant,
	 * and failure/repair times per Performer.
	 * Called at the end of run() before closing loggers.
	 */

	private void writeRunReport() {

		new File(config.getOutputFolder()).mkdirs();

		// --- tokens CSV ---
		try (PrintWriter pw = new PrintWriter(new FileWriter(
				config.getOutputFolder() + "run_report_tokens.csv"))) {

			pw.println("participant;token_id;completion_time;service_time");

			for (Participant participant : model.getParticipants()) {
				End end = (End) participant.getEndNode();
				for (double[] record : end.getTokenRecords()) {
					pw.printf("%s;%.0f;%.3f;%.3f%n",
							participant.getName(), record[0], record[1], record[2]);
				}
			}

		} catch (IOException e) {
			System.err.println("[ENGINE] Warning: could not write run_report_tokens.csv — "
					+ e.getMessage());
		}

		// --- failures CSV ---
		try (PrintWriter pw = new PrintWriter(new FileWriter(
				config.getOutputFolder() + "run_report_failures.csv"))) {

			pw.println("performer;failure_time;repair_time;downtime");

			for (Performer p : registeredPerformers) {
				for (double[] r : p.getFailureRepairRecords()) {
					if (Double.isNaN(r[1])) {
						pw.printf("%s;%.3f;;%n", p.getName(), r[0]);
					} else {
						pw.printf("%s;%.3f;%.3f;%.3f%n",
								p.getName(), r[0], r[1], r[1] - r[0]);
					}
				}
			}

		} catch (IOException e) {
			System.err.println("[ENGINE] Warning: could not write run_report_failures.csv — "
					+ e.getMessage());
		}
	}

	private void init() {
		/*
		 * create an initial set of INCOMING_TOKEN events according to
		 * the interarrival time and the number of token specified for each participant
		 * */
		Start s;

		System.out.println("\n*** Initialization Process...");

		for (Participant participant : model.getParticipants()) {
            System.out.println("*** Found Participant Name: " + participant.getName());
            s= (Start) participant.getStartNode();

            if(participant.isInitiating()) {
            	System.out.println("*** Found Start Node: " + s.getName() + " Tokens: "+ s.getnTokens() + " to: " + s.getNextNode().getName());

                double timestamp=0;
                for(int i=1; i<=s.getnTokens();i++) {
                	eventsList.addEvent(new StartProcess(timestamp,s));
					timestamp += s.getNextInterarrivalTime();
                }
            }
            
        }
		System.out.println("*** Initialization Completed \n");
	}

}
