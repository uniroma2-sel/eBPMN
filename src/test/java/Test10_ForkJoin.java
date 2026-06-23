import it.uniroma2.sel.ebpmn.bpmn.Collaboration;
import it.uniroma2.sel.ebpmn.bpmn.Participant;
import it.uniroma2.sel.ebpmn.bpmn.events.End;
import it.uniroma2.sel.ebpmn.bpmn.events.Start;
import it.uniroma2.sel.ebpmn.bpmn.gateways.ParallelConvergingGateway;
import it.uniroma2.sel.ebpmn.bpmn.gateways.ParallelDivergingGateway;
import it.uniroma2.sel.ebpmn.bpmn.tasks.Task;
import it.uniroma2.sel.ebpmn.bpmn.tasks.SendTask;
import it.uniroma2.sel.ebpmn.bpmn.tasks.ReceiveTask;
import it.uniroma2.sel.ebpmn.configuration.SimulationConfig;
import it.uniroma2.sel.ebpmn.engine.ExecutionEngine;
import it.uniroma2.sel.ebpmn.generators.DeterministicGenerator;
import it.uniroma2.sel.ebpmn.logger.CSVLogger;
import it.uniroma2.sel.ebpmn.resources.Performer;

/**
 * Test 10 — Three-participant collaboration with parallel send tasks and receive tasks.
 *
 * Participant 1 "Seller":
 *   Start → CollectData → [split] → SendShippingInfo → [merge] → Finalize → End
 *                                  → SendBillingInfo  ↗
 *
 * Participant 2 "ShippingServices":
 *   Start → ReceiveOrderInfo → PrepareOrder → End
 *
 * Participant 3 "BillingServices":
 *   Start → ReceiveBillingInfo → PrepareBill → End
 *
 * Message flows:
 *   SendShippingInfo → ReceiveOrderInfo  (payload: "orderInfo")
 *   SendBillingInfo  → ReceiveBillingInfo (payload: "billingInfo")
 *
 * Resources:
 *   Seller:           ProcessManager (CollectData, SendShippingInfo, Finalize)
 *                     BillingManager (SendBillingInfo)
 *   ShippingServices: ShippingResource (ReceiveOrderInfo, PrepareOrder)
 *   BillingServices:  BillingResource  (ReceiveBillingInfo, PrepareBill)
 *
 * All service times: 1s (deterministic)
 * Seller interarrival: 10s — simulation ends after 10 tokens from Seller
 * Participant 2 & 3 interarrival: 5s
 */
public class Test10_ForkJoin {

