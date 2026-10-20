package Role;

public class LogisticsManager extends User {
    public LogisticsManager(String id, String fullName, String password) {
        super(id, fullName, Roles.LOGISTICS, "N/A", password);
    }

    @Override
    public String getRoleDescription() {
        return "LOGISTICS";
    }
}