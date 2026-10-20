package Role;

public class BloodBank extends User {
    public BloodBank(String id, String fullName, String password) {
        super(id, fullName, Roles.SUPER_ADMIN, "N/A", password);
    }

    @Override
    public String getRoleDescription() {
        return "Super Admin";
    }
}