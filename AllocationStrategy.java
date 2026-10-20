public interface AllocationStrategy {
    BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType) throws InsufficientStockException;
    String getStrategyName();
}