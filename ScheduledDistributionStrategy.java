/**
 * Strategy implementation for routine, non-critical blood distributions.
 * Routes blood requests via scheduled regular transport batches using standard
 * logistics delivery windows.
 */
public class ScheduledDistributionStrategy implements DistributionStrategy {

    private static final String DISTRIBUTION_MODE = "Scheduled Regular Transport";

    @Override
    public void dispatch(BloodRequest request) {
        if (request == null) {
            System.out.println("[Scheduled Transport] Error: Cannot dispatch null blood request.");
            return;
        }

        System.out.println("================================================================================");
        System.out.println("                 ROUTINE LOGISTICS ROUTING: SCHEDULED TRANSPORT                 ");
        System.out.println("================================================================================");
        System.out.println("  Mode             : " + DISTRIBUTION_MODE);
        System.out.println("  Routing Type     : Standard Batch Transit / Refrigerated Fleet Route");
        System.out.println("  Priority Level   : Standard (Non-Emergency)");
        System.out.println("  Dispatch Status  : Queued into upcoming scheduled distribution batch");
        System.out.println("  Consignment Details: " + request);
        System.out.println("================================================================================");
    }

    @Override
    public String getDistributionMode() {
        return DISTRIBUTION_MODE;
    }
}
