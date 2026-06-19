import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.bpmn.tasks.SendTask;
import it.uniroma2.sel.ebpmn.bpmn.tasks.ReceiveTask;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.generators.DeterministicGenerator;
import it.uniroma2.sel.ebpmn.logger.CSVLogger;
import it.uniroma2.sel.ebpmn.resources.Performer;
import it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Test 9 — SendTask / ReceiveTask collaboration with KEEP-DELAY failure policy.
 *
 */
public class Test9_SendReceive_WithFailure {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_Test9.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);

        CSVLogger logA = new CSVLogger(config.getOutputFolder() + "test9_processA.csv");
        CSVLogger logB = new CSVLogger(config.getOutputFolder() + "test9_processB.csv");

        // -----------------------------------------------------------------------
        // Participants
        // -----------------------------------------------------------------------
        Participant pA = new Participant("ProcessA", true);
        Participant pB = new Participant("ProcessB", true);

        // -----------------------------------------------------------------------
        // Resources — with failure model, KEEP-DELAY policy
        // -----------------------------------------------------------------------
        Performer rTask1 = new Performer("R_Task1", pA,
                new DeterministicGenerator(20),
                new DeterministicGenerator(5));
        rTask1.setTokenOnFailure(TokenOnFailure.DELAY);
        rTask1.setQueueOnFailure(QueueOnFailure.KEEP);

        Performer rSendTask = new Performer("R_SendTask", pA,
                new DeterministicGenerator(15),
                new DeterministicGenerator(4));
        rSendTask.setTokenOnFailure(TokenOnFailure.DELAY);
        rSendTask.setQueueOnFailure(QueueOnFailure.KEEP);

        Performer rReceive = new Performer("R_Receive", pB,
                new DeterministicGenerator(18),
                new DeterministicGenerator(6));
        rReceive.setTokenOnFailure(TokenOnFailure.DELAY);
        rReceive.setQueueOnFailure(QueueOnFailure.KEEP);

        Performer rTask3 = new Performer("R_Task3", pB,
                new DeterministicGenerator(25),
                new DeterministicGenerator(5));
        rTask3.setTokenOnFailure(TokenOnFailure.DELAY);
        rTask3.setQueueOnFailure(QueueOnFailure.KEEP);

        // -----------------------------------------------------------------------
        // Process A: Start → Task1 → SendTask → End1
        // -----------------------------------------------------------------------
        Start startA = new Start("StartA", pA,
                new DeterministicGenerator(5),
                config.getNumberOfTokens());

        Task task1 = new Task("Task1", pA,
                new DeterministicGenerator(3));
        task1.addResource(rTask1);

        SendTask sendTask = new SendTask("SendTask", pA,
                new DeterministicGenerator(2));
        sendTask.addResource(rSendTask);

        End endA = new End("EndA", pA);

        startA.addOutGoingEdge(task1);
        task1.addOutGoingEdge(sendTask);
        sendTask.addOutGoingEdge(endA);

        // -----------------------------------------------------------------------
        // Process B: Start → ReceiveTask → Task3 → End2
        // -----------------------------------------------------------------------
        Start startB = new Start("StartB", pB,
                new DeterministicGenerator(5),
                config.getNumberOfTokens());

        ReceiveTask receiveTask = new ReceiveTask("ReceiveTask", pB,
                new DeterministicGenerator(4));
        receiveTask.addResource(rReceive);

        Task task3 = new Task("Task3", pB,
                new DeterministicGenerator(3));
        task3.addResource(rTask3);

        End endB = new End("EndB", pB);

        startB.addOutGoingEdge(receiveTask);
        receiveTask.addOutGoingEdge(task3);
        task3.addOutGoingEdge(endB);

        // -----------------------------------------------------------------------
        // Message flow: SendTask → ReceiveTask
        // -----------------------------------------------------------------------
        sendTask.addMessageFlow(receiveTask, "order");

        // -----------------------------------------------------------------------
        // Collaboration
        // -----------------------------------------------------------------------
        Collaboration model = new Collaboration("SendReceiveCollaboration");
        model.addParticipant(pA);
        model.addParticipant(pB);

        engine.setModel(model);
        engine.setLogger(pA.getName(), logA);
        engine.setLogger(pB.getName(), logB);
        engine.run();
    }
}
