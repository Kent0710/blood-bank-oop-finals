import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * SuperAdminController coordinates the Super Admin Master Dashboard,
 * employee provisioning (Hospital Admins and Logistics Dispatchers),
 * and links to inventory, intake, audit, and user ledger services.
 */
public class SuperAdminController {

    public SuperAdminController() {
    }

    /**
     * Instance method adapter matching AuthController reflection dispatch.
     *
     * @param sc Scanner console input
     * @param admin Authenticated BloodBank Super Admin user
     */
    public void handleBloodBankSuperAdminMenu(Scanner sc, BloodBank admin) {
        showDashboard(sc, admin);
    }

    /**
     * Static alias for menu dispatch.
     */
    public static void handleSuperAdminMenu(Scanner sc, BloodBank admin) {
        showDashboard(sc, admin);
    }

    /**
     * Displays the 12-Option Super Admin Master Dashboard and handles user selection.
     *
     * @param sc Scanner console input
     * @param admin Authenticated BloodBank Super Admin user
     */
    public static void showDashboard(Scanner sc, BloodBank admin) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        String adminName = (admin != null) ? admin.getName() : "Super Admin";
        String adminId = (admin != null) ? admin.getId() : "BB-1000";

        Runnable printDashboardMenu = () -> {
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| CENTRAL BLOOD BANK MANAGEMENT SYSTEM - SUPER ADMIN DASHBOARD                      |");
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| [1]  Create Employee Account (Hospital Admin / Logistics Personnel)               |");
            System.out.println("| [2]  Review & Process Incoming Hospital Blood Requests                            |");
            System.out.println("| [3]  Store Blood from Hospital (Clinical Intake Pipeline)                         |");
            System.out.println("| [4]  Store Blood from Organization Blood Drive                                    |");
            System.out.println("| [5]  View Central Inventory (Active Stock Ledger)                                 |");
            System.out.println("| [6]  View Quarantine Stock (Compromised & Expired Units)                          |");
            System.out.println("| [7]  Audit & Purge Expired Blood Units                                            |");
            System.out.println("| [8]  Configure Low-Stock Alert Threshold                                          |");
            System.out.println("| [9]  Switch Allocation Strategy (FIFO vs FEFO)                                    |");
            System.out.println("| [10] Manage User Accounts (View Ledger / Toggle Archive / Delete)                 |");
            System.out.println("| [11] Logistics & Distribution Console                                             |");
            System.out.println("| [12] Log out                                                                      |");
            System.out.println("+-----------------------------------------------------------------------------------+");
        };

        boolean isRunning = true;
        while (isRunning) {
            try {
                System.out.println();
                printDashboardMenu.run();
                int choice = InputValidator.readInt(sc, "Enter choice (1-12): ", 1, 12, printDashboardMenu);

                switch (choice) {
                    case 1:
                        createEmployeeAccount(sc);
                        break;
                    case 2:
                        reviewRequisitions(sc);
                        break;
                    case 3:
                        storeBloodHospital(sc);
                        break;
                    case 4:
                        storeBloodDrive(sc);
                        break;
                    case 5:
                        viewInventoryLedger();
                        break;
                    case 6:
                        viewQuarantineLedger();
                        break;
                    case 7:
                        performAuditAndPurge();
                        break;
                    case 8:
                        setLowStockThreshold(sc);
                        break;
                    case 9:
                        switchAllocationStrategy(sc);
                        break;
                    case 10:
                        manageUserAccounts(sc);
                        break;
                    case 11:
                        logisticsConsole(sc);
                        break;
                    case 12:
                        System.out.println("\n[Session] Super Admin logged out successfully. Returning to main menu...");
                        isRunning = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                // Input stream exhausted (EOF / test suite execution)
                break;
            } catch (Exception e) {
                System.out.println("\n  [Dashboard Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to Super Admin Dashboard...");
            }
        }
    }

