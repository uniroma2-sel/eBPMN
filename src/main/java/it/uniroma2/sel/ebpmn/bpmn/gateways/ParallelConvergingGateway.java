package it.uniroma2.sel.ebpmn.bpmn.gateways;

import java.util.HashMap;
import java.util.Map;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;

/**
 * BPMN Parallel (AND) Converging Gateway — join that waits for all branches.
 *
 * <p>Collects tokens from all incoming parallel branches before forwarding a
 * single merged token to the outgoing edge.  Branches are correlated by token
 * ID: every token copy produced by a {@link ParallelDivergingGateway} shares
 * the same ID, so the gateway counts arrivals per ID and fires only when the
 * count reaches {@link #expectedBranches}.
 *
 * <p>{@code expectedBranches} must equal the number of branches created by the
 * matching {@link ParallelDivergingGateway} (or the number of parallel paths
 * that converge here).
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see ParallelDivergingGateway
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class ParallelConvergingGateway extends FlowNode {

    /** Number of branch tokens that must arrive before forwarding. */
    private final int expectedBranches;
    /** Per-token-ID arrival counter; cleared when all branches have arrived. */
    private final Map<String, Integer> arrivals = new HashMap<>();

    public ParallelConvergingGateway(String name, Participant p, int expectedBranches) {
        super(name, p);
        this.expectedBranches = expectedBranches;
    }

    public int getExpectedBranches() {
        return expectedBranches;
    }

    @Override
    public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent {
        String id = incomingToken.getTokenId();
        int count = arrivals.getOrDefault(id, 0) + 1;
        arrivals.put(id, count);

        System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName()
                + " - " + this.getName()
                + ": Received INCOMING TOKEN, Token ID " + id
                + " [" + count + "/" + expectedBranches + " branches arrived]");

        if (count == expectedBranches) {
            arrivals.remove(id);
            IncomingToken merged = new IncomingToken(
                    id,
                    incomingToken.getTime(),
                    this.getNextNode(),
                    incomingToken.getStartTimestamp());
            this.getNextNode().receiveEvent(merged);

            System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName()
                    + " - " + this.getName()
                    + ": All " + expectedBranches + " branches arrived."
                    + " Scheduled INCOMING TOKEN for " + this.getNextNode().getName());
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
