import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Controller managing Hospital Admin operations: blood requisitioning with lead-time checks,
 * donor intake, cold-chain returns, and clinical patient enrollment.
 */
public class HospitalAdminController {
    private final UserService userService;
    private final CentralInventory centralInventory;
    private final LogisticsService logisticsService;
    private final InspectionService inspectionService;
    private static int requestSequence = 1001;

    public HospitalAdminController() {
        this.userService = UserService.getInstance();
        this.centralInventory = CentralInventory.getInstance();
        this.logisticsService = LogisticsService.getInstance();
        this.inspectionService = InspectionService.getInstance();
    }

    public static void showDashboard(Scanner sc, HospitalAdmin admin) {
        new HospitalAdminController().handleHospitalAdminMenu(sc, admin);
    }

    public void handleHospitalAdminMenuInstance(Scanner sc, HospitalAdmin admin) {
        handleHospitalAdminMenu(sc, admin);
    }

    public void handleHospitalAdminMenu(Scanner scanner, HospitalAdmin hospitalAdmin) {
        if (scanner == null) scanner = new Scanner(System.in);

        Runnable printHospMenu = () -> {
            String adminName = (hospitalAdmin != null && hospitalAdmin.getName() != null) ? hospitalAdmin.getName() : "Dr. Nicole";
            String hospital = (hospitalAdmin != null && hospitalAdmin.getHospitalName() != null) ? hospitalAdmin.getHospitalName() : "General Hospital";

            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", "HOSPITAL ADMIN DASHBOARD: " + truncate(adminName + " [ID: " + hospitalAdmin.getAccountId() + "]", 54));
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| [1] Request Blood from Blood Bank (Hospital -> Blood Bank)                        |");
            System.out.println("| [2] Store Blood Donation to Blood Bank (Complete 5-Stage Processing Pipeline)     |");
            System.out.println("| [3] Return Unused / Excess Blood to Blood Bank (Cold-Chain Restock)               |");
            System.out.println("| [4] View Registered Patients (Read)                                               |");
            System.out.println("| [5] Register New Patient (Create)                                                 |");
            System.out.println("| [6] Log out                                                                       |");
            System.out.println("+-----------------------------------------------------------------------------------+");
        };

        boolean inHospMenu = true;
        while (inHospMenu) {
            try {
                System.out.println();
                printHospMenu.run();

                int choice = InputValidator.readInteger(scanner, "Enter choice (1-6): ", 1, 6, printHospMenu);

                switch (choice) {
                    case 1:
                        handleHospitalRequestBlood(scanner, hospitalAdmin);
                        break;
                    case 2:
                        handleHospitalStoreDonation(scanner, hospitalAdmin);
                        break;
                    case 3:
                        handleHospitalReturnBlood(scanner, hospitalAdmin);
                        break;
                    case 4:
                        displayPatients();
                        break;
                    case 5:
                        handleRegisterPatient(scanner);
                        break;
                    case 6:
                        System.out.println("Logging out from Hospital Admin...");
                        inHospMenu = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                break;
            } catch (Exception e) {
                System.out.println("\n  [Hospital Admin Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to dashboard menu...");
            }
        }
    }

    private void handleHospitalRequestBlood(Scanner scanner, HospitalAdmin admin) {
        System.out.println("\n--- Request Blood from Blood Bank (" + admin.getHospitalName() + " -> Blood Bank) ---");

        List<Patient> patients = userService.getActivePatients();
        String patientName;
        BloodType neededType;

        if (!patients.isEmpty()) {
            System.out.println("Select a patient from active clinical records or enter a new name:");
            for (int i = 0; i < patients.size(); i++) {
                Patient p = patients.get(i);
                System.out.printf("[%d] %-20s (%s) | Blood Type: %s\n",
                        i + 1, p.getName(), p.getAccountId(), (p.getBloodType() != null ? p.getBloodType().getLabel() : "N/A"));
            }
            System.out.println("[" + (patients.size() + 1) + "] Enter other patient name manually");

            int sel = InputValidator.readInteger(scanner, "Enter choice: ", 1, patients.size() + 1);
            if (sel <= patients.size()) {
                Patient selected = patients.get(sel - 1);
                patientName = selected.getName();
                neededType = selected.getBloodType();
                System.out.println(">> Selected patient: " + patientName + " (" + (neededType != null ? neededType.getLabel() : "N/A") + ")");
            } else {
                patientName = InputValidator.readValidName(scanner, "Enter Patient Name: ");
                neededType = PipelineHelper.promptBloodType(scanner);
            }
        } else {
            patientName = InputValidator.readValidName(scanner, "Enter Patient Name: ");
            neededType = PipelineHelper.promptBloodType(scanner);
        }

        LocalDateTime neededDateTime = InputValidator.readFutureDateTime(scanner);

        LocalDateTime now = LocalDateTime.now();
        Duration diff = Duration.between(now, neededDateTime);
        boolean isUnder48Hours = diff.toHours() < 48;

        BloodRequest.RequestType calculatedPriority;
        if (isUnder48Hours) {
            calculatedPriority = BloodRequest.RequestType.URGENT;
            System.out.println("\n================================================================================");
            System.out.println("  Notice: Target delivery time is within 48 hours (" + neededDateTime.format(BloodRequest.DATE_TIME_FORMATTER) + ").");
            System.out.println("  [AUTO-ESCALATION] Priority automatically upgraded to: URGENT");
            System.out.println("  Emergency priority automatically applied. Enqueued to Emergency Logistics Dispatch Queue.");
            System.out.println("================================================================================");
        } else {
            calculatedPriority = BloodRequest.RequestType.SCHEDULED;
            System.out.println(">> Standard delivery timeline confirmed (> 48 hours lead time). Priority: SCHEDULED.");
        }

        String reqId = "REQ-" + (requestSequence++);
        BloodRequest request = new BloodRequest(reqId, patientName, admin.getHospitalName(), neededType, calculatedPriority, neededDateTime);
        inspectionService.addRequisition(request);
        logisticsService.registerRequest(request);

        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                          REQUISITION SUBMITTED SUCCESSFULLY                       |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Requisition ID", request.getRequestId());
        System.out.printf("| %-20s | %-58s |\n", "Requesting Hospital", truncate(admin.getHospitalName(), 58));
        System.out.printf("| %-20s | %-58s |\n", "Patient Name", truncate(patientName, 58));
        System.out.printf("| %-20s | %-58s |\n", "Requested Blood Type", neededType != null ? neededType.getLabel() : "N/A");
        System.out.printf("| %-20s | %-58s |\n", "Priority Classification", calculatedPriority);
        System.out.printf("| %-20s | %-58s |\n", "Needed Target Time", request.getFormattedNeededDateTime());
        System.out.printf("| %-20s | %-58s |\n", "Requisition Status", request.getStatus());
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.println("✓ Requisition forward to Blood Bank Super Admin for clinical verification and stock allocation.");
    }

    private void handleHospitalStoreDonation(Scanner scanner, HospitalAdmin admin) {
        System.out.println("\n--- Store Collected Blood Donation from " + admin.getHospitalName() + " ---");
        PipelineHelper.executeFullProcessingPipeline(scanner, admin.getHospitalName(), centralInventory, inspectionService, logisticsService);
    }

    private void handleHospitalReturnBlood(Scanner scanner, HospitalAdmin admin) {
        System.out.println("\n--- Return Unused / Excess Blood to Blood Bank (" + admin.getHospitalName() + " -> Blood Bank) ---");

        String din = InputValidator.readValidDIN(scanner, "Enter DIN of the unit being returned (e.g. DIN-1001): ");
        BloodType bloodType = PipelineHelper.promptBloodType(scanner);

        Runnable printCompMenu = () -> {
            System.out.println("Select component being returned:");
            System.out.println("[1] Whole Blood");
            System.out.println("[2] Packed Red Blood Cells (PRBC)");
            System.out.println("[3] Fresh Frozen Plasma (FFP)");
            System.out.println("[4] Platelets");
        };
        printCompMenu.run();
        int compSel = InputValidator.readInteger(scanner, "Enter choice (1-4): ", 1, 4, printCompMenu);
        BloodComponent comp = BloodComponent.fromChoice(String.valueOf(compSel));

        Runnable printReasonMenu = () -> {
            System.out.println("Select reason for return:");
            System.out.println("[1] Patient discharged / surgery cancelled");
            System.out.println("[2] Excess unit ordered");
            System.out.println("[3] Approaching expiration date");
            System.out.println("[4] Cold chain transfer failure");
            System.out.println("[5] Other");
        };
        printReasonMenu.run();
        int reasonChoice = InputValidator.readInteger(scanner, "Enter choice (1-5): ", 1, 5, printReasonMenu);
        String reasonStr;
        switch (reasonChoice) {
            case 1: reasonStr = "Patient discharged / surgery cancelled"; break;
            case 2: reasonStr = "Excess unit ordered"; break;
            case 3: reasonStr = "Approaching expiration date"; break;
            case 4: reasonStr = "Cold chain transfer failure"; break;
            default:
                System.out.print("Specify reason: ");
                reasonStr = scanner.nextLine();
                break;
        }

        // Rigorous Cold-Chain Checks
        System.out.println("\n--- Cold-Chain Verification for Blood Return ---");
        double temp = InputValidator.readDouble(scanner, "Enter custody temperature measured upon handoff in °C (-5.0 to 30.0): ", -5.0, 30.0);
        boolean sealIntact = InputValidator.readBoolean(scanner, "Is the sterile unit seal completely intact and unopened? (y/n): ");

        if (!sealIntact || temp < 1.0 || temp > 6.0) {
            System.out.println("\n  [SAFETY VIOLATION] Blood unit " + din + " failed return safety criteria!");
            if (!sealIntact) System.out.println("  - Sterile seal is compromised or opened.");
            if (temp < 1.0 || temp > 6.0) System.out.printf("  - Storage temperature (%.1f°C) breached allowed cold-chain limits (1.0°C - 6.0°C).\n", temp);
            System.out.println(">> Unit cannot be returned to active inventory. Routed to quarantine for biohazard disposal.");
            return;
        }

        // Restock into Central Inventory
        BloodUnit returnedUnit = new BloodUnit(din, bloodType, comp, "Returned from: " + admin.getHospitalName(), LocalDate.now(), comp.getStandardLifespanDays(), temp, false);
        returnedUnit.setMlsPassed(true);
        returnedUnit.setAdminApproved(true);
        returnedUnit.setStatus(UnitStatus.STORED);
        centralInventory.addUnit(returnedUnit);

        logisticsService.performIntakeTransport(returnedUnit, admin.getHospitalName() + " (Excess Return: " + reasonStr + ")");

        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                            BLOOD RETURN SUCCESSFUL                                |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Returned Unit DIN", din);
        System.out.printf("| %-20s | %-58s |\n", "Originating Hospital", truncate(admin.getHospitalName(), 58));
        System.out.printf("| %-20s | %-58s |\n", "Blood Type & Comp", (bloodType != null ? bloodType.getLabel() : "N/A") + " - " + comp.getDisplayName());
        System.out.printf("| %-20s | %-58s |\n", "Return Reason", truncate(reasonStr, 58));
        System.out.printf("| %-20s | %-58s |\n", "Inventory Status", "RESTOCKED (Central Inventory Active Stock)");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.println("✓ Blood unit " + din + " successfully reintegrated into Central Blood Bank inventory.");
    }

    public void displayPatients() {
        TableRenderer table = new TableRenderer("Account ID", "Name", "Blood Type", "Status")
                .withTitle("REGISTERED PATIENT RECORDS");
        table.setEmptyMessage("No registered patients found.");

        List<Patient> patients = userService.getAllPatients();
        for (Patient p : patients) {
            table.addRow(p.getAccountId(), truncate(p.getName(), 24),
                    (p.getBloodType() != null ? p.getBloodType().getLabel() : "N/A"),
                    p.isActive() ? "Active" : "Archived");
        }
        System.out.println();
        table.render();
    }

    public void handleRegisterPatient(Scanner scanner) {
        System.out.println("\n--- Register New Patient ---");
        String name = InputValidator.readValidName(scanner, "Enter Patient Name: ");
        BloodType bt = PipelineHelper.promptBloodType(scanner);
        String pass = InputValidator.readValidPassword(scanner, "Set Patient Portal Password: ");
        String generatedPatId = userService.generateUniqueAccountId(Roles.PATIENT);

        Patient patient = new Patient(generatedPatId, name, pass, bt);
        userService.registerUser(patient);

        printAccountCreationReceipt(patient.getAccountId(), patient.getName(), "Patient (" + (bt != null ? bt.getLabel() : "N/A") + ")");
    }

    private void printAccountCreationReceipt(String accountId, String name, String roleTitle) {
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

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
