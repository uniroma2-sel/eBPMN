package it.uniroma2.sel.ebpmn.bpmn.gateways;

import java.util.ArrayList;

import it.uniroma2.sel.ebpmn.bpmn.FlowNode;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.events.*;
import it.uniroma2.sel.ebpmn.exceptions.BPMNException;
import it.uniroma2.sel.ebpmn.exceptions.UnexpectedEvent;
import it.uniroma2.sel.ebpmn.generators.RandomGenerator;

/**
 * BPMN Exclusive (XOR) Diverging Gateway — probabilistic token routing.
 *
 * <p>Routes an incoming token to exactly one of its outgoing branches based on
 * the routing probabilities assigned to each branch via
 * {@link #addOutGoingEdge(it.uniroma2.sel.ebpmn.bpmn.FlowNode, double)}.
 *
 * <p>The implementation assumes the sum of all routing probabilities equals
 * exactly {@code 1.0}; no normalisation is performed.  A random number is drawn
 * from {@link it.uniroma2.sel.ebpmn.generators.RandomGenerator} and compared
 * against the cumulative probability distribution to select the branch.
 *
 * @author Paolo Bocciarelli
 * @version 1.0-SNAPSHOT
 * @see ExclusiveConvergingGateway
 * @see it.uniroma2.sel.ebpmn.bpmn.FlowNode
 */
public class ExclusiveDivergingGateway extends FlowNode{

	/** Per-branch routing probabilities (parallel to {@link it.uniroma2.sel.ebpmn.bpmn.FlowNode#nodes}). */
	private ArrayList<Double> routingProb;

	/**
	 * Creates an ExclusiveDivergingGateway with the given name and owning participant.
	 *
	 * @param name the node name
	 * @param p    the owning participant (pool)
	 */
	public ExclusiveDivergingGateway(String name, Participant p) {
		super(name,p);
		routingProb = new ArrayList<>();
	}

	/**
	 * Adds an outgoing branch with the given routing probability.
	 *
	 * @param n routing probability assigned to this branch
	 * @param p the probability (0.0–1.0) that the token is routed to {@code n}
	 * @throws it.uniroma2.sel.ebpmn.exceptions.BPMNException if the cumulative probability would exceed 1.0
	 */
	public  void addOutGoingEdge(FlowNode n, double p) throws BPMNException{
		this.nodes.add(n);			//adding edge
		this.routingProb.add(Double.valueOf(p));	//adding routing probability

		//check routing probability consistency
		double sum = 0;
		for(int i=0; i<=this.routingProb.size()-1; i++)
			sum+=routingProb.get(i);

		if(sum>1) throw new BPMNException("The sum of routing probability for the " + this.getName() + " gateway is greater that 1.0");
	}

	@Override
	public void handleEvent(IncomingToken incomingToken) throws UnexpectedEvent {
		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - " + this.getName()
				+ ": Received INCOMING TOKEN, Token ID " + incomingToken.getTokenId());

		IncomingToken t = new IncomingToken(incomingToken.getTokenId(), incomingToken.getTime(),
				this.nodes.get(this.getRoute()), incomingToken.getStartTimestamp());
		this.getNextNode().receiveEvent(t);
		//engine.getEventsList().addEvent(t);

		//LOG
		System.out.println(incomingToken.getTime() + ") " + this.getParticipant().getName() + " - "  + this.getName()
				+ ": Scheduled INCOMING TOKEN event at time "
				+ t.getTime() + " "
				+ "for " + t.getHandlerEntity().getName());
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
	protected void unexpectedEvent(Event e) throws UnexpectedEvent{
		throw new UnexpectedEvent("Node " + this.getName() +
				"has received an unexpected " + e.getClass().getName()+ " event type");
	}

	@Override
	/*
	 * If no routing probability is specified, it is assumed that
	 * there is only one outgoing edge
	 */
	public void addOutGoingEdge(FlowNode n) {
		this.nodes.add(n);//adding edge
		this.routingProb.add(1.0);	//adding routing probability
	}

	/*
	 * According to the routing probabilities, the method identifies
	 * the index of an outgoing edge (which is then used to forward
	 * the token to the appropriate FlowNode element)
	 *
	 */
	private int getRoute() {
		int i=0;
		Double prob = this.routingProb.get(i);
		while(prob<= RandomGenerator.nextDouble()) {
			i++;
			prob += this.routingProb.get(i);
		}
		return i;
	}

	//Gets the next node according to the routing probabilities
	@Override
	public FlowNode getNextNode() {
		return this.nodes.get(getRoute());
	}


}
