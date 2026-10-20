/**
 * Strategy interface for dispatching and distributing blood consignments to hospitals.
 * Supports scheduled regular transport and immediate urgent emergency courier routing.
 */
public interface DistributionStrategy {
    void dispatch(BloodRequest request);
    String getDistributionMode();
}
