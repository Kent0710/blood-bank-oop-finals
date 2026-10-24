import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * InspectionService: Thread-safe Singleton managing hospital and patient blood requisitions,
 * laboratory testing, component centrifugation, DIN scanning, and quality validation workflows.
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

    /**
     * Stage 2: MLS Laboratory Testing
     */
    public boolean performMLSInspection(BloodUnit unit) {
        System.out.println("\n--- [Processing Pipeline: Stage 2 - MLS Laboratory Testing] ---");
        System.out.println(">> [MLS] Inspecting unit DIN: " + unit.getDin() + " | Type: " + (unit.getBloodType() != null ? unit.getBloodType().getLabel() : "N/A"));
        unit.setStatus(UnitStatus.IN_TESTING);

        boolean passed = true;
        StringBuilder report = new StringBuilder();

        // Contamination test
        if (unit.isContaminated()) {
            report.append("FAIL: Serological/bacterial contamination detected; ");
            passed = false;
        } else {
            report.append("PASS: Serological markers non-reactive (No contamination); ");
        }

        // Temperature test
        if (unit.getTemperatureCelsius() < 1.0 || unit.getTemperatureCelsius() > 6.0) {
            report.append(String.format("FAIL: Temperature out of cold chain limits (%.1f°C, allowed 1.0°C-6.0°C); ", unit.getTemperatureCelsius()));
            passed = false;
        } else {
            report.append(String.format("PASS: Cold chain temperature compliant (%.1f°C); ", unit.getTemperatureCelsius()));
        }

        // Lifespan test
        LocalDate today = LocalDate.now();
        if (unit.isExpiredOn(today)) {
            report.append("FAIL: Unit expired; ");
            passed = false;
        } else {
            long remaining = unit.getRemainingDays(today);
            report.append("PASS: Viable lifespan (").append(remaining).append(" days remaining); ");
        }

        unit.setMlsInspected(true);
        unit.setMlsPassed(passed);
        unit.setInspectionNotes(report.toString());

        System.out.println(">> [MLS Result]: " + (passed ? "PASSED (Quality verified)" : "FAILED (Quality violations detected)"));
        System.out.println("   Report: " + report);

        return passed;
    }

    /**
     * Stage 3: Centrifugation & Separation
     */
    public void processAndSeparateComponent(BloodUnit unit, BloodComponent chosenComponent) {
        System.out.println("\n--- [Processing Pipeline: Stage 3 - Centrifugation & Separation] ---");
        System.out.println(">> [Laboratory] Centrifuging raw unit DIN: " + unit.getDin());
        System.out.println(">> [Laboratory] Separated into component: " + (chosenComponent != null ? chosenComponent.getDisplayName() : "Whole Blood"));
        unit.setComponent(chosenComponent);
        if (chosenComponent != null) {
            unit.setLifespanDays(chosenComponent.getStandardLifespanDays());
            System.out.println(">> [Laboratory] Calibrated lifespan: " + chosenComponent.getStandardLifespanDays() + " days (Expiry: " + unit.getExpiryDate() + ").");
        }
    }

    /**
     * Stage 4: Barcode / DIN Scanning
     */
    public void scanAndLogUnit(BloodUnit unit) {
        System.out.println("\n--- [Processing Pipeline: Stage 4 - Scientist Barcode / DIN Scanning] ---");
        System.out.println(">> [Scientist] Optical scanner active. Scanning DIN: " + unit.getDin());
        System.out.println(">> [Scientist] Unit metadata and MLS test results logged into quarantine inventory.");
        unit.setScientistLogged(true);
    }

    /**
     * Stage 5: Super Admin Decision
     */
    public boolean reviewAndDecide(BloodUnit unit, boolean adminDecision, CentralInventory inventory) {
        System.out.println("\n--- [Processing Pipeline: Stage 5 - Super Admin Storage Approval] ---");
        if (adminDecision && unit.isMlsPassed()) {
            unit.setAdminApproved(true);
            inventory.addUnit(unit);
            System.out.println(">> [Super Admin] APPROVED: Unit " + unit.getDin() + " successfully placed into active Central Inventory.");
            return true;
        } else {
            unit.setAdminApproved(false);
            unit.setStatus(UnitStatus.REJECTED);
            System.out.println(">> [Super Admin] REJECTED: Unit " + unit.getDin() + " rejected (MLS Passed: " + unit.isMlsPassed() + "). Tagged for disposal.");
            return false;
        }
    }
}
