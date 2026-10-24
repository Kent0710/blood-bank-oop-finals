public class HospitalAdmin extends User {
    public HospitalAdmin(String id, String fullName, String password) {
        super(id, fullName, Roles.HOSPITAL_ADMIN, "N/A", password);
    }

    public HospitalAdmin(String id, String fullName, String hospitalAffiliation, String password) {
        super(id, fullName, Roles.HOSPITAL_ADMIN, hospitalAffiliation, password);
    }

    public String getHospitalName() {
        return getHospitalAffiliation();
    }

    public void setHospitalName(String hospitalName) {
        setHospitalAffiliation(hospitalName);
    }

    public void setHospitalAffiliation(String hospitalAffiliation) {
        this.hospitalAffiliation = hospitalAffiliation != null ? hospitalAffiliation : "N/A";
    }

    @Override
    public String getRoleDescription() {
        return "Sub Admin: Requests blood on behalf of hospital patients, registers patients, returns excess blood to Blood Bank.";
    }
}