    public static void main(String[] args) throws Exception {

        SimulationConfig config = SimulationConfig.load(
                "src/test/resources/simulationConfig_Test10.json");
        System.out.println(config.toString());

        ExecutionEngine engine = ExecutionEngine.initialize(config);

        CSVLogger logSeller   = new CSVLogger(config.getOutputFolder() + "test10_seller.csv");
        CSVLogger logShipping = new CSVLogger(config.getOutputFolder() + "test10_shipping.csv");
        CSVLogger logBilling  = new CSVLogger(config.getOutputFolder() + "test10_billing.csv");

        // -----------------------------------------------------------------------
        // Participants
        // -----------------------------------------------------------------------
        Participant seller   = new Participant("SellerOffice",           true);
        Participant shipping = new Participant("ShippingOffice", true);
        Participant billing  = new Participant("BillingOffice",  true);

        // -----------------------------------------------------------------------
        // Resources
        // -----------------------------------------------------------------------
        Performer processManager  = new Performer("ProcessManager",  "processmanager");
        Performer billingManager  = new Performer("BillingManager",  "accountmanager");
        Performer shippingResource = new Performer("ShippingResource", "shippingmanager");
        Performer billingResource  = new Performer("BillingResource",  "billingmanager");

        // -----------------------------------------------------------------------
        // Participant 1 — Seller
        // Start → CollectData → split → SendShippingInfo → merge → Finalize → End
        //                             → SendBillingInfo  ↗
        // -----------------------------------------------------------------------
        Start startSeller = new Start("StartSeller", seller,
                new DeterministicGenerator(10),
                config.getNumberOfTokens());

        Task collectData = new Task("CollectData", seller,
                new DeterministicGenerator(1));
        collectData.addResource(processManager);

        ParallelDivergingGateway split = new ParallelDivergingGateway("Split", seller);

        SendTask sendShippingInfo = new SendTask("SendShippingInfo", seller,
                new DeterministicGenerator(1));
        sendShippingInfo.addResource(processManager);

        SendTask sendBillingInfo = new SendTask("SendBillingInfo", seller,
                new DeterministicGenerator(1));
        sendBillingInfo.addResource(billingManager);

        ParallelConvergingGateway merge = new ParallelConvergingGateway("Merge", seller,2);

        Task finalize = new Task("Finalize", seller,
                new DeterministicGenerator(1));
        finalize.addResource(processManager);

        End endSeller = new End("EndSeller", seller);

        // Seller flow
        startSeller.addOutGoingEdge(collectData);
        collectData.addOutGoingEdge(split);
        split.addOutGoingEdge(sendBillingInfo);
       split.addOutGoingEdge(sendShippingInfo);
        sendShippingInfo.addOutGoingEdge(merge);
        sendBillingInfo.addOutGoingEdge(merge);
        merge.addOutGoingEdge(finalize);
        finalize.addOutGoingEdge(endSeller);

        // -----------------------------------------------------------------------
        // Participant 2 — ShippingServices
        // Start → ReceiveOrderInfo → PrepareOrder → End
        // -----------------------------------------------------------------------
        Start startShipping = new Start("StartShipping", shipping,
                new DeterministicGenerator(5),
                config.getNumberOfTokens());

        ReceiveTask receiveOrderInfo = new ReceiveTask("ReceiveOrderInfo", shipping,
                new DeterministicGenerator(1));
        receiveOrderInfo.addResource(shippingResource);

        Task prepareOrder = new Task("PrepareOrder", shipping,
                new DeterministicGenerator(1));
        prepareOrder.addResource(shippingResource);

        End endShipping = new End("EndShipping", shipping);

        startShipping.addOutGoingEdge(receiveOrderInfo);
        receiveOrderInfo.addOutGoingEdge(prepareOrder);
        prepareOrder.addOutGoingEdge(endShipping);

        // -----------------------------------------------------------------------
        // Participant 3 — BillingServices
        // Start → ReceiveBillingInfo → PrepareBill → End
        // -----------------------------------------------------------------------
        Start startBilling = new Start("StartBilling", billing,
                new DeterministicGenerator(5),
                config.getNumberOfTokens());

        ReceiveTask receiveBillingInfo = new ReceiveTask("ReceiveBillingInfo", billing,
                new DeterministicGenerator(1));
        receiveBillingInfo.addResource(billingResource);

        Task prepareBill = new Task("PrepareBill", billing,
                new DeterministicGenerator(1));
        prepareBill.addResource(billingResource);

        End endBilling = new End("EndBilling", billing);

        startBilling.addOutGoingEdge(receiveBillingInfo);
        receiveBillingInfo.addOutGoingEdge(prepareBill);
        prepareBill.addOutGoingEdge(endBilling);

        // -----------------------------------------------------------------------
        // Message flows
        // -----------------------------------------------------------------------
        sendShippingInfo.addMessageFlow(receiveOrderInfo,  "orderInfo");
        sendBillingInfo.addMessageFlow(receiveBillingInfo, "billingInfo");

        // -----------------------------------------------------------------------
        // Collaboration
        // -----------------------------------------------------------------------
        Collaboration model = new Collaboration("SellerCollaboration");
        model.addParticipant(seller);
        model.addParticipant(shipping);
        model.addParticipant(billing);

        engine.setModel(model);
        engine.setLogger(seller.getName(),   logSeller);
        engine.setLogger(shipping.getName(), logShipping);
        engine.setLogger(billing.getName(),  logBilling);
        engine.run();
    }
}
