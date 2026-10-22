public class Patient extends User {
    private BloodType patientBloodType;

    public Patient(String id, String fullName, String hospitalAffiliation, String password, BloodType patientBloodType) {
        super(id, fullName, Roles.PATIENT, hospitalAffiliation, password);
        this.patientBloodType = patientBloodType;
    }

    public BloodType getPatientBloodType() {
        return patientBloodType;
    }

    public BloodType getBloodType() {
        return patientBloodType;
    }

    public void setBloodType(BloodType patientBloodType) {
        this.patientBloodType = patientBloodType;
    }

    public String getPatientId() {
        return getId();
    }

    @Override
    public String getRoleDescription() {
        return "Patient";
    }
}
