public enum UnitStatus {
    ACTIVE("Stored (Available in Stock)"),
    RESERVED("Reserved (Allocated to Requisition)"),
    QUARANTINED("Quarantined (Pending Testing)"),
    EXPIRED("Archived (Expired)"),
    PURGED("Purged (Disposed)"),
    IN_TESTING("In Testing (MLS Inspection)"),
    STORED("Stored (Available in Stock)"),
    IN_TRANSIT("In Transit (Logistics Distribution)"),
    DISTRIBUTED("Distributed (Received by Hospital)"),
    ARCHIVED_EXPIRED("Archived (Expired)"),
    ARCHIVED_CONTAMINATED("Archived (Contaminated)"),
    REJECTED("Rejected (Failed Inspection)");

    private final String description;

    UnitStatus(String description) {
        this.description = description;
    }

    UnitStatus() {
        this.description = name();
    }

    public String getDescription() {
        return description != null ? description : name();
    }
}
