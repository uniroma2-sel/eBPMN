package it.uniroma2.sel.ebpmn.hla;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.IncomingMessage;
import it.uniroma2.sel.ebpmn.events.IncomingToken;

/**
 * Portico/Pitch RTI implementation of {@link HlaAdapter}.
 *
 * <p>{@code HlaAdapterImpl} manages the complete lifecycle of a federate's RTI-side
 * interactions. It resolves and caches all {@link InteractionClassHandle} and
 * {@link ParameterHandle} instances required by the eBPMN FOM, publishes and subscribes
 * the relevant interaction classes at federation join time, and translates between eBPMN
 * domain events ({@link it.uniroma2.sel.ebpmn.events.IncomingToken},
 * {@link it.uniroma2.sel.ebpmn.events.IncomingMessage}) and their HLA wire representation.</p>
 *
 * <p>Time management uses the HLA {@code nextMessageRequest} service (conservative
 * synchronisation), with the associated TAG wait delegated to
 * {@link FederateAmbassadorImpl}.</p>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see HlaAdapter
 * @see FederateAmbassadorImpl
 * @see it.uniroma2.sel.ebpmn.configuration.SimulationConfig
 */
public class HlaAdapterImpl implements HlaAdapter {

    /** RTI ambassador used to call all RTI services. */
    private final RTIambassador rti;

    /** Factory for encoding/decoding HLA data types (unicode strings, integers, floats). */
    private final EncoderFactory encoderFactory;

    /** Federate ambassador that receives all RTI-to-federate callbacks. */
    private final FederateAmbassadorImpl ambassador;

    /** HLA float-64 time factory used to construct logical time and interval values. */
    private final HLAfloat64TimeFactory timeFactory;

    /** Interaction class handle for {@code HLAinteractionRoot.IncomingToken}. */
    private InteractionClassHandle incomingTokenHandle;

    /** Parameter handle for the {@code tokenID} field of the IncomingToken interaction. */
    private ParameterHandle tIDHandle;

    /** Parameter handle for the {@code tokenTargetEntity} field of the IncomingToken interaction. */
    private ParameterHandle tTargeEntityHandle;

    /** Parameter handle for the {@code tokenTargetNode} field of the IncomingToken interaction. */
    private ParameterHandle tTargeNodeHandle;

    /** Parameter handle for the {@code startTime} field of the IncomingToken interaction. */
    private ParameterHandle tStartTimeHandle;

    /** Interaction class handle for {@code HLAinteractionRoot.IncomingMessage}. */
    private InteractionClassHandle incomingMessageHandle;

    /** Parameter handle for the {@code messageSourceEntity} field of the IncomingMessage interaction. */
    private ParameterHandle mSourceEntityHandle;

    /** Parameter handle for the {@code messageTargetEntity} field of the IncomingMessage interaction. */
    private ParameterHandle mTargetEntityHandle;

    /** Parameter handle for the {@code messageTargetNode} field of the IncomingMessage interaction. */
    private ParameterHandle mTargetNodeHandle;

    /** Parameter handle for the {@code message} (payload) field of the IncomingMessage interaction. */
    private ParameterHandle messageDataHandle;

    /** Interaction class handle for {@code HLAinteractionRoot.FederationJoined}. */
    private InteractionClassHandle joinNotificationClassHandle;


    /**
     * Constructs an {@code HlaAdapterImpl} with the RTI and supporting objects provided by
     * {@link HlaFactory} after the federate has joined the federation execution.
     *
     * @param rti            the RTI ambassador for issuing RTI service calls
     * @param encoderFactory the encoder factory for marshalling HLA data elements
     * @param ambassador     the federate ambassador that will receive RTI callbacks
     * @param timeFactory    the HLA float-64 time factory for time and interval values
     */
    public HlaAdapterImpl(RTIambassador rti, EncoderFactory encoderFactory, FederateAmbassadorImpl ambassador,
                          HLAfloat64TimeFactory timeFactory) {
        this.rti = rti;
        this.encoderFactory = encoderFactory;
        this.ambassador = ambassador;
        this.timeFactory = timeFactory;
    }

    @Override
    public FederateAmbassadorImpl getAmbassador() {
        return ambassador;
    }

    public EncoderFactory getEncoderFactory() {
        return encoderFactory;
    }

    public InteractionClassHandle getIncomingMessageHandle() {
        return incomingMessageHandle;
    }

    public InteractionClassHandle getIncomingTokenHandle() {
        return incomingTokenHandle;
    }

    public ParameterHandle getMessageDataHandle() {
        return messageDataHandle;
    }

    public ParameterHandle getMessageSourceEntityHandle() {
        return mSourceEntityHandle;
    }

    public ParameterHandle getMessageTargetEntityHandle() {
        return mTargetEntityHandle;
    }

    public ParameterHandle getMessageTargetNodeHandle() {
        return mTargetNodeHandle;
    }

    public ParameterHandle getTokenIDHandle() {
        return tIDHandle;
    }

