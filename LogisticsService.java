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
    private final List<BloodRequest> allRequests = new ArrayList<>();
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

    public synchronized void registerRequest(BloodRequest request) {
        if (request != null && !allRequests.contains(request)) {
            allRequests.add(request);
        }
    }

    public synchronized List<BloodRequest> getAllRequests() {
        List<BloodRequest> combined = new ArrayList<>(allRequests);
        for (BloodRequest r : urgentQueue) {
            if (!combined.contains(r)) combined.add(r);
        }
        for (BloodRequest r : scheduledQueue) {
            if (!combined.contains(r)) combined.add(r);
        }
        for (BloodRequest r : InspectionService.getInstance().getAllRequisitions()) {
            if (!combined.contains(r)) combined.add(r);
        }
        return combined;
    }

    public synchronized List<BloodRequest> getRequestsForPatient(String patientName) {
        List<BloodRequest> result = new ArrayList<>();
        if (patientName == null) return result;
        for (BloodRequest req : getAllRequests()) {
            if (req.getPatientName() != null && req.getPatientName().equalsIgnoreCase(patientName.trim())) {
                result.add(req);
            }
        }
        return result;
    }

    /**
     * Enqueues a requisition into the appropriate dispatch queue based on priority.
     */
    public synchronized void enqueue(BloodRequest req) {
        if (req == null) return;
        registerRequest(req);

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

    public synchronized void enqueueApprovedRequest(BloodRequest req) {
        if (req == null) return;
        req.setStatus(RequisitionStatus.APPROVED);
        registerRequest(req);

        if (req.getPriority() == PriorityLevel.URGENT || req.getRequestType() == BloodRequest.RequestType.URGENT) {
            if (!urgentQueue.contains(req)) {
                urgentQueue.add(req);
            }
            System.out.println(">> [Logistics Hub] Priority URGENT request " + req.getRequestId() +
                    " enqueued at FRONT of urgent dispatch line.");
        } else {
            if (!scheduledQueue.contains(req)) {
                scheduledQueue.add(req);
            }
            System.out.println(">> [Logistics Hub] Scheduled request " + req.getRequestId() +
                    " added to standard delivery queue.");
        }
    }

    public synchronized boolean processScheduledDistribution(CentralInventory inventory) {
        if (scheduledQueue.isEmpty()) {
            System.out.println(">> [Logistics Hub] No scheduled requests pending in dispatch queue.");
            return false;
        }
        BloodRequest request = scheduledQueue.remove(0);
        return executeDistribution(request, inventory, scheduledStrategy);
    }

    public synchronized boolean processUrgentDistribution(CentralInventory inventory) {
        if (urgentQueue.isEmpty()) {
            System.out.println(">> [Logistics Hub] No urgent requests pending in priority queue.");
            return false;
        }
        BloodRequest request = urgentQueue.remove(0);
        return executeDistribution(request, inventory, urgentStrategy);
    }

    public synchronized boolean executeDistribution(BloodRequest request, CentralInventory inventory, DistributionStrategy strategy) {
        System.out.println("\n--- [Logistics Dispatch: " + request.getRequestId() + "] ---");
        System.out.println(">> [Logistics Hub] Active Strategy: " + strategy.getModeName());
        System.out.println(">> [Logistics Hub] Destination: " + request.getHospitalName() + " | Patient: " + request.getPatientName());
        System.out.println(">> [Logistics Hub] Required Blood: " + (request.getRequestedBloodType() != null ? request.getRequestedBloodType().getLabel() : "N/A"));

        try {
            BloodUnit unit = inventory.allocateBlood(request.getRequestedBloodType(), request.getNeededByDate());
            request.setAllocatedUnit(unit);

            strategy.prepareForDispatch(request, unit);
            request.setStatus(RequisitionStatus.IN_TRANSIT);

            strategy.executeDelivery(request, unit);
            request.setStatus(RequisitionStatus.COMPLETED);

            String logEntry = String.format("%-10s | %-10s | %-26s | %-20s | %-10s | %-8s",
                    request.getRequestType(), request.getRequestId(),
                    truncate(request.getHospitalName(), 26),
                    truncate(request.getPatientName(), 20),
                    unit != null ? unit.getDin() : "N/A",
                    unit != null && unit.getBloodType() != null ? unit.getBloodType().getLabel() : "N/A");
            transportLog.add(logEntry);

            System.out.println(">> [Logistics Hub] Delivery SUCCESSFUL! Verified chain-of-custody transfer.");
            return true;
        } catch (InsufficientStockException e) {
            System.out.println(">> [Logistics Alert] DISTRIBUTION HALTED: " + e.getMessage());
            System.out.println("   Request returned to pending dispatch line.");
            if (request.getPriority() == PriorityLevel.URGENT) {
                urgentQueue.add(0, request);
            } else {
                scheduledQueue.add(0, request);
            }
            return false;
        }
    }

    public synchronized void performIntakeTransport(BloodUnit unit, String sourceName) {
        System.out.println("\n--- [Logistics: Collection / Intake Transport] ---");
        System.out.println(">> [Logistics Hub] Intake mission assigned for source: " + sourceName);
        System.out.println(">> [Logistics Hub] Securing blood unit (DIN: " + (unit != null ? unit.getDin() : "N/A") + ") in certified cold-chain carrier (2.0°C - 6.0°C)...");
        System.out.println(">> [Logistics Hub] Transport completed. Handed over to Blood Bank Laboratory dock.");
        transportLog.add(String.format("%-10s | %-10s | %-26s | %-20s | %-10s | %-8s",
                "INTAKE", "INTAKE", truncate(sourceName, 26), "Blood Bank Lab",
                (unit != null ? unit.getDin() : "N/A"),
                (unit != null && unit.getBloodType() != null ? unit.getBloodType().getLabel() : "N/A")));
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

    public synchronized void displayQueues() {
        System.out.println("\n+-------------------------------------------------------------------------------------------------------------------+");
        System.out.println("|                                            LOGISTICS DISPATCH & TRANSIT HUB                                       |");
        System.out.println("+-------------------------------------------------------------------------------------------------------------------+");

        // Priority Urgent Queue
        System.out.println("\n[1] PRIORITY URGENT DISPATCH QUEUE (" + urgentQueue.size() + " pending):");
        if (urgentQueue.isEmpty()) {
            System.out.println("  (No urgent requests currently in queue)");
        } else {
            printRequestTableHeader();
            for (BloodRequest req : urgentQueue) {
                printRequestTableRow(req);
            }
            printRequestTableFooter();
        }

        // Standard Scheduled Queue
        System.out.println("\n[2] STANDARD SCHEDULED DISPATCH QUEUE (" + scheduledQueue.size() + " pending):");
        if (scheduledQueue.isEmpty()) {
            System.out.println("  (No scheduled requests currently in queue)");
        } else {
            printRequestTableHeader();
            for (BloodRequest req : scheduledQueue) {
                printRequestTableRow(req);
            }
            printRequestTableFooter();
        }

        // Historical Transport Records
        System.out.println("\n[3] HISTORICAL TRANSPORT & DISTRIBUTION RECORDS (" + transportLog.size() + " completed):");
        if (transportLog.isEmpty()) {
            System.out.println("  (No distribution records completed yet)");
        } else {
            System.out.println("+------------+------------+----------------------------+----------------------+------------+----------+");
            System.out.printf("| %-10s | %-10s | %-26s | %-20s | %-10s | %-8s |\n",
                    "Type", "Req ID", "Destination / Source", "Patient / Target", "Unit DIN", "Blood");
            System.out.println("+------------+------------+----------------------------+----------------------+------------+----------+");
            for (String log : transportLog) {
                System.out.printf("| %s |\n", log);
            }
            System.out.println("+------------+------------+----------------------------+----------------------+------------+----------+");
        }
    }

    private void printRequestTableHeader() {
        System.out.println("+------------+----------------------------+----------------------+--------+------------+-------------------+");
        System.out.printf("| %-10s | %-26s | %-20s | %-6s | %-10s | %-17s |\n",
                "Req ID", "Hospital", "Patient", "Type", "Priority", "Needed Target Time");
        System.out.println("+------------+----------------------------+----------------------+--------+------------+-------------------+");
    }

    private void printRequestTableRow(BloodRequest req) {
        System.out.printf("| %-10s | %-26s | %-20s | %-6s | %-10s | %-17s |\n",
                req.getRequestId(),
                truncate(req.getHospitalName(), 26),
                truncate(req.getPatientName(), 20),
                req.getRequestedBloodType() != null ? req.getRequestedBloodType().getLabel() : "N/A",
                req.getRequestType(),
                req.getFormattedNeededDateTime());
    }

    private void printRequestTableFooter() {
        System.out.println("+------------+----------------------------+----------------------+--------+------------+-------------------+");
    }

    public synchronized void clear() {
        scheduledQueue.clear();
        urgentQueue.clear();
        allRequests.clear();
        transportLog.clear();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