    /**
     * Option [1]: Provisions a new employee account (Hospital Admin or Logistics Personnel).
     * Enforces hospital name validation for Hospital Admins and saves to UserService.
     *
     * @param sc Scanner console input
     */
    public static void createEmployeeAccount(Scanner sc) {
        System.out.println("\n>>> PROVISION EMPLOYEE ACCOUNT");
        System.out.println("Select Employee Role to Provision:");
        System.out.println("  [1] Hospital Admin (Generates HA-xxxx ID)");
        System.out.println("  [2] Logistics Personnel (Generates LG-xxxx ID)");
        System.out.println("  [0] Cancel / Return to Dashboard");

        int roleChoice = InputValidator.readInt(sc, "Enter role type (1-2, 0 to cancel): ", 0, 2);
        if (roleChoice == 0) {
            System.out.println("  [Action Cancelled] Returning to Super Admin Dashboard.");
            return;
        }

        String name = InputValidator.readValidName(sc, "Enter Employee Full Name: ");
        String hospitalAffiliation = "N/A";
        Roles role;

        if (roleChoice == 1) {
            role = Roles.HOSPITAL_ADMIN;
            hospitalAffiliation = InputValidator.readValidName(sc, "Enter Hospital Affiliation (e.g. St. Jude Hospital): ");
        } else {
            role = Roles.LOGISTICS;
            hospitalAffiliation = "Central Blood Bank Logistics";
        }

        String password = InputValidator.readValidPassword(sc, "Set Initial Password: ");

        User employee = registerEmployee(role, name, hospitalAffiliation, password);
        printEmployeeCreationReceipt(employee);

        System.out.println("[SUCCESS] Employee account successfully provisioned and registered in UserService.");
    }

    /**
     * Programmatic helper to register an employee account and persist in UserService.
     *
     * @param role Roles.HOSPITAL_ADMIN or Roles.LOGISTICS
     * @param name Full name
     * @param hospitalAffiliation Hospital name (mandatory for HA, default for LG)
     * @param password Account password
     * @return the created User object
     */
    public static User registerEmployee(Roles role, String name, String hospitalAffiliation, String password) {
        UserService userService = UserService.getInstance();
        String accountId = userService.generateId(role);
        User employee;

        if (role == Roles.HOSPITAL_ADMIN) {
            employee = new HospitalAdmin(accountId, name, hospitalAffiliation, password);
        } else if (role == Roles.LOGISTICS) {
            employee = new LogisticsManager(accountId, name, hospitalAffiliation, password);
        } else {
            throw new IllegalArgumentException("Unsupported employee role: " + role);
        }

        userService.registerUser(employee);
        return employee;
    }

    private static void printEmployeeCreationReceipt(User employee) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                            ACCOUNT CREATED SUCCESSFULLY                           |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Generated Account ID", employee.getId());
        System.out.printf("| %-20s | %-58s |\n", "Account Holder", employee.getFullName());
        System.out.printf("| %-20s | %-58s |\n", "Affiliation", employee.getHospitalAffiliation());
        System.out.printf("| %-20s | %-58s |\n", "Role Classification", employee.getRoleDescription());
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-81s |\n", ">> IMPORTANT: Remember your Account ID (" + employee.getId() + ") to sign in later!");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    // =========================================================================
    // FEATURE PLACEHOLDERS & INTEGRATION HOOKS (Sprints 3 & 4)
    // =========================================================================

    /** Option [2] Placeholder: Connected in Issue #13 */
    protected static void reviewRequisitions(Scanner sc) {
        System.out.println("\n>>> REVIEW & PROCESS HOSPITAL REQUISITIONS");
        System.out.println("  [Notice] Full Requisition Review & Processing Engine is scheduled for Milestone 4 (Issue #13).");
    }

    /** Option [3] Placeholder: Clinical Intake Pipeline */
    protected static void storeBloodHospital(Scanner sc) {
        System.out.println("\n>>> STORE BLOOD FROM HOSPITAL");
        System.out.println("  [Notice] Hospital Blood Intake Pipeline is coordinated via PipelineHelper (Issue #5).");
    }

    /** Option [4] Placeholder: Mobile Blood Drive Intake */
    protected static void storeBloodDrive(Scanner sc) {
        System.out.println("\n>>> STORE BLOOD FROM ORGANIZATION DRIVE");
        System.out.println("  [Notice] Organization Blood Drive Intake Pipeline is coordinated via PipelineHelper (Issue #5).");
    }

    /** Option [5]: View Central Inventory Active Stock with Low-Stock Red Alerts (Issue #15) */
    public static void viewInventoryLedger() {
        CentralInventory inv = CentralInventory.getInstance();
        inv.printLowStockAlerts();

        TableRenderer table = new TableRenderer()
                .withTitle("CENTRAL INVENTORY STOCK LEDGER")
                .addHeaders("#", "Unit ID", "Blood Type", "Component", "Expiry Date", "Status");

        List<BloodUnit> units = inv.getActiveUnits();
        int index = 1;
        for (BloodUnit u : units) {
            table.addRow(
                    String.valueOf(index++),
                    u.getUnitId(),
                    u.getBloodType() != null ? u.getBloodType().getCode() : "N/A",
                    u.getComponent() != null ? u.getComponent().name() : "N/A",
                    u.getExpirationDate() != null ? u.getExpirationDate().toString() : "N/A",
                    u.getStatus() != null ? u.getStatus().name() : "N/A"
            );
        }
        table.render();
    }

