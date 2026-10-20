public class FifoAllocationStrategy implements AllocationStrategy {
    @Override
    public BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType) throws InsufficientStockException {
        // Filters active units by requestedType, sorts by donationDate ASC (oldest first)
    }
    @Override
    public String getStrategyName() {
        return "FIFO (First In, First Out)";
    }
}