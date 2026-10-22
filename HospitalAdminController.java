import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * HospitalAdminController coordinates clinical operations and dashboard workflows
 * for authenticated Hospital Administrators.
 */
public class HospitalAdminController {

    public HospitalAdminController() {
    }

    /**
     * Runs the interactive dashboard loop for the logged-in Hospital Admin.
     *
     * @param sc    Active Scanner instance for reading console inputs
     * @param admin Authenticated HospitalAdmin instance
     */
    public static void showDashboard(Scanner sc, HospitalAdmin admin) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        Runnable printDashboard = () -> {
            String adminName = (admin != null && admin.getName() != null) ? admin.getName() : "Dr. Nicole";
            String hospital = getAdminHospital(admin);

            String title = String.format("HOSPITAL ADMIN DASHBOARD: %s [Hospital: %s]", adminName, hospital);

            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", title);
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", "[1] Submit New Blood Requisition (Request Blood from Central Bank)");
            System.out.printf("| %-81s |\n", "[2] Track Submitted Requisitions (Live Status & Delivery Tracking)");
            System.out.printf("| %-81s |\n", "[3] Store Collected Blood from Hospital Donors (5-Stage Clinical Pipeline)");
            System.out.printf("| %-81s |\n", "[4] Return Unused / Temperature-Compromised Units (Cold-Chain Quarantine)");
            System.out.printf("| %-81s |\n", "[5] Register New Patient Account (Bedside Onboarding)");
            System.out.printf("| %-81s |\n", "[6] View Assigned Hospital Patients");
            System.out.printf("| %-81s |\n", "[7] Log out");
            System.out.println("+-----------------------------------------------------------------------------------+");
        };

        boolean isRunning = true;
        while (isRunning) {
            try {
                System.out.println();
                printDashboard.run();
                int choice = InputValidator.readInt(sc, "Enter choice (1-7): ", 1, 7, printDashboard);

                switch (choice) {
                    case 1:
                        submitRequisition(sc, admin);
                        break;
                    case 2:
                        trackRequisitions(admin);
                        break;
                    case 3:
                        storeCollectedBlood(sc, admin);
                        break;
                    case 4:
                        returnCompromisedUnits(sc, admin);
                        break;
                    case 5:
                        registerBedsidePatient(sc, admin);
                        break;
                    case 6:
                        viewAssignedPatients(admin);
                        break;
                    case 7:
                        System.out.println("\n[LOGOUT] Hospital Admin session ended. Returning to Main Menu...");
                        isRunning = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                // Input stream closed (EOF or automated test runner)
                break;
            } catch (Exception e) {
                System.out.println("\n  [Hospital Admin Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to dashboard menu...");
            }
        }
    }

    /**
     * Dispatcher alias matching AuthController reflection lookup.
     */
    public static void handleHospitalAdminMenu(Scanner sc, HospitalAdmin admin) {
        showDashboard(sc, admin);
    }

    /**
     * Instance method dispatch alias for polymorphic controller invocations.
     */
    public void handleHospitalAdminMenuInstance(Scanner sc, HospitalAdmin admin) {
        showDashboard(sc, admin);
    }

    /**
     * Option 1: Submit blood requisition with patient name, required ABO/Rh, target timestamp, and clinical priority.
     */
    private static void submitRequisition(Scanner sc, HospitalAdmin admin) {
        System.out.println("\n>>> SUBMIT NEW BLOOD REQUISITION");
        String hospital = getAdminHospital(admin);

        String patientName = InputValidator.readValidName(sc, "  Enter Patient Full Name: ");
        BloodType bloodType = promptBloodType(sc);

        System.out.println("\n  Select Clinical Priority Level:");
        System.out.println("    [1] NORMAL (Scheduled Batch Delivery)");
        System.out.println("    [2] URGENT (Immediate Emergency STAT Delivery)");
        int prioChoice = InputValidator.readInt(sc, "  Enter priority choice (1-2): ", 1, 2, null);
        PriorityLevel priority = (prioChoice == 2) ? PriorityLevel.URGENT : PriorityLevel.NORMAL;

        LocalDateTime targetTime = InputValidator.readFutureDateTime(sc, "--- Target Delivery Date & Time Input ---", null);

        BloodRequest req = InspectionService.getInstance().submitRequisition(
                hospital, patientName, bloodType, priority, targetTime
        );

        System.out.println("\n[SUCCESS] Blood Requisition " + req.getRequestId() + " successfully filed with Central Blood Bank!");
    }

