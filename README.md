# Central Blood Bank Management & Logistics System

**Course:** Object-Oriented Programming (OOP) Midterm Project  
**Language & Platform:** Pure Java (JDK 11+ Compatible Standard Library)

---

## 1. Team Members & Domain Contributions

| Member | Assigned Domain & Responsibilities | Key Implemented Modules |
|---|---|---|
| **Nicole** | Clinical Domain Models, 5-Stage Blood Donation Intake Pipeline, Hospital Requisitions & Bedside Patient Registration | `BloodUnit`, `BloodType`, `BloodComponent`, `UnitStatus`, `BloodRequest`, `PriorityLevel`, `PipelineHelper`, `HospitalAdminController` |
| **Irish** | Central Inventory Management, GoF Allocation Strategy (FIFO vs FEFO), Low-Stock Threshold Alerts & Stock Purge | `CentralInventory`, `AllocationStrategy`, `FifoAllocationStrategy`, `FefoAllocationStrategy`, `CentralInventoryUnitTest`, `Sprint4InventoryUnitTest` |
| **Nigel** | Transport Records, Logistics Priority Queues, Patient Portal, Dynamic ASCII Table Renderer Engine & Coordinator | `TableRenderer`, `DistributionStrategy`, `ScheduledDistributionStrategy`, `UrgentDistributionStrategy`, `LogisticsManager`, `LogisticsService` |
| **Kent** | System Architecture, Input Validation Engine, User Hierarchy, Core Singletons, Authentication Controller, Super Admin Controller, Multi-Criteria Filtering/Sorting, DevOps Automation & Coordinator | `User`, `BloodBank`, `HospitalAdmin`, `Patient`, `Custom Exceptions`, `InputValidator`, `UserService`, `InspectionService`, `AuthController`, `SuperAdminController`, `Main`, `TestRunner`, `run.sh`, `run.bat` |

---

## 2. Project Overview

The **Central Blood Bank Management & Logistics System** is an enterprise-grade Java terminal application designed to streamline blood collection, rigorous safety testing, component processing, central inventory management, hospital distribution, and patient transfusion tracking.

Healthcare systems often face severe operational challenges: mismatched blood units, expired components, cold-chain violations, and lack of real-time logistics tracking. This application addresses these bottlenecks by providing:
- Single-source-of-truth inventory management using Donation Identification Numbers (DIN).
- Configurable **GoF Allocation Strategies (FIFO vs FEFO)** with ABO/Rh compatibility matching.
- **48-Hour Lead-Time Auto-Escalation** upgrading urgent clinical requisitions to emergency dispatch.
- Decoupled role dashboards for Super Admins, Hospital Admins, Logistics Dispatchers, and Patients.
- Strict input validation and custom clinical exception handling to ensure zero runtime crashes.

---

## 3. Gang of Four (GoF) Design Patterns

### 1. Singleton Design Pattern
Stateful core managers enforce thread-safe global access points with synchronized instances:
- **`CentralInventory.getInstance()`**: Guarantees a single source of truth for all active stock, quarantine stock, low-supply alert thresholds, and automated expiration purges.
- **`UserService.getInstance()`**: Manages role-prefixed account ID generation (`BB-xxxx`, `HA-xxxx`, `LG-xxxx`, `PA-xxxx`), credentials, authentication, and active/archived account status.
- **`InspectionService.getInstance()`**: Coordinates central requisition storage, hospital requests, and the 48-hour auto-escalation engine.
- **`LogisticsService.getInstance()`**: Coordinates dual-queue physical dispatching (`urgentQueue` for STAT couriers and `scheduledQueue` for batch deliveries) and transport audit logs.

### 2. Strategy Design Pattern

#### A. Blood Unit Allocation Strategy (`AllocationStrategy`)
Decouples unit allocation algorithms from `CentralInventory`, allowing dynamic strategy switching at runtime:
- **`FifoAllocationStrategy`** *(Default)*: First-In, First-Out by donation collection date. Allocates the oldest compatible blood unit to prevent inventory aging.
- **`FefoAllocationStrategy`**: First-Expired, First-Out by expiration date. Allocates units closest to expiry to minimize clinical component wastage.

```
       +---------------------------------------------+
       |           <<interface>>                     |
       |         AllocationStrategy                  |
       +---------------------------------------------+
       | + allocate(stock, type): BloodUnit          |
       | + getStrategyName(): String                 |
       +---------------------------------------------+
                              ^
             ┌────────────────┴─────────────────┐
             │                                   │
+───────────────────────+           +───────────────────────+
| FifoAllocationStrategy|           | FefoAllocationStrategy|
+───────────────────────+           +───────────────────────+
| (Oldest Collection)   |           | (Nearest Expiry Date) |
+───────────────────────+           +───────────────────────+
```

