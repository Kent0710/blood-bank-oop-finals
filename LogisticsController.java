import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console dashboard for scheduled and emergency blood transport. */
public class LogisticsController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public LogisticsController() { }

    /** Compatibility entry point used by AuthController's role router. */
    public static void handleLogisticsMenu(Scanner sc, LogisticsManager user) {
        showDashboard(sc, user);
    }

    /** Compatibility overload retained for callers that pass the inspection service. */
    public static void handleLogisticsMenu(Scanner sc, LogisticsManager user, InspectionService inspectionService) {
        showDashboard(sc, user);
    }

    public static void showDashboard(Scanner sc, LogisticsManager logisticsUser) {
        if (sc == null) sc = new Scanner(System.in);
        if (logisticsUser == null) {
            System.out.println("[Access Denied] A valid Logistics account is required.");
            return;
        }
        runDashboard(sc);
    }

    /** Trusted entry point for Super Admin option [11]. */
    public static void showAdminDashboard(Scanner sc) {
        runDashboard(sc == null ? new Scanner(System.in) : sc);
    }

    private static void runDashboard(Scanner sc) {
        boolean running = true;
        while (running) {
            try {
                printMenu();
                int choice = InputValidator.readInt(sc, "Enter choice (1-5): ", 1, 5, LogisticsController::printMenu);
                switch (choice) {
                    case 1: viewScheduledQueue(); break;
                    case 2: viewUrgentQueue(); break;
                    case 3: dispatchRequisition(sc); break;
                    case 4: viewTransportAuditTrail(); break;
                    case 5:
                        System.out.println("[LOGOUT] Logistics session ended. Returning to previous menu...");
                        running = false;
                        break;
                    default: break;
                }
            } catch (NoSuchElementException e) {
                running = false;
            } catch (Exception e) {
                System.out.println("[Logistics Notice] Unable to complete that action: " + safeMessage(e));
            }
        }
    }

    private static void printMenu() {
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.println("| LOGISTICS & DISTRIBUTION DASHBOARD                                                |");
        System.out.println("+-----------------------------------------------------------------------------------+");
        System.out.println("| [1] View Scheduled Transport Queue (Standard 48h Deliveries)                      |");
        System.out.println("| [2] View Urgent Emergency Dispatch Queue (Immediate Priority)                     |");
        System.out.println("| [3] Dispatch Requisition (Update Delivery Status)                                 |");
        System.out.println("| [4] View Transport Audit Trail                                                    |");
        System.out.println("| [5] Log out                                                                       |");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    private static void viewScheduledQueue() {
        List<BloodRequest> eligible = approved(LogisticsService.getInstance().getScheduledQueue());
        renderQueue("SCHEDULED TRANSPORT QUEUE - STANDARD 48H DELIVERIES", eligible);
    }

    private static void viewUrgentQueue() {
        List<BloodRequest> eligible = approved(LogisticsService.getInstance().getUrgentQueue());
        renderQueue("URGENT EMERGENCY DISPATCH QUEUE - IMMEDIATE PRIORITY", eligible);
    }

    private static List<BloodRequest> approved(List<BloodRequest> requests) {
        List<BloodRequest> eligible = new ArrayList<>();
        if (requests != null) {
            for (BloodRequest request : requests) {
                if (request != null && request.getStatus() == RequisitionStatus.APPROVED) eligible.add(request);
            }
        }
        return eligible;
    }

    private static void renderQueue(String title, List<BloodRequest> requests) {
        TableRenderer table = new TableRenderer().withTitle(title)
                .addHeaders("Req ID", "Hospital", "Patient", "Blood Type", "Priority", "Target", "Status");
        for (BloodRequest r : requests) {
            table.addRow(value(r.getRequestId()), value(r.getHospitalName()), value(r.getPatientName()),
                    r.getBloodType() == null ? "N/A" : r.getBloodType().getCode(),
                    r.getPriority() == null ? "N/A" : r.getPriority().name(),
                    r.getTargetTime() == null ? "N/A" : r.getTargetTime().format(TIME_FORMAT),
                    r.getStatus() == null ? "UNKNOWN" : r.getStatus().name());
        }
        if (requests.isEmpty()) System.out.println("No approved requisitions are waiting in this queue.");
        table.render();
    }

    private static void dispatchRequisition(Scanner sc) {
        System.out.println("\n>>> DISPATCH APPROVED REQUISITION");
        String reqId = InputValidator.readNonBlankString(sc, "Enter requisition ID (or 0 to cancel): ", null).trim();
        if ("0".equals(reqId)) {
            System.out.println("[Action Cancelled] No requisition dispatched.");
            return;
        }
        BloodRequest request = InspectionService.getInstance().getRequisitionById(reqId);
        if (request == null) {
            System.out.println("[Validation Error] No requisition found with ID '" + reqId + "'.");
            return;
        }
        if (request.getStatus() != RequisitionStatus.APPROVED) {
            System.out.println("[Validation Error] Only APPROVED requisitions can be dispatched. Current status: "
                    + (request.getStatus() == null ? "UNKNOWN" : request.getStatus().name()) + ".");
            return;
        }

        // Record the departure state before the service completes delivery and audit logging.
        request.setStatus(RequisitionStatus.IN_TRANSIT);
        LogisticsService.getInstance().dispatchUnit(request.getRequestId());
        if (request.getStatus() == RequisitionStatus.DELIVERED) {
            System.out.println("[SUCCESS] " + request.getRequestId() + " completed: APPROVED -> IN_TRANSIT -> DELIVERED.");
        } else {
            // Keep failed dispatches eligible for review instead of leaving a false in-transit state.
            request.setStatus(RequisitionStatus.APPROVED);
            System.out.println("[Dispatch Error] Delivery did not complete; requisition remains APPROVED.");
        }
    }

    private static void viewTransportAuditTrail() {
        List<String> entries = LogisticsService.getInstance().getTransportLog();
        System.out.println("\n>>> TRANSPORT AUDIT TRAIL");
        if (entries == null || entries.isEmpty()) {
            System.out.println("No completed transport events have been recorded.");
            return;
        }
        for (int i = 0; i < entries.size(); i++) System.out.println((i + 1) + ". " + entries.get(i));
    }

    private static String value(String s) { return s == null || s.trim().isEmpty() ? "N/A" : s.trim(); }
    private static String safeMessage(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
