import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
                        storeBloodFromHospital(sc);
                        break;
                    case 4:
                        storeBloodFromOrganization(sc);
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

    /**
     * Option [2]: Review & Process Incoming Hospital Blood Requests.
     * Renders incoming requisitions in dynamic ASCII table format,
     * supports multi-criteria sorting and filtering, and processes approval/rejection.
     *
     * @param sc Scanner console input
     */
    public static void reviewRequisitions(Scanner sc) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        InspectionService inspectionService = InspectionService.getInstance();
        List<BloodRequest> currentView = new ArrayList<>(inspectionService.getAllRequisitions());

        boolean inReview = true;
        while (inReview) {
            System.out.println();
            renderRequisitionTable(currentView);

            System.out.println("\nRequisition Actions: [1] Process Request  [2] Sort Requests  [3] Filter Requests  [0] Back");
            int action = InputValidator.readInt(sc, "Enter choice (0-3): ", 0, 3);

            switch (action) {
                case 1:
                    if (currentView.isEmpty()) {
                        System.out.println("  [Notice] No requisitions available in current view to process.");
                        break;
                    }
                    int sel = InputValidator.readInt(sc, "Enter requisition # from table above (1-" + currentView.size() + ", 0 to cancel): ", 0, currentView.size());
                    if (sel > 0) {
                        BloodRequest req = currentView.get(sel - 1);
                        processSingleRequisition(sc, req);
                    }
                    break;
                case 2:
                    sortRequisitions(sc, currentView);
                    break;
                case 3:
                    filterRequisitions(sc, currentView);
                    break;
                case 0:
                    inReview = false;
                    break;
            }
        }
    }

    /**
     * Renders requisitions into a dynamic ASCII table with column headers matching the UI specification.
     */
    private static void renderRequisitionTable(List<BloodRequest> reqs) {
        TableRenderer table = new TableRenderer()
                .withTitle("INCOMING HOSPITAL BLOOD REQUISITIONS")
                .addHeaders("#", "Req ID", "Hospital", "Patient", "Type", "Priority", "S");

        if (reqs != null) {
            for (int i = 0; i < reqs.size(); i++) {
                BloodRequest r = reqs.get(i);
                String statusShort = "P";
                if (r.getStatus() != null) {
                    switch (r.getStatus()) {
                        case APPROVED: statusShort = "A"; break;
                        case REJECTED: statusShort = "R"; break;
                        case IN_TRANSIT: statusShort = "T"; break;
                        case DELIVERED: statusShort = "D"; break;
                        case COMPLETED: statusShort = "C"; break;
                        default: statusShort = "P"; break;
                    }
                }
                table.addRow(
                        String.valueOf(i + 1),
                        r.getRequestId() != null ? r.getRequestId() : "N/A",
                        r.getHospitalName() != null ? truncate(r.getHospitalName(), 26) : "N/A",
                        r.getPatientName() != null ? truncate(r.getPatientName(), 10) : "N/A",
                        r.getBloodType() != null ? r.getBloodType().getCode() : "N/A",
                        r.getPriority() != null ? r.getPriority().name() : "N/A",
                        statusShort
                );
            }
        }

        table.render();
    }

    /**
     * Multi-criteria filtering by Blood Type, Priority Level, Requisition Status, or view reset.
     *
     * @param sc Scanner console input
     * @param reqs Target mutable list of requisitions
     */
    public static void filterRequisitions(Scanner sc, List<BloodRequest> reqs) {
        if (reqs == null) return;

        System.out.println("\n>>> FILTER REQUISITIONS");
        System.out.println("  [1] Filter by Blood Type");
        System.out.println("  [2] Filter by Priority Level (URGENT / NORMAL)");
        System.out.println("  [3] Filter by Requisition Status (PENDING / APPROVED / REJECTED / DELIVERED)");
        System.out.println("  [4] Reset to All Requisitions");
        System.out.println("  [0] Cancel / Back");

        int choice = InputValidator.readInt(sc, "Enter filter option (0-4): ", 0, 4);
        switch (choice) {
            case 1:
                System.out.println("\nSelect Blood Type to Filter:");
                System.out.println("  [1] O-   [2] O+   [3] A-   [4] A+   [5] B-   [6] B+   [7] AB-   [8] AB+");
                int btc = InputValidator.readInt(sc, "Enter choice (1-8): ", 1, 8);
                BloodType[] types = BloodType.values();
                if (btc >= 1 && btc <= types.length) {
                    BloodType targetType = types[btc - 1];
                    reqs.removeIf(r -> r.getBloodType() != targetType);
                    System.out.println("[SUCCESS] Filtered to blood type: " + targetType.getCode() + " (" + reqs.size() + " matches)");
                }
                break;
            case 2:
                System.out.println("\nSelect Priority Level to Filter:");
                System.out.println("  [1] URGENT");
                System.out.println("  [2] NORMAL");
                int pc = InputValidator.readInt(sc, "Enter choice (1-2): ", 1, 2);
                PriorityLevel targetPriority = (pc == 1) ? PriorityLevel.URGENT : PriorityLevel.NORMAL;
                reqs.removeIf(r -> r.getPriority() != targetPriority);
                System.out.println("[SUCCESS] Filtered to priority: " + targetPriority + " (" + reqs.size() + " matches)");
                break;
            case 3:
                System.out.println("\nSelect Requisition Status to Filter:");
                System.out.println("  [1] PENDING");
                System.out.println("  [2] APPROVED");
                System.out.println("  [3] REJECTED");
                System.out.println("  [4] IN_TRANSIT");
                System.out.println("  [5] DELIVERED");
                int scChoice = InputValidator.readInt(sc, "Enter choice (1-5): ", 1, 5);
                RequisitionStatus[] statuses = RequisitionStatus.values();
                if (scChoice >= 1 && scChoice <= statuses.length) {
                    RequisitionStatus targetStatus = statuses[scChoice - 1];
                    reqs.removeIf(r -> r.getStatus() != targetStatus);
                    System.out.println("[SUCCESS] Filtered to status: " + targetStatus + " (" + reqs.size() + " matches)");
                }
                break;
            case 4:
                reqs.clear();
                reqs.addAll(InspectionService.getInstance().getAllRequisitions());
                System.out.println("[SUCCESS] All filters cleared. Showing " + reqs.size() + " total requisitions.");
                break;
            case 0:
                System.out.println("  [Notice] Filter operation cancelled.");
                break;
        }
    }

    /**
     * Multi-criteria sorting by Target Delivery Date, Priority Level, or Requisition ID.
     *
     * @param sc Scanner console input
     * @param reqs Target mutable list of requisitions
     */
    public static void sortRequisitions(Scanner sc, List<BloodRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            System.out.println("  [Notice] No requisitions to sort.");
            return;
        }

        System.out.println("\n>>> SORT REQUISITIONS");
        System.out.println("  [1] Target Delivery Date (Earliest / Urgent First)");
        System.out.println("  [2] Target Delivery Date (Latest First)");
        System.out.println("  [3] Priority Level (URGENT First, then NORMAL)");
        System.out.println("  [4] Requisition ID (Ascending)");
        System.out.println("  [0] Cancel / Back");

        int choice = InputValidator.readInt(sc, "Enter sort option (0-4): ", 0, 4);
        switch (choice) {
            case 1:
                reqs.sort(Comparator.comparing(BloodRequest::getTargetTime, Comparator.nullsLast(Comparator.naturalOrder())));
                System.out.println("[SUCCESS] Sorted by Target Delivery Date (Earliest first).");
                break;
            case 2:
                reqs.sort(Comparator.comparing(BloodRequest::getTargetTime, Comparator.nullsLast(Comparator.reverseOrder())));
                System.out.println("[SUCCESS] Sorted by Target Delivery Date (Latest first).");
                break;
            case 3:
                reqs.sort((a, b) -> {
                    if (a.getPriority() == b.getPriority()) {
                        LocalDateTime t1 = a.getTargetTime();
                        LocalDateTime t2 = b.getTargetTime();
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t1.compareTo(t2);
                    }
                    return (a.getPriority() == PriorityLevel.URGENT) ? -1 : 1;
                });
                System.out.println("[SUCCESS] Sorted by Priority Level (URGENT first).");
                break;
            case 4:
                reqs.sort(Comparator.comparing(BloodRequest::getRequestId, Comparator.nullsLast(Comparator.naturalOrder())));
                System.out.println("[SUCCESS] Sorted by Requisition ID (Ascending).");
                break;
            case 0:
                System.out.println("  [Notice] Sort operation cancelled.");
                break;
        }
    }

    /**
     * Interactively processes a single hospital blood requisition (Approve & Allocate, Reject, or Cancel).
     *
     * @param sc Scanner console input
     * @param req BloodRequest to process
     */
    public static void processSingleRequisition(Scanner sc, BloodRequest req) {
        if (req == null) return;

        System.out.println("\n>>> PROCESS REQUISITION " + req.getRequestId());
        System.out.println("Hospital       : " + req.getHospitalName());
        System.out.println("Patient        : " + req.getPatientName());
        System.out.println("Requested Type : " + (req.getBloodType() != null ? req.getBloodType().getCode() : "N/A"));
        System.out.println("Priority Level : " + req.getPriority());
        System.out.println("Target Time    : " + req.getTargetTime());
        System.out.println("Current Status : " + req.getStatus());

        if (req.getStatus() != RequisitionStatus.PENDING) {
            System.out.println("  [Notice] Requisition " + req.getRequestId() + " is currently in state: " + req.getStatus() + ".");
            System.out.println("  Only PENDING requisitions can be processed (Approved or Rejected).");
            return;
        }

        System.out.println("\n[Action] [1] Approve & Allocate Unit  [2] Reject Request  [0] Cancel");
        int action = InputValidator.readInt(sc, "Enter choice (0-2): ", 0, 2);

        if (action == 0) {
            System.out.println("  [Action Cancelled] Returning to requisition list.");
            return;
        }

        if (action == 1) {
            try {
                BloodUnit allocatedUnit = approveRequisition(req);
                String strategyName = CentralInventory.getInstance().getAllocationStrategy() != null
                        ? CentralInventory.getInstance().getAllocationStrategy().getStrategyName()
                        : "FEFO";
                System.out.println("[SUCCESS] Unit " + allocatedUnit.getUnitId() + " (" + allocatedUnit.getBloodType().getCode() + ") allocated via " + strategyName + " and assigned to " + req.getRequestId() + "!");
                System.out.println("Requisition status updated to: APPROVED -> Enqueued to Logistics.");
            } catch (InsufficientStockException e) {
                System.out.println("[ERROR] Cannot allocate: " + e.getMessage());
                System.out.println("Cannot approve requisition without available compatible stock in Central Inventory.");
            }
        } else if (action == 2) {
            String reason = InputValidator.readNonBlankString(sc, "Enter clinical rejection reason: ");
            rejectRequisition(req, reason);
            System.out.println("[SUCCESS] Requisition " + req.getRequestId() + " marked as REJECTED.");
            System.out.println("Clinical Rejection Reason: " + reason);
        }
    }

    /**
     * Programmatic approval helper allocating compatible unit from CentralInventory,
     * binding Unit ID, setting status to APPROVED, and enqueuing to LogisticsService.
     *
     * @param req BloodRequest to approve
     * @return the allocated BloodUnit
     * @throws InsufficientStockException if compatible active stock is unavailable
     */
    public static BloodUnit approveRequisition(BloodRequest req) throws InsufficientStockException {
        if (req == null) {
            throw new IllegalArgumentException("BloodRequest cannot be null.");
        }
        CentralInventory inventory = CentralInventory.getInstance();
        BloodUnit allocatedUnit = inventory.allocateUnit(req.getBloodType(), req.getRequestId());
        req.setAssignedUnitId(allocatedUnit.getUnitId());
        req.setStatus(RequisitionStatus.APPROVED);
        LogisticsService.getInstance().enqueue(req);
        return allocatedUnit;
    }

    /**
     * Programmatic rejection helper setting status to REJECTED.
     *
     * @param req BloodRequest to reject
     * @param reason Clinical reason
     */
    public static void rejectRequisition(BloodRequest req, String reason) {
        if (req == null) return;
        req.setStatus(RequisitionStatus.REJECTED);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    // Method: Option [3] Store Blood from Hospital
    private static void storeBloodFromHospital(Scanner sc) {
        // Prompts source hospital and launches PipelineHelper.runClinicalIntakePipeline(sc, hospitalName, reprompt).
        // Adds resulting unit to CentralInventory.
        System.out.println("\n>>> STORE BLOOD FROM HOSPITAL");
        String hospitalName = InputValidator.readNonBlankString(sc, "  Enter Source Hospital Name: ");
        BloodUnit unit = PipelineHelper.runClinicalIntakePipeline(sc, hospitalName, null);
        if (unit != null) {
            CentralInventory.getInstance().addUnit(unit);
            System.out.println("[SUCCESS] Hospital unit " + unit.getUnitId() + " (" + unit.getBloodType().getCode() + " " + unit.getComponent().name() + ") added to Central Inventory active ledger.");
        }
    }

    // Method: Option [4] Store Blood from Organization Blood Drive
    private static void storeBloodFromOrganization(Scanner sc) {
        // Prompts sponsoring organization name (e.g. Red Cross, University Drive).
        // Launches PipelineHelper.runClinicalIntakePipeline(sc, orgName, reprompt).
        // Adds resulting unit to CentralInventory.
        System.out.println("\n>>> STORE BLOOD FROM ORGANIZATION BLOOD DRIVE");
        String orgName = InputValidator.readNonBlankString(sc, "  Enter Sponsoring Organization Name (e.g. Red Cross, University Drive): ");
        BloodUnit unit = PipelineHelper.runClinicalIntakePipeline(sc, orgName, null);
        if (unit != null) {
            CentralInventory.getInstance().addUnit(unit);
            System.out.println("[SUCCESS] Organization drive unit " + unit.getUnitId() + " (" + unit.getBloodType().getCode() + " " + unit.getComponent().name() + ") added to Central Inventory active ledger.");
        }
    }

    // Backward compatibility aliases
    protected static void storeBloodHospital(Scanner sc) {
        storeBloodFromHospital(sc);
    }

    protected static void storeBloodDrive(Scanner sc) {
        storeBloodFromOrganization(sc);
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
