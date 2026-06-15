package it.uniroma2.sel.ebpmn.bpmn;



import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;


/**
 * Proxy node that represents a remote BPMN entity in a distributed simulation.
 *
 * <p>A RemoteNode stands in for a {@link Participant} whose process logic runs
 * inside a different HLA federate.  When a local flow node sends an event to a
 * RemoteNode, the node translates the event into an HLA interaction and dispatches
 * it to the RTI via the {@link it.uniroma2.sel.ebpmn.hla.HlaAdapter}.  The
 * receiving federate maps the interaction back to a local event and schedules it
 * on its own {@link it.uniroma2.sel.ebpmn.engine.ExecutionEngine}.
 *
 * <p>From the perspective of the sending federate, routing to a RemoteNode is
 * identical to routing to a local {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode}:
 * the application code does not change between local and distributed runs.
 *
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see it.uniroma2.sel.ebpmn.hla.HlaAdapter
 * @see it.uniroma2.sel.ebpmn.hla.HlaAdapterImpl
 */
public class RemoteNode extends Node {

    public RemoteNode(String nodeName, Participant p) {
        super(nodeName,p);
    }

    @Override
    public void receiveEvent(IncomingMessage m) throws UnexpectedEvent{
        ExecutionEngine.getInstance().getAdapter().sendIncomingMessage(m);
    }

    @Override
    public void receiveEvent(IncomingToken t) throws UnexpectedEvent{
        ExecutionEngine.getInstance().getAdapter().sendIncomingToken(t);
    }

    @Override
    public void receiveEvent(TokenServiceCompleted e) throws UnexpectedEvent {
        this.unexpectedEvent(e);
    }

    @Override
    public void receiveEvent(StartProcess e) throws UnexpectedEvent {
        this.unexpectedEvent(e);
    }

    @Override
    protected void unexpectedEvent(Event e) throws UnexpectedEvent {
        throw new UnexpectedEvent("RemoteEntityProxy " + this.getName() +
                " is unable to route " + e.getClass().getName()+ " event type");
    }


}
