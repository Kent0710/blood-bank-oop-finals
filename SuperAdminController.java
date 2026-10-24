import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Controller managing Blood Bank Super Admin operations: employee provisioning,
 * multi-criteria requisition review, inventory audits, allocation strategy configuration,
 * user ledger management, and logistics oversight.
 */
public class SuperAdminController {

    private final UserService userService;
    private final CentralInventory centralInventory;
    private final LogisticsService logisticsService;
    private final InspectionService inspectionService;

    public SuperAdminController() {
        this.userService = UserService.getInstance();
        this.centralInventory = CentralInventory.getInstance();
        this.logisticsService = LogisticsService.getInstance();
        this.inspectionService = InspectionService.getInstance();
    }

    public void handleBloodBankSuperAdminMenu(Scanner sc, BloodBank admin) {
        showDashboard(sc, admin);
    }

    public static void handleSuperAdminMenu(Scanner sc, BloodBank admin) {
        showDashboard(sc, admin);
    }

    public static void showDashboard(Scanner sc, BloodBank admin) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        CentralInventory centralInventory = CentralInventory.getInstance();
        InspectionService inspectionService = InspectionService.getInstance();
        LogisticsService logisticsService = LogisticsService.getInstance();

        String adminName = (admin != null) ? admin.getName() : "Super Admin";
        String adminId = (admin != null) ? admin.getAccountId() : "BB-1001";

