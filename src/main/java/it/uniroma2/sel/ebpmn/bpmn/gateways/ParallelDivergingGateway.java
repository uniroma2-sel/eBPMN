package it.uniroma2.sel.ebpmn.bpmn.gateways;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * BPMN Parallel (AND) Diverging Gateway — fork that fires all outgoing branches.
 *
 * <p>Upon receiving an {@link it.uniroma2.sel.ebpmn.events.IncomingToken}, the
 * gateway creates one copy of the token per outgoing edge and dispatches all
 * copies simultaneously at the same logical time.  All token copies carry the
 * same token ID so that the matching {@link ParallelConvergingGateway} can
 * correlate them.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see ParallelConvergingGateway
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class ParallelDivergingGateway extends FlowNode {

    public ParallelDivergingGateway(String name, Participant p) {
        super(name, p);
    }

    @Override
    public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent {
        System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName()
                + " - " + this.getName()
                + ": Received INCOMING TOKEN, Token ID " + incomingToken.getTokenId()
                + " - forking to " + this.nodes.size() + " branches");

        for (FlowNode node : this.nodes) {
            IncomingToken branch = new IncomingToken(
                    incomingToken.getTokenId(),
                    incomingToken.getTime(),
                    node,
                    incomingToken.getStartTimestamp());
            node.receiveEvent(branch);

            System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName()
                    + " - " + this.getName()
                    + ": Scheduled INCOMING TOKEN for branch " + node.getName());
        }
    }

    @Override
    public void handleEvent(IncomingMessage incomingMessage) throws UnexpectedEvent {
        this.unexpectedEvent(incomingMessage);
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
    protected void unexpectedEvent(Event e) throws UnexpectedEvent {
        throw new UnexpectedEvent("Node " + this.getName()
                + " has received an unexpected " + e.getClass().getName() + " event type");
    }
}
