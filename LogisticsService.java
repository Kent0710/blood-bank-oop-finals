import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LogisticsService: Thread-safe Singleton coordinating physical transport,
 * prioritized queuing, and dispatching strategies for blood consignments.
 */
public class LogisticsService {
    private static LogisticsService instance;
    private final List<BloodRequest> scheduledQueue = new ArrayList<>();
    private final List<BloodRequest> urgentQueue = new ArrayList<>();
    private final List<String> transportLog = new ArrayList<>();

    private final DistributionStrategy scheduledStrategy;
    private final DistributionStrategy urgentStrategy;

    private LogisticsService() {
        this.scheduledStrategy = new ScheduledDistributionStrategy();
        this.urgentStrategy = new UrgentDistributionStrategy();
    }

    public static synchronized LogisticsService getInstance() {
        if (instance == null) {
            instance = new LogisticsService();
        }
        return instance;
    }

    /**
     * Enqueues a requisition into the appropriate dispatch queue based on priority.
     */
    public synchronized void enqueue(BloodRequest req) {
        if (req == null) return;

        if (req.getPriority() == PriorityLevel.URGENT) {
            if (!urgentQueue.contains(req)) {
                urgentQueue.add(req);
            }
        } else {
            if (!scheduledQueue.contains(req)) {
                scheduledQueue.add(req);
            }
        }
    }

    /**
     * Dispatches a blood unit for the specified requisition ID.
     * Updates status from IN_TRANSIT to DELIVERED.
     */
    public synchronized void dispatchUnit(String reqId) {
        if (reqId == null) return;

        BloodRequest target = null;
        boolean wasUrgent = false;

        // Check urgent queue first
        for (Iterator<BloodRequest> it = urgentQueue.iterator(); it.hasNext(); ) {
            BloodRequest r = it.next();
            if (r.getRequestId() != null && r.getRequestId().equalsIgnoreCase(reqId.trim())) {
                target = r;
                wasUrgent = true;
                it.remove();
                break;
            }
        }

        // If not in urgent queue, check scheduled queue
        if (target == null) {
            for (Iterator<BloodRequest> it = scheduledQueue.iterator(); it.hasNext(); ) {
                BloodRequest r = it.next();
                if (r.getRequestId() != null && r.getRequestId().equalsIgnoreCase(reqId.trim())) {
                    target = r;
                    wasUrgent = false;
                    it.remove();
                    break;
                }
            }
        }

        // If not found in queues, search central InspectionService requisitions
        if (target == null) {
            target = InspectionService.getInstance().getRequisitionById(reqId);
            if (target != null) {
                wasUrgent = (target.getPriority() == PriorityLevel.URGENT);
            }
        }

        if (target == null) {
            System.out.println("[Logistics Hub] Error: No requisition found with ID '" + reqId + "'.");
            return;
        }

        // Status transition: IN_TRANSIT -> DELIVERED
        target.setStatus(RequisitionStatus.IN_TRANSIT);

        // Execute strategy dispatch logging
        DistributionStrategy strategy = wasUrgent ? urgentStrategy : scheduledStrategy;
        strategy.dispatch(target);

        target.setStatus(RequisitionStatus.DELIVERED);

        String logEntry = String.format("%-10s | %-10s | %-26s | %-20s | %-6s | %s",
                target.getPriority(),
                target.getRequestId(),
                target.getHospitalName() != null ? target.getHospitalName() : "N/A",
                target.getPatientName() != null ? target.getPatientName() : "N/A",
                target.getBloodType() != null ? target.getBloodType().getCode() : "N/A",
                target.getStatus());
        transportLog.add(logEntry);

        System.out.println(">> [Logistics Hub] Requisition " + target.getRequestId() + " successfully delivered to " + target.getHospitalName() + ".");
    }

    public synchronized List<BloodRequest> getScheduledQueue() {
        return new ArrayList<>(scheduledQueue);
    }

    public synchronized List<BloodRequest> getUrgentQueue() {
        return new ArrayList<>(urgentQueue);
    }

    public synchronized List<BloodRequest> getAllQueuedRequests() {
        List<BloodRequest> all = new ArrayList<>(urgentQueue);
        all.addAll(scheduledQueue);
        return all;
    }

    public synchronized List<String> getTransportLog() {
        return new ArrayList<>(transportLog);
    }

    public synchronized void enqueueApprovedRequest(BloodRequest req) {
        if (req != null) {
            req.setStatus(RequisitionStatus.APPROVED);
            enqueue(req);
        }
    }

    public synchronized void displayQueues() {
        System.out.println("================================================================================");
        System.out.println("                     ACTIVE LOGISTICS DISPATCH QUEUES                           ");
        System.out.println("================================================================================");
        System.out.println("[URGENT DISPATCH QUEUE (" + urgentQueue.size() + " items)]:");
        if (urgentQueue.isEmpty()) {
            System.out.println("  (No urgent requisitions pending)");
        } else {
            for (BloodRequest req : urgentQueue) {
                System.out.println("  - " + req);
            }
        }

        System.out.println("\n[SCHEDULED BATCH QUEUE (" + scheduledQueue.size() + " items)]:");
        if (scheduledQueue.isEmpty()) {
            System.out.println("  (No scheduled requisitions pending)");
        } else {
            for (BloodRequest req : scheduledQueue) {
                System.out.println("  - " + req);
            }
        }
        System.out.println("================================================================================");
    }

    public synchronized void clear() {
        scheduledQueue.clear();
        urgentQueue.clear();
        transportLog.clear();
    }
}
