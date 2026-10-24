import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Controller managing the Patient Portal and read-only transfusion tracking.
 */
public class PatientController {
    private final LogisticsService logisticsService;

    public PatientController() {
        this.logisticsService = LogisticsService.getInstance();
    }

    public static void showDashboard(Scanner scanner, Patient patient) {
        new PatientController().handlePatientMenu(scanner, patient);
    }

    public void handlePatientMenu(Scanner scanner, Patient patient) {
        if (scanner == null) scanner = new Scanner(System.in);
        String name = (patient != null && patient.getName() != null) ? patient.getName() : "Patient";
        String id = (patient != null && patient.getAccountId() != null) ? patient.getAccountId() : "PA-4001";

        Runnable printPatientMenu = () -> {
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", "PATIENT PORTAL: " + truncate(name + " [ID: " + id + "]", 63));
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| [1] View Patient Blood Profile & Compatibility Details (Read)                     |");
            System.out.println("| [2] Track Blood Request Status & Status Chain (Read)                              |");
            System.out.println("| [3] Log out                                                                       |");
            System.out.println("+-----------------------------------------------------------------------------------+");
        };

        boolean inPatientMenu = true;
        while (inPatientMenu) {
            try {
                System.out.println();
                printPatientMenu.run();

                int choice = InputValidator.readInteger(scanner, "Enter choice (1-3): ", 1, 3, printPatientMenu);

                switch (choice) {
                    case 1:
                        patient.displayBloodProfile();
                        break;
                    case 2:
                        handlePatientTrackRequests(patient);
                        break;
                    case 3:
                        System.out.println("Logging out from Patient Portal...");
                        inPatientMenu = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                break;
            } catch (Exception e) {
                System.out.println("\n  [Patient Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to patient portal menu...");
            }
        }
    }

    public void handlePatientTrackRequests(Patient patient) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                                BLOOD REQUEST TRACKER                              |");
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.printf("| Patient: %-25s | ID: %-42s |\n", patient.getName(), patient.getAccountId());
        System.out.println("+-----------------------------------------------------------------------------------+");

        List<BloodRequest> patientRequests = logisticsService.getRequestsForPatient(patient.getName());

        if (patientRequests.isEmpty()) {
            System.out.println("| No blood requests currently filed under your clinical record.                     |");
            System.out.println("| Note: Requests are submitted on your behalf by your Hospital Admin.               |");
            System.out.println("+-----------------------------------------------------------------------------------+");
            return;
        }

        for (BloodRequest req : patientRequests) {
            System.out.printf("| Request ID      : %-63s |\n", req.getRequestId());
            System.out.printf("| Hospital        : %-63s |\n", req.getHospitalName());
            System.out.printf("| Requested Blood : %-63s |\n", (req.getRequestedBloodType() != null ? req.getRequestedBloodType().getLabel() : "N/A"));
            System.out.printf("| Priority        : %-63s |\n", req.getRequestType());
            System.out.printf("| Needed Target   : %-63s |\n", req.getFormattedNeededDateTime());
            System.out.printf("| Current Status  : %-63s |\n", req.getStatus());
            System.out.printf("| Status Chain    : %-63s |\n", req.getStatusChainDisplay());
            if (req.getAllocatedUnit() != null) {
                System.out.printf("| Allocated Unit  : DIN %-10s (%-46s) |\n",
                        req.getAllocatedUnit().getDin(),
                        (req.getAllocatedUnit().getComponent() != null ? req.getAllocatedUnit().getComponent().getDisplayName() : "N/A"));
            }
            System.out.println("+-----------------------------------------------------------------------------------+");
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
