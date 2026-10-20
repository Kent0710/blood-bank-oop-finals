import java.util.List;

/**
 * Strategy interface for allocating blood units from inventory.
 * Supports algorithms such as FIFO (First In, First Out) and
 * FEFO (First Expired, First Out).
 */
public interface AllocationStrategy {
    BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType) throws InsufficientStockException;
    String getStrategyName();
}
