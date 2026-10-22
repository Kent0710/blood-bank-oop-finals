import java.time.LocalDateTime;

public class BloodRequest {
    private String requestId;
    private String hospitalName;
    private String patientName;
    private BloodType bloodType;
    private PriorityLevel priority; 
    private LocalDateTime targetTime;
    private RequisitionStatus status; 
    private String assignedUnitId;

    public BloodRequest(String requestId, String hospitalName, String patientName, BloodType bloodType, PriorityLevel priority, LocalDateTime targetTime) {
        this.requestId = requestId;
        this.hospitalName = hospitalName;
        this.patientName = patientName;
        this.bloodType = bloodType;
        this.priority = priority;
        this.targetTime = targetTime;
        this.status = RequisitionStatus.PENDING; 
    }
    
    //Getters
    public String getRequestId() { return requestId; }
    public String getHospitalName() { return hospitalName; }
    public String getPatientName() { return patientName; }
    public BloodType getBloodType() { return bloodType; }
    public PriorityLevel getPriority() { return priority; }
    public LocalDateTime getTargetTime() { return targetTime; }
    public RequisitionStatus getStatus() { return status; }
    public String getAssignedUnitId() { return assignedUnitId; }
    //Setters
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public void setTargetTime(LocalDateTime targetTime) { this.targetTime = targetTime; }
    public void setStatus(RequisitionStatus status) { this.status = status; }
    public void setAssignedUnitId(String assignedUnitId) { this.assignedUnitId = assignedUnitId; }
    //toString
    @Override
    public String toString() {
        return requestId + " | " + hospitalName + " | " + patientName + " | "
                + bloodType.getCode() + " | " + priority + " | " + targetTime
                + " | " + status + " | " + (assignedUnitId == null ? "none" : assignedUnitId);
    }

}