import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
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
 * Demonstration of structured Resource support
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
public class Test3_ResourceHierarchy {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_ResourceTest.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);
        CSVLogger log = new CSVLogger(config.getOutputFolder() + "test3_output.csv");

        Participant p1 = new Participant("ProductionLine", true);

        // -----------------------------------------------------------------------
        // Performers (atomic resources with MTTF / MTTR)
        // -----------------------------------------------------------------------

        // PickingUnitA — primary pick-and-place arm
        Performer pickingUnitA = new Performer("PickingUnitA", "role1",
                new LognormalGenerator(5*60, 10),
                new ExponentialGenerator(1.0 / (3*60)));

        // PickingUnitB — backup pick-and-place arm
        Performer pickingUnitB = new Performer("PickingUnitB", "role1",
                new LognormalGenerator(7*60, 10),
                new ExponentialGenerator(1.0 / (3*60)));

        // ConveyorBelt
        Performer conveyorBelt = new Performer("ConveyorBelt", "role2",
                new LognormalGenerator(30*60, 60),
                new ExponentialGenerator(1.0 / (60*10)));

        // -----------------------------------------------------------------------
        // Broker: redundant pick-and-place tool
        //   Switch time ~ Exponential(lambda=1/30) → E[X] ≈ 30 s
        // -----------------------------------------------------------------------
        Broker pickTool = new Broker("PickTool", "picker", StandbyMode.HOT,
                new ExponentialGenerator(1.0 / 30));
        pickTool.addAlternative(pickingUnitA);
        pickTool.addAlternative(pickingUnitB);

        // -----------------------------------------------------------------------
        // Subsystem: feeding station (series composition)
        // -----------------------------------------------------------------------
        Subsystem feedingStation = new Subsystem("FeedingStation", "feeder");
        feedingStation.addComponent(pickTool);
        feedingStation.addComponent(conveyorBelt);

        // -----------------------------------------------------------------------
        // Process flow
        //   Interarrival ~ Exponential(lambda=1/300) → one token every ~5 min avg
        //   Service time ~ Normal(5.0, 0.5) seconds
        // -----------------------------------------------------------------------
        Start start = new Start("Start", p1,
                new ExponentialGenerator(1.0 / (5*60)),
                config.getNumberOfTokens());

        Task componentFeeding = new Task("ComponentFeeding", p1,
                new NormalGenerator(5.0, 0.5));
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
