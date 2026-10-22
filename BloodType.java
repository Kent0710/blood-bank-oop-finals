public enum BloodType {
    O_POS("O+"),
    O_NEG("O-"),
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-");

    private final String code;
    BloodType(String code) { this.code = code; }
    public String getCode() { return code; }
    public static BloodType fromString(String code) { 
        for (BloodType type : BloodType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid blood type: " + code);
     }
}
