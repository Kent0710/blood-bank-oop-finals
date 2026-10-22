public class HospitalAdmin extends User {
    public HospitalAdmin(String id, String fullName, String password) {
        super(id, fullName, Roles.HOSPITAL_ADMIN, "N/A", password);
    }

    @Override
    public String getRoleDescription() {
        return "Hospital Admin";
    }
}