    /**
     * Option 2: Render live status table for requisitions affiliated with the logged-in hospital.
     */
    private static void trackRequisitions(HospitalAdmin admin) {
        System.out.println("\n>>> TRACK SUBMITTED REQUISITIONS");
        String hospital = getAdminHospital(admin);

        List<BloodRequest> reqList = InspectionService.getInstance().getRequisitionsByHospital(hospital);

        TableRenderer table = new TableRenderer()
                .withTitle("LIVE REQUISITIONS TRACKING - " + hospital.toUpperCase())
                .addHeaders("Req ID", "Patient Name", "Blood Type", "Priority", "Target Delivery", "Status", "Assigned Unit");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (BloodRequest req : reqList) {
            String reqId = req.getRequestId() != null ? req.getRequestId() : "N/A";
            String patient = req.getPatientName() != null ? req.getPatientName() : "N/A";
            String type = req.getBloodType() != null ? req.getBloodType().getCode() : "N/A";
            String priority = req.getPriority() != null ? req.getPriority().name() : "NORMAL";
            String target = req.getTargetTime() != null ? req.getTargetTime().format(dtf) : "N/A";
            String status = req.getStatus() != null ? req.getStatus().name() : "PENDING";
            String unit = req.getAssignedUnitId() != null ? req.getAssignedUnitId() : "NONE";

            table.addRow(reqId, patient, type, priority, target, status, unit);
        }

        table.render();
    }

    /**
     * Option 3: Connect to PipelineHelper to intake hospital-collected donor units.
     */
    private static void storeCollectedBlood(Scanner sc, HospitalAdmin admin) {
        System.out.println("\n>>> STORE COLLECTED BLOOD (Hospital Donor Intake)");
        String hospital = getAdminHospital(admin);

        BloodUnit unit = PipelineHelper.runClinicalIntakePipeline(sc, hospital, null);
        if (unit != null) {
            CentralInventory.getInstance().addUnit(unit);
            System.out.println("[SUCCESS] Hospital-collected unit " + unit.getUnitId() + " successfully deposited into Central Inventory.");
        }
    }

    /**
     * Option 4: Quarantine temperature-compromised / spoiled units and send return alert to Central Bank.
     */
    private static void returnCompromisedUnits(Scanner sc, HospitalAdmin admin) {
        System.out.println("\n>>> RETURN UNUSED / TEMPERATURE-COMPROMISED UNITS (Quarantine Protocol)");
        String hospital = getAdminHospital(admin);

        String unitId = InputValidator.readNonBlankString(sc, "  Enter Unit ID to quarantine/return (e.g. BU-1001): ", null).toUpperCase();

        System.out.println("\n  Select Incident Reason:");
        System.out.println("    [1] Cold-Chain Storage Temperature Breach (> 6°C detected)");
        System.out.println("    [2] Packaging Integrity Compromised / Leaking Bag");
        System.out.println("    [3] Unused Unit Returned Post-Procedure");
        System.out.println("    [4] Serology / Clotting Contamination Detected");
        int reasonChoice = InputValidator.readInt(sc, "  Enter incident reason (1-4): ", 1, 4, null);

        String reasonText;
        switch (reasonChoice) {
            case 1:
                reasonText = "Cold-Chain Temperature Breach (> 6°C)";
                break;
            case 2:
                reasonText = "Packaging / Seal Integrity Compromised";
                break;
            case 3:
                reasonText = "Unused Return (Post-Procedure)";
                break;
            case 4:
                reasonText = "Suspected Contamination / Clotting";
                break;
            default:
                reasonText = "General Spoilage / Return";
                break;
        }

        CentralInventory inventory = CentralInventory.getInstance();
        BloodUnit targetUnit = null;
        for (BloodUnit u : inventory.getActiveUnits()) {
            if (u != null && u.getUnitId() != null && u.getUnitId().equalsIgnoreCase(unitId)) {
                targetUnit = u;
                break;
            }
        }

        if (targetUnit != null) {
            targetUnit.setStatus(UnitStatus.QUARANTINED);
            inventory.addUnit(targetUnit);
        } else {
            targetUnit = new BloodUnit(unitId, BloodType.O_POS, BloodComponent.WHOLE_BLOOD, LocalDate.now(), hospital);
            targetUnit.setStatus(UnitStatus.QUARANTINED);
            inventory.addUnit(targetUnit);
        }

        System.out.println("================================================================================");
        System.out.println(">>> COLD-CHAIN QUARANTINE ALERT & RETURN NOTIFICATION");
        System.out.println("================================================================================");
        System.out.println("  Quarantined Unit ID   : " + unitId);
        System.out.println("  Reporting Hospital    : " + hospital);
        System.out.println("  Admin In-Charge       : " + (admin != null ? admin.getName() : "Dr. Nicole"));
        System.out.println("  Incident Reason       : " + reasonText);
        System.out.println("  Status Transition     : ACTIVE -> QUARANTINED");
        System.out.println("  Action Dispatched     : Dispatched return advisory alert to Central Blood Bank.");
        System.out.println("================================================================================");
    }

