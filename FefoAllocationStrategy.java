public class FefoAllocationStrategy implements AllocationStrategy {
    @Override
    public BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType) throws InsufficientStockException {
        // Filters active units by requestedType, sorts by expirationDate ASC (earliest expiring first)
    }
    @Override
    public String getStrategyName() {
        return "FEFO (First Expired, First Out)";
    }
}