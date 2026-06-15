package it.uniroma2.sel.ebpmn.hla;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;

import java.io.File;
import java.net.URL;

/**
 * Static factory that bootstraps the HLA infrastructure for a distributed simulation.
 *
 * <p>{@link #create(SimulationConfig)} performs the complete federation join sequence:
 * <ol>
 *   <li>Connects to the RTI and creates / joins the federation execution.</li>
 *   <li>Coordinates the {@code READY_TO_RUN} synchronisation barrier so all
 *       required federates are present before the simulation starts (based on
 *       Kuhl et al., "Creating Computer Simulation Systems").</li>
 *   <li>Enables time regulation and time constraining.</li>
 *   <li>Publishes and subscribes the eBPMN HLA interactions.</li>
 *   <li>Returns a fully initialised {@link HlaAdapter}.</li>
 * </ol>
 *
 * <p>The returned adapter is stored in the {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}
 * and used for all subsequent RTI operations.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see HlaAdapter
 * @see HlaAdapterImpl
 * @see FederateAmbassadorImpl
 */
public class HlaFactory {

    /** Label of the federation-wide startup barrier synchronisation point. */
    private static final String READY_TO_RUN = "READY_TO_RUN";

    /** Utility class — not instantiable. */
    private HlaFactory() {}

    /**
     * Bootstraps HLA and returns a ready-to-use {@link HlaAdapter}.
     *
     * @param config simulation configuration holding federation parameters
     * @return an initialised {@link HlaAdapter} connected to the RTI
     * @throws Exception if any RTI or federation operation fails
     */
    public static HlaAdapter create(SimulationConfig config) throws Exception {

        HLAfloat64TimeFactory timeFactory;
        String federateName = config.getFederateName();
        String federationName = config.getFederationName();

        System.out.println("[" + federateName + "] Initializing " + federationName);

        // -- Creation of RTIAmbassador, FederateAmbassador and EncoderFactory --
        RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
        RTIambassador rtiAmb = rtiFactory.getRtiAmbassador();
        EncoderFactory encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        FederateAmbassadorImpl fedAmb = new FederateAmbassadorImpl(federateName);

        //-- Initialization of Federation Execution --
        // RTI connection
        rtiAmb.connect(fedAmb, CallbackModel.HLA_IMMEDIATE, "crcAddress=" + config.getRtiHostName());
        System.out.println("[" + federateName + "] Connected to RTI" );

        // Federation Join
        URL[] fom = new URL[]{
                (new File("src/main/resources/ebpmnFOM.xml")).toURI().toURL(),
        };//FOM

        try {
            rtiAmb.createFederationExecution(federationName, fom);
            System.out.println("[" + federateName + "] created " + federationName + " federation execution" );
        } catch (FederationExecutionAlreadyExists ignored) {}//ignored




        rtiAmb.joinFederationExecution(federateName,federationName);
        System.out.println("[" + federateName + "] joined federation " + federationName);

        //time factory creation
        timeFactory = (HLAfloat64TimeFactory) rtiAmb.getTimeFactory();

        //creation of the pitch-specific HLA adapter
        HlaAdapterImpl adapter = new HlaAdapterImpl(rtiAmb, encoderFactory, fedAmb, timeFactory);
        fedAmb.setHlaAdapter(adapter);

        /*
         * The Controller Federate must wait for all required federates to join the federation execution
         * before announcing the READY TO RUN synchronization point.
         * The following procedure is adopted [based on Kuhl F. et al. "Creating Computer Simulation Systems":
         * a) The Controller Federate subscribes to the “Join Notification” interaction.
         * b) Non-controller federate publishes and sends the same interaction when they join the federation to
         * inform the controller which.
         * The number of federates must be specified in the Simulation Configuration json file.
         */
        if(config.isController()) {
            adapter.subscribeJoinNotificationInteraction();
            synchronized (fedAmb) {
                fedAmb.notifyJoinedFederate(); //ask fedAmb to increase joined federates counter
                System.out.println("[" + federateName + "] Waiting for the remaining " + (config.getNumbersOfFederates() - fedAmb.getJoinedFederates()) + " federates");
                while (fedAmb.getJoinedFederates() < config.getNumbersOfFederates()) fedAmb.wait();
            }
            System.out.println("[" + federateName + "] All " + fedAmb.getJoinedFederates() + " federates has joined");
            rtiAmb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
            System.out.println("[" + federateName + "] Sync Point " + READY_TO_RUN + " Registered");
        }
        else{
            // Each non-controller federate sends a "Join Notification" interaction (with no parameters)
            adapter.publishJoinNotificationInteraction();
            adapter.sendJoinNotificationInteraction();
        }

        synchronized (fedAmb) {
            while (!fedAmb.isReadyToRunAnnounced()) fedAmb.wait();
        }
        System.out.println("[" + federateName + "] Sync Point " + READY_TO_RUN + " Announced");

        // Setup time policy (Time Regulating + Constrained)
        rtiAmb.enableTimeRegulation(timeFactory.makeInterval(config.getLookahead()));
        synchronized (fedAmb) {
            while (!fedAmb.isTimeRegulating()) fedAmb.wait();  //wait for the RTI to invoke the timeRegulationEnabled callback
        }
        System.out.println("[" + federateName + "] is time regulating");

        rtiAmb.enableTimeConstrained();
        synchronized (fedAmb) {
            while (!fedAmb.isTimeConstrained()) fedAmb.wait();
        }
        System.out.println("[" + federateName + "] is time constrained");

        // Publish/Subscribe BPMN interactions
        adapter.publishAndSubscribeEBPMNInteractions();

        //Sync Point ReadToRun Achieve. Wait for other federates
        rtiAmb.synchronizationPointAchieved(READY_TO_RUN);
        synchronized (fedAmb) {
            while (!fedAmb.isReadyToRunAchieved()) fedAmb.wait();
        }
        System.out.println("[" + federateName + "] Sync Point " + READY_TO_RUN + " Achieved");
        System.out.println("[" + federateName + "] Synchronization of Federation " + federationName + " Completed");

        return adapter;
    }

}
