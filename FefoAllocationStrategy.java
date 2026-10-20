import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete Allocation Strategy implementing FEFO (First Expired, First Out).
 * Filters active blood units matching the requested blood type and sorts them
 * by expirationDate in ascending order (earliest expiring first) to minimize
 * clinical inventory spoilage.
 */
public class FefoAllocationStrategy implements AllocationStrategy {

    @Override
    public BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType) throws InsufficientStockException {
        return allocate(availableUnits, requestedType, null);
    }

    /**
     * Overloaded allocation method that accepts a specific requisition/request ID
     * for visual tracking and audit logs matching the terminal execution specification.
     */
    public BloodUnit allocate(List<BloodUnit> availableUnits, BloodType requestedType, String requestId) throws InsufficientStockException {
        if (availableUnits == null || requestedType == null) {
            throw new InsufficientStockException("Cannot allocate: available units list or requested blood type is null.");
        }

        List<BloodUnit> matchingUnits = new ArrayList<>();
        for (BloodUnit unit : availableUnits) {
            if (unit != null && unit.getStatus() == UnitStatus.ACTIVE && unit.getBloodType() == requestedType) {
                matchingUnits.add(unit);
            }
        }

        if (matchingUnits.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock: No active units found for blood type " + requestedType.getCode() + ".");
        }

        // Filters active units by requestedType, sorts by expirationDate ASC (earliest expiring first)
        Collections.sort(matchingUnits, new Comparator<BloodUnit>() {
            @Override
            public int compare(BloodUnit u1, BloodUnit u2) {
                LocalDate exp1 = u1.getExpirationDate();
                LocalDate exp2 = u2.getExpirationDate();
                if (exp1 == null && exp2 == null) return 0;
                if (exp1 == null) return 1;
                if (exp2 == null) return -1;
                int cmp = exp1.compareTo(exp2);
                if (cmp != 0) return cmp;

                LocalDate don1 = u1.getDonationDate();
                LocalDate don2 = u2.getDonationDate();
                if (don1 == null && don2 == null) return 0;
                if (don1 == null) return 1;
                if (don2 == null) return -1;
                return don1.compareTo(don2);
            }
        });

        BloodUnit selectedUnit = matchingUnits.get(0);

        printExecutionBanner(requestedType.getCode(), matchingUnits.size(), selectedUnit, requestId);

        return selectedUnit;
    }

    /**
     * Overloaded allocation method accepting a BloodRequest.
     */
    public BloodUnit allocate(List<BloodUnit> availableUnits, BloodRequest request) throws InsufficientStockException {
        if (request == null) {
            throw new InsufficientStockException("Cannot allocate: BloodRequest is null.");
        }
        return allocate(availableUnits, request.getBloodType(), request.getRequestId());
    }

    private void printExecutionBanner(String typeCode, int matchCount, BloodUnit selected, String requestId) {
        System.out.println("================================================================================");
        System.out.println(">>> ALLOCATION STRATEGY ENGINE EXECUTION");
        System.out.println("================================================================================");
        System.out.println("Active Strategy : " + getStrategyName());
        System.out.println("Requested Type  : " + typeCode);
        System.out.println("Matching Units  : " + matchCount + (matchCount == 1 ? " unit found" : " units found"));
        System.out.println("Selected Unit   : " + selected.getUnitId() + " (Expiry: " + selected.getExpirationDate() + " -> Earliest Expiring)");
        if (requestId != null && !requestId.trim().isEmpty()) {
            System.out.println("Status          : Allocated -> Assigned to " + requestId.trim());
        } else {
            System.out.println("Status          : Allocated");
        }
        System.out.println("================================================================================");
    }

    @Override
    public String getStrategyName() {
        return "FEFO (First Expired, First Out)";
    }
}
