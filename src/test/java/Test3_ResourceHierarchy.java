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
import it.uniroma2.sel.ebpmn.resources.policies.TokenOnFailure;

/**
 * Demonstration of structured Resource support (Step 1-9 of claude_addresource.md).
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
 * Parameters tuned so that several failure/repair cycles occur within the
 * 10 000 s simulation window:
 *   PickingUnitA  MTTF ≈ exp(6.5) ≈ 665 s, MTTR ≈ 200 s
 *   PickingUnitB  MTTF ≈ exp(6.8) ≈ 898 s, MTTR ≈ 200 s
 *   ConveyorBelt  MTTF ≈ exp(7.5) ≈ 1808 s, MTTR ≈ 600 s
 *   Switch time   mean ≈ 30 s
 *
 * Expected console output: multiple "[Broker/Subsystem] failed/repaired" lines.
 */
public class Test3_ResourceHierarchy {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_ResourceTest.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);
        CSVLogger log = new CSVLogger("src/test/resources/test3_output.csv");

        Participant p1 = new Participant("ProductionLine", true);

        // -----------------------------------------------------------------------
        // Performers (atomic resources with MTTF / MTTR)
        // -----------------------------------------------------------------------

        // PickingUnitA — primary pick-and-place arm
        //   MTTF ~ Lognormal(mu=6.5, sigma=0.3) → E[X] ≈ 665 s (~11 min)
        //   MTTR ~ Exponential(lambda=1/200)     → E[X] ≈ 200 s (~3 min)
        Performer pickingUnitA = new Performer("PickingUnitA", p1,
                new LognormalGenerator(6.5, 0.3),
                new ExponentialGenerator(1.0 / 200),
                TokenOnFailure.DELAY,
                QueueOnFailure.KEEP);

        // PickingUnitB — backup pick-and-place arm
        //   MTTF ~ Lognormal(mu=6.8, sigma=0.3) → E[X] ≈ 898 s (~15 min)
        //   MTTR ~ Exponential(lambda=1/200)
        Performer pickingUnitB = new Performer("PickingUnitB", p1,
                new LognormalGenerator(6.8, 0.3),
                new ExponentialGenerator(1.0 / 200),
                TokenOnFailure.DELAY,
                QueueOnFailure.KEEP);

        // ConveyorBelt
        //   MTTF ~ Lognormal(mu=7.5, sigma=0.3) → E[X] ≈ 1808 s (~30 min)
        //   MTTR ~ Exponential(lambda=1/600)     → E[X] ≈ 600 s (~10 min)
        Performer conveyorBelt = new Performer("ConveyorBelt", p1,
                new LognormalGenerator(7.5, 0.3),
                new ExponentialGenerator(1.0 / 600),
                TokenOnFailure.DELAY,
                QueueOnFailure.KEEP);

        // -----------------------------------------------------------------------
        // Broker: redundant pick-and-place tool
        //   Switch time ~ Exponential(lambda=1/30) → E[X] ≈ 30 s
        // -----------------------------------------------------------------------
        Broker pickTool = new Broker("PickTool", p1,
                new ExponentialGenerator(1.0 / 30));
        pickTool.addAlternative(pickingUnitA);
        pickTool.addAlternative(pickingUnitB);

        // -----------------------------------------------------------------------
        // Subsystem: feeding station (series composition)
        // -----------------------------------------------------------------------
        Subsystem feedingStation = new Subsystem("FeedingStation", p1);
        feedingStation.addComponent(pickTool);
        feedingStation.addComponent(conveyorBelt);

        // -----------------------------------------------------------------------
        // Process flow
        //   Interarrival ~ Exponential(lambda=1/300) → one token every ~5 min avg
        //   Service time ~ Normal(5.0, 0.5) seconds
        // -----------------------------------------------------------------------
        Start start = new Start("Start", p1,
                new ExponentialGenerator(1.0 / 300),
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
