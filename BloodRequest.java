import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BloodRequest {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public enum RequestType {
        SCHEDULED,
        URGENT
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        IN_TRANSIT,
        COMPLETED,
        REJECTED
    }

    private String requestId;
    private String hospitalName;
    private String patientName;
    private BloodType bloodType;
    private PriorityLevel priority;
    private LocalDateTime targetTime;
    private RequisitionStatus status;
    private String assignedUnitId;
    private BloodUnit allocatedUnit;

    public BloodRequest(String requestId, String hospitalName, String patientName, BloodType bloodType, PriorityLevel priority, LocalDateTime targetTime) {
        this.requestId = requestId;
        this.hospitalName = hospitalName;
        this.patientName = patientName;
        this.bloodType = bloodType;
        this.priority = priority != null ? priority : PriorityLevel.NORMAL;
        this.targetTime = targetTime;
        this.status = RequisitionStatus.PENDING;
        this.assignedUnitId = null;
        this.allocatedUnit = null;
    }

    public BloodRequest(String requestId, String patientName, String hospitalName, BloodType requestedBloodType, RequestType requestType, LocalDateTime neededByDateTime) {
        this(requestId, hospitalName, patientName, requestedBloodType,
             (requestType == RequestType.URGENT ? PriorityLevel.URGENT : PriorityLevel.NORMAL),
             neededByDateTime);
    }

    public BloodRequest(String requestId, String patientName, String hospitalName, BloodType requestedBloodType, RequestType requestType, LocalDate neededByDate) {
        this(requestId, patientName, hospitalName, requestedBloodType, requestType,
             neededByDate != null ? neededByDate.atStartOfDay() : LocalDateTime.now());
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getHospitalName() { return hospitalName; }
    public String getPatientName() { return patientName; }
    public BloodType getBloodType() { return bloodType; }
    public BloodType getRequestedBloodType() { return bloodType; }
    public PriorityLevel getPriority() { return priority; }
    public RequestType getRequestType() {
        return priority == PriorityLevel.URGENT ? RequestType.URGENT : RequestType.SCHEDULED;
    }
    public LocalDateTime getTargetTime() { return targetTime; }
    public LocalDateTime getNeededByDateTime() { return targetTime; }
    public LocalDate getNeededByDate() {
        return targetTime != null ? targetTime.toLocalDate() : LocalDate.now();
    }
    public String getFormattedNeededDateTime() {
        if (targetTime == null) return "N/A";
        return targetTime.format(DATE_TIME_FORMATTER);
    }
    public RequisitionStatus getStatus() { return status; }
    public String getAssignedUnitId() { return assignedUnitId; }
    public BloodUnit getAllocatedUnit() { return allocatedUnit; }

    // Setters
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public void setTargetTime(LocalDateTime targetTime) { this.targetTime = targetTime; }
    public void setStatus(RequisitionStatus status) { this.status = status; }
    public void setStatus(RequestStatus requestStatus) {
        if (requestStatus == null) return;
        switch (requestStatus) {
            case PENDING: this.status = RequisitionStatus.PENDING; break;
            case APPROVED: this.status = RequisitionStatus.APPROVED; break;
            case IN_TRANSIT: this.status = RequisitionStatus.IN_TRANSIT; break;
            case COMPLETED: this.status = RequisitionStatus.COMPLETED; break;
            case REJECTED: this.status = RequisitionStatus.REJECTED; break;
        }
    }
    public void setAssignedUnitId(String assignedUnitId) { this.assignedUnitId = assignedUnitId; }
    public void setAllocatedUnit(BloodUnit allocatedUnit) {
        this.allocatedUnit = allocatedUnit;
        if (allocatedUnit != null) {
            this.assignedUnitId = allocatedUnit.getUnitId();
        }
    }

    public String getStatusChainDisplay() {
        if (status == null) {
            return "[Pending] -> Approved / Rejected -> In transit -> Completed";
        }
        switch (status) {
            case PENDING:
                return "[Pending] -> Approved / Rejected -> In transit -> Completed";
            case APPROVED:
                return "Pending -> [Approved] -> In transit -> Completed";
            case IN_TRANSIT:
                return "Pending -> Approved -> [In transit] -> Completed";
            case DELIVERED:
            case COMPLETED:
                return "Pending -> Approved -> In transit -> [Completed]";
            case REJECTED:
                return "Pending -> [Rejected]";
            default:
                return status.name();
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s | Patient: %s | Hospital: %s | Type: %s | Priority: %s | Needed: %s | Status: %s",
                status, requestId, patientName, hospitalName,
                (bloodType != null ? bloodType.getLabel() : "N/A"),
                priority, getFormattedNeededDateTime(), status);
    }
}
