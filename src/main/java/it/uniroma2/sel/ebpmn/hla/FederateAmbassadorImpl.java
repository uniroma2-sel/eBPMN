package it.uniroma2.sel.ebpmn.hla;

import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAinteger64Time;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.IncomingMessage;
import it.uniroma2.sel.ebpmn.events.IncomingToken;
import it.uniroma2.sel.ebpmn.exceptions.PastEventException;

import java.util.HashSet;
import java.util.Set;


/**
 * RTI callback handler for an eBPMN federate.
 *
 * <p>Extends {@code NullFederateAmbassador} (IEEE 1516e) and overrides the
 * callbacks relevant to eBPMN distributed simulation:
 *
 * <ul>
 *   <li><b>Time management</b>: {@link #timeAdvanceGrant}, {@link #timeConstrainedEnabled},
 *       {@link #timeRegulationEnabled} — update boolean flags that the
 *       {@link HlaAdapterImpl} polls (via {@code synchronized} wait/notify)
 *       to implement blocking time-advance semantics.</li>
 *   <li><b>Synchronisation points</b>: {@link #announceSynchronizationPoint},
 *       {@link #federationSynchronized} — handle the {@code READY_TO_RUN}
 *       barrier used for federate startup coordination.</li>
 *   <li><b>Interaction reception</b>: three overloads of {@code receiveInteraction}
 *       — decode HLA parameters and schedule local
 *       {@link it.uniroma2.sel.ebpmn.events.IncomingToken} or
 *       {@link it.uniroma2.sel.ebpmn.events.IncomingMessage} events on the engine.</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see HlaAdapterImpl
 * @see it.uniroma2.sel.ebpmn.hla.HlaAdapter
 */
public class FederateAmbassadorImpl extends NullFederateAmbassador {
    /** Name of this federate (used for routing and logging). */
    private String federateName;
    /** Back-reference to the adapter for handle lookups. */
    private HlaAdapterImpl adapter;
    /** Reference to the engine for scheduling decoded events. */
    private ExecutionEngine engine;
    /** Label of the federation-wide barrier synchronisation point. */
    private static final String READY_TO_RUN = "READY_TO_RUN";
    /** {@code true} after the RTI grants time-regulation. */
    private boolean isTimeRegulating  = false;
    /** {@code true} after the RTI grants time-constraining. */
    private boolean isTimeConstrained = false;
    /** {@code true} after the READY_TO_RUN sync point is announced. */
    private boolean isReadyToRunAnnounced = false;
    /** {@code true} after the federation synchronises on READY_TO_RUN. */
    private boolean isReadyToRunAchieved = false;
    /** {@code true} while a time-advance request is outstanding. */
    private boolean isAdvancing = false;
    /** Count of federates that have joined the federation execution so far. */
    private int joinedFederates = 0;

    /** Tracks discovered object instances (reserved for future use). */
    private final Set<ObjectInstanceHandle> discoveredHandles = new HashSet<>();

    /**
     * Creates a FederateAmbassadorImpl for the federate with the given name.
     *
     * @param federateName name of this federate (used for routing and logging)
     */
    public FederateAmbassadorImpl(String federateName){
        super();
        this.federateName = federateName;
    }

    /** @return the name of this federate */
    public String getFederateName(){ return federateName;}

    /**
     * Injects the HLA adapter back-reference (called after adapter creation).
     *
     * @param hlaAdapter the adapter that owns this ambassador
     */
    public void setHlaAdapter(HlaAdapterImpl hlaAdapter){
        adapter=hlaAdapter;
    }

    /** Increments the joined-federate counter (called by the controller). */
    public void notifyJoinedFederate(){
        joinedFederates++;
    }

    /** @return number of federates that have joined so far */
    public int getJoinedFederates(){
        return joinedFederates;
    }

    /** @return {@code true} if the RTI has granted time regulation */
    public boolean isTimeRegulating() {
        return isTimeRegulating;
    }

    /** @return {@code true} if the RTI has granted time constraining */
    public boolean isTimeConstrained() {
        return isTimeConstrained;
    }

    /** Marks this federate as currently advancing (called internally by the adapter). */
    void setAdvancingState(){
        isAdvancing = true;
    }

    /** @return {@code true} while a TAR is outstanding */
    public boolean isAdvancing() {
        return isAdvancing;
    }

    /** @return {@code true} after the READY_TO_RUN sync point has been announced */
    public boolean isReadyToRunAnnounced() {
        return isReadyToRunAnnounced;
    }

    /** @return {@code true} after the federation has synchronised on READY_TO_RUN */
    public boolean isReadyToRunAchieved() {
        return isReadyToRunAchieved;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime theTime) throws FederateInternalError {
        synchronized (this) {
            isAdvancing = false;
            this.notifyAll();
        }
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) throws FederateInternalError {
        synchronized (this) {
            isTimeConstrained = true;
            this.notify();
        }
    }

    @Override
    public void timeRegulationEnabled(LogicalTime time) throws FederateInternalError {
        synchronized (this) {
            isTimeRegulating = true;
            this.notify();
        }
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label,
                                                       SynchronizationPointFailureReason reason) throws FederateInternalError {
        System.out.println("[FederateAmbassador] Sync Point Registration Failed:  " + label + reason);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) {
        if (label.equals(READY_TO_RUN)) {
            synchronized (this) {
                isReadyToRunAnnounced = true;
                this.notifyAll();
            }
        }
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        if (label.equals(READY_TO_RUN)) {
            synchronized (this) {
                isReadyToRunAchieved = true;
                this.notifyAll();
            }
        }
    }


    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, MessageRetractionHandle retractionHandle, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        //event timestamp
        double timestamp = ((HLAfloat64Time)theTime).getValue();
        engine = ExecutionEngine.getInstance();

