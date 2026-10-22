import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe Central Inventory Singleton managing blood units,
 * active/quarantine stock segmentation, automated expiration audits,
 * and dynamic allocation strategy dispatching.
 */
public class CentralInventory {

    private static CentralInventory instance;

    private final List<BloodUnit> activeUnits = new ArrayList<>();
    private final List<BloodUnit> quarantineUnits = new ArrayList<>();
    private AllocationStrategy allocationStrategy = new FifoAllocationStrategy();
    private int lowStockThreshold = 5;

    /**
     * Private constructor enforcing the Singleton pattern.
     */
    private CentralInventory() {
    }

    /**
     * Retrieves the thread-safe global instance of CentralInventory.
     *
     * @return the singleton CentralInventory instance
     */
    public static synchronized CentralInventory getInstance() {
        if (instance == null) {
            instance = new CentralInventory();
        }
        return instance;
    }

    /**
     * Resets the singleton instance and stored units (primarily for testing and clean re-initialization).
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.activeUnits.clear();
            instance.quarantineUnits.clear();
            instance.allocationStrategy = new FifoAllocationStrategy();
            instance.lowStockThreshold = 5;
        }
        instance = null;
    }

    /**
     * Adds a blood unit to inventory.
     * Expired or quarantined units are directed to quarantine storage;
     * active, non-expired units are placed in active inventory.
     *
     * @param unit the BloodUnit to add
     */
    public synchronized void addUnit(BloodUnit unit) {
        if (unit == null) {
            return;
        }

        LocalDate expirationDate = unit.getExpirationDate();
        boolean isExpired = expirationDate != null && expirationDate.isBefore(LocalDate.now());

        if (isExpired) {
            unit.setStatus(UnitStatus.EXPIRED);
            if (!quarantineUnits.contains(unit)) {
                quarantineUnits.add(unit);
            }
            activeUnits.remove(unit);
        } else if (unit.getStatus() == UnitStatus.QUARANTINED || unit.getStatus() == UnitStatus.EXPIRED || unit.getStatus() == UnitStatus.PURGED) {
            if (!quarantineUnits.contains(unit)) {
                quarantineUnits.add(unit);
            }
            activeUnits.remove(unit);
        } else {
            unit.setStatus(UnitStatus.ACTIVE);
            if (!activeUnits.contains(unit)) {
                activeUnits.add(unit);
            }
        }
    }

    /**
     * Allocates a blood unit matching the requested blood type delegating
     * to the currently configured AllocationStrategy (e.g. FIFO or FEFO).
     * The allocated unit is transitioned from active inventory to RESERVED status.
     *
     * @param type the requested BloodType
     * @return the allocated BloodUnit
     * @throws InsufficientStockException if no compatible active units exist
     */
    public synchronized BloodUnit allocateUnit(BloodType type) throws InsufficientStockException {
        if (type == null) {
            throw new InsufficientStockException("Cannot allocate: requested blood type cannot be null.");
        }

        if (allocationStrategy == null) {
            allocationStrategy = new FifoAllocationStrategy();
        }

        BloodUnit allocated = allocationStrategy.allocate(activeUnits, type);
        if (allocated != null) {
            activeUnits.remove(allocated);
            allocated.setStatus(UnitStatus.RESERVED);
        }

        return allocated;
    }

    /**
     * Overloaded convenience method for allocating with a specific request ID for tracking.
     *
     * @param type the requested BloodType
     * @param requestId the requesting hospital requisition ID
     * @return the allocated BloodUnit
     * @throws InsufficientStockException if no units are available
     */
    public synchronized BloodUnit allocateUnit(BloodType type, String requestId) throws InsufficientStockException {
        if (type == null) {
            throw new InsufficientStockException("Cannot allocate: requested blood type cannot be null.");
        }

        if (allocationStrategy instanceof FifoAllocationStrategy) {
            BloodUnit allocated = ((FifoAllocationStrategy) allocationStrategy).allocate(activeUnits, type, requestId);
            if (allocated != null) {
                activeUnits.remove(allocated);
                allocated.setStatus(UnitStatus.RESERVED);
            }
            return allocated;
        }

        return allocateUnit(type);
    }