    public ParameterHandle getTokenStartTimeHandle() {
        return tStartTimeHandle;
    }

    public ParameterHandle getTokenTargetEntityHandle() {
        return tTargeEntityHandle;
    }

    public ParameterHandle getTokenTargetNodeHandle() {
        return tTargeNodeHandle;
    }

    public InteractionClassHandle getJoinNotificationClassHandle() {
        return joinNotificationClassHandle;
    }

    @Override
    public void publishJoinNotificationInteraction(){
        //Non controller federates publish the Join Notification interaction to notify when they join the federation
        try {
            joinNotificationClassHandle = rti.getInteractionClassHandle("HLAinteractionRoot.FederationJoined");
            rti.publishInteractionClass(joinNotificationClassHandle);
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                 | InteractionClassNotDefined
                 | SaveInProgress | RestoreInProgress e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeJoinNotificationInteraction() {
        //The controller subscribes the Join Notification interaction to be aware of new federates that join the federation
        try {
            joinNotificationClassHandle = rti.getInteractionClassHandle("HLAinteractionRoot.FederationJoined");
            rti.subscribeInteractionClass(joinNotificationClassHandle);
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined
                | SaveInProgress | RestoreInProgress e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void publishAndSubscribeEBPMNInteractions(){
        //Publish and subscribe the eBPMN-related interactions: IncomingToken, IncomingMessage
        try {
            incomingTokenHandle = rti.getInteractionClassHandle("HLAinteractionRoot.IncomingToken");
            //Handles for the IncomingToken interaction
            tIDHandle = rti.getParameterHandle(incomingTokenHandle,"tokenID");
            tTargeEntityHandle = rti.getParameterHandle(incomingTokenHandle, "tokenTargetEntity");
            tTargeNodeHandle = rti.getParameterHandle(incomingTokenHandle, "tokenTargetNode");
            tStartTimeHandle = rti.getParameterHandle(incomingTokenHandle, "startTime");

            rti.publishInteractionClass(incomingTokenHandle);
            rti.subscribeInteractionClass(incomingTokenHandle);

            incomingMessageHandle = rti.getInteractionClassHandle("HLAinteractionRoot.IncomingMessage");
            //handles for the IncomingMessage interaction
            mSourceEntityHandle = rti.getParameterHandle(incomingMessageHandle, "messageSourceEntity");
            mTargetEntityHandle = rti.getParameterHandle(incomingMessageHandle, "messageTargetEntity");
            mTargetNodeHandle = rti.getParameterHandle(incomingMessageHandle, "messageTargetNode");
            messageDataHandle = rti.getParameterHandle(incomingMessageHandle, "message");

            rti.publishInteractionClass(incomingMessageHandle);
            rti.subscribeInteractionClass(incomingMessageHandle);

        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError |
                 InteractionClassNotDefined | SaveInProgress | RestoreInProgress |
                 FederateServiceInvocationsAreBeingReportedViaMOM | InvalidInteractionClassHandle e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendJoinNotificationInteraction() {
        //Non controller federates send the Join Notification interaction to notify when they join the federation
        ParameterHandleValueMap parameters = null; //no parameters are needed
        try {
            parameters = rti.getParameterHandleValueMapFactory().create(0);
            rti.sendInteraction(joinNotificationClassHandle, parameters, null);
        } catch (FederateNotExecutionMember | NotConnected | InteractionClassNotPublished |
                 InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress |
                 RTIinternalError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendIncomingToken(IncomingToken token)  {
        String targetEntity = token.getHandlerEntity().getParticipant().getName();
        String targetNode = token.getHandlerEntity().getName();

        /*
         * To prevent routing tokens from flow nodes that introduce no delay (i.e., intermediate events and gateways)
         * from causing an Invalid Logical Time exception, an additional virtual delay is introduced on the sender side.
         * */

        double virtualDelay = 0;
        double currentTime = ExecutionEngine.getInstance().getSimulationTime();
        double lookahead = ExecutionEngine.getInstance().getLookahead();
        if (token.getTime()<currentTime + Double.MIN_VALUE + lookahead)
            virtualDelay = currentTime + Double.MIN_VALUE + lookahead - token.getTime();

        try {
            ParameterHandleValueMap params = rti.getParameterHandleValueMapFactory().create(3);
            params.put(tIDHandle, encoderFactory.createHLAunicodeString(token.getTokenId()).toByteArray());
            params.put(tTargeEntityHandle, encoderFactory.createHLAunicodeString(targetEntity).toByteArray());
            params.put(tTargeNodeHandle,encoderFactory.createHLAunicodeString(targetNode).toByteArray());
            params.put(tStartTimeHandle,encoderFactory.createHLAfloat64BE(token.getStartTimestamp()).toByteArray());

            rti.sendInteraction(incomingTokenHandle, params, null,timeFactory.makeTime(token.getTime()+virtualDelay));

            //LOG
            System.out.println("["+ ambassador.getFederateName() +"] sent IncomingToken Interaction to "
                    + targetEntity + "::" + targetNode + " at time " + token.getTime() + " with delay " + virtualDelay);


        } catch (FederateNotExecutionMember | NotConnected | InteractionParameterNotDefined | RestoreInProgress |
                 InteractionClassNotDefined | InteractionClassNotPublished | SaveInProgress | RTIinternalError |
                 InvalidLogicalTime e) {
            throw new RuntimeException(e);}
    }

    @Override
    public void sendIncomingMessage(IncomingMessage message){
        String sourceEntity = message.getSourceEntity();
        String targetEntity = message.getTargetEntity().getParticipant().getName();
        String targetNode = message.getTargetEntity().getName();

        /*
         * To prevent routing messages from flow nodes that introduce no delay (i.e., intermediate events)
         * from causing an Invalid Logical Time exception, an additional virtual delay is introduced on the sender side.
         * */
        double virtualDelay = 0;
        double currentTime = ExecutionEngine.getInstance().getSimulationTime();
        double lookahead = ExecutionEngine.getInstance().getLookahead();
        //Math.nextUp(currentTime) returns the smallest floating-point number greater than currentTime
        double nextValidTime = lookahead + Math.nextUp(currentTime);
        if (message.getTime() < nextValidTime)
            virtualDelay = nextValidTime - message.getTime();

        //LOG
        System.out.println("["+ ambassador.getFederateName() +"] current time: " + currentTime +
                " lookahead: " + lookahead);
        System.out.println("\t message timestamp: " + message.getTime() + " Next valid time: " + nextValidTime +  " delay: " + virtualDelay);

        try {
            ParameterHandleValueMap params;
            params = rti.getParameterHandleValueMapFactory().create(3);

            params.put(mSourceEntityHandle, encoderFactory.createHLAunicodeString(sourceEntity).toByteArray());
            params.put(mTargetEntityHandle,encoderFactory.createHLAunicodeString(targetEntity).toByteArray());
            params.put(mTargetNodeHandle,encoderFactory.createHLAunicodeString(targetNode).toByteArray());
            params.put(messageDataHandle,encoderFactory.createHLAunicodeString(message.getMessagePayload()).toByteArray());

            rti.sendInteraction(incomingMessageHandle, params, null,timeFactory.makeTime(message.getTime() + virtualDelay) );

            //LOG
            System.out.println("["+ ambassador.getFederateName() +"] sent IncomingMessage Interaction to "
                    + targetEntity + "::" + targetNode + " at time " + message.getTime() + " with delay " + virtualDelay);

        } catch (FederateNotExecutionMember | NotConnected | RTIinternalError | InteractionClassNotPublished |
                 InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress |
                 InvalidLogicalTime e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void requestTimeAdvance(double time) {
        try {
            rti.nextMessageRequest(timeFactory.makeTime(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public LogicalTime queryRTILogicalTime() {
        try {
            return rti.queryLogicalTime();
        } catch (SaveInProgress e) {
            throw new RuntimeException(e);
        } catch (RestoreInProgress e) {
            throw new RuntimeException(e);
        } catch (FederateNotExecutionMember e) {
            throw new RuntimeException(e);
        } catch (NotConnected e) {
            throw new RuntimeException(e);
        } catch (RTIinternalError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void waitForTimeAdvanceGrant() throws InterruptedException {
        ambassador.setAdvancingState();
        //wait for the RTI to invoke the timeAdvanceGrant callback
        synchronized (ambassador) {
            while(ambassador.isAdvancing()){
                ambassador.wait();
            }
        }
    }

    @Override
    public void resign(){
        try {
            rti.resignFederationExecution(ResignAction.NO_ACTION);
            System.out.println("[FederateAmbassador - " +  ambassador.getFederateName() + "] Resigned from Federation");
        } catch (InvalidResignAction e) {
            throw new RuntimeException(e);
        } catch (OwnershipAcquisitionPending e) {
            throw new RuntimeException(e);
        } catch (FederateOwnsAttributes e) {
            throw new RuntimeException(e);
        } catch (FederateNotExecutionMember e) {
            throw new RuntimeException(e);
        } catch (NotConnected e) {
            throw new RuntimeException(e);
        } catch (CallNotAllowedFromWithinCallback e) {
            throw new RuntimeException(e);
        } catch (RTIinternalError e) {
            throw new RuntimeException(e);
        }
        try {
            rti.destroyFederationExecution((ExecutionEngine.getInstance()).getFederationName());
            System.out.println("[FederateAmbassador - " +  ambassador.getFederateName() + "] Federation Execution Destroyed");
        } catch (FederatesCurrentlyJoined e) {
            //ignore
        } catch (FederationExecutionDoesNotExist e) {
            throw new RuntimeException(e);
        } catch (NotConnected e) {
            throw new RuntimeException(e);
        } catch (RTIinternalError e) {
            throw new RuntimeException(e);
        }
    }
}