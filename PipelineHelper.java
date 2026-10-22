import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * PipelineHelper coordinates the modular 5-stage clinical intake workflow
 * for screened blood donations entering the Central Blood Bank inventory.
 */
public class PipelineHelper {

    private static final Random RANDOM = new Random();
    private static final Set<String> ISSUED_UNIT_IDS = new HashSet<>();

    /**
     * Executes the modular 5-stage clinical intake pipeline.
     *
     * @param sc       Active Scanner instance for reading console inputs
     * @param source   Default origin facility / hospital affiliation (can be null or "N/A")
     * @param reprompt Optional callback invoked when validation errors occur
     * @return The validated and registered BloodUnit
     */
    public static BloodUnit runClinicalIntakePipeline(Scanner sc, String source, Runnable reprompt) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        System.out.println("------------------------------------------------------------");
        System.out.println(">>> CLINICAL BLOOD INTAKE & SCREENING PIPELINE");
        System.out.println("------------------------------------------------------------");

        // Stage 1: Donor Identification -> Prompt Donor ID & Collection Facility
        String[] stage1Result = stage1DonorIdentification(sc, source, reprompt);
        String donorId = stage1Result[0];
        String facility = stage1Result[1];

        // Stage 2: Serology & Typing -> Prompt ABO/Rh Blood Type (e.g. O+, A-)
        BloodType bloodType = stage2SerologyAndTyping(sc, reprompt);

        // Stage 3: Component Separation -> Select [1] Whole Blood [2] PRBC [3] Platelets [4] Plasma
        BloodComponent component = stage3ComponentSeparation(sc, reprompt);

        // Stage 4: Cold-Chain Storage -> Auto-calculate 42-day expiration date
        LocalDate donationDate = LocalDate.now();
        LocalDate expirationDate = stage4ColdChainStorage(donationDate);

        // Stage 5: Quality Release -> Generate Unit ID (e.g. BU-8391) & Set Status ACTIVE
        BloodUnit bloodUnit = stage5QualityRelease(bloodType, component, donationDate, facility);

        // Render completion receipt banner
        renderCompletionBanner(bloodUnit);

        return bloodUnit;
    }

    /**
     * Overload for runClinicalIntakePipeline without reprompt callback.
     */
    public static BloodUnit runClinicalIntakePipeline(Scanner sc, String source) {
        return runClinicalIntakePipeline(sc, source, null);
    }

    /**
     * Overload for runClinicalIntakePipeline without default source and reprompt callback.
     */
    public static BloodUnit runClinicalIntakePipeline(Scanner sc) {
        return runClinicalIntakePipeline(sc, null, null);
    }

    /**
     * Stage 1: Donor Identification & Collection Facility
     * Collects donor ID and origin facility.
     */
    public static String[] stage1DonorIdentification(Scanner sc, String source, Runnable reprompt) {
        System.out.println("Stage 1: Donor Identification -> Prompt Donor ID & Collection Facility");
        String donorId = InputValidator.readNonBlankString(sc, "  Enter Donor ID: ", reprompt);

        String facility = source;
        if (facility == null || facility.trim().isEmpty() || facility.equalsIgnoreCase("N/A")) {
            facility = InputValidator.readNonBlankString(sc, "  Enter Collection Facility: ", reprompt);
        } else {
            facility = facility.trim();
            System.out.println("  Collection Facility: " + facility);
        }

        return new String[]{ donorId, facility };
    }

    /**
     * Stage 2: Serology & Typing
     * Validates and parses ABO/Rh blood type with enum parsing.
     */
    public static BloodType stage2SerologyAndTyping(Scanner sc, Runnable reprompt) {
        System.out.println("Stage 2: Serology & Typing -> Prompt ABO/Rh Blood Type (e.g. O+, A-)");
        BloodType bloodType = null;

        while (bloodType == null) {
            System.out.print("  Enter ABO/Rh Blood Type (e.g. O+, A-, B+, AB-): ");
            String input = sc.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Blood type cannot be empty.");
                if (reprompt != null) {
                    reprompt.run();
                }
                continue;
            }

            try {
                bloodType = BloodType.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("  [Validation Error] " + e.getMessage() + " (Allowed: O+, O-, A+, A-, B+, B-, AB+, AB-)");
                if (reprompt != null) {
                    reprompt.run();
                }
            }
        }
        return bloodType;
    }

    /**
     * Stage 3: Component Separation
     * Prompts user to select from component separation options.
     */
    public static BloodComponent stage3ComponentSeparation(Scanner sc, Runnable reprompt) {
        System.out.println("Stage 3: Component Separation -> Select [1] Whole Blood [2] PRBC [3] Platelets [4] Plasma");
        int choice = InputValidator.readInt(sc, "  Enter component choice (1-4): ", 1, 4, reprompt);

        switch (choice) {
            case 1:
                return BloodComponent.WHOLE_BLOOD;
            case 2:
                return BloodComponent.PACKED_RED_CELLS;
            case 3:
                return BloodComponent.PLATELETS;
            case 4:
                return BloodComponent.PLASMA;
            default:
                return BloodComponent.WHOLE_BLOOD;
        }
    }

    /**
     * Stage 4: Cold-Chain Storage
     * Automatically calculates the 42-day cold-chain expiration date.
     */
    public static LocalDate stage4ColdChainStorage(LocalDate donationDate) {
        System.out.println("Stage 4: Cold-Chain Storage -> Auto-calculate 42-day expiration");
        if (donationDate == null) {
            donationDate = LocalDate.now();
        }
        return donationDate.plusDays(42);
    }

    /**
     * Stage 5: Quality Release
     * Generates a unique unit identifier (BU-xxxx) and initializes the BloodUnit with ACTIVE status.
     */
    public static BloodUnit stage5QualityRelease(BloodType bloodType, BloodComponent component, LocalDate donationDate, String facility) {
        System.out.println("Stage 5: Quality Release -> Generate Unit ID (e.g. BU-8391) & Set Status ACTIVE");
        String unitId = generateUnitId();
        BloodUnit unit = new BloodUnit(unitId, bloodType, component, donationDate, facility);
        unit.setStatus(UnitStatus.ACTIVE);
        return unit;
    }

    /**
     * Generates a unique unit identifier in BU-xxxx format.
     */
    public static synchronized String generateUnitId() {
        String id;
        do {
            int randomNum = 1000 + RANDOM.nextInt(9000);
            id = "BU-" + randomNum;
        } while (ISSUED_UNIT_IDS.contains(id));
        ISSUED_UNIT_IDS.add(id);
        return id;
    }

    /**
     * Renders the completion receipt banner with unit details.
     */
    public static void renderCompletionBanner(BloodUnit unit) {
        System.out.println("------------------------------------------------------------");
        System.out.printf("[SUCCESS] Unit %s (%s %s) released to Central Inventory!\n",
                unit.getUnitId(), unit.getBloodType().getCode(), unit.getComponent().name());
        System.out.println("------------------------------------------------------------");
    }
}
