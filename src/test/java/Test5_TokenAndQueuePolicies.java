import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.generators.DeterministicGenerator;
import it.uniroma2.sel.ebpmn.logger.CSVLogger;
import it.uniroma2.sel.ebpmn.resources.Broker;
import it.uniroma2.sel.ebpmn.resources.Performer;
import it.uniroma2.sel.ebpmn.resources.Subsystem;
import it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure;
import it.uniroma2.sel.ebpmn.resources.policies.StandbyMode;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Test of failure policies to handle the token currently in service
 * and the enqueued tokens.
 *
 */
public class Test5_TokenAndQueuePolicies {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_Test5.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);
        CSVLogger log = new CSVLogger(config.getOutputFolder() + "test5_output.csv");

        Participant p1 = new Participant("ProductionLine", true);

        // -----------------------------------------------------------------------
        // Performers (atomic resources with MTTF / MTTR)
        // -----------------------------------------------------------------------

        // PickingUnitA — primary pick-and-place arm
        Performer pickingUnit = new Performer("PickingUnitA", p1,
               new DeterministicGenerator(32),
               new DeterministicGenerator(11));


        pickingUnit.setTokenOnFailure(TokenOnFailure.DELAY);
        pickingUnit.setQueueOnFailure(QueueOnFailure.FLUSH);


        // -----------------------------------------------------------------------
        // Process flow
        //   Interarrival: one token every 5 sec
        //   Service time: 1 second
        // -----------------------------------------------------------------------
        Start start = new Start("Start", p1,
                new DeterministicGenerator(1),
                config.getNumberOfTokens());

        Task componentFeeding = new Task("ComponentFeeding", p1,
                new DeterministicGenerator(10));
        componentFeeding.addResource(pickingUnit);

        End end = new End("End", p1);

        start.addOutGoingEdge(componentFeeding);
        componentFeeding.addOutGoingEdge(end);

        // -----------------------------------------------------------------------
        // Collaboration
        // -----------------------------------------------------------------------
        Collaboration model = new Collaboration("ProductionLine");
        model.addParticipant(p1);

        engine.setModel(model);
        engine.setLogger(p1.getName(), log);
        engine.run();
    }
}
