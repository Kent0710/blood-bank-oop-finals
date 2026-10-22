import java.time.LocalDate;

public class BloodUnit {
    private String unitId;
    private BloodType bloodType;
    private BloodComponent component;
    private LocalDate donationDate;
    private LocalDate expirationDate; // Automatically set to donationDate.plusDays(42)
    private String donorSource;
    private UnitStatus status; // Default: UnitStatus.ACTIVE

    public BloodUnit(String unitId, BloodType bloodType, BloodComponent component, LocalDate donationDate, String donorSource) { 
        this.unitId = unitId;
        this.bloodType = bloodType;
        this.component = component;
        this.donationDate = donationDate;
        this.expirationDate = donationDate.plusDays(42);
        this.donorSource = donorSource;
        this.status = UnitStatus.ACTIVE; // Default status
    }
    // Standard getters, setters, toString()
    //Getters
    public String getUnitId() { return unitId; }
    public BloodType getBloodType() { return bloodType; }
    public BloodComponent getComponent() { return component; }
    public LocalDate getDonationDate() { return donationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getDonorSource() { return donorSource; }
    public UnitStatus getStatus() { return status; }
    //Setters
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }
    public void setComponent(BloodComponent component) { this.component = component; }
    public void setDonorSource(String donorSource) { this.donorSource = donorSource; }
    public void setStatus(UnitStatus status) { this.status = status; }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
        this.expirationDate = donationDate.plusDays(42);
    }
    //to String
    @Override
    public String toString() {
        return String.format("%-10s | %-4s | %-18s | %s | %s | %s",
            unitId, bloodType.getCode(), component,
            donationDate, expirationDate, status);
}
}