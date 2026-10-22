import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Rigorous Unit Test Suite for SuperAdminController (Issue #12).
 */
public class SuperAdminControllerUnitTest {

    public static void main(String[] args) {
        System.out.println(">>> RUNNING SUPER ADMIN CONTROLLER UNIT TESTS <<<\n");

        testEmployeeRegistrationProgrammatic();
        testConsoleEmployeeCreationSimulation();
        testDashboardLogoutOption();

        System.out.println("\n[SUCCESS] ALL SUPER ADMIN CONTROLLER UNIT TESTS PASSED WITH 100% SUCCESS!");
    }

    private static void testEmployeeRegistrationProgrammatic() {
        System.out.println("[TEST 1] Testing Programmatic Employee Registration...");
        UserService userService = UserService.getInstance();

        // 1. Register Hospital Admin
        User ha = SuperAdminController.registerEmployee(
                Roles.HOSPITAL_ADMIN,
                "Dr. Gregory House",
                "Princeton-Plainsboro Teaching Hospital",
                "vicodin123"
        );

        if (ha == null || !(ha instanceof HospitalAdmin)) {
            throw new AssertionError("Expected HospitalAdmin instance");
        }
        if (!ha.getId().startsWith("HA-")) {
            throw new AssertionError("Hospital Admin ID must start with HA-, got: " + ha.getId());
        }
        if (!"Princeton-Plainsboro Teaching Hospital".equals(ha.getHospitalAffiliation())) {
            throw new AssertionError("Hospital affiliation mismatch: " + ha.getHospitalAffiliation());
        }
        if (userService.getUserById(ha.getId()) != ha) {
            throw new AssertionError("Hospital Admin was not persisted in UserService");
        }

        // 2. Register Logistics Dispatcher
        User lg = SuperAdminController.registerEmployee(
                Roles.LOGISTICS,
                "John Walker",
                "Central Blood Bank Logistics",
                "logistics456"
        );

        if (lg == null || !(lg instanceof LogisticsManager)) {
            throw new AssertionError("Expected LogisticsManager instance");
        }
        if (!lg.getId().startsWith("LG-")) {
            throw new AssertionError("Logistics Manager ID must start with LG-, got: " + lg.getId());
        }
        if (userService.getUserById(lg.getId()) != lg) {
            throw new AssertionError("Logistics Manager was not persisted in UserService");
        }

        // 3. Authenticate registered employees
        try {
            User authHa = userService.authenticate(ha.getId(), "vicodin123");
            if (authHa != ha) throw new AssertionError("Authentication returned incorrect user");

            User authLg = userService.authenticate(lg.getId(), "logistics456");
            if (authLg != lg) throw new AssertionError("Authentication returned incorrect user");
        } catch (Exception e) {
            throw new AssertionError("Failed to authenticate registered employee: " + e.getMessage());
        }

        System.out.println("  -> Programmatic employee registration PASSED.");
    }

    private static void testConsoleEmployeeCreationSimulation() {
        System.out.println("[TEST 2] Testing Console Employee Creation (Option 1)...");
        // Simulated user input:
        // 1 (Hospital Admin)
        // Dr. Allison Cameron (Name)
        // St. Jude Children Hospital (Affiliation)
        // cameron789 (Password)
        String simulatedInput = "1\nDr. Allison Cameron\nSt. Jude Children Hospital\ncameron789\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            Scanner sc = new Scanner(System.in);
            SuperAdminController.createEmployeeAccount(sc);

            UserService userService = UserService.getInstance();
            boolean foundCameron = false;
            for (User u : userService.getAllUsers()) {
                if ("Dr. Allison Cameron".equals(u.getFullName()) && u.getRole() == Roles.HOSPITAL_ADMIN) {
                    foundCameron = true;
                    if (!u.getId().startsWith("HA-")) {
                        throw new AssertionError("Account ID should start with HA-");
                    }
                    if (!"St. Jude Children Hospital".equals(u.getHospitalAffiliation())) {
                        throw new AssertionError("Affiliation mismatch: " + u.getHospitalAffiliation());
                    }
                    break;
                }
            }
            if (!foundCameron) {
                throw new AssertionError("Dr. Allison Cameron was not found in UserService after console creation");
            }
        } finally {
            System.setIn(originalIn);
        }

        System.out.println("  -> Console employee creation PASSED.");
    }

    private static void testDashboardLogoutOption() {
        System.out.println("[TEST 3] Testing Dashboard Option 12 (Log out)...");
        // Simulated input: 12 (Log out)
        String simulatedInput = "12\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
            Scanner sc = new Scanner(System.in);
            BloodBank admin = new BloodBank("BB-1001", "Admin Root", "adminpass");
            SuperAdminController.showDashboard(sc, admin);
            // Reaches here without hanging or crashing
        } finally {
            System.setIn(originalIn);
        }

        System.out.println("  -> Dashboard logout PASSED.");
    }
}
