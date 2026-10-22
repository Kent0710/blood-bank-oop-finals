import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Rigorous Unit Test Suite for Issue #15:
 * Dynamic Inventory Low-Stock Thresholds and Strategy Switcher.
 */
public class Sprint4InventoryUnitTest {

    public static void main(String[] args) {
        System.out.println(">>> RUNNING SPRINT 4 INVENTORY & STRATEGY UNIT TESTS <<<\n");

        testLowStockAlertBanner();
        testViewInventoryAndQuarantineLedger();
        testThresholdConfigurationConsole();
        testAllocationStrategySwitcherConsole();
        testPerformAuditAndPurge();

        System.out.println("\n[SUCCESS] ALL SPRINT 4 INVENTORY & STRATEGY TESTS PASSED WITH 100% SUCCESS!");
    }

    private static void testLowStockAlertBanner() {
        System.out.println("[TEST 1] Testing Low-Stock Alert Detection and Banner...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();
        inv.setLowStockThreshold(5);

        // Initially inventory is empty, so all types have 0 < 5
        if (!inv.hasLowStockAlerts()) {
            throw new AssertionError("Empty inventory should trigger low-stock alerts");
        }
        if (inv.getLowStockBloodTypes().size() != BloodType.values().length) {
            throw new AssertionError("All blood types should be below threshold when empty");
        }

        // Add 5 units of O+ and 1 unit of O-
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 5; i++) {
            inv.addUnit(new BloodUnit("BU-OP-" + i, BloodType.O_POS, BloodComponent.PACKED_RED_CELLS, today, "Donor"));
        }
        inv.addUnit(new BloodUnit("BU-ON-1", BloodType.O_NEG, BloodComponent.PACKED_RED_CELLS, today, "Donor"));

        if (inv.getLowStockBloodTypes().contains(BloodType.O_POS)) {
            throw new AssertionError("O+ has 5 units and meets threshold of 5, should not be low stock");
        }
        if (!inv.getLowStockBloodTypes().contains(BloodType.O_NEG)) {
            throw new AssertionError("O- has only 1 unit and should trigger low stock alert");
        }

        System.out.println("Printing low-stock alerts:");
        inv.printLowStockAlerts();
        System.out.println("  -> Low-stock alert detection PASSED.");
    }

    private static void testViewInventoryAndQuarantineLedger() {
        System.out.println("\n[TEST 2] Testing View Inventory and Quarantine Ledgers (TableRenderer)...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        inv.addUnit(new BloodUnit("BU-1049", BloodType.O_POS, BloodComponent.PACKED_RED_CELLS, today, "Donor Hospital"));
        BloodUnit expiredUnit = new BloodUnit("BU-9999", BloodType.A_NEG, BloodComponent.WHOLE_BLOOD, today.minusDays(50), "Donor Clinic");
        inv.addUnit(expiredUnit); // Goes to quarantine

        // Call SuperAdminController view methods
        SuperAdminController.viewInventoryLedger();
        SuperAdminController.viewQuarantineLedger();

        System.out.println("  -> Inventory and Quarantine ledger views PASSED.");
    }

    private static void testThresholdConfigurationConsole() {
        System.out.println("\n[TEST 3] Testing Option [8] setLowStockThreshold Console Input...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        String simulatedInput = "10\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            Scanner sc = new Scanner(System.in);
            SuperAdminController.setLowStockThreshold(sc);

            if (inv.getLowStockThreshold() != 10) {
                throw new AssertionError("Expected low-stock threshold 10, got: " + inv.getLowStockThreshold());
            }
        } finally {
            System.setIn(originalIn);
        }

        System.out.println("  -> Threshold configuration console input PASSED.");
    }

    private static void testAllocationStrategySwitcherConsole() {
        System.out.println("\n[TEST 4] Testing Option [9] switchAllocationStrategy Console Input...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        // 1. Switch to FEFO (choice 2)
        String simulatedInput = "2\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            Scanner sc = new Scanner(System.in);
            SuperAdminController.switchAllocationStrategy(sc);

            if (!(inv.getAllocationStrategy() instanceof FefoAllocationStrategy)) {
                throw new AssertionError("Strategy should have been updated to FEFO");
            }
        } finally {
            System.setIn(originalIn);
        }

        // 2. Switch back to FIFO (choice 1)
        simulatedInput = "1\n";
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            Scanner sc = new Scanner(System.in);
            SuperAdminController.switchAllocationStrategy(sc);

            if (!(inv.getAllocationStrategy() instanceof FifoAllocationStrategy)) {
                throw new AssertionError("Strategy should have been updated to FIFO");
            }
        } finally {
            System.setIn(originalIn);
        }

        System.out.println("  -> Strategy switcher console input PASSED.");
    }

    private static void testPerformAuditAndPurge() {
        System.out.println("\n[TEST 5] Testing Option [7] performAuditAndPurge...");
        CentralInventory.resetInstance();
        CentralInventory inv = CentralInventory.getInstance();

        LocalDate today = LocalDate.now();
        inv.getActiveUnits().add(new BloodUnit("BU-EXP-1", BloodType.B_POS, BloodComponent.WHOLE_BLOOD, today.minusDays(45), "Donor"));
        inv.getActiveUnits().add(new BloodUnit("BU-ACT-1", BloodType.B_POS, BloodComponent.WHOLE_BLOOD, today, "Donor"));

        SuperAdminController.performAuditAndPurge();

        if (inv.getActiveUnits().size() != 1) {
            throw new AssertionError("Active units should only retain 1 active unit");
        }
        if (inv.getQuarantineUnits().size() != 1) {
            throw new AssertionError("Quarantine units should have 1 purged unit");
        }

        System.out.println("  -> performAuditAndPurge execution PASSED.");
    }
}