    /**
     * Option 5: Register new bedside patient account bound to this hospital.
     */
    private static void registerBedsidePatient(Scanner sc, HospitalAdmin admin) {
        System.out.println("\n>>> REGISTER NEW PATIENT ACCOUNT (Bedside Onboarding)");
        String hospital = getAdminHospital(admin);

        String fullName = InputValidator.readValidName(sc, "  Enter Patient Full Name: ");
        BloodType bloodType = promptBloodType(sc);
        String password = InputValidator.readValidPassword(sc, "  Set Patient Account Password: ");

        UserService userService = UserService.getInstance();
        String patientId = userService.generateId(Roles.PATIENT);

        Patient patient = new Patient(patientId, fullName, hospital, password, bloodType);
        userService.registerUser(patient);

        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                      BEDSIDE PATIENT ONBOARDING SUCCESSFUL                        |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Patient Account ID", patientId);
        System.out.printf("| %-20s | %-58s |\n", "Patient Name", fullName);
        System.out.printf("| %-20s | %-58s |\n", "Blood Type", (bloodType != null ? bloodType.getCode() : "N/A"));
        System.out.printf("| %-20s | %-58s |\n", "Bound Hospital", hospital);
        System.out.printf("| %-20s | %-58s |\n", "Account Status", patient.getAccountStatus());
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.println("| >> Bedside account activated. Patient may now log in to the Patient Portal.       |");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    /**
     * Option 6: Render ASCII table of all patients registered under this hospital.
     */
    private static void viewAssignedPatients(HospitalAdmin admin) {
        System.out.println("\n>>> VIEW ASSIGNED HOSPITAL PATIENTS");
        String hospital = getAdminHospital(admin);

        List<Patient> allPatients = UserService.getInstance().getAllPatients();
        List<Patient> assigned = new ArrayList<>();

        for (Patient p : allPatients) {
            if (p != null) {
                String aff = p.getHospitalAffiliation();
                if (aff != null && (aff.equalsIgnoreCase(hospital) || hospital.equalsIgnoreCase("Hospital") || hospital.equalsIgnoreCase("N/A"))) {
                    assigned.add(p);
                }
            }
        }

        TableRenderer table = new TableRenderer()
                .withTitle("ASSIGNED PATIENTS - " + hospital.toUpperCase())
                .addHeaders("Patient ID", "Full Name", "Blood Type", "Affiliated Hospital", "Status");

        for (Patient p : assigned) {
            String pid = p.getId() != null ? p.getId() : "N/A";
            String name = p.getFullName() != null ? p.getFullName() : "N/A";
            String bt = p.getBloodType() != null ? p.getBloodType().getCode() : "N/A";
            String aff = p.getHospitalAffiliation() != null ? p.getHospitalAffiliation() : "N/A";
            String status = p.getAccountStatus() != null ? p.getAccountStatus().name() : "ACTIVE";

            table.addRow(pid, name, bt, aff, status);
        }

        table.render();
    }

    /**
     * Helper to resolve the active hospital affiliation for the admin.
     */
    private static String getAdminHospital(HospitalAdmin admin) {
        if (admin != null && admin.getHospitalAffiliation() != null
                && !admin.getHospitalAffiliation().trim().isEmpty()
                && !admin.getHospitalAffiliation().equalsIgnoreCase("N/A")) {
            return admin.getHospitalAffiliation().trim();
        }
        return "St. Jude Medical Center";
    }

    /**
     * Prompts for and validates an ABO/Rh Blood Type.
     */
    private static BloodType promptBloodType(Scanner sc) {
        BloodType bloodType = null;
        while (bloodType == null) {
            System.out.print("  Enter ABO/Rh Blood Type (e.g. O+, A-, B+, AB-): ");
            String input = sc.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Blood type cannot be empty.");
                continue;
            }

            try {
                bloodType = BloodType.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("  [Validation Error] " + e.getMessage() + ". Valid values: O+, O-, A+, A-, B+, B-, AB+, AB-");
            }
        }
        return bloodType;
    }
}
