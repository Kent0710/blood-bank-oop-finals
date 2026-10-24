import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BloodUnit {
    private String unitId;
    private BloodType bloodType;
    private BloodComponent component;
    private LocalDate donationDate;
    private LocalDate expirationDate;
    private String donorSource;
    private UnitStatus status;
    private int lifespanDays;
    private double temperatureCelsius;
    private boolean contaminated;
    private boolean mlsInspected;
    private boolean mlsPassed;
    private boolean scientistLogged;
    private boolean adminApproved;
    private String inspectionNotes;

    public BloodUnit(String unitId, BloodType bloodType, BloodComponent component, LocalDate donationDate, String donorSource) {
        this.unitId = unitId;
        this.bloodType = bloodType;
        this.component = component != null ? component : BloodComponent.WHOLE_BLOOD;
        this.donationDate = donationDate;
        this.lifespanDays = this.component.getStandardLifespanDays();
        this.expirationDate = (donationDate != null) ? donationDate.plusDays(lifespanDays) : null;
        this.donorSource = donorSource;
        this.status = UnitStatus.ACTIVE;
        this.temperatureCelsius = 4.0;
        this.contaminated = false;
        this.mlsInspected = true;
        this.mlsPassed = true;
        this.scientistLogged = true;
        this.adminApproved = true;
        this.inspectionNotes = "";
    }

    public BloodUnit(String din, BloodType bloodType, BloodComponent component, String source,
                     LocalDate collectionDate, int lifespanDays, double temperatureCelsius, boolean contaminated) {
        this.unitId = din;
        this.bloodType = bloodType;
        this.component = component != null ? component : BloodComponent.WHOLE_BLOOD;
        this.donorSource = source;
        this.donationDate = collectionDate;
        this.lifespanDays = lifespanDays > 0 ? lifespanDays : this.component.getStandardLifespanDays();
        this.expirationDate = (collectionDate != null) ? collectionDate.plusDays(this.lifespanDays) : null;
        this.temperatureCelsius = temperatureCelsius;
        this.contaminated = contaminated;
        this.status = UnitStatus.QUARANTINED;
        this.mlsInspected = false;
        this.mlsPassed = false;
        this.scientistLogged = false;
        this.adminApproved = false;
        this.inspectionNotes = "";
    }

    public BloodUnit(String din, BloodType bloodType, String source, LocalDate collectionDate,
                     int lifespanDays, double temperatureCelsius, boolean contaminated) {
        this(din, bloodType, BloodComponent.WHOLE_BLOOD, source, collectionDate, lifespanDays, temperatureCelsius, contaminated);
    }

    // Identifiers
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getDin() { return unitId; }
    public void setDin(String din) { this.unitId = din; }

    // Domain models
    public BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }

    public BloodComponent getComponent() { return component; }
    public void setComponent(BloodComponent component) {
        this.component = component;
        if (component != null && donationDate != null) {
            this.lifespanDays = component.getStandardLifespanDays();
            this.expirationDate = donationDate.plusDays(this.lifespanDays);
        }
    }

    // Dates
    public LocalDate getDonationDate() { return donationDate; }
    public LocalDate getCollectionDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
        if (donationDate != null) {
            this.expirationDate = donationDate.plusDays(lifespanDays > 0 ? lifespanDays : 42);
        }
    }
    public void setCollectionDate(LocalDate collectionDate) {
        setDonationDate(collectionDate);
    }

    public LocalDate getExpirationDate() { return expirationDate; }
    public LocalDate getExpiryDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public boolean isExpiredOn(LocalDate targetDate) {
        if (expirationDate == null || targetDate == null) return false;
        return targetDate.isAfter(expirationDate);
    }

    public boolean isCurrentlyExpired() {
        return isExpiredOn(LocalDate.now());
    }

    public long getRemainingDays(LocalDate targetDate) {
        if (expirationDate == null || targetDate == null) return 0;
        return ChronoUnit.DAYS.between(targetDate, expirationDate);
    }

    // Source
    public String getDonorSource() { return donorSource; }
    public void setDonorSource(String donorSource) { this.donorSource = donorSource; }
    public String getSource() { return donorSource; }
    public void setSource(String source) { this.donorSource = source; }

    // Status
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }

    // Lifespan & Cold-chain
    public int getLifespanDays() { return lifespanDays; }
    public void setLifespanDays(int lifespanDays) {
        this.lifespanDays = lifespanDays;
        if (donationDate != null && lifespanDays > 0) {
            this.expirationDate = donationDate.plusDays(lifespanDays);
        }
    }

    public double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public boolean isContaminated() { return contaminated; }
    public void setContaminated(boolean contaminated) { this.contaminated = contaminated; }

    // Inspection flags
    public boolean isMlsInspected() { return mlsInspected; }
    public void setMlsInspected(boolean mlsInspected) { this.mlsInspected = mlsInspected; }

    public boolean isMlsPassed() { return mlsPassed; }
    public void setMlsPassed(boolean mlsPassed) { this.mlsPassed = mlsPassed; }

    public boolean isScientistLogged() { return scientistLogged; }
    public void setScientistLogged(boolean scientistLogged) { this.scientistLogged = scientistLogged; }

    public boolean isAdminApproved() { return adminApproved; }
    public void setAdminApproved(boolean adminApproved) { this.adminApproved = adminApproved; }

    public String getInspectionNotes() { return inspectionNotes; }
    public void setInspectionNotes(String inspectionNotes) { this.inspectionNotes = inspectionNotes; }

    @Override
    public String toString() {
        return String.format("%-10s | %-4s | %-18s | %s | %s | %s",
                unitId, (bloodType != null ? bloodType.getCode() : "N/A"),
                (component != null ? component.getDisplayName() : "N/A"),
                donationDate, expirationDate, status);
    }
}
