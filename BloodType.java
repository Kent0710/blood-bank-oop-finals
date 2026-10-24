import java.util.ArrayList;
import java.util.List;

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

    BloodType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return code;
    }

    public static BloodType fromString(String text) {
        if (text == null) return null;
        for (BloodType type : BloodType.values()) {
            if (type.code.equalsIgnoreCase(text) || type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid blood type: " + text);
    }

    public static boolean isCompatible(BloodType donor, BloodType recipient) {
        if (donor == null || recipient == null) return false;
        if (donor == recipient) return true;
        if (donor == O_NEG) return true;
        if (recipient == AB_POS) return true;

        switch (recipient) {
            case A_POS:
                return donor == A_NEG || donor == O_POS || donor == O_NEG;
            case A_NEG:
                return donor == O_NEG;
            case B_POS:
                return donor == B_NEG || donor == O_POS || donor == O_NEG;
            case B_NEG:
                return donor == O_NEG;
            case AB_NEG:
                return donor == A_NEG || donor == B_NEG || donor == O_NEG;
            case O_POS:
                return donor == O_NEG;
            case O_NEG:
                return donor == O_NEG;
            default:
                return false;
        }
    }

    public List<String> getCompatibleDonors() {
        List<String> list = new ArrayList<>();
        for (BloodType donor : BloodType.values()) {
            if (isCompatible(donor, this)) {
                list.add(donor.getLabel());
            }
        }
        return list;
    }

    public List<String> getCompatibleRecipients() {
        List<String> list = new ArrayList<>();
        for (BloodType recipient : BloodType.values()) {
            if (isCompatible(this, recipient)) {
                list.add(recipient.getLabel());
            }
        }
        return list;
    }
}
