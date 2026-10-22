import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Read-only self-service dashboard for authenticated patients. */
public class PatientController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PatientController() { }

    /** Compatibility entry point used by AuthController's role router. */
    public static void handlePatientMenu(Scanner sc, Patient patient) {
        showDashboard(sc, patient);
    }

    public static void showDashboard(Scanner sc, Patient patient) {
        if (sc == null) sc = new Scanner(System.in);
        if (patient == null || patient.getId() == null || patient.getId().trim().isEmpty()) {
            System.out.println("[Access Denied] A valid patient account is required.");
            return;
        }

        boolean running = true;
        while (running) {
            try {
                printMenu(patient);
                int choice = InputValidator.readInt(sc, "Enter choice (1-4): ", 1, 4, () -> printMenu(patient));
                switch (choice) {
                    case 1: viewMyRequisitions(patient); break;
                    case 2: viewTransfusionHistory(patient); break;
                    case 3: viewProfile(patient); break;
                    case 4:
                        System.out.println("[LOGOUT] Patient session ended. Returning to main menu...");
                        running = false;
                        break;
                    default: break;
                }
            } catch (NoSuchElementException e) {
                running = false;
            } catch (Exception e) {
                System.out.println("[Patient Portal Notice] Unable to display that information: " + safeMessage(e));
            }
        }
    }

    private static void printMenu(Patient patient) {
        String name = safe(patient.getFullName());
        String bloodType = patient.getBloodType() == null ? "N/A" : patient.getBloodType().getCode();
        String title = "PATIENT SELF-SERVICE PORTAL: " + name + " [Blood Type: " + bloodType + "]";
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.printf("| %-81s |%n", truncate(title, 81));
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.println("| [1] View My Active Blood Requests (Live Requisition Status)                       |");
        System.out.println("| [2] View Transfusion Medical History                                              |");
        System.out.println("| [3] View My Profile                                                               |");
        System.out.println("| [4] Log out                                                                       |");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    private static void viewMyRequisitions(Patient patient) {
        List<BloodRequest> requests = new ArrayList<>();
        for (BloodRequest r : getPatientRequests(patient)) {
            if (r.getStatus() == RequisitionStatus.PENDING || r.getStatus() == RequisitionStatus.APPROVED
                    || r.getStatus() == RequisitionStatus.IN_TRANSIT) requests.add(r);
        }
        TableRenderer table = new TableRenderer().withTitle("MY BLOOD REQUESTS - LIVE STATUS")
                .addHeaders("Req ID", "Hospital", "Blood Type", "Priority", "Target Delivery", "Status");
        for (BloodRequest r : requests) {
            table.addRow(safe(r.getRequestId()), safe(r.getHospitalName()),
                    r.getBloodType() == null ? "N/A" : r.getBloodType().getCode(),
                    r.getPriority() == null ? "N/A" : r.getPriority().name(),
                    r.getTargetTime() == null ? "N/A" : r.getTargetTime().format(TIME_FORMAT),
                    r.getStatus() == null ? "UNKNOWN" : r.getStatus().name());
        }
        if (requests.isEmpty()) System.out.println("No blood requests are currently associated with your account.");
        table.render();
    }

    private static void viewTransfusionHistory(Patient patient) {
        List<BloodRequest> completed = new ArrayList<>();
        for (BloodRequest r : getPatientRequests(patient)) {
            if (r.getStatus() == RequisitionStatus.DELIVERED || r.getStatus() == RequisitionStatus.COMPLETED) completed.add(r);
        }
        TableRenderer table = new TableRenderer().withTitle("TRANSFUSION MEDICAL HISTORY")
                .addHeaders("Req ID", "Hospital", "Blood Type", "Priority", "Delivery Date", "Status");
        for (BloodRequest r : completed) {
            table.addRow(safe(r.getRequestId()), safe(r.getHospitalName()),
                    r.getBloodType() == null ? "N/A" : r.getBloodType().getCode(),
                    r.getPriority() == null ? "N/A" : r.getPriority().name(),
                    r.getTargetTime() == null ? "N/A" : r.getTargetTime().format(TIME_FORMAT),
                    r.getStatus().name());
        }
        if (completed.isEmpty()) System.out.println("No completed transfusion deliveries are recorded for your account.");
        table.render();
        System.out.println("History is derived from delivered or completed requisitions recorded by the system.");
    }

    private static void viewProfile(Patient patient) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("| CLINICAL PROFILE                                                                 |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        profileRow("Patient ID", safe(patient.getId()));
        profileRow("Full Name", safe(patient.getFullName()));
        profileRow("Blood Type", patient.getBloodType() == null ? "N/A" : patient.getBloodType().getCode());
        profileRow("Hospital", safe(patient.getHospitalAffiliation()));
        profileRow("Account Status", patient.isArchived() ? "ARCHIVED" : "ACTIVE");
        System.out.println("+----------------------+------------------------------------------------------------+");
    }

    private static List<BloodRequest> getPatientRequests(Patient patient) {
        List<BloodRequest> matches = InspectionService.getInstance().getRequisitionsByPatient(patient.getFullName());
        String affiliation = patient.getHospitalAffiliation();
        if (affiliation == null || affiliation.trim().isEmpty() || affiliation.equalsIgnoreCase("Walk-in")
                || affiliation.equalsIgnoreCase("N/A")) return matches;

        // Hospital-affiliated patient accounts only see same-name requests for their hospital.
        List<BloodRequest> scoped = new ArrayList<>();
        for (BloodRequest request : matches) {
            if (request.getHospitalName() != null && request.getHospitalName().equalsIgnoreCase(affiliation.trim())) {
                scoped.add(request);
            }
        }
        return scoped;
    }

    private static void profileRow(String label, String value) {
        System.out.printf("| %-20s | %-58s |%n", label, truncate(value, 58));
    }

    private static String safe(String s) { return s == null || s.trim().isEmpty() ? "N/A" : s.trim(); }
    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "..."; }
    private static String safeMessage(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