#### B. Distribution & Transport Strategy (`DistributionStrategy`)
Encapsulates logistics transport packaging and courier routing:
- **`ScheduledDistributionStrategy`**: Standard refrigerated fleet transport for routine hospital orders.
- **`UrgentDistributionStrategy`**: Point-to-point emergency courier routing bypassing scheduled batch lines for critical orders.

---

## 4. Object-Oriented Programming (OOP) Architecture

```
                                  +-------------------+
                                  |   <<abstract>>    |
                                  |       User        |
                                  +-------------------+
                                  | - id: String      |
                                  | - fullName: String|
                                  | - role: Roles     |
                                  | - archived: bool  |
                                  +-------------------+
                                  | + getRoleDesc()*  |
                                  +-------------------+
                                            ^
          +-------------------+-------------+-------------+-------------------+
          |                   |                           |                   |
+-------------------+ +-----------------------+ +--------------------+ +-------------------+
|     BloodBank     | |     HospitalAdmin     | |  LogisticsManager  | |      Patient      |
+-------------------+ +-----------------------+ +--------------------+ +-------------------+
| (Super Admin)     | | (Hospital Sub Admin)  | | (Logistics Dispatch| | (Self-Service     |
| BB-1001           | | HA-2001+              | |  LG-3001+)         | |  PA-4001+)        |
+-------------------+ +-----------------------+ +--------------------+ +-------------------+
```

- **Encapsulation**: All fields are private or protected with defensive copying on collections to prevent external state tampering.
- **Inheritance**: Base `User` class shares authentication and identifier logic across specialized role types.
- **Polymorphism**: `getRoleDescription()` implemented polymorphically; dynamic role dispatching in `AuthController` and `Main`.
- **Abstraction**: Complex multi-step operations (5-stage intake, allocation, cold-chain transport, quarantine isolation) are hidden behind clear, cohesive service contracts.

---

## 5. Custom Exception Hierarchy

```
                      +----------------------+
                      | java.lang.Exception  |
                      +----------------------+
                                 ^
                                 │
                      +----------------------+
                      |  BloodBankException  |
                      +----------------------+
                                 ^
         ┌───────────────────────┼───────────────────────┬───────────────────────┐
         │                       │                       │                       │
+───────────────────+   +─────────────────+     +─────────────────+     +──────────────────────+
| InsufficientStock |   | ExpiredBlood    |     | Contaminated    |     | UserNotFoundException|
| Exception         |   | Exception       |     | Exception       |     | / AuthenticationEx.  |
+───────────────────+   +─────────────────+     +─────────────────+     +──────────────────────+
```

| Exception Class | Trigger Condition | Handling Strategy |
|---|---|---|
| `InsufficientStockException` | No compatible active blood units exist for requisition. | Requisition held; Super Admin alerted with restock prompt. |
| `ExpiredBloodException` | Unit lifespan exceeded on current date. | Unit rejected from distribution and isolated to Quarantine. |
| `ContaminatedBloodException` | Bacterial / serological contamination detected by MLS. | Unit quarantined with biohazard tag. |
| `UserNotFoundException` | Account ID does not exist in `UserService`. | Access denied with friendly guidance. |
| `AuthenticationException` | Incorrect password or account is archived/disabled. | Access denied; prevents unauthorized session start. |
| `InvalidInputException` | Malformed name, DIN, or out-of-range numeric input. | Handled gracefully with input reprompt callbacks. |

---

## 6. Core Features by Role

### 1. Super Admin Dashboard (Blood Bank Central Hub)
1. **Provision Employee Accounts**: Create verified `HospitalAdmin` (`HA-xxxx`) and `LogisticsManager` (`LG-xxxx`) accounts.
2. **Review & Process Hospital Requisitions**: Dynamic ASCII table view with multi-criteria filtering (Blood Type, Priority, Status) and sorting (Target Date, Priority). Allocates compatible blood via active GoF strategy and routes to Logistics.
3. **Store Blood from Hospital**: Runs the 5-Stage Clinical Intake Pipeline for hospital collections.
4. **Store Blood from Organization Blood Drive**: Runs intake pipeline with bulk drive affiliation tagging.
5. **View Central Inventory Stock Ledger**: Displays active units with real-time **Low-Stock Safety Alerts**.
6. **View Quarantine Stock Ledger**: Inspects expired, contaminated, or quarantined blood units.
7. **Audit & Purge Expired Blood Units**: Scans active inventory and relocates expired units to quarantine with an audit receipt.
8. **Configure Low-Stock Alert Threshold**: Customize the safety threshold (1 to 50 units).
9. **Switch Allocation Strategy**: Toggle between **FIFO** (oldest collection date) and **FEFO** (nearest expiry date) at runtime.
10. **User Account Ledger**: View all registered users, toggle archive/disabled status, and permanently delete accounts (primary Super Admin `BB-1001` is strictly protected).

