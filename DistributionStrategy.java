public interface DistributionStrategy {
    void dispatch(BloodRequest request);
    String getDistributionMode();
}