    /** Option [6]: View Quarantine Stock (Compromised & Expired Units) (Issue #15) */
    public static void viewQuarantineLedger() {
        CentralInventory inv = CentralInventory.getInstance();

        TableRenderer table = new TableRenderer()
                .withTitle("QUARANTINE & COMPROMISED STOCK LEDGER")
                .addHeaders("#", "Unit ID", "Blood Type", "Component", "Expiry Date", "Status");

        List<BloodUnit> units = inv.getQuarantineUnits();
        int index = 1;
        for (BloodUnit u : units) {
            table.addRow(
                    String.valueOf(index++),
                    u.getUnitId(),
                    u.getBloodType() != null ? u.getBloodType().getCode() : "N/A",
                    u.getComponent() != null ? u.getComponent().name() : "N/A",
                    u.getExpirationDate() != null ? u.getExpirationDate().toString() : "N/A",
                    u.getStatus() != null ? u.getStatus().name() : "N/A"
            );
        }
        table.render();
    }

    /** Option [7]: Audit & Purge Expired Blood Units (Issue #15) */
    public static void performAuditAndPurge() {
        CentralInventory inv = CentralInventory.getInstance();
        int purged = inv.auditAndPurgeExpiredStock();
        if (purged == 0) {
            System.out.println("  [Audit Complete] No expired blood units detected. All active inventory remains clinical-grade.");
        } else {
            System.out.println("  [Audit Complete] Successfully purged " + purged + " expired unit(s) to quarantine stock.");
        }
    }

    /** Option [8]: Configure Low-Stock Alert Threshold (Issue #15) */
    public static void setLowStockThreshold(Scanner sc) {
        System.out.println("\n>>> CONFIGURE LOW-STOCK ALERT THRESHOLD");
        CentralInventory inv = CentralInventory.getInstance();
        System.out.println("Current Low-Stock Threshold: " + inv.getLowStockThreshold() + " units");
        int newThreshold = InputValidator.readInt(sc, "Enter new safety threshold (1-50): ", 1, 50);
        inv.setLowStockThreshold(newThreshold);
        System.out.println("[SUCCESS] Low-stock safety threshold updated to " + newThreshold + " units.");
    }

    public static void configureLowStockThreshold(Scanner sc) {
        setLowStockThreshold(sc);
    }

    /** Option [9]: Switch Allocation Strategy (FIFO vs FEFO) (Issue #15) */
    public static void switchAllocationStrategy(Scanner sc) {
        System.out.println("\n>>> SWITCH ALLOCATION STRATEGY");
        CentralInventory inv = CentralInventory.getInstance();
        System.out.println("Current Active Strategy: " + inv.getAllocationStrategy().getStrategyName());
        System.out.println("Select New Strategy: [1] FIFO (First In, First Out)  [2] FEFO (First Expired, First Out)");
        int choice = InputValidator.readInt(sc, "Enter choice (1-2): ", 1, 2);
        if (choice == 1) {
            inv.setAllocationStrategy(new FifoAllocationStrategy());
        } else {
            inv.setAllocationStrategy(new FefoAllocationStrategy());
        }
        System.out.println("[SUCCESS] Active allocation strategy updated to: " + inv.getAllocationStrategy().getStrategyName());
    }

    /** Option [10] Placeholder: User Account Management (Issue #14) */
    protected static void manageUserAccounts(Scanner sc) {
        System.out.println("\n>>> MANAGE USER ACCOUNTS");
        System.out.println("  [Notice] User Ledger Management (Archive / Delete) is scheduled for Milestone 4 (Issue #14).");
    }

    /** Option [11] Placeholder: Logistics Console (Issue #10) */
    protected static void logisticsConsole(Scanner sc) {
        System.out.println("\n>>> LOGISTICS & DISTRIBUTION CONSOLE");
        System.out.println("  [Notice] Logistics & Distribution Console is scheduled for Milestone 3 (Issue #10).");
    }
}
