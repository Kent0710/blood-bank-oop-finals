public enum BloodComponent {
    WHOLE_BLOOD("Whole Blood", 42),
    PACKED_RED_CELLS("Packed Red Blood Cells (PRBC)", 42),
    FRESH_FROZEN_PLASMA("Fresh Frozen Plasma (FFP)", 365),
    PLASMA("Plasma", 365),
    PLATELETS("Platelets", 7);

    private final String displayName;
    private final int standardLifespanDays;

    BloodComponent(String displayName, int standardLifespanDays) {
        this.displayName = displayName;
        this.standardLifespanDays = standardLifespanDays;
    }

    BloodComponent() {
        this.displayName = name();
        this.standardLifespanDays = 42;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name();
    }

    public int getStandardLifespanDays() {
        return standardLifespanDays;
    }

    public static BloodComponent fromChoice(String choice) {
        if (choice == null) return WHOLE_BLOOD;
        switch (choice.trim()) {
            case "1": return WHOLE_BLOOD;
            case "2": return PACKED_RED_CELLS;
            case "3": return FRESH_FROZEN_PLASMA;
            case "4": return PLATELETS;
            default: return WHOLE_BLOOD;
        }
    }
}
