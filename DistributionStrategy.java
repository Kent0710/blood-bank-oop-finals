/**
 * Strategy interface for dispatching and distributing blood consignments to hospitals.
 * Supports scheduled regular transport and immediate urgent emergency courier routing.
 */
public interface DistributionStrategy {
    void dispatch(BloodRequest request);
    String getDistributionMode();

    default String getModeName() {
        return getDistributionMode();
    }

    default void prepareForDispatch(BloodRequest request, BloodUnit unit) {
        dispatch(request);
    }

    default void executeDelivery(BloodRequest request, BloodUnit unit) {
        // Concrete strategies override for delivery confirmation
    }
}