        Runnable printDashboardMenu = () -> {
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", "SUPER ADMIN DASHBOARD: " + truncate(adminName + " [ID: " + adminId + "]", 55));
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| [1]  Create Employee Account (Hospital Admin / Logistics)                         |");
            System.out.println("| [2]  Review & Process Incoming Hospital Blood Requests (Approve / Reject)         |");
            System.out.println("| [3]  Store Blood from Hospital (Complete 5-Stage Processing Pipeline)             |");
            System.out.println("| [4]  Store Blood from Organization (Complete 5-Stage Processing Pipeline)         |");
            System.out.println("| [5]  View Central Inventory & Low-Supply Alerts (Read)                            |");
            System.out.println("| [6]  View Archived & Expired Units (Read)                                         |");
            System.out.println("| [7]  Audit & Purge Expired / Contaminated Stock (Delete/Archive)                  |");
            System.out.println("| [8]  Configure Low Stock Alert Threshold (Update)                                 |");
            System.out.println("| [9]  Configure Allocation Strategy (GoF Strategy: FIFO vs FEFO)                   |");
            System.out.println("| [10] Manage User Accounts (Archive/Deactivate Account)                            |");
            System.out.println("| [11] View Logistics Queues & Transport Records                                    |");
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
                        PipelineHelper.executeFullProcessingPipeline(sc, "Hospital", centralInventory, inspectionService, logisticsService);
                        break;
                    case 4:
                        PipelineHelper.executeFullProcessingPipeline(sc, "Organization", centralInventory, inspectionService, logisticsService);
                        break;
                    case 5:
                        centralInventory.displayInventory();
                        break;
                    case 6:
                        centralInventory.displayArchivedUnits();
                        break;
                    case 7:
                        handleAuditStock();
                        break;
                    case 8:
                        handleConfigureThreshold(sc);
                        break;
                    case 9:
                        handleConfigureAllocationStrategy(sc);
                        break;
                    case 10:
                        manageUserAccounts(sc, admin);
                        break;
                    case 11:
                        logisticsService.displayQueues();
                        break;
                    case 12:
                        System.out.println("Logging out from Super Admin...");
                        isRunning = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                break;
            } catch (Exception e) {
                System.out.println("\n  [Dashboard Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to Super Admin Dashboard...");
            }
        }
    }

    /**
     * Option [1]: Provisions a new employee account (Hospital Admin or Logistics Manager).
     */
    public static void createEmployeeAccount(Scanner sc) {
        Runnable printEmpMenu = () -> {
            System.out.println("--- Create Employee Account ---");
            System.out.println("Select employee type:");
            System.out.println("[1] Hospital Admin");
            System.out.println("[2] Logistics Manager");
            System.out.println("[3] Cancel");
        };

        System.out.println();
        printEmpMenu.run();

        int choice = InputValidator.readInteger(sc, "Enter choice (1-3): ", 1, 3, printEmpMenu);
        if (choice == 3) return;

        String name = InputValidator.readValidName(sc, "Enter employee full name: ");

        UserService userService = UserService.getInstance();
        if (choice == 1) {
            String hosp = InputValidator.readValidName(sc, "Enter Hospital Name: ");
            String pass = InputValidator.readValidPassword(sc, "Set Employee Password: ");
            String generatedHaId = userService.generateUniqueAccountId(Roles.HOSPITAL_ADMIN);
            HospitalAdmin ha = new HospitalAdmin(generatedHaId, name, hosp, pass);
            userService.registerUser(ha);

            printAccountCreationReceipt(ha.getAccountId(), ha.getName(), "Hospital Admin (" + ha.getHospitalName() + ")");
        } else {
            String pass = InputValidator.readValidPassword(sc, "Set Employee Password: ");
            String generatedLmId = userService.generateUniqueAccountId(Roles.LOGISTICS);
            LogisticsManager lm = new LogisticsManager(generatedLmId, name, "Central Blood Bank Logistics", pass);
            userService.registerUser(lm);

            printAccountCreationReceipt(lm.getAccountId(), lm.getName(), "Logistics Manager");
        }
    }

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

    /**
     * Option [2]: Review & Process Incoming Hospital Blood Requests.
     */
    public static void reviewRequisitions(Scanner scanner) {
        LogisticsService logisticsService = LogisticsService.getInstance();
        CentralInventory centralInventory = CentralInventory.getInstance();

        RequisitionStatus statusFilter = null;
        PriorityLevel priorityFilter = null;
        BloodType bloodTypeFilter = null;
        String hospitalFilter = null;
        int sortOption = 1;

        boolean inReviewMenu = true;
        while (inReviewMenu) {
            List<BloodRequest> allReqs = logisticsService.getAllRequests();

            // Apply Filters
            List<BloodRequest> filteredReqs = new ArrayList<>(allReqs);
            if (statusFilter != null) {
                final RequisitionStatus sf = statusFilter;
                filteredReqs = filteredReqs.stream().filter(r -> r.getStatus() == sf).collect(Collectors.toList());
            }
            if (priorityFilter != null) {
                final PriorityLevel pf = priorityFilter;
                filteredReqs = filteredReqs.stream().filter(r -> r.getPriority() == pf).collect(Collectors.toList());
            }
            if (bloodTypeFilter != null) {
                final BloodType btf = bloodTypeFilter;
                filteredReqs = filteredReqs.stream().filter(r -> r.getBloodType() == btf).collect(Collectors.toList());
            }
            if (hospitalFilter != null && !hospitalFilter.trim().isEmpty()) {
                final String hf = hospitalFilter.trim().toLowerCase();
                filteredReqs = filteredReqs.stream().filter(r -> r.getHospitalName() != null && r.getHospitalName().toLowerCase().contains(hf)).collect(Collectors.toList());
            }

            // Apply Sorting
            switch (sortOption) {
                case 1:
                    filteredReqs.sort(Comparator.comparing(BloodRequest::getNeededByDateTime, Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case 2:
                    filteredReqs.sort(Comparator.comparing(BloodRequest::getNeededByDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
                    break;
                case 3:
                    filteredReqs.sort((a, b) -> {
                        if (a.getPriority() == b.getPriority()) {
                            if (a.getNeededByDateTime() == null || b.getNeededByDateTime() == null) return 0;
                            return a.getNeededByDateTime().compareTo(b.getNeededByDateTime());
                        }
                        return a.getPriority() == PriorityLevel.URGENT ? -1 : 1;
                    });
                    break;
                case 4:
                    filteredReqs.sort(Comparator.comparing(BloodRequest::getRequestId, Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case 5:
                    filteredReqs.sort(Comparator.comparing(BloodRequest::getHospitalName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    break;
                case 6:
                    filteredReqs.sort(Comparator.comparing(BloodRequest::getPatientName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    break;
            }

            final List<BloodRequest> currentView = filteredReqs;
            final String sortDesc = getSortDescription(sortOption);
            final String filterDesc = String.format("[Status: %s] [Priority: %s] [Type: %s] [Hospital: %s]",
                    statusFilter == null ? "ALL" : statusFilter.name(),
                    priorityFilter == null ? "ALL" : priorityFilter.name(),
                    bloodTypeFilter == null ? "ALL" : bloodTypeFilter.getLabel(),
                    hospitalFilter == null || hospitalFilter.isEmpty() ? "ALL" : hospitalFilter);

            Runnable printTableAndMenu = () -> {
                System.out.println("+-------------------------------------------------------------------------------------------------------------------+");
                System.out.println("|                                  INCOMING HOSPITAL BLOOD REQUISITIONS                                             |");
                System.out.println("+-------------------------------------------------------------------------------------------------------------------+");
                System.out.println("Active Filters: " + filterDesc);
                System.out.println("Active Sort   : " + sortDesc);
                System.out.println("+----+------------+----------------------------+----------------------+--------+------------+-------------------+--------------+");
                System.out.printf("| %-2s | %-10s | %-26s | %-20s | %-6s | %-10s | %-17s | %-12s |\n",
                        "#", "Req ID", "Hospital", "Patient", "Type", "Priority", "Needed Target Time", "Status");
                System.out.println("+----+------------+----------------------------+----------------------+--------+------------+-------------------+--------------+");

                if (currentView.isEmpty()) {
                    System.out.println("| (No requisitions match the current view criteria)                                                                 |");
                } else {
                    for (int i = 0; i < currentView.size(); i++) {
                        BloodRequest req = currentView.get(i);
                        System.out.printf("| %-2d | %-10s | %-26s | %-20s | %-6s | %-10s | %-17s | %-12s |\n",
                                i + 1,
                                req.getRequestId(),
                                truncate(req.getHospitalName(), 26),
                                truncate(req.getPatientName(), 20),
                                req.getRequestedBloodType() != null ? req.getRequestedBloodType().getLabel() : "N/A",
                                req.getPriority(),
                                req.getFormattedNeededDateTime(),
                                req.getStatus());
                    }
                }
                System.out.println("+----+------------+----------------------------+----------------------+--------+------------+-------------------+--------------+");
                long pendingCount = currentView.stream().filter(r -> r.getStatus() == RequisitionStatus.PENDING).count();
                System.out.printf("Total Displayed: %d | Pending Approval: %d\n", currentView.size(), pendingCount);

                System.out.println("\nRequisition Management Operations:");
                System.out.println("[1] Review & Process a Requisition (Approve / Reject)");
                System.out.println("[2] Filter Requisitions (Status, Priority, Blood Type, Hospital)");
                System.out.println("[3] Sort Requisitions (Target Time, Priority, ID, Hospital, Patient)");
                System.out.println("[4] Reset Filters & Sorting");
                System.out.println("[5] Return to Super Admin Dashboard");
            };

            System.out.println();
            printTableAndMenu.run();

            int menuChoice = InputValidator.readInteger(scanner, "Enter choice (1-5): ", 1, 5, printTableAndMenu);

            switch (menuChoice) {
                case 1:
                    if (currentView.isEmpty()) {
                        System.out.println("  [Notice] No requisitions available in current view to process.");
                        break;
                    }

                    int sel = InputValidator.readInteger(scanner, "Select # from table above to process (0 to cancel): ", 0, currentView.size());
                    if (sel == 0) break;

                    BloodRequest targetReq = currentView.get(sel - 1);
                    processSingleRequisition(scanner, targetReq);
                    break;

                case 2:
                    filterRequisitions(scanner, currentView);
                    break;

                case 3:
                    sortRequisitions(scanner, currentView);
                    break;

                case 4:
                    statusFilter = null;
                    priorityFilter = null;
                    bloodTypeFilter = null;
                    hospitalFilter = null;
                    sortOption = 1;
                    System.out.println("✓ Reset all filters and sort order to defaults.");
                    break;

                case 5:
                    inReviewMenu = false;
                    break;
            }
        }
    }

    public static void processSingleRequisition(Scanner scanner, BloodRequest targetReq) {
        if (targetReq == null) return;
        CentralInventory centralInventory = CentralInventory.getInstance();
        LogisticsService logisticsService = LogisticsService.getInstance();

        System.out.println("\n--- Review Requisition Details ---");
        System.out.println("Request ID     : " + targetReq.getRequestId());
        System.out.println("Hospital       : " + targetReq.getHospitalName());
        System.out.println("Patient        : " + targetReq.getPatientName());
        System.out.println("Blood Type     : " + (targetReq.getRequestedBloodType() != null ? targetReq.getRequestedBloodType().getLabel() : "N/A"));
        System.out.println("Priority Tier  : " + targetReq.getPriority());
        System.out.println("Needed Time    : " + targetReq.getFormattedNeededDateTime());
        System.out.println("Current Status : " + targetReq.getStatus());

        if (targetReq.getStatus() != RequisitionStatus.PENDING) {
            System.out.println("\n>> [Notice] Requisition " + targetReq.getRequestId() + " is currently in state: " + targetReq.getStatus() + ".");
            System.out.println("   Only PENDING requisitions can be Approved or Rejected.");
            return;
        }

        boolean isAvailable = centralInventory.isBloodAvailable(targetReq.getRequestedBloodType(), targetReq.getNeededByDate());
        System.out.println("Inventory Check: " + (isAvailable ? "✓ Compatible unit AVAILABLE in central stock" : "✗ NO compatible stock available"));

        Runnable printActionMenu = () -> {
            System.out.println("\nSelect Action for " + targetReq.getRequestId() + ":");
            System.out.println("[1] Approve Request (Allocate & Enqueue to Logistics Dispatch)");
            System.out.println("[2] Reject Request (Notify Hospital)");
            System.out.println("[3] Cancel / Back");
        };
        printActionMenu.run();
        int action = InputValidator.readInteger(scanner, "Enter choice (1-3): ", 1, 3, printActionMenu);

        if (action == 1) {
            if (!isAvailable) {
                System.out.println(">> WARNING: Central inventory has insufficient available units for " +
                        (targetReq.getRequestedBloodType() != null ? targetReq.getRequestedBloodType().getLabel() : "") + " in active stock.");
                System.out.println("   Cannot approve without available stock. Please restock or reject.");
                return;
            }
            approveRequisition(targetReq);
        } else if (action == 2) {
            rejectRequisition(targetReq, "Rejected by Super Admin");
        }
    }

    public static BloodUnit approveRequisition(BloodRequest req) {
        if (req == null) return null;
        CentralInventory inventory = CentralInventory.getInstance();
        LogisticsService logistics = LogisticsService.getInstance();

        try {
            BloodUnit allocated = inventory.allocateUnit(req.getBloodType(), req.getRequestId());
            req.setStatus(RequisitionStatus.APPROVED);
            req.setAllocatedUnit(allocated);
            logistics.enqueueApprovedRequest(req);

            System.out.println("✓ [Blood Bank Super Admin] Request " + req.getRequestId() + " APPROVED successfully!");
            System.out.println("  Forwarded to Logistics Manager (" + req.getPriority() + " dispatch line).");
            return allocated;
        } catch (InsufficientStockException e) {
            System.out.println(">> WARNING: Central inventory has insufficient available units: " + e.getMessage());
            return null;
        }
    }

    public static void rejectRequisition(BloodRequest req, String reason) {
        if (req == null) return;
        req.setStatus(RequisitionStatus.REJECTED);
        System.out.println("✗ [Blood Bank Super Admin] Request " + req.getRequestId() + " marked as REJECTED (" + reason + ").");
    }

    public static void filterRequisitions(Scanner scanner, List<BloodRequest> reqs) {
        Runnable printFilterMenu = () -> {
            System.out.println("\n--- Filter Requisitions ---");
            System.out.println("[1] Filter by Status");
            System.out.println("[2] Filter by Priority Level (URGENT / NORMAL)");
            System.out.println("[3] Filter by Blood Type");
            System.out.println("[4] Filter by Hospital Name");
            System.out.println("[5] Clear All Filters");
            System.out.println("[6] Back");
        };
        printFilterMenu.run();
        int fChoice = InputValidator.readInteger(scanner, "Enter filter option (1-6): ", 1, 6, printFilterMenu);

        switch (fChoice) {
            case 1:
                System.out.println("\nSelect Status to Filter:");
                System.out.println("[1] PENDING  [2] APPROVED  [3] IN_TRANSIT  [4] DELIVERED  [5] REJECTED");
                int sc = InputValidator.readInteger(scanner, "Enter choice (1-5): ", 1, 5);
                RequisitionStatus targetSt = RequisitionStatus.PENDING;
                if (sc == 2) targetSt = RequisitionStatus.APPROVED;
                else if (sc == 3) targetSt = RequisitionStatus.IN_TRANSIT;
                else if (sc == 4) targetSt = RequisitionStatus.DELIVERED;
                else if (sc == 5) targetSt = RequisitionStatus.REJECTED;
                final RequisitionStatus fst = targetSt;
                reqs.removeIf(r -> r.getStatus() != fst);
                System.out.println("✓ Filter applied.");
                break;
            case 2:
                System.out.println("\nSelect Priority to Filter:");
                System.out.println("[1] URGENT  [2] NORMAL");
                int pc = InputValidator.readInteger(scanner, "Enter choice (1-2): ", 1, 2);
                PriorityLevel targetPr = (pc == 1) ? PriorityLevel.URGENT : PriorityLevel.NORMAL;
                reqs.removeIf(r -> r.getPriority() != targetPr);
                System.out.println("✓ Filter applied.");
                break;
            case 3:
                System.out.println("\nSelect Blood Type to Filter:");
                System.out.println("[1] O-   [2] O+   [3] A-   [4] A+   [5] B-   [6] B+   [7] AB-   [8] AB+");
                int btc = InputValidator.readInteger(scanner, "Enter choice (1-8): ", 1, 8);
                BloodType[] allTypes = {BloodType.O_NEG, BloodType.O_POS, BloodType.A_NEG, BloodType.A_POS,
                        BloodType.B_NEG, BloodType.B_POS, BloodType.AB_NEG, BloodType.AB_POS};
                BloodType targetBt = allTypes[btc - 1];
                reqs.removeIf(r -> r.getBloodType() != targetBt);
                System.out.println("✓ Filter applied.");
                break;
            case 4:
                System.out.print("Enter hospital name or keyword: ");
                String hInput = scanner.nextLine();
                if (hInput != null && !hInput.trim().isEmpty()) {
                    String keyword = hInput.trim().toLowerCase();
                    reqs.removeIf(r -> r.getHospitalName() == null || !r.getHospitalName().toLowerCase().contains(keyword));
                }
                System.out.println("✓ Filter applied.");
                break;
            case 5:
                reqs.clear();
                reqs.addAll(LogisticsService.getInstance().getAllRequests());
                System.out.println("✓ All filters cleared.");
                break;
            case 6:
                break;
        }
    }

    public static void sortRequisitions(Scanner scanner, List<BloodRequest> reqs) {
        Runnable printSortMenu = () -> {
            System.out.println("\n--- Sort Requisitions ---");
            System.out.println("[1] Target Needed Date/Time (Earliest First - Chronological)");
            System.out.println("[2] Target Needed Date/Time (Latest First)");
            System.out.println("[3] Priority Tier (Urgent First, then Scheduled)");
            System.out.println("[4] Request ID (Ascending)");
            System.out.println("[5] Hospital Name (Alphabetical A-Z)");
            System.out.println("[6] Patient Name (Alphabetical A-Z)");
            System.out.println("[7] Cancel / Back");
        };
        printSortMenu.run();
        int sChoice = InputValidator.readInteger(scanner, "Enter sort option (1-7): ", 1, 7, printSortMenu);

        switch (sChoice) {
            case 1:
                reqs.sort(Comparator.comparing(BloodRequest::getNeededByDateTime, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case 2:
                reqs.sort(Comparator.comparing(BloodRequest::getNeededByDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case 3:
                reqs.sort((a, b) -> (a.getPriority() == PriorityLevel.URGENT) ? -1 : 1);
                break;
            case 4:
                reqs.sort(Comparator.comparing(BloodRequest::getRequestId, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case 5:
                reqs.sort(Comparator.comparing(BloodRequest::getHospitalName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                break;
            case 6:
                reqs.sort(Comparator.comparing(BloodRequest::getPatientName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                break;
        }
        System.out.println("✓ Sorting updated.");
    }

    public static void handleConfigureAllocationStrategy(Scanner scanner) {
        CentralInventory centralInventory = CentralInventory.getInstance();
        Runnable printStratMenu = () -> {
            System.out.println("--- Configure Allocation Strategy (GoF Strategy Pattern) ---");
            System.out.println("Current Active Strategy: " + centralInventory.getAllocationStrategy().getStrategyName());
            System.out.println("[1] FIFO (First-In, First-Out: Oldest collection date)");
            System.out.println("[2] FEFO (First-Expired, First-Out: Nearest expiration date)");
        };

        System.out.println();
        printStratMenu.run();

        int sel = InputValidator.readInteger(scanner, "Select strategy (1-2): ", 1, 2, printStratMenu);
        if (sel == 1) {
            centralInventory.setAllocationStrategy(new FifoAllocationStrategy());
        } else {
            centralInventory.setAllocationStrategy(new FefoAllocationStrategy());
        }
        System.out.println("✓ Active Allocation Strategy updated to: " + centralInventory.getAllocationStrategy().getStrategyName());
    }

    public static void handleConfigureThreshold(Scanner scanner) {
        CentralInventory centralInventory = CentralInventory.getInstance();
        System.out.println("\n--- Configure Low Stock Alert Threshold ---");
        System.out.println("Current threshold: <= " + centralInventory.getLowStockThreshold() + " units triggers alert.");
        int newThreshold = InputValidator.readInteger(scanner, "Enter new threshold (1 to 20): ", 1, 20);
        centralInventory.setLowStockThreshold(newThreshold);
        System.out.println("✓ Low stock alert threshold updated to <= " + newThreshold + " units.");
    }

    public static void handleAuditStock() {
        CentralInventory centralInventory = CentralInventory.getInstance();
        System.out.println("\n--- Auditing Central Inventory for Expired & Contaminated Units ---");
        List<BloodUnit> purged = centralInventory.auditAndPurgeInvalidStock();
        if (purged.isEmpty()) {
            System.out.println("✓ Audit complete. No expired or contaminated units found in active stock.");
        } else {
            System.out.println("! Purged " + purged.size() + " invalid unit(s) from active stock to Archive:");
            for (BloodUnit u : purged) {
                System.out.println("  * DIN: " + u.getDin() + " | Type: " + (u.getBloodType() != null ? u.getBloodType().getLabel() : "N/A") + " | Reason: " + (u.getStatus() != null ? u.getStatus().getDescription() : "N/A"));
            }
        }
    }

    public static void viewInventoryLedger() {
        CentralInventory.getInstance().displayInventory();
    }

    public static void viewQuarantineLedger() {
        CentralInventory.getInstance().displayArchivedUnits();
    }

    public static void setLowStockThreshold(Scanner sc) {
        handleConfigureThreshold(sc);
    }

    public static void switchAllocationStrategy(Scanner sc) {
        handleConfigureAllocationStrategy(sc);
    }

    public static int performAuditAndPurge() {
        int count = CentralInventory.getInstance().auditAndPurgeExpiredStock();
        handleAuditStock();
        return count;
    }

    public static void manageUserAccounts(Scanner scanner) {
        manageUserAccounts(scanner, null);
    }

    public static void manageUserAccounts(Scanner scanner, BloodBank currentAdmin) {
        UserService userService = UserService.getInstance();
        boolean inLedger = true;
        while (inLedger) {
            List<User> users = userService.getAllUsers();

            Runnable printLedgerAndMenu = () -> {
                System.out.println("+-----------------------------------------------------------------------------------------------------------------------+");
                System.out.println("|                                                  USER ACCOUNT LEDGER                                                  |");
                System.out.println("+----+----------------------+-----------------------+-----------------------+--------------------------------+--------------+");
                System.out.printf("| %-2s | %-20s | %-21s | %-21s | %-30s | %-12s |\n",
                        "#", "Account ID", "Name", "Role", "Affiliation / Hospital", "Status");
                System.out.println("+----+----------------------+-----------------------+-----------------------+--------------------------------+--------------+");

                if (users.isEmpty()) {
                    System.out.println("| (No registered users found)                                                                                           |");
                } else {
                    for (int i = 0; i < users.size(); i++) {
                        User u = users.get(i);
                        String extra = "";
                        if (u instanceof HospitalAdmin) extra = ((HospitalAdmin) u).getHospitalName();
                        else if (u instanceof Patient) extra = "Clinical Patient Record";
                        else extra = "N/A";

                        System.out.printf("| %-2d | %-20s | %-21s | %-21s | %-30s | %-12s |\n",
                                i + 1,
                                truncate(u.getAccountId(), 20),
                                truncate(u.getName(), 21),
                                u.getRole(),
                                truncate(extra, 30),
                                u.isActive() ? "ACTIVE" : "DISABLED");
                    }
                }
                System.out.println("+----+----------------------+-----------------------+-----------------------+--------------------------------+--------------+");
                System.out.println("\nUser Ledger Operations:");
                System.out.println("[1] Disable / Enable Account");
                System.out.println("[2] Delete Account");
                System.out.println("[3] Return to Super Admin Dashboard");
            };

            System.out.println();
            printLedgerAndMenu.run();

            int choice = InputValidator.readInteger(scanner, "Enter choice (1-3): ", 1, 3, printLedgerAndMenu);

            switch (choice) {
                case 1:
                    if (users.isEmpty()) {
                        System.out.println("  [Notice] No accounts exist in the ledger.");
                        break;
                    }
                    int selToggle = InputValidator.readInteger(scanner, "Select user # from table above to disable/enable (0 to cancel): ", 0, users.size());
                    if (selToggle == 0) break;

                    User targetToggle = users.get(selToggle - 1);
                    if (currentAdmin != null && targetToggle.getAccountId().equalsIgnoreCase(currentAdmin.getAccountId())) {
                        System.out.println("  [Operation Denied] You cannot disable your own active Super Admin account while logged in.");
                        break;
                    }
                    if (targetToggle.getAccountId().equalsIgnoreCase("BB-1001")) {
                        System.out.println("  [Operation Denied] Primary Master Super Admin (BB-1001) cannot be disabled.");
                        break;
                    }

                    toggleUserArchive(targetToggle.getAccountId());
                    System.out.println("✓ Account '" + targetToggle.getName() + "' (" + targetToggle.getAccountId() + ") status updated to: " + (targetToggle.isActive() ? "ACTIVE" : "DISABLED"));
                    break;

                case 2:
                    if (users.isEmpty()) {
                        System.out.println("  [Notice] No accounts exist in the ledger.");
                        break;
                    }
                    int selDelete = InputValidator.readInteger(scanner, "Select user # from table above to DELETE (0 to cancel): ", 0, users.size());
                    if (selDelete == 0) break;

                    User targetDelete = users.get(selDelete - 1);
                    if (currentAdmin != null && targetDelete.getAccountId().equalsIgnoreCase(currentAdmin.getAccountId())) {
                        System.out.println("  [Operation Denied] You cannot delete your own Super Admin account while logged in.");
                        break;
                    }
                    if (targetDelete.getAccountId().equalsIgnoreCase("BB-1001")) {
                        System.out.println("  [Operation Denied] Primary Master Super Admin (BB-1001) cannot be deleted.");
                        break;
                    }

                    boolean confirm = InputValidator.readBoolean(scanner, "Are you sure you want to permanently delete account '" + targetDelete.getName() + "' (" + targetDelete.getAccountId() + ")? (y/n): ");
                    if (confirm) {
                        deleteUser(targetDelete.getAccountId());
                        System.out.println("✓ Account '" + targetDelete.getName() + "' (" + targetDelete.getAccountId() + ") has been permanently deleted.");
                    } else {
                        System.out.println(">> Account deletion cancelled.");
                    }
                    break;

                case 3:
                    inLedger = false;
                    break;
            }
        }
    }

    public static boolean toggleUserArchiveStatus(Scanner sc) {
        manageUserAccounts(sc);
        return true;
    }

    public static boolean deleteUserPermanently(Scanner sc) {
        manageUserAccounts(sc);
        return true;
    }

    public static boolean toggleUserArchive(String accountId) {
        if (accountId == null) return false;
        if (accountId.equalsIgnoreCase("BB-1001")) {
            System.out.println("  [Operation Denied] Primary Master Super Admin (BB-1001) cannot be archived.");
            return false;
        }

        UserService userService = UserService.getInstance();
        User user = userService.getUserById(accountId);
        if (user == null) return false;

        boolean newStatus = !user.isArchived();
        user.setArchived(newStatus);
        return true;
    }

    public static boolean deleteUser(String accountId) {
        if (accountId == null) return false;
        if (accountId.equalsIgnoreCase("BB-1001")) {
            System.out.println("  [Operation Denied] Primary Master Super Admin (BB-1001) cannot be deleted.");
            return false;
        }

        UserService userService = UserService.getInstance();
        User user = userService.getUserById(accountId);
        if (user == null) return false;

        return userService.deleteUser(user);
    }

    private static void printAccountCreationReceipt(String accountId, String name, String roleTitle) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                            ACCOUNT CREATED SUCCESSFULLY                           |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Generated Account ID", accountId);
        System.out.printf("| %-20s | %-58s |\n", "Account Holder", name);
        System.out.printf("| %-20s | %-58s |\n", "Role Classification", roleTitle);
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-81s |\n", ">> IMPORTANT: Remember your Account ID (" + accountId + ") to sign in later!");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    private static String getSortDescription(int sortOption) {
        switch (sortOption) {
            case 1: return "Target Needed Time (Earliest First)";
            case 2: return "Target Needed Time (Latest First)";
            case 3: return "Priority Tier (Urgent First)";
            case 4: return "Request ID (Ascending)";
            case 5: return "Hospital Name (Alphabetical A-Z)";
            case 6: return "Patient Name (Alphabetical A-Z)";
            default: return "Default";
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
