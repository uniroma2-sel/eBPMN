import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.generators.DeterministicGenerator;
import it.uniroma2.sel.ebpmn.generators.ExponentialGenerator;
import it.uniroma2.sel.ebpmn.generators.LognormalGenerator;
import it.uniroma2.sel.ebpmn.generators.NormalGenerator;
import it.uniroma2.sel.ebpmn.logger.CSVLogger;
import it.uniroma2.sel.ebpmn.resources.Broker;
import it.uniroma2.sel.ebpmn.resources.Performer;
import it.uniroma2.sel.ebpmn.resources.Subsystem;
import it.uniroma2.sel.ebpmn.resources.policies.QueueOnFailure;
import it.uniroma2.sel.ebpmn.resources.policies.StandbyMode;
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Demonstration of structured Resource support.
 *
 * Resource hierarchy:
 *
 *   feedingStation : Subsystem  [series — fails if ANY child fails]
 *     ├── pickTool : Broker      [parallel — fails only if ALL alternatives fail]
 *     │     ├── pickingUnitA : Performer   (primary)
 *     │     └── pickingUnitB : Performer   (backup)
 *     └── conveyorBelt : Performer
 *
 * Process: Start → componentFeeding (Task using feedingStation) → End
 *
 * Expected console output: multiple "[Broker/Subsystem] failed/repaired" lines.
 */
public class Test4_DeterministicBehavior {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_ResourceTestDeterministic.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);
        CSVLogger log = new CSVLogger(config.getOutputFolder() + "test4_output.csv");

        Participant p1 = new Participant("ProductionLine", true);

        // -----------------------------------------------------------------------
        // Performers (atomic resources with MTTF / MTTR)
        // -----------------------------------------------------------------------

        // PickingUnitA — primary pick-and-place arm
        Performer pickingUnitA = new Performer("PickingUnitA", p1,
                new DeterministicGenerator(25),
                new DeterministicGenerator(20));

        // PickingUnitB — backup pick-and-place arm
        Performer pickingUnitB = new Performer("PickingUnitB", p1,
                new DeterministicGenerator(35),
                new DeterministicGenerator(20));

        // ConveyorBelt
        Performer conveyorBelt = new Performer("ConveyorBelt", p1,
                new DeterministicGenerator(12),
                new DeterministicGenerator(20));

        // -----------------------------------------------------------------------
        // Broker: redundant pick-and-place tool, no switch time
        // -----------------------------------------------------------------------
        Broker pickTool = new Broker("PickTool", p1, StandbyMode.HOT);
        pickTool.addAlternative(pickingUnitA);
        pickTool.addAlternative(pickingUnitB);

        // -----------------------------------------------------------------------
        // Subsystem: feeding station (series composition)
        // -----------------------------------------------------------------------
        Subsystem feedingStation = new Subsystem("FeedingStation", p1);
        feedingStation.addComponent(pickTool);
        feedingStation.addComponent(conveyorBelt);
        feedingStation.setTokenOnFailure(TokenOnFailure.RESTART);
        feedingStation.setQueueOnFailure(QueueOnFailure.KEEP);

        // -----------------------------------------------------------------------
        // Process flow
        //   Interarrival: one token every 5 sec
        //   Service time: 1 second
        // -----------------------------------------------------------------------
        Start start = new Start("Start", p1,
                new DeterministicGenerator(5),
                config.getNumberOfTokens());

        Task componentFeeding = new Task("ComponentFeeding", p1,
                new DeterministicGenerator(8));
        componentFeeding.addResource(feedingStation);

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
