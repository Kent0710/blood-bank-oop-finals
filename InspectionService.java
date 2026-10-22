import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * InspectionService: Thread-safe Singleton managing hospital and patient blood requisitions,
 * central requisition storage, and 48-hour auto-escalation rules.
 */
public class InspectionService {
    private static InspectionService instance;
    private final List<BloodRequest> requisitions = new ArrayList<>();
    private int reqCounter = 1000;

    private InspectionService() {}

    public static synchronized InspectionService getInstance() {
        if (instance == null) {
            instance = new InspectionService();
        }
        return instance;
    }

    /**
     * Submits a central blood requisition.
     * Auto-escalation rule: If target delivery time is within 48 hours of now,
     * priority automatically escalates to PriorityLevel.URGENT.
     */
    public synchronized BloodRequest submitRequisition(String hospital, String patient, BloodType type, PriorityLevel priority, LocalDateTime targetTime) {
        if (priority == null) {
            priority = PriorityLevel.NORMAL;
        }

        String reqId = "REQ-" + (++reqCounter);
        boolean autoEscalated = false;

        LocalDateTime now = LocalDateTime.now();
        if (targetTime != null && targetTime.isBefore(now.plusHours(48))) {
            if (priority != PriorityLevel.URGENT) {
                priority = PriorityLevel.URGENT;
                autoEscalated = true;
            }
        }

        BloodRequest req = new BloodRequest(reqId, hospital, patient, type, priority, targetTime);
        requisitions.add(req);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedTime = targetTime != null ? targetTime.format(dtf) : "N/A";

        if (autoEscalated) {
            System.out.println("================================================================================");
            System.out.println("[REQUISITION SUBMITTED] ID: " + reqId);
            System.out.println("Notice: Target delivery time is within 48 hours (" + formattedTime + ").");
            System.out.println("[AUTO-ESCALATION] Priority automatically upgraded to: URGENT");
            System.out.println("Enqueued to Emergency Logistics Dispatch Queue.");
            System.out.println("================================================================================");
        } else {
            System.out.println("================================================================================");
            System.out.println("[REQUISITION SUBMITTED] ID: " + reqId);
            System.out.println("Hospital: " + hospital + " | Patient: " + patient + " | Type: " + (type != null ? type.getCode() : "N/A") + " | Priority: " + priority);
            if (priority == PriorityLevel.URGENT) {
                System.out.println("Enqueued to Emergency Logistics Dispatch Queue.");
            } else {
                System.out.println("Enqueued to Scheduled Batch Logistics Queue.");
            }
            System.out.println("================================================================================");
        }

        // Enqueue into centralized LogisticsService
        LogisticsService.getInstance().enqueue(req);

        return req;
    }

    public synchronized List<BloodRequest> getAllRequisitions() {
        return new ArrayList<>(requisitions);
    }

    public synchronized List<BloodRequest> getRequisitionsByHospital(String hospital) {
        List<BloodRequest> matches = new ArrayList<>();
        if (hospital == null) return matches;
        for (BloodRequest req : requisitions) {
            if (req.getHospitalName() != null && req.getHospitalName().equalsIgnoreCase(hospital.trim())) {
                matches.add(req);
            }
        }
        return matches;
    }

    public synchronized List<BloodRequest> getRequisitionsByPatient(String patient) {
        List<BloodRequest> matches = new ArrayList<>();
        if (patient == null) return matches;
        for (BloodRequest req : requisitions) {
            if (req.getPatientName() != null && req.getPatientName().equalsIgnoreCase(patient.trim())) {
                matches.add(req);
            }
        }
        return matches;
    }

    public synchronized BloodRequest getRequisitionById(String reqId) {
        if (reqId == null) return null;
        for (BloodRequest req : requisitions) {
            if (req.getRequestId() != null && req.getRequestId().equalsIgnoreCase(reqId.trim())) {
                return req;
            }
        }
        return null;
    }

    public synchronized void addRequisition(BloodRequest req) {
        if (req != null && !requisitions.contains(req)) {
            requisitions.add(req);
        }
    }

    public synchronized void clear() {
        requisitions.clear();
    }
}
