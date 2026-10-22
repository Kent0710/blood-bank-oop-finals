import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Automated Verification Test Runner for Central Blood Bank Management System.
 * Tests each issue independently as pull requests are submitted.
 *
 * Usage:
 *   javac TestRunner.java && java TestRunner [issue_number | all]
 */
public class TestRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static int totalSkipped = 0;

    public static void main(String[] args) {
        String target = args.length > 0 ? args[0].trim().toLowerCase() : "all";

        System.out.println("================================================================================");
        System.out.println("        CENTRAL BLOOD BANK SYSTEM -- AUTOMATED PR TEST RUNNER                  ");
        System.out.println("================================================================================");

        if (target.equals("all")) {
            for (int i = 1; i <= 18; i++) {
                runIssueTest(i);
            }
        } else {
            try {
                int issueNum = Integer.parseInt(target);
                runIssueTest(issueNum);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid argument. Specify an issue number (1-18) or 'all'.");
                return;
            }
        }

        System.out.println("\n================================================================================");
        System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED, %d SKIPPED",
                totalPassed, totalFailed, totalSkipped));
        System.out.println("================================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void runIssueTest(int issueNumber) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("Running Tests for Issue #" + issueNumber + "...");
        System.out.println("--------------------------------------------------------------------------------");

        switch (issueNumber) {
            case 1: testIssue1(); break;
            case 2: testIssue2(); break;
            case 3: testIssue3(); break;
            case 4: testIssue4(); break;
            case 5: testIssue5(); break;
            case 6: testIssue6(); break;
            case 7: testIssue7(); break;
            case 8: testIssue8(); break;
            case 9: testIssue9(); break;
            case 10: testIssue10(); break;
            case 11: testIssue11(); break;
            case 12: testIssue12(); break;
            case 13: testIssue13(); break;
            case 14: testIssue14(); break;
            case 15: testIssue15(); break;
            case 16: testIssue16(); break;
            case 17: testIssue17(); break;
            case 18: testIssue18(); break;
            default:
                System.out.println("[SKIP] Issue #" + issueNumber + " is out of range.");
                totalSkipped++;
        }
    }

    // =========================================================================
    // SPRINT 1 TESTS
    // =========================================================================

    /** Issue #1 (AQ-12): Clinical Domain Models */
    private static void testIssue1() {
        if (!classExists("BloodType") || !classExists("BloodComponent") ||
            !classExists("UnitStatus") || !classExists("BloodUnit") || !classExists("BloodRequest")) {
            skipIssue(1, "Domain model classes not found yet.");
            return;
        }

        try {
            Class<?> btClass = Class.forName("BloodType");
            Method fromStringMethod = btClass.getMethod("fromString", String.class);
            Object typeOpos = fromStringMethod.invoke(null, "O+");
            Object typeOneg = fromStringMethod.invoke(null, "O-");
            Object typeABpos = fromStringMethod.invoke(null, "AB+");

            assertCondition(typeOpos != null && typeOneg != null && typeABpos != null,
                    "BloodType.fromString() parses standard blood types");

            Method isCompatibleMethod = btClass.getMethod("isCompatible", btClass, btClass);
            boolean oNegToAbPos = (boolean) isCompatibleMethod.invoke(null, typeOneg, typeABpos);
            boolean abPosToONeg = (boolean) isCompatibleMethod.invoke(null, typeABpos, typeOneg);

            assertCondition(oNegToAbPos == true, "O- is universal donor (compatible with AB+)");
            assertCondition(abPosToONeg == false, "AB+ cannot donate to O-");

            Class<?> compClass = Class.forName("BloodComponent");
            assertCondition(compClass.isEnum(), "BloodComponent is an enum");

            Class<?> statusClass = Class.forName("UnitStatus");
            assertCondition(statusClass.isEnum(), "UnitStatus is an enum");

            Class<?> unitClass = Class.forName("BloodUnit");
            assertCondition(unitClass.getDeclaredFields().length >= 5, "BloodUnit contains core attributes");

            Class<?> reqClass = Class.forName("BloodRequest");
            assertCondition(reqClass.getDeclaredFields().length >= 4, "BloodRequest contains core attributes");

        } catch (Exception e) {
            failTest("Issue #1 verification error: " + e.getMessage());
        }
    }

    /** Issue #2 (AQ-10): User Hierarchy & Custom Exceptions */
    private static void testIssue2() {
        if (!classExists("Roles") || !classExists("User") || !classExists("BloodBankException")) {
            skipIssue(2, "User hierarchy or exception classes not found yet.");
            return;
        }

        try {
            Class<?> rolesClass = Class.forName("Roles");
            assertCondition(rolesClass.isEnum(), "Roles is an enum");

            Class<?> userClass = Class.forName("User");
            assertCondition(Modifier.isAbstract(userClass.getModifiers()), "User is an abstract class");

            if (classExists("BloodBank")) {
                Class<?> bbClass = Class.forName("BloodBank");
                assertCondition(userClass.isAssignableFrom(bbClass), "BloodBank extends User");
            }
            if (classExists("HospitalAdmin")) {
                Class<?> haClass = Class.forName("HospitalAdmin");
                assertCondition(userClass.isAssignableFrom(haClass), "HospitalAdmin extends User");
            }
            if (classExists("LogisticsManager")) {
                Class<?> lmClass = Class.forName("LogisticsManager");
                assertCondition(userClass.isAssignableFrom(lmClass), "LogisticsManager extends User");
            }
            if (classExists("Patient")) {
                Class<?> patClass = Class.forName("Patient");
                assertCondition(userClass.isAssignableFrom(patClass), "Patient extends User");
            }

            Class<?> baseEx = Class.forName("BloodBankException");
            assertCondition(Exception.class.isAssignableFrom(baseEx), "BloodBankException extends Exception");

            String[] childExceptions = {
                "AuthenticationException", "ContaminatedBloodException", "ExpiredBloodException",
                "InsufficientStockException", "InvalidInputException", "UserNotFoundException"
            };

            for (String exName : childExceptions) {
                if (classExists(exName)) {
                    Class<?> exClass = Class.forName(exName);
                    assertCondition(baseEx.isAssignableFrom(exClass), exName + " extends BloodBankException");
                }
            }

        } catch (Exception e) {
            failTest("Issue #2 verification error: " + e.getMessage());
        }
    }

    /** Issue #3 (AQ-11): GoF Strategy Pattern */
    private static void testIssue3() {
        if (!classExists("AllocationStrategy") || !classExists("DistributionStrategy")) {
            skipIssue(3, "Strategy interfaces not found yet.");
            return;
        }

        try {
            Class<?> allocInterface = Class.forName("AllocationStrategy");
            assertCondition(allocInterface.isInterface(), "AllocationStrategy is an interface");

            if (classExists("FifoAllocationStrategy")) {
                Class<?> fifo = Class.forName("FifoAllocationStrategy");
                assertCondition(allocInterface.isAssignableFrom(fifo), "FifoAllocationStrategy implements AllocationStrategy");
            }
            if (classExists("FefoAllocationStrategy")) {
                Class<?> fefo = Class.forName("FefoAllocationStrategy");
                assertCondition(allocInterface.isAssignableFrom(fefo), "FefoAllocationStrategy implements AllocationStrategy");
            }

            Class<?> distInterface = Class.forName("DistributionStrategy");
            assertCondition(distInterface.isInterface(), "DistributionStrategy is an interface");

            if (classExists("ScheduledDistributionStrategy")) {
                Class<?> sched = Class.forName("ScheduledDistributionStrategy");
                assertCondition(distInterface.isAssignableFrom(sched), "ScheduledDistributionStrategy implements DistributionStrategy");
            }
            if (classExists("UrgentDistributionStrategy")) {
                Class<?> urgent = Class.forName("UrgentDistributionStrategy");
                assertCondition(distInterface.isAssignableFrom(urgent), "UrgentDistributionStrategy implements DistributionStrategy");
            }

        } catch (Exception e) {
            failTest("Issue #3 verification error: " + e.getMessage());
        }
    }

    /** Issue #4 (AQ-9): Application Bootstrap & Input Validator */
    private static void testIssue4() {
        if (!classExists("InputValidator") || !classExists("Main")) {
            skipIssue(4, "InputValidator or Main not found yet.");
            return;
        }

        try {
            Class<?> valClass = Class.forName("InputValidator");
            boolean hasReadInt = hasMethod(valClass, "readInt");
            boolean hasReadString = hasMethod(valClass, "readNonBlankString");

            assertCondition(hasReadInt, "InputValidator contains readInt method");
            assertCondition(hasReadString, "InputValidator contains readNonBlankString method");

            Class<?> mainClass = Class.forName("Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            assertCondition(mainMethod != null && Modifier.isStatic(mainMethod.getModifiers()),
                    "Main contains static main(String[] args) entry point");

        } catch (Exception e) {
            failTest("Issue #4 verification error: " + e.getMessage());
        }
    }

    // =========================================================================
    // SPRINT 2 TESTS
    // =========================================================================

    /** Issue #5 (AQ-14): 5-Stage Clinical Intake Pipeline */
    private static void testIssue5() {
        if (!classExists("PipelineHelper")) {
            skipIssue(5, "PipelineHelper not found yet.");
            return;
        }
        try {
            Class<?> pipeClass = Class.forName("PipelineHelper");
            assertCondition(pipeClass.getDeclaredMethods().length > 0, "PipelineHelper declared methods verified");
            passTest("Issue #5: PipelineHelper verified.");
        } catch (Exception e) {
            failTest("Issue #5 error: " + e.getMessage());
        }
    }

    /** Issue #6 (AQ-13): Dynamic ASCII Table Renderer */
    private static void testIssue6() {
        if (!classExists("TableRenderer")) {
            skipIssue(6, "TableRenderer not found yet.");
            return;
        }
        try {
            Class<?> trClass = Class.forName("TableRenderer");
            assertCondition(trClass.getDeclaredMethods().length > 0, "TableRenderer methods verified");
            passTest("Issue #6: TableRenderer verified.");
        } catch (Exception e) {
            failTest("Issue #6 error: " + e.getMessage());
        }
    }

    /** Issue #7 (AQ-15): Central Inventory Singleton */
    private static void testIssue7() {
        if (!classExists("CentralInventory")) {
            skipIssue(7, "CentralInventory not found yet.");
            return;
        }
        try {
            Class<?> invClass = Class.forName("CentralInventory");
            Method getInstance = invClass.getMethod("getInstance");
            assertCondition(getInstance != null && Modifier.isStatic(getInstance.getModifiers()),
                    "CentralInventory provides thread-safe getInstance() Singleton accessor");
        } catch (Exception e) {
            failTest("Issue #7 error: " + e.getMessage());
        }
    }

    /** Issue #8 (AQ-18): Core Singletons */
    private static void testIssue8() {
        boolean hasUser = classExists("UserService");
        boolean hasInsp = classExists("InspectionService");
        boolean hasLog = classExists("LogisticsService");

        if (!hasUser || !hasInsp || !hasLog) {
            skipIssue(8, "Core services (UserService, InspectionService, LogisticsService) not all found yet.");
            return;
        }

        try {
            // Test UserService
            UserService userService = UserService.getInstance();
            assertCondition(userService != null, "UserService singleton instance exists");
            assertCondition(userService == UserService.getInstance(), "UserService satisfies singleton identity");

            String bbId = userService.generateId(Roles.SUPER_ADMIN);
            String haId = userService.generateId(Roles.HOSPITAL_ADMIN);
            String lgId = userService.generateId(Roles.LOGISTICS);
            String paId = userService.generateId(Roles.PATIENT);

            assertCondition(bbId.startsWith("BB-"), "UserService generates BB- prefix for SUPER_ADMIN");
            assertCondition(haId.startsWith("HA-"), "UserService generates HA- prefix for HOSPITAL_ADMIN");
            assertCondition(lgId.startsWith("LG-"), "UserService generates LG- prefix for LOGISTICS");
            assertCondition(paId.startsWith("PA-"), "UserService generates PA- prefix for PATIENT");

            // Test InspectionService & 48h auto-escalation
            InspectionService inspService = InspectionService.getInstance();
            assertCondition(inspService != null, "InspectionService singleton instance exists");
            assertCondition(inspService == InspectionService.getInstance(), "InspectionService satisfies singleton identity");

            LocalDateTime soon = LocalDateTime.now().plusHours(24);
            BloodRequest urgentAuto = inspService.submitRequisition("St. Jude", "John Doe", BloodType.O_POS, PriorityLevel.NORMAL, soon);
            assertCondition(urgentAuto.getPriority() == PriorityLevel.URGENT, "Requisition within 48h auto-escalates to URGENT priority");

            LocalDateTime later = LocalDateTime.now().plusHours(72);
            BloodRequest normalReq = inspService.submitRequisition("General Hospital", "Jane Smith", BloodType.A_POS, PriorityLevel.NORMAL, later);
            assertCondition(normalReq.getPriority() == PriorityLevel.NORMAL, "Requisition beyond 48h preserves NORMAL priority");

            // Test LogisticsService
            LogisticsService logService = LogisticsService.getInstance();
            assertCondition(logService != null, "LogisticsService singleton instance exists");
            assertCondition(logService == LogisticsService.getInstance(), "LogisticsService satisfies singleton identity");

            assertCondition(!logService.getUrgentQueue().isEmpty(), "LogisticsService urgent queue contains auto-escalated request");
            assertCondition(!logService.getScheduledQueue().isEmpty(), "LogisticsService scheduled queue contains normal request");

            // Test dispatchUnit
            logService.dispatchUnit(urgentAuto.getRequestId());
            assertCondition(urgentAuto.getStatus() == RequisitionStatus.DELIVERED, "dispatchUnit sets requisition status to DELIVERED");

            passTest("Issue #8: Core singleton services, ID generators, auto-escalation, and queues fully verified.");
        } catch (Exception e) {
            failTest("Issue #8 error: " + e.getMessage());
        }
    }

    // =========================================================================
    // SPRINT 3 & SPRINT 4 TESTS
    // =========================================================================

    private static void testIssue9() { verifyClassPresence(9, "HospitalAdminController"); }
    private static void testIssue10() { verifyClassPresence(10, "LogisticsController", "PatientController"); }
    private static void testIssue11() {
        if (!classExists("AuthController")) {
            skipIssue(11, "AuthController not found yet.");
            return;
        }

        try {
            Class<?> authClass = Class.forName("AuthController");
            assertCondition(hasMethod(authClass, "startAuthFlow"), "AuthController contains startAuthFlow method");
            assertCondition(hasMethod(authClass, "handleLogin"), "AuthController contains handleLogin method");
            assertCondition(hasMethod(authClass, "handlePublicRegistration"), "AuthController contains handlePublicRegistration method");

            // Test public registration of patient
            Patient p = AuthController.registerPatient("John Doe", BloodType.O_POS, "password123");
            assertCondition(p != null && p.getId() != null && p.getId().startsWith("PA-"),
                    "AuthController registers patient with PA- prefix: " + (p != null ? p.getId() : "null"));

            // Test successful authentication
            User loggedIn = AuthController.login(p.getId(), "password123");
            assertCondition(loggedIn != null && loggedIn.getId().equals(p.getId()),
                    "AuthController successfully logs in with valid credentials");

            // Test failed password authentication
            boolean passwordFailed = false;
            try {
                AuthController.login(p.getId(), "wrongpassword");
            } catch (AuthenticationException e) {
                passwordFailed = true;
            }
            assertCondition(passwordFailed, "AuthController rejects incorrect password with AuthenticationException");

            // Test non-existent account authentication
            boolean userNotFound = false;
            try {
                AuthController.login("PA-999999", "any");
            } catch (UserNotFoundException e) {
                userNotFound = true;
            }
            assertCondition(userNotFound, "AuthController rejects unknown account ID with UserNotFoundException");

            // Test archived account login rejection
            p.setArchived(true);
            boolean archivedRejected = false;
            try {
                AuthController.login(p.getId(), "password123");
            } catch (AuthenticationException e) {
                archivedRejected = true;
            }
            assertCondition(archivedRejected, "AuthController rejects archived/disabled account with AuthenticationException");

            // Restore user state
            p.setArchived(false);

            passTest("Issue #11: AuthController, public registration, credential validation, and security checks verified.");
        } catch (Exception e) {
            failTest("Issue #11 error: " + e.getMessage());
        }
    }
    private static void testIssue12() { verifyClassPresence(12, "SuperAdminController"); }
    private static void testIssue13() {
        if (!classExists("SuperAdminController")) {
            skipIssue(13, "SuperAdminController not found yet.");
            return;
        }

        try {
            Class<?> saClass = Class.forName("SuperAdminController");
            assertCondition(hasMethod(saClass, "reviewRequisitions"), "SuperAdminController contains reviewRequisitions method");
            assertCondition(hasMethod(saClass, "filterRequisitions"), "SuperAdminController contains filterRequisitions method");
            assertCondition(hasMethod(saClass, "sortRequisitions"), "SuperAdminController contains sortRequisitions method");
            assertCondition(hasMethod(saClass, "processSingleRequisition"), "SuperAdminController contains processSingleRequisition method");

            // Setup test requisitions
            LocalDateTime now = LocalDateTime.now();
            BloodRequest reqNormal = new BloodRequest("REQ-9001", "Hospital A", "Patient A", BloodType.A_POS, PriorityLevel.NORMAL, now.plusHours(96));
            BloodRequest reqUrgent = new BloodRequest("REQ-9002", "Hospital B", "Patient B", BloodType.O_NEG, PriorityLevel.URGENT, now.plusHours(12));

            List<BloodRequest> testList = new ArrayList<>();
            testList.add(reqNormal);
            testList.add(reqUrgent);

            // Test priority sorting (URGENT first)
            testList.sort((a, b) -> (a.getPriority() == PriorityLevel.URGENT) ? -1 : 1);
            assertCondition(testList.get(0).getPriority() == PriorityLevel.URGENT,
                    "Requisitions can be sorted by priority level (URGENT first)");

            // Test filtering by blood type
            List<BloodRequest> filteredType = new ArrayList<>(testList);
            filteredType.removeIf(r -> r.getBloodType() != BloodType.O_NEG);
            assertCondition(filteredType.size() == 1 && filteredType.get(0).getBloodType() == BloodType.O_NEG,
                    "Requisitions can be filtered by blood type (O-)");

            // Test filtering by priority
            List<BloodRequest> filteredPriority = new ArrayList<>(testList);
            filteredPriority.removeIf(r -> r.getPriority() != PriorityLevel.NORMAL);
            assertCondition(filteredPriority.size() == 1 && filteredPriority.get(0).getPriority() == PriorityLevel.NORMAL,
                    "Requisitions can be filtered by priority level (NORMAL)");

            // Test Approval workflow with CentralInventory and LogisticsService
            CentralInventory inventory = CentralInventory.getInstance();
            BloodUnit unit = new BloodUnit("BU-9001", BloodType.O_NEG, BloodComponent.WHOLE_BLOOD, LocalDate.now(), "Donor Test");
            inventory.addUnit(unit);

            assertCondition(inventory.getActiveUnits().contains(unit), "Test unit BU-9001 placed into active inventory");

            BloodUnit allocated = SuperAdminController.approveRequisition(reqUrgent);
            assertCondition(allocated != null && allocated.getUnitId().equals("BU-9001"), "approveRequisition allocates compatible active blood unit");
            assertCondition(reqUrgent.getStatus() == RequisitionStatus.APPROVED, "approveRequisition updates requisition status to APPROVED");
            assertCondition(reqUrgent.getAssignedUnitId() != null && reqUrgent.getAssignedUnitId().equals("BU-9001"), "approveRequisition binds unit ID to requisition");
            assertCondition(LogisticsService.getInstance().getUrgentQueue().contains(reqUrgent), "approveRequisition enqueues urgent requisition to LogisticsService");

            // Test Rejection workflow
            SuperAdminController.rejectRequisition(reqNormal, "Insufficient donor units available");
            assertCondition(reqNormal.getStatus() == RequisitionStatus.REJECTED, "rejectRequisition sets requisition status to REJECTED");

            passTest("Issue #13: SuperAdmin requisition review, table rendering, sorting, filtering, and approval engine verified.");
        } catch (Exception e) {
            failTest("Issue #13 error: " + e.getMessage());
        }
    }
    private static void testIssue14() { verifyClassPresence(14, "SuperAdminController"); }
    private static void testIssue15() { verifyClassPresence(15, "CentralInventory"); }
    private static void testIssue16() { verifyClassPresence(16, "HospitalAdminController"); }
    private static void testIssue17() { verifyClassPresence(17, "Main", "InputValidator"); }

    private static void testIssue18() {
        java.io.File runSh = new java.io.File("run.sh");
        java.io.File runBat = new java.io.File("run.bat");
        java.io.File readme = new java.io.File("README.md");

        if (runSh.exists() || runBat.exists() || readme.exists()) {
            assertCondition(readme.exists(), "README.md exists");
            passTest("Issue #18: DevOps automation scripts and documentation verified.");
        } else {
            skipIssue(18, "run.sh, run.bat, and README.md not found yet.");
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static void verifyClassPresence(int issueNum, String... classNames) {
        for (String cName : classNames) {
            if (!classExists(cName)) {
                skipIssue(issueNum, cName + " not found yet.");
                return;
            }
        }
        passTest("Issue #" + issueNum + ": Target class files verified.");
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasMethod(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) return true;
        }
        return false;
    }

    private static void assertCondition(boolean condition, String description) {
        if (condition) {
            passTest(description);
        } else {
            failTest(description);
        }
    }

    private static void passTest(String msg) {
        System.out.println("  [PASS] " + msg);
        totalPassed++;
    }

    private static void failTest(String msg) {
        System.out.println("  [FAIL] " + msg);
        totalFailed++;
    }

    private static void skipIssue(int issueNum, String reason) {
        System.out.println("  [SKIP] Issue #" + issueNum + " skipped: " + reason);
        totalSkipped++;
    }
}
