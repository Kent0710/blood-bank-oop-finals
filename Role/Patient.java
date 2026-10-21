package Role;

public class Patient extends User {
    private BloodType patientBloodType;

    public Patient(String id, String fullName, String hospitalAffiliation, String password, BloodType patientBloodType) {
        super(id, fullName, Roles.PATIENT, hospitalAffiliation, password);
        this.patientBloodType = patientBloodType;
    }

    @Override
    public String getRoleDescription() {
        return "Patient";
    }

    public enum BloodType {
        A_POSITIVE, A_NEGATIVE,
        B_POSITIVE, B_NEGATIVE,
        AB_POSITIVE, AB_NEGATIVE,
        O_POSITIVE, O_NEGATIVE
    }
}