    /**
     * Audits all active blood units, automatically identifies units whose expirationDate
     * precedes the current date, updates their status to UnitStatus.EXPIRED,
     * and relocates them to quarantine storage.
     * Prints an ASCII audit log summary.
     *
     * @return the count of expired units purged and quarantined
     */
    public synchronized int auditAndPurgeExpiredStock() {
        LocalDate today = LocalDate.now();
        int activeEvaluated = activeUnits.size();
        List<BloodUnit> expiredList = new ArrayList<>();

        for (BloodUnit unit : activeUnits) {
            if (unit != null) {
                LocalDate exp = unit.getExpirationDate();
                if (exp != null && exp.isBefore(today)) {
                    expiredList.add(unit);
                }
            }
        }

        int expiredCount = expiredList.size();
        for (BloodUnit expUnit : expiredList) {
            activeUnits.remove(expUnit);
            expUnit.setStatus(UnitStatus.EXPIRED);
            if (!quarantineUnits.contains(expUnit)) {
                quarantineUnits.add(expUnit);
            }
        }

        printPurgeAuditLog(today, activeEvaluated, expiredCount);

        return expiredCount;
    }

    /**
     * Aggregates stock counts across all ABO/Rh blood types for active units.
     *
     * @return map of BloodType to current available count
     */
    public synchronized Map<BloodType, Long> getStockCounts() {
        Map<BloodType, Long> counts = new LinkedHashMap<>();
        for (BloodType bt : BloodType.values()) {
            counts.put(bt, 0L);
        }

        for (BloodUnit unit : activeUnits) {
            if (unit != null && unit.getStatus() == UnitStatus.ACTIVE && unit.getBloodType() != null) {
                counts.put(unit.getBloodType(), counts.get(unit.getBloodType()) + 1L);
            }
        }

        return counts;
    }

    /**
     * Identifies blood types whose active stock count falls below the lowStockThreshold.
     *
     * @return list of BloodTypes with critical/low inventory
     */
    public synchronized List<BloodType> getLowStockBloodTypes() {
        List<BloodType> lowStock = new ArrayList<>();
        Map<BloodType, Long> counts = getStockCounts();
        for (Map.Entry<BloodType, Long> entry : counts.entrySet()) {
            if (entry.getValue() < lowStockThreshold) {
                lowStock.add(entry.getKey());
            }
        }
        return lowStock;
    }

    /**
     * Returns true if any blood type active count is below the low stock threshold.
     */
    public synchronized boolean hasLowStockAlerts() {
        return !getLowStockBloodTypes().isEmpty();
    }

    /**
     * Renders the critical red alert banner for blood types below safety threshold.
     */
    public synchronized void printLowStockAlerts() {
        List<BloodType> lowStock = getLowStockBloodTypes();
        if (lowStock.isEmpty()) {
            return;
        }

        Map<BloodType, Long> counts = getStockCounts();
        System.out.println("================================================================================");
        System.out.printf("  [CRITICAL ALERT] The following blood types are BELOW safety threshold (%d units):%n", lowStockThreshold);
        for (BloodType bt : lowStock) {
            long count = counts.getOrDefault(bt, 0L);
            String urgency = (count == 0) ? "(CRITICAL STOCK DEPLETION)" : "(URGENT REPLENISHMENT REQUIRED)";
            System.out.printf("    - %-4s : %d units available %s%n", bt.getCode(), count, urgency);
        }
        System.out.println("================================================================================");
    }

    // Dynamic Strategy & Configuration Accessors

    public synchronized void setAllocationStrategy(AllocationStrategy strategy) {
        if (strategy != null) {
            this.allocationStrategy = strategy;
        }
    }

    public synchronized AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }

    public synchronized void setLowStockThreshold(int threshold) {
        this.lowStockThreshold = threshold;
    }

    public synchronized int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public synchronized List<BloodUnit> getActiveUnits() {
        return activeUnits;
    }

    public synchronized List<BloodUnit> getQuarantineUnits() {
        return quarantineUnits;
    }

    private void printPurgeAuditLog(LocalDate date, int evaluated, int expired) {
        System.out.println("================================================================================");
        System.out.println("  >>> INVENTORY PURGE & AUDIT LOG");
        System.out.println("================================================================================");
        System.out.printf("  Current Date           : %s%n", date);
        System.out.printf("  Active Units Evaluated : %d units%n", evaluated);
        System.out.printf("  Expired Units Detected : %d units%n", expired);
        System.out.printf("  Action Taken           : Moved %d units to QUARANTINED stock (Status: EXPIRED)%n", expired);
        System.out.println("================================================================================");
    }
}