        /*
        * To prevent interactions generated by flow nodes that do not introduce any delay
        * (i.e., intermediate events and gateways) from causing an Invalid Logical Time exception,
        * an additional virtual delay is introduced on the sender side.
        * The local event is then created using the actual timestamp of the received event.
        * */

        try {
            //INCOMING TOKEN
            if (interactionClass.equals(adapter.getIncomingTokenHandle() )) {
                //decode tokenID
                HLAunicodeString tokenIdE = adapter.getEncoderFactory().createHLAunicodeString();
                tokenIdE.decode(theParameters.get(adapter.getTokenIDHandle()));
                String tokenId = tokenIdE.getValue();

                //decode target entity
                HLAunicodeString targetEntityE = adapter.getEncoderFactory().createHLAunicodeString();
                targetEntityE.decode(theParameters.get(adapter.getTokenTargetEntityHandle()));
                String targetEntity = targetEntityE.getValue();

                //decode target node
                HLAunicodeString targetNodeE = adapter.getEncoderFactory().createHLAunicodeString();
                targetNodeE.decode(theParameters.get(adapter.getTokenTargetNodeHandle()));
                String targetNode = targetNodeE.getValue();

                //decode start time
                HLAinteger64BE startTimeE = adapter.getEncoderFactory().createHLAinteger64BE();
                startTimeE.decode(theParameters.get(adapter.getTokenStartTimeHandle()));
                long startTime = startTimeE.getValue();

                //LOG
                System.out.print("[FederateAmbassador - " +  this.getFederateName() +"] current time: " + engine.getSimulationTime()
                        + " - received IncomingToken interaction with timestamp: " + timestamp);
                System.out.println(String.format("\nMessage - ID: %s TargetEntity: %s::%s starTime: %d",
                        tokenId, targetNode, targetEntity, startTime));

                if(targetEntity.equals(this.getFederateName())){
                    //A LOCAL EVENT IS SCHEDULED IF THIS FEDERATE IS THE TARGET ENTITY
                    IncomingToken token = new IncomingToken(tokenId, timestamp, engine.getNode(targetEntity, targetNode), startTime);
                    engine.scheduleLocalEvent(token);
                }else {
                    System.out.println("-- Ignored");
                }
            }
            //INCOMING MESSAGE
            else if (interactionClass.equals(adapter.getIncomingMessageHandle())) {
                //decode source entity
                HLAunicodeString mSourceEntityE = adapter.getEncoderFactory().createHLAunicodeString();
                mSourceEntityE.decode(theParameters.get(adapter.getMessageSourceEntityHandle() ));
                String mSourceEntity = mSourceEntityE.getValue();

                //decode target entity
                HLAunicodeString mTargetEntityE = adapter.getEncoderFactory().createHLAunicodeString();
                mTargetEntityE.decode(theParameters.get(adapter.getMessageTargetEntityHandle()));
                String mTargetEntity = mTargetEntityE.getValue();

                //decode target node
                HLAunicodeString mTargetNodeE = adapter.getEncoderFactory().createHLAunicodeString();
                mTargetNodeE.decode(theParameters.get(adapter.getMessageTargetNodeHandle()));
                String mTargetNode = mTargetNodeE.getValue();

                //decode message
                HLAunicodeString messageDataE = adapter.getEncoderFactory().createHLAunicodeString();
                messageDataE.decode(theParameters.get(adapter.getMessageDataHandle()));
                String messageData = messageDataE.getValue();

                //LOG
                System.out.print("[FederateAmbassador - " +  this.getFederateName() +"] current time: " + engine.getSimulationTime()
                        + " -  received IncomingMessage interaction with timestamp: " + timestamp);
                System.out.println(String.format("\nMessage - SourceEntity: %s TargetEntity: %s::%s message: %s",
                        mSourceEntity, mTargetEntity, mTargetNode, messageData));

                if(mTargetEntity.equals(this.getFederateName())){
                    //A LOCAL EVENT IS SCHEDULED IF THIS FEDERATE IS THE TARGET ENTITY
                    IncomingMessage message = new IncomingMessage(timestamp, mSourceEntity, engine.getNode(mTargetEntity, mTargetNode), messageData);
                    engine.scheduleLocalEvent(message);
                }else {
                    System.out.println("-- Ignored");
                }

            }else{
                System.err.println("[FederateAmbassador - " +  this.getFederateName() +"] Unknown interaction class: " + interactionClass.getClass().getName());
            }
       } catch (Exception e) {
        e.printStackTrace();
        }
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        System.out.println("[FederateAmbassador - " +  this.getFederateName() + " received interaction " + interactionClass.getClass().getName());
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        /*
        * The Join Notification interaction is received when a new federate joins the federation
        * */
        if (interactionClass.equals(adapter.getJoinNotificationClassHandle())) {
            synchronized (this) {
                joinedFederates++;
                System.out.println("[FederateAmbassador - " +  this.getFederateName() + "] Federate joined (" + joinedFederates + ")");
                notifyAll();
            }
        }
    }
}