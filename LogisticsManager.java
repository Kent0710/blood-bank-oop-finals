public class LogisticsManager extends User {
    public LogisticsManager(String id, String fullName, String password) {
        super(id, fullName, Roles.LOGISTICS, "N/A", password);
    }

    public LogisticsManager(String id, String fullName, String hospitalAffiliation, String password) {
        super(id, fullName, Roles.LOGISTICS, hospitalAffiliation != null ? hospitalAffiliation : "N/A", password);
    }

    @Override
    public String getRoleDescription() {
        return "Logistics Dispatcher";
    }
}
