import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Rigorous Unit Test for CentralInventory (Issue #7).
 */
public class CentralInventoryUnitTest {

    public static void main(String[] args) {
        System.out.println(">>> RUNNING RIGOROUS CENTRAL INVENTORY UNIT TESTS <<<\n");

        testSingletonInstance();
        testAddAndSegmentUnits();
        testAllocateUnitWithStrategy();
        testAuditAndPurgeExpiredStock();
        testStockAggregation();
        testStrategySwitchingAndThresholds();

        System.out.println("\n[SUCCESS] ALL CENTRAL INVENTORY UNIT TESTS PASSED WITH 100% SUCCESS!");
    }

    private static void testSingletonInstance() {
        System.out.println("[TEST 1] Testing Singleton Pattern...");
        CentralInventory.resetInstance();
        CentralInventory inv1 = CentralInventory.getInstance();
        CentralInventory inv2 = CentralInventory.getInstance();

        if (inv1 == null || inv2 == null || inv1 != inv2) {
            throw new AssertionError("Singleton getInstance() failed: instances are not identical!");
        }
        System.out.println("  -> Singleton verification PASSED.");
    }

    private static void testAddAndSegmentUnits() {
        System.out.println("[TEST 2] Testing Unit Adding and Segmentation...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        BloodUnit validUnit = new BloodUnit("BU-1001", BloodType.O_POS, BloodComponent.PACKED_RED_CELLS, today.minusDays(5), "Red Cross");
        BloodUnit expiredUnit = new BloodUnit("BU-1002", BloodType.A_POS, BloodComponent.WHOLE_BLOOD, today.minusDays(50), "Hospital Donor");

        inv.addUnit(validUnit);
        inv.addUnit(expiredUnit);

        if (!inv.getActiveUnits().contains(validUnit)) {
            throw new AssertionError("Valid unit should be in activeUnits");
        }
        if (!inv.getQuarantineUnits().contains(expiredUnit)) {
            throw new AssertionError("Already expired unit should be added directly to quarantineUnits");
        }
        if (expiredUnit.getStatus() != UnitStatus.EXPIRED) {
            throw new AssertionError("Expired unit status should be set to EXPIRED");
        }
        System.out.println("  -> Unit segmentation PASSED.");
    }

    private static void testAllocateUnitWithStrategy() {
        System.out.println("[TEST 3] Testing Unit Allocation via Strategy...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        BloodUnit olderOpos = new BloodUnit("BU-2001", BloodType.O_POS, BloodComponent.PACKED_RED_CELLS, today.minusDays(10), "Source A");
        BloodUnit newerOpos = new BloodUnit("BU-2002", BloodType.O_POS, BloodComponent.PACKED_RED_CELLS, today.minusDays(2), "Source B");

        inv.addUnit(newerOpos);
        inv.addUnit(olderOpos);

        try {
            BloodUnit allocated = inv.allocateUnit(BloodType.O_POS);
            if (!"BU-2001".equals(allocated.getUnitId())) {
                throw new AssertionError("FIFO Allocation failed: expected BU-2001 (older) but got " + allocated.getUnitId());
            }
            if (allocated.getStatus() != UnitStatus.RESERVED) {
                throw new AssertionError("Allocated unit status should be RESERVED");
            }
            if (inv.getActiveUnits().contains(allocated)) {
                throw new AssertionError("Allocated unit must be removed from activeUnits");
            }
        } catch (InsufficientStockException e) {
            throw new AssertionError("Unexpected InsufficientStockException: " + e.getMessage());
        }

        // Test Insufficient Stock Exception
        try {
            inv.allocateUnit(BloodType.AB_NEG);
            throw new AssertionError("Expected InsufficientStockException for unavailable blood type AB-");
        } catch (InsufficientStockException expected) {
            System.out.println("  -> InsufficientStockException caught as expected: " + expected.getMessage());
        }

        System.out.println("  -> Unit allocation verification PASSED.");
    }

    private static void testAuditAndPurgeExpiredStock() {
        System.out.println("[TEST 4] Testing Audit & Purge Expired Stock...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        // Create unit that expires in the future
        BloodUnit freshUnit = new BloodUnit("BU-3001", BloodType.B_POS, BloodComponent.PLASMA, today, "Donor");
        // Create unit whose expirationDate is forced to yesterday
        BloodUnit expiringUnit1 = new BloodUnit("BU-3002", BloodType.B_NEG, BloodComponent.WHOLE_BLOOD, today.minusDays(43), "Donor");
        BloodUnit expiringUnit2 = new BloodUnit("BU-3003", BloodType.O_NEG, BloodComponent.PLATELETS, today.minusDays(45), "Donor");

        // Manually add to activeUnits to simulate units that expired while sitting on shelf
        inv.getActiveUnits().add(freshUnit);
        inv.getActiveUnits().add(expiringUnit1);
        inv.getActiveUnits().add(expiringUnit2);

        int purgedCount = inv.auditAndPurgeExpiredStock();
        if (purgedCount != 2) {
            throw new AssertionError("Expected 2 expired units purged, but got " + purgedCount);
        }
        if (inv.getActiveUnits().size() != 1 || !inv.getActiveUnits().contains(freshUnit)) {
            throw new AssertionError("Active units should only retain freshUnit");
        }
        if (inv.getQuarantineUnits().size() != 2) {
            throw new AssertionError("Quarantine units should have received 2 expired units");
        }
        if (expiringUnit1.getStatus() != UnitStatus.EXPIRED || expiringUnit2.getStatus() != UnitStatus.EXPIRED) {
            throw new AssertionError("Purged units must have status EXPIRED");
        }

        System.out.println("  -> Audit & Purge verification PASSED.");
    }

    private static void testStockAggregation() {
        System.out.println("[TEST 5] Testing Stock Aggregation Map...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        inv.addUnit(new BloodUnit("BU-4001", BloodType.A_POS, BloodComponent.PACKED_RED_CELLS, today, "Donor"));
        inv.addUnit(new BloodUnit("BU-4002", BloodType.A_POS, BloodComponent.PACKED_RED_CELLS, today, "Donor"));
        inv.addUnit(new BloodUnit("BU-4003", BloodType.O_NEG, BloodComponent.PACKED_RED_CELLS, today, "Donor"));

        Map<BloodType, Long> counts = inv.getStockCounts();
        if (counts.get(BloodType.A_POS) != 2L) {
            throw new AssertionError("Expected 2 units of A+, found " + counts.get(BloodType.A_POS));
        }
        if (counts.get(BloodType.O_NEG) != 1L) {
            throw new AssertionError("Expected 1 unit of O-, found " + counts.get(BloodType.O_NEG));
        }
        if (counts.get(BloodType.B_POS) != 0L) {
            throw new AssertionError("Expected 0 units of B+, found " + counts.get(BloodType.B_POS));
        }

        System.out.println("  -> Stock counts aggregation PASSED.");
    }

    private static void testStrategySwitchingAndThresholds() {
        System.out.println("[TEST 6] Testing Strategy Switching and Thresholds...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        if (!(inv.getAllocationStrategy() instanceof FifoAllocationStrategy)) {
            throw new AssertionError("Default allocation strategy should be FIFO");
        }

        inv.setAllocationStrategy(new FefoAllocationStrategy());
        if (!(inv.getAllocationStrategy() instanceof FefoAllocationStrategy)) {
            throw new AssertionError("Allocation strategy failed to update to FEFO");
        }

        if (inv.getLowStockThreshold() != 5) {
            throw new AssertionError("Default low stock threshold should be 5");
        }
        inv.setLowStockThreshold(10);
        if (inv.getLowStockThreshold() != 10) {
            throw new AssertionError("Low stock threshold should be updated to 10");
        }

        System.out.println("  -> Strategy switching and thresholds PASSED.");
    }
}
