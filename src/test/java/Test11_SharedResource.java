import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.gateways.ParallelConvergingGateway;
import it.uniroma2.sel.ebpmn.bpmn.gateways.ParallelDivergingGateway;
import it.uniroma2.sel.ebpmn.bpmn.tasks.ReceiveTask;
import it.uniroma2.sel.ebpmn.bpmn.tasks.SendTask;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.generators.DeterministicGenerator;
import it.uniroma2.sel.ebpmn.logger.CSVLogger;
import it.uniroma2.sel.ebpmn.resources.Performer;
import it.uniroma2.sel.ebpmn.resources.Resource;

/**
 * Test 11 — Single participant with an unique resource (performer) shared
 * among different tasks.
 * No logger is used to generate a CSV of the collaboration.
 */
public class Test11_SharedResource {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_Test11.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);

        // -----------------------------------------------------------------------
        // Participants
        // -----------------------------------------------------------------------
        Participant p   = new Participant("Participant",true);

        // -----------------------------------------------------------------------
        // Resources
        // -----------------------------------------------------------------------
        Resource sharedResource  = new Performer("SharedResource",
                "SharedResource",
                new DeterministicGenerator(152),
                new DeterministicGenerator(10));


        // -----------------------------------------------------------------------
        // Participant 1
        // -----------------------------------------------------------------------
        Start start = new Start("Start", p,
                new DeterministicGenerator(1),
                config.getNumberOfTokens());

        Task task1 = new Task("Task1", p,
                new DeterministicGenerator(10));
        task1.addResource(sharedResource);

        Task task2 = new Task("Task2", p,
                new DeterministicGenerator(5));
        task2.addResource(sharedResource);

        Task task3 = new Task("Task3", p,
                new DeterministicGenerator(2));
        task3.addResource(sharedResource);

        End end = new End("End", p);

        // Seller flow
        start.addOutGoingEdge(task1);
        task1.addOutGoingEdge(task2);
        task2.addOutGoingEdge(task3);
        task3.addOutGoingEdge(end);

        // -----------------------------------------------------------------------
        // Collaboration
        // -----------------------------------------------------------------------
        Collaboration model = new Collaboration("SellerCollaboration");
        model.addParticipant(p);

        engine.setModel(model);
        engine.run();
    }
}
