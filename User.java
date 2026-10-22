public abstract class User {
    protected String id;
    protected String fullName;
    protected Roles role;
    protected String hospitalAffiliation;
    protected String password;
    protected boolean archived;

    public User(String id, String fullName, Roles role, String hospitalAffiliation, String password) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.hospitalAffiliation = hospitalAffiliation;
        this.password = password;
        this.archived = false;
    }

    public String getId() { return id; }
    public String getAccountId() { return id; }
    public String getFullName() { return fullName; }
    public String getName() { return fullName; }
    public Roles getRole() { return role; }
    public String getHospitalAffiliation() { return hospitalAffiliation; }
    public void setHospitalAffiliation(String hospitalAffiliation) { this.hospitalAffiliation = hospitalAffiliation; }
    public boolean isArchived() { return archived; }
    public boolean isActive() { return !archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public boolean checkPassword(String password) {
        return this.password != null && this.password.equals(password);
    }
    public boolean authenticate(String password) {
        return checkPassword(password);
    }
    public abstract String getRoleDescription();

    public enum AccountStatus { ACTIVE, PENDING, SUSPENDED }
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    public AccountStatus getAccountStatus() { return accountStatus; }
}
