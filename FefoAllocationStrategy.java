import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete Allocation Strategy implementing FEFO (First Expired, First Out).
 * Filters active blood units matching the requested blood type and sorts them
 * by expirationDate in ascending order (nearest expiration first) to minimize
 * blood inventory spoilage and wastage.
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
            if (unit != null && (unit.getStatus() == UnitStatus.ACTIVE || unit.getStatus() == UnitStatus.STORED)) {
                if (unit.getBloodType() == requestedType || BloodType.isCompatible(unit.getBloodType(), requestedType)) {
                    matchingUnits.add(unit);
                }
            }
        }

        if (matchingUnits.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock: No active units found for blood type " + requestedType.getCode() + ".");
        }

        // Filters active units by requestedType, sorts by expirationDate ASC (nearest expiration first)
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

        BloodUnit selected = matchingUnits.get(0);

        System.out.println("================================================================================");
        System.out.println(">>> ALLOCATION STRATEGY ENGINE EXECUTION");
        System.out.println("================================================================================");
        System.out.println("Active Strategy : " + getStrategyName());
        System.out.println("Requested Type  : " + requestedType.getCode());
        System.out.println("Matching Units  : " + matchingUnits.size() + (matchingUnits.size() == 1 ? " unit found" : " units found"));
        System.out.println("Selected Unit   : " + selected.getUnitId() + " (Expiration: " + selected.getExpirationDate() + " -> Nearest Expiration First)");
        if (requestId != null && !requestId.trim().isEmpty()) {
            System.out.println("Status          : Allocated -> Assigned to " + requestId);
        } else {
            System.out.println("Status          : Allocated -> Reserved for requisition");
        }
        System.out.println("================================================================================");

        return selected;
    }

    @Override
    public BloodUnit selectUnit(List<BloodUnit> stock, BloodType requestedType, LocalDate targetDate)
            throws InsufficientStockException {
        if (stock == null || requestedType == null) {
            throw new InsufficientStockException("Cannot allocate: stock or requested type is null.");
        }
        LocalDate checkDate = (targetDate != null) ? targetDate : LocalDate.now();
        return stock.stream()
                .filter(u -> (u.getStatus() == UnitStatus.STORED || u.getStatus() == UnitStatus.ACTIVE) &&
                        !u.isExpiredOn(checkDate) &&
                        !u.isContaminated() &&
                        BloodType.isCompatible(u.getBloodType(), requestedType))
                .min(Comparator.comparing(BloodUnit::getExpirationDate))
                .orElseThrow(() -> new InsufficientStockException(
                        "FEFO Allocation Failed: No compatible, non-expired unit found for blood type '" +
                                requestedType.getLabel() + "' valid on date " + checkDate));
    }

    @Override
    public String getStrategyName() {
        return "FEFO (First Expired, First Out)";
    }
}
