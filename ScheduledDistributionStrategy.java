/**
 * Concrete Distribution Strategy for routine, non-critical blood distributions.
 * Routes blood requests via scheduled regular transport batches using standard
 * logistics delivery windows (e.g., standard 48-hour batches).
 */
public class ScheduledDistributionStrategy implements DistributionStrategy {

    private static final String DISTRIBUTION_MODE = "Standard Scheduled Cold-Chain Transit";

    @Override
    public void dispatch(BloodRequest request) {
        if (request == null) {
            System.out.println("[Scheduled Transport] Error: Cannot dispatch null blood request.");
            return;
        }

        System.out.println("================================================================================");
        System.out.println(">>> DISTRIBUTION STRATEGY ENGINE EXECUTION");
        System.out.println("================================================================================");
        System.out.println("Active Strategy : " + DISTRIBUTION_MODE);
        System.out.println("Routing Type    : Standard Batch Transit / Refrigerated Fleet Route");
        System.out.println("Priority Level  : Normal (Routine 48-Hour Batch)");
        System.out.println("Dispatch Status : Queued into scheduled distribution route");
        System.out.println("Target Request  : " + request);
        System.out.println("================================================================================");
    }

    @Override
    public String getDistributionMode() {
        return DISTRIBUTION_MODE;
    }

    @Override
    public String getModeName() {
        return DISTRIBUTION_MODE;
    }

    @Override
    public void prepareForDispatch(BloodRequest request, BloodUnit unit) {
        System.out.println(">> [Distribution Strategy - Scheduled] Packing unit DIN " + (unit != null ? unit.getDin() : "N/A") +
                " (" + (unit != null && unit.getComponent() != null ? unit.getComponent().getDisplayName() : "Blood") +
                ") into standard temperature-controlled cold-chain container (2°C - 6°C)...");
        System.out.println(">> [Distribution Strategy - Scheduled] Assigned standard delivery route to " +
                (request != null ? request.getHospitalName() : "Hospital") + ".");
    }

    @Override
    public void executeDelivery(BloodRequest request, BloodUnit unit) {
        System.out.println(">> [Distribution Strategy - Scheduled] Transport completed. Verified arrival at " +
                (request != null ? request.getHospitalName() : "Hospital") + " for scheduled transfusion.");
    }
}
