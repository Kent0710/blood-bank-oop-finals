/**
 * Concrete Distribution Strategy for emergency and critical requisitions.
 * Routes blood requests via immediate urgent emergency courier, bypassing
 * scheduled transport batching to ensure rapid point-to-point hospital delivery.
 */
public class UrgentDistributionStrategy implements DistributionStrategy {

    private static final String DISTRIBUTION_MODE = "Immediate Urgent Emergency Courier";

    @Override
    public void dispatch(BloodRequest request) {
        if (request == null) {
            System.out.println("[Urgent Courier] Error: Cannot dispatch null blood request.");
            return;
        }

        System.out.println("================================================================================");
        System.out.println(">>> DISTRIBUTION STRATEGY ENGINE EXECUTION");
        System.out.println("================================================================================");
        System.out.println("Active Strategy : " + DISTRIBUTION_MODE);
        System.out.println("Routing Type    : Expedited Direct Point-to-Point Emergency Transit");
        System.out.println("Priority Level  : STAT / CRITICAL EMERGENCY");
        System.out.println("Dispatch Status : Dispatched immediately via dedicated emergency courier");
        System.out.println("Target Request  : " + request);
        System.out.println("================================================================================");
    }

    @Override
    public String getDistributionMode() {
        return DISTRIBUTION_MODE;
    }
}
