public class Patient extends User {
    private BloodType patientBloodType;

    public Patient(String id, String fullName, String hospitalAffiliation, String password, BloodType patientBloodType) {
        super(id, fullName, Roles.PATIENT, hospitalAffiliation, password);
        this.patientBloodType = patientBloodType;
    }

    public Patient(String id, String fullName, String password, BloodType patientBloodType) {
        this(id, fullName, "Walk-in", password, patientBloodType);
    }

    public Patient(String fullName, BloodType patientBloodType, String id, String password) {
        this(id, fullName, "Walk-in", password, patientBloodType);
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
        return "Patient (Read-Focused Access): Views personal blood profile, compatibility matrix, and tracks status of blood requests filed by hospital.";
    }

    public void displayBloodProfile() {
        String receiveList = (patientBloodType != null) ? String.join(", ", patientBloodType.getCompatibleDonors()) : "N/A";
        String donateList = (patientBloodType != null) ? String.join(", ", patientBloodType.getCompatibleRecipients()) : "N/A";

        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                                PATIENT BLOOD PROFILE                              |");
        System.out.println("+-------------------+---------------------------------------------------------------+");
        System.out.printf("| %-17s | %-61s |\n", "Patient Account ID", getAccountId());
        System.out.printf("| %-17s | %-61s |\n", "Patient Name", getName());
        System.out.printf("| %-17s | %-61s |\n", "Account Status", isActive() ? "Active" : "Archived");
        System.out.printf("| %-17s | %-61s |\n", "Blood Type", patientBloodType != null ? patientBloodType.getLabel() : "N/A");
        System.out.printf("| %-17s | %-61s |\n", "Can Receive From", receiveList);
        System.out.printf("| %-17s | %-61s |\n", "Can Donate To", donateList);
        System.out.println("+-------------------+---------------------------------------------------------------+");
    }
}
