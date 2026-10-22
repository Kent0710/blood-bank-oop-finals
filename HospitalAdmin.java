public class HospitalAdmin extends User {
    public HospitalAdmin(String id, String fullName, String password) {
        super(id, fullName, Roles.HOSPITAL_ADMIN, "N/A", password);
    }

    public HospitalAdmin(String id, String fullName, String hospitalAffiliation, String password) {
        super(id, fullName, Roles.HOSPITAL_ADMIN, hospitalAffiliation, password);
    }

    public void setHospitalAffiliation(String hospitalAffiliation) {
        this.hospitalAffiliation = hospitalAffiliation;
    }

    @Override
    public String getRoleDescription() {
        return "Hospital Admin";
    }
}