### 2. Hospital Admin Dashboard
- **Submit Blood Requisition**: Specify hospital name, patient name, blood type, priority, and needed target time.
- **48-Hour Lead-Time Auto-Escalation**: Requisitions with target delivery within 48 hours are automatically escalated from `NORMAL` to `URGENT` priority and routed to emergency couriers.
- **Bedside Patient Onboarding**: Register new patients directly from the hospital interface (`PA-xxxx`).
- **Track Hospital Requisitions**: Live tracking of order lifecycle (`PENDING` -> `APPROVED` -> `IN_TRANSIT` -> `DELIVERED`).

### 3. Logistics Personnel Console
- **Dual Dispatch Queues**: Inspect separate queues for urgent emergency orders and scheduled batch distributions.
- **Dispatch Unit**: Process pending deliveries, transitioning status from `IN_TRANSIT` to `DELIVERED` using the corresponding distribution strategy.
- **Cold-Chain Intake Transport**: Log secure cold-chain intake consignments into central laboratory receiving docks.

### 4. Patient Portal
- **Self-Service Registration**: Patients register self-service accounts via Option [2] of the Main Menu (`PA-xxxx`).
- **Blood Profile Inspection**: View ABO/Rh blood type, compatible donor types (*"Can Receive From"*), and compatible recipient types (*"Can Donate To"*).
- **Personal Transfusion Tracking**: Real-time status inspection of clinical requests filed on their behalf.

---

## 7. Clinical Domain Rules & Compatibility Matrix

### 1. ABO/Rh Compatibility Matrix

| Recipient Type | Compatible Donor Blood Types |
|---|---|
| **O-** *(Universal Donor)* | `O-` |
| **O+** | `O+`, `O-` |
| **A-** | `A-`, `O-` |
| **A+** | `A+`, `A-`, `O+`, `O-` |
| **B-** | `B-`, `O-` |
| **B+** | `B+`, `B-`, `O+`, `O-` |
| **AB-** | `AB-`, `A-`, `B-`, `O-` |
| **AB+** *(Universal Recipient)*| `All Blood Types (AB+, AB-, A+, A-, B+, B-, O+, O-)` |

Compatibility is strictly verified programmatically via `BloodType.isCompatible(donor, recipient)`.

### 2. Component Lifespans & Storage Protocols
- **Whole Blood**: 42 days lifespan, stored at 1.0°C to 6.0°C.
- **Packed Red Blood Cells (PRBC)**: 42 days lifespan, stored at 1.0°C to 6.0°C.
- **Fresh Frozen Plasma (FFP)**: 365 days lifespan, stored frozen (<= -18.0°C).
- **Platelets**: 7 days lifespan, stored at 20.0°C to 24.0°C with continuous agitation.

### 3. 5-Stage Blood Donation Intake Pipeline
1. **Stage 1: Donor Identification**: Record Donor ID and collection facility.
2. **Stage 2: Serology & Typing**: Verify ABO/Rh blood type and validate serological markers.
3. **Stage 3: Component Separation**: Centrifugation separates whole blood into clinical components with calibrated lifespans.
4. **Stage 4: Cold-Chain Storage**: Automatically calculates expiration dates and verifies temperature storage requirements.
5. **Stage 5: Quality Release**: Assigns Donation Identification Number (DIN, e.g. `BU-1001`), sets status to `ACTIVE`, and releases to Central Inventory.

---

## 8. Zero-Seed Initial State Notice

To facilitate live dynamic demonstration and university evaluation, the application starts with a **clean, unseeded state**:
- No hardcoded mock users or preloaded inventory units exist on startup.
- Walk-in patients can self-register directly through Main Menu **Option [2]** (`Create account`).
- Super Admin and employee accounts can be created dynamically through the CLI.
- All blood units, requisitions, and inventory states are created and transitioned live in real time.

---

## 9. Quick-Start Guide & Execution

### Prerequisites
- Java Development Kit (JDK 11 or higher).

### 1. 1-Click Execution Scripts

#### macOS / Linux:
```bash
./run.sh
```
*(If permissions are needed: `chmod +x run.sh && ./run.sh`)*

#### Windows:
Double-click `run.bat` or run in Command Prompt:
```cmd
run.bat
```

---

### 2. Manual Compilation & Execution
```bash
# Compile all source files
javac *.java

# Run the application
java Main
```

---

### 3. Automated Test Runner (PR Verification)
The project includes a comprehensive standalone test runner testing all 18 sprint issues:
```bash
# Run all automated tests
java TestRunner all

# Run tests for a specific issue (e.g. Issue #13)
java TestRunner 13
```
Test results verify clinical models, user hierarchy, GoF strategy patterns, input validators, 5-stage pipeline, ASCII table rendering, central inventory, singletons, controllers, filtering/sorting, stock purges, and devops automation.
