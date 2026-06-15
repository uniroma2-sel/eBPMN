package it.uniroma2.sel.ebpmn.hla;

import hla.rti1516e.LogicalTime;
import it.uniroma2.sel.ebpmn.events.IncomingMessage;
import it.uniroma2.sel.ebpmn.events.IncomingToken;

/**
 * Abstraction over the HLA RTI operations used by the eBPMN distributed simulation engine.
 *
 * <p>This interface decouples the simulation model and the {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}
 * from any specific RTI implementation (Portico, Pitch, etc.). The engine interacts with the
 * federation exclusively through this contract, so switching RTI vendors requires only a new
 * implementation class without touching simulation logic.</p>
 *
 * <p>The interface covers three groups of operations:</p>
 * <ul>
 *   <li><b>Federation lifecycle</b> — join-notification publish/subscribe/send and resign.</li>
 *   <li><b>eBPMN event exchange</b> — translating {@link IncomingToken} and
 *       {@link IncomingMessage} events to/from HLA interactions.</li>
 *   <li><b>HLA Time Management</b> — issuing Time Advance Requests (TAR) and waiting
 *       for Time Advance Grants (TAG).</li>
 * </ul>
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see HlaAdapterImpl
 * @see FederateAmbassadorImpl
 * @see it.uniroma2.sel.ebpmn.engine.DistributedTimeManager
 */
public interface HlaAdapter {

    /**
     * Publishes the {@code HLAinteractionRoot.FederationJoined} interaction class to the RTI.
     *
     * <p>Called by non-controller federates so they can later notify the controller federate
     * that they have joined the federation execution.</p>
     */
    void publishJoinNotificationInteraction();

    /**
     * Subscribes to the {@code HLAinteractionRoot.FederationJoined} interaction class.
     *
     * <p>Called by the controller federate so it receives a callback each time a new
     * federate joins, allowing it to count participants before starting the simulation.</p>
     */
    void subscribeJoinNotificationInteraction();

    /**
     * Sends a {@code HLAinteractionRoot.FederationJoined} interaction to the RTI.
     *
     * <p>Called once by each non-controller federate immediately after joining the
     * federation execution to inform the controller that this federate is ready.</p>
     */
    void sendJoinNotificationInteraction();

    /**
     * Resolves, publishes, and subscribes to all eBPMN-specific HLA interaction classes.
     *
     * <p>This method obtains {@link hla.rti1516e.InteractionClassHandle} and
     * {@link hla.rti1516e.ParameterHandle} instances for the two core eBPMN interactions:
     * {@code HLAinteractionRoot.IncomingToken} and {@code HLAinteractionRoot.IncomingMessage}.
     * Every federate must call this method before the simulation loop begins.</p>
     */
    void publishAndSubscribeEBPMNInteractions();

    /**
     * Sends an {@link IncomingToken} event to a remote federate as an HLA interaction.
     *
     * <p>Encodes the token's ID, target entity, target node, and start timestamp into
     * the {@code HLAinteractionRoot.IncomingToken} interaction parameters and dispatches
     * it with a timestamped send. A virtual delay may be added to satisfy the RTI's
     * minimum lookahead constraint for flow nodes that introduce no service time.</p>
     *
     * @param token the token event to transmit; its {@code handlerEntity} must resolve
     *              to the remote federate's participant name and node name
     */
    void sendIncomingToken(IncomingToken token);

    /**
     * Sends an {@link IncomingMessage} event to a remote federate as an HLA interaction.
     *
     * <p>Encodes the source entity, target entity, target node, and message payload into
     * the {@code HLAinteractionRoot.IncomingMessage} interaction parameters and dispatches
     * it with a timestamped send. A virtual delay may be added when the message timestamp
     * falls below {@code currentTime + lookahead}.</p>
     *
     * @param message the message event to transmit; its {@code targetEntity} must resolve
     *                to the remote federate's participant name and node name
     */
    void sendIncomingMessage(IncomingMessage message);

    /**
     * Issues a Next Message Request (NMR) / Time Advance Request to the RTI.
     *
     * <p>The RTI will eventually respond with a Time Advance Grant via the
     * {@link FederateAmbassadorImpl#timeAdvanceGrant} callback. The caller should
     * subsequently block on {@link #waitForTimeAdvanceGrant()} until the grant arrives.</p>
     *
     * @param time the logical time to advance to
     */
    void requestTimeAdvance(double time);

    /**
     * Blocks the calling thread until the RTI grants the pending time advance request.
     *
     * <p>Sets the adapter into an advancing state and waits on the
     * {@link FederateAmbassadorImpl} monitor object. The wait ends when
     * {@link FederateAmbassadorImpl#timeAdvanceGrant} resets the advancing flag and
     * notifies all waiting threads.</p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void waitForTimeAdvanceGrant() throws InterruptedException;

    /**
     * Queries the RTI for the federate's current granted logical time.
     *
     * @return the current {@link LogicalTime} as reported by the RTI
     */
    LogicalTime queryRTILogicalTime();

    /**
     * Resigns the federate from the federation execution and attempts to destroy the
     * federation execution if no other federates remain.
     *
     * <p>Uses {@code ResignAction.NO_ACTION}. A {@code FederatesCurrentlyJoined}
     * exception during federation destruction is silently ignored, as other federates
     * may still be running.</p>
     */
    void resign();

    /**
     * Returns the {@link FederateAmbassadorImpl} associated with this adapter.
     *
     * <p>The ambassador handles RTI-to-federate callbacks (TAG, receiveInteraction,
     * synchronization points) and exposes state flags that the adapter polls during
     * setup and the simulation loop.</p>
     *
     * @return the federate ambassador instance
     */
    FederateAmbassadorImpl getAmbassador();
}