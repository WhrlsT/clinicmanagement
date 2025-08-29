package boundary;


import adt.ADTInterface;
import adt.CustomADT;
import control.TreatmentMaintenance;
import entity.Consultation;
import entity.Medication;
import entity.Treatment;
import utility.InputUtil;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * Console UI runner for Treatment module. Delegates logic to control.TreatmentMaintenance.
 */
public class TreatmentMaintenanceUI {
    // Print header helper
    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }
    // Helper to get short term for treatment type or ID
    private String getTreatmentShort(String type) {
        if (type == null) return "UNK";
        type = type.toUpperCase();
        if (type.startsWith("MEDICATION")) return "MED ";
        if (type.startsWith("PROCEDURE")) return "PR  ";
        if (type.startsWith("THERAPY")) return "TH  ";
        if (type.startsWith("IMAGING")) return "IM  ";
        if (type.startsWith("LAB")) return "LAB ";
        // fallback: first two letters, padded
        return (type.length() >= 2 ? type.substring(0,2) : type).toUpperCase() + "  ";
    }
    private final TreatmentMaintenance control = new TreatmentMaintenance();
    private final Scanner sc = new Scanner(System.in);

    public void run(){
        try {
            control.refreshTreatmentFromFile();
        } catch (Exception e) {
            System.out.println("Error loading treatments: " + e.getMessage());
        }
        int c = -1;
        do {
            try {
                InputUtil.clearScreen();
                printHeader("Clinic Treatment Maintenance");
                printTable(rows(filterNonCompleted(control.getAllTreatments())));
                c = menu();
                switch(c){
                    case 1 -> handleAdd();
                    case 2 -> handleUpdate();
                    case 3 -> handleDelete();
                    case 4 -> { // view
                        InputUtil.clearScreen();
                        printHeader("Clinic Treatment Maintenance");
                        printTable(rows(filterNonCompleted(control.getAllTreatments())));
                        InputUtil.pauseScreen();
                    }
                    case 5 -> handleSort();
                    case 6 -> showReportsMenu();
                    case 7 -> handleCompleteTreatment();
                    case 8 -> showCompletedHistory();
                    case 9 -> {return;}
                    default -> System.out.println("Invalid");
                }
                if (c != 8 && c != 4) InputUtil.pauseScreen();
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        } while (c!=9);
    }
    private String rows(ADTInterface<Treatment> list){
        if (list==null || list.size()==0) return "(none)\n";
        StringBuilder sb=new StringBuilder();
    String border = "+-----------------------------------------------------------------------------------------------------------------------------------------------+\n";
    sb.append(border);
    sb.append(String.format("| %-8s | %-10s | %-20s | %-12s | %-28s | %-10s | %-10s | %-12s | %-8s | %-6s |\n",
        "ID", "ConsultID", "Patient Name", "Type", "Name", "Status", "Cost(RM)", "Date", "Severity", "MedCnt"));
    sb.append(border);
    // Load all patients for lookup
    ADTInterface<entity.Patient> patients = new dao.PatientDAO().retrieveFromFile();
    for (int i=0;i<list.size();i++){
        Treatment t = list.get(i);
        int medCount = t.getMedicationIds()==null?0:t.getMedicationIds().length;
        String id = (t.getId()==null||t.getId().toString().isBlank()) ? "blank" : t.getId().toString();
        String consultId = (t.getConsultationId()==null||t.getConsultationId().toString().isBlank()) ? "blank" : t.getConsultationId().toString();
        String type = (t.getType()==null||t.getType().toString().isBlank()) ? "blank" : t.getType().toString();
        String name = (t.getName()==null||t.getName().isBlank()) ? "blank" : t.getName();
        String status = (t.getStatus()==null||t.getStatus().toString().isBlank()) ? "blank" : t.getStatus().toString();
        String cost = (t.getCost()==null||String.format("%.2f", t.getCost()).isBlank()) ? "blank" : String.format("%.2f", t.getCost());
        String date = (t.getOrderedDate()==null||t.getOrderedDate().toString().isBlank()) ? "blank" : t.getOrderedDate().toString();
        String severity = (t.getDiagnosis()==null||t.getDiagnosis().isBlank()) ? "blank" : t.getDiagnosis();
        String medCnt = (medCount==0) ? "blank" : String.valueOf(medCount);
        // Lookup patient name via consultation
        String patientName = "";
        Consultation consult = control.findConsultationById(consultId);
        if (consult != null) {
            String patientId = consult.getPatientId();
            for (int p = 0; p < patients.size(); p++) {
                entity.Patient pat = patients.get(p);
                if (pat.getId().equals(patientId)) { patientName = pat.getName(); break; }
            }
        }
        if (patientName.isBlank()) patientName = "(unknown)";
        sb.append(String.format("| %-8s | %-10s | %-20s | %-12s | %-28s | %-10s | %-10s | %-12s | %-8s | %-6s |\n",
            id, consultId, patientName, type, name, status, cost, date, severity, medCnt));
    }
    sb.append(border);
    return sb.toString();
    }

    private void handleAdd(){
        try {
            InputUtil.clearScreen();
            printHeader("Clinic Treatment Maintenance");
            showAddHeader();
            showAddIntro();

            // Choose ONGOING consultation
            CustomADT<Consultation> ongoing = control.getOngoingConsultations();
            if (ongoing.size() == 0) {
                System.out.println("No ONGOING consultations available. You can only add treatments to ONGOING consultations.");
                return;
            }
            ConsultationMaintenanceUI cUI = new ConsultationMaintenanceUI();
            String consultRows = buildConsultationRowsForUI(ongoing);
            cUI.displayConsultationsTable(consultRows, false);

            String consultId;
            String patientName = "";
            while (true) {
                consultId = InputUtil.getInput(sc, "Consultation ID (ONGOING only, '0' to cancel): ").trim();
                if (consultId.equals("0")) return;
                Consultation chosen = control.findConsultationById(consultId);
                if (chosen != null && chosen.getStatus() == Consultation.Status.ONGOING) {
                    // Lookup patient name
                    ADTInterface<entity.Patient> patients = new dao.PatientDAO().retrieveFromFile();
                    for (int p = 0; p < patients.size(); p++) {
                        entity.Patient pat = patients.get(p);
                        if (pat.getId().equals(chosen.getPatientId())) { patientName = pat.getName(); break; }
                    }
                    System.out.println("Selected Patient: " + patientName);
                    break;
                }
                System.out.println("Invalid. Please enter an ONGOING consultation ID from the list.");
            }

            System.out.println("Severity Level:");
            System.out.println("1. Mild");
            System.out.println("2. Moderate");
            System.out.println("3. Severe");
            System.out.println("4. Critical");
            int severity = -1;
            while (true) {
                try {
                    severity = InputUtil.getIntInput(sc, "Select severity (1-4, 0 to cancel): ");
                } catch (Exception e) {
                    System.out.println("Invalid input. Please enter a number.");
                    continue;
                }
                if (severity == 0) return;
                if (severity >= 1 && severity <= 4) break;
                System.out.println("Invalid. Please enter a number between 1 and 4.");
            }
            String diagnosis = switch (severity) {
                case 1 -> "Mild";
                case 2 -> "Moderate";
                case 3 -> "Severe";
                case 4 -> "Critical";
                default -> "";
            };
            System.out.println("Type: 1=MEDICATION 2=PROCEDURE 3=THERAPY 4=IMAGING 5=LAB");
            int t = -1;
            try {
                t = InputUtil.getIntInput(sc, "Choose (0 to cancel): ");
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                return;
            }
            if (t==0) return;
            Treatment.Type type = switch(t){
                case 1 -> Treatment.Type.MEDICATION;
                case 2 -> Treatment.Type.PROCEDURE;
                case 3 -> Treatment.Type.THERAPY;
                case 4 -> Treatment.Type.IMAGING;
                case 5 -> Treatment.Type.LAB;
                default -> Treatment.Type.PROCEDURE;
            };
            String name = "";
            if (type == Treatment.Type.MEDICATION) {
                name = ""; // No name, will link medication
            } else if (type == Treatment.Type.PROCEDURE) {
                String[] options = {"Minor Surgery", "Vaccination / Immunization", "Wound Dressing / Suturing", "Endoscopy", "Biopsy"};
                System.out.println("Choose Procedure:");
                for (int i = 0; i < options.length; i++) {
                    System.out.printf("%d. %s\n", i+1, options[i]);
                }
                int choice = InputUtil.getIntInput(sc, "Choose (1-5, 0 to cancel): ");
                if (choice == 0) return;
                if (choice < 1 || choice > options.length) { System.out.println("Invalid choice."); return; }
                name = options[choice-1];
            } else if (type == Treatment.Type.THERAPY) {
                String[] options = {"Physiotherapy", "Occupational Therapy", "Speech Therapy", "Counseling / Psychological Therapy", "Chiropractic Therapy"};
                System.out.println("Choose Therapy:");
                for (int i = 0; i < options.length; i++) {
                    System.out.printf("%d. %s\n", i+1, options[i]);
                }
                int choice = InputUtil.getIntInput(sc, "Choose (1-5, 0 to cancel): ");
                if (choice == 0) return;
                if (choice < 1 || choice > options.length) { System.out.println("Invalid choice."); return; }
                name = options[choice-1];
            } else if (type == Treatment.Type.IMAGING) {
                String[] options = {"X-Ray", "Ultrasound", "MRI (Magnetic Resonance Imaging)", "CT Scan (Computed Tomography)", "Mammography"};
                System.out.println("Choose Imaging:");
                for (int i = 0; i < options.length; i++) {
                    System.out.printf("%d. %s\n", i+1, options[i]);
                }
                int choice = InputUtil.getIntInput(sc, "Choose (1-5, 0 to cancel): ");
                if (choice == 0) return;
                if (choice < 1 || choice > options.length) { System.out.println("Invalid choice."); return; }
                name = options[choice-1];
            } else if (type == Treatment.Type.LAB) {
                String[] options = {"Complete Blood Count (CBC)", "Urinalysis", "Blood Glucose Test", "Lipid Profile", "Liver Function Test (LFT)"};
                System.out.println("Choose Lab Test:");
                for (int i = 0; i < options.length; i++) {
                    System.out.printf("%d. %s\n", i+1, options[i]);
                }
                int choice = InputUtil.getIntInput(sc, "Choose (1-5, 0 to cancel): ");
                if (choice == 0) return;
                if (choice < 1 || choice > options.length) { System.out.println("Invalid choice."); return; }
                name = options[choice-1];
            }
            Treatment tr = new Treatment(null, consultId, type, name);
            if (!diagnosis.isBlank()) tr.setDiagnosis(diagnosis);
            tr.setOrderedDate(LocalDate.now());
            tr.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
            tr.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
            // Set fixed cost for non-medication types
            double fixedCost = 0.0;
            if (type == Treatment.Type.PROCEDURE) {
                switch (name) {
                    case "Minor Surgery" -> fixedCost = 1500;
                    case "Vaccination / Immunization" -> fixedCost = 120;
                    case "Wound Dressing / Suturing" -> fixedCost = 250;
                    case "Endoscopy" -> fixedCost = 2500;
                    case "Biopsy" -> fixedCost = 2000;
                }
                tr.setCost(fixedCost);
            } else if (type == Treatment.Type.THERAPY) {
                switch (name) {
                    case "Physiotherapy" -> fixedCost = 200;
                    case "Occupational Therapy" -> fixedCost = 250;
                    case "Speech Therapy" -> fixedCost = 220;
                    case "Counseling / Psychological Therapy" -> fixedCost = 300;
                    case "Chiropractic Therapy" -> fixedCost = 180;
                }
                tr.setCost(fixedCost);
            } else if (type == Treatment.Type.IMAGING) {
                switch (name) {
                    case "X-Ray" -> fixedCost = 80;
                    case "Ultrasound" -> fixedCost = 120;
                    case "MRI (Magnetic Resonance Imaging)" -> fixedCost = 1800;
                    case "CT Scan (Computed Tomography)" -> fixedCost = 1500;
                    case "Mammography" -> fixedCost = 200;
                }
                tr.setCost(fixedCost);
            } else if (type == Treatment.Type.LAB) {
                switch (name) {
                    case "Complete Blood Count (CBC)" -> fixedCost = 25;
                    case "Urinalysis" -> fixedCost = 15;
                    case "Blood Glucose Test" -> fixedCost = 10;
                    case "Lipid Profile" -> fixedCost = 40;
                    case "Liver Function Test (LFT)" -> fixedCost = 45;
                }
                tr.setCost(fixedCost);
            } else if (type == Treatment.Type.MEDICATION) {
                // allow adding multiple medication IDs
                printMedicationTable(control.getAllMedications());
                while (true){
                    String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                    if (mid.isEmpty()) break;
                    if (control.findMedicationById(mid)==null){ System.out.println("Medication not found"); continue; }
                    tr.addMedicationId(mid);
                    System.out.println("Linked "+mid);
                }
                // Compute medication total from linked medications
                double medTotal = 0.0;
                if (tr.getMedicationIds() != null) {
                    for (String mid : tr.getMedicationIds()){
                        if (mid==null||mid.isBlank()) continue;
                        Medication m = control.findMedicationById(mid);
                        if (m!=null && m.getPrice()!=null) medTotal += m.getPrice();
                    }
                }
                String extraCostInput = InputUtil.getInput(sc, "Additional Cost (opt, will be added to medication prices): ");
                if (!extraCostInput.isEmpty()){
                    try{ double extra = Double.parseDouble(extraCostInput); tr.setCost(medTotal + extra); } catch(Exception e){ System.out.println("Invalid cost input. Using medication total only."); tr.setCost(medTotal); }
                } else if (medTotal>0) {
                    tr.setCost(medTotal);
                }
            }

            Treatment added = control.addTreatment(tr);
            displayTreatmentAddedMessage(added);
        } catch (Exception e) {
            System.out.println("An error occurred while adding treatment: " + e.getMessage());
        }
    }

    private void handleUpdate(){
        try {
            InputUtil.clearScreen();
            printHeader("Clinic Treatment Maintenance");
            showUpdateHeader();
            printTable(rows(control.getAllTreatments()));
            String id = InputUtil.getInput(sc, "Treatment ID ('0' to cancel): ");
            if (id.equals("0")) return;
            Treatment tr = control.findTreatmentById(id); if (tr==null){ displayNotFoundMessage(id); return; }
            showUpdateIntro(id);
            String v;
            System.out.println("Severity Level:");
            System.out.println("1. Mild");
            System.out.println("2. Moderate");
            System.out.println("3. Severe");
            System.out.println("4. Critical");
            String currentSeverity = tr.getDiagnosis() == null ? "" : tr.getDiagnosis();
            System.out.println("Current Severity: " + currentSeverity);
            String severityPrompt = "Select severity (1-4, blank to keep, 0 to cancel): ";
            String severityInput = InputUtil.getInput(sc, severityPrompt);
            if (severityInput.equals("0")) return;
            if (!severityInput.isBlank()) {
                try {
                    int severity = Integer.parseInt(severityInput);
                    if (severity >= 1 && severity <= 4) {
                        String newSeverity = switch (severity) {
                            case 1 -> "Mild";
                            case 2 -> "Moderate";
                            case 3 -> "Severe";
                            case 4 -> "Critical";
                            default -> currentSeverity;
                        };
                        tr.setDiagnosis(newSeverity);
                    } else {
                        System.out.println("Invalid severity. Keeping previous value.");
                    }
                } catch (Exception e) {
                    System.out.println("Invalid input. Keeping previous value.");
                }
            }
            v = InputUtil.getInput(sc, "Name (blank keep): "); if(!v.isEmpty()) tr.setName(v);
            v = InputUtil.getInput(sc, "Instructions (blank keep): "); if(!v.isEmpty()) tr.setInstructions(v);
            v = InputUtil.getInput(sc, "Notes (blank keep): "); if(!v.isEmpty()) tr.setNotes(v);
            v = InputUtil.getInput(sc, "Cost (blank keep - interpreted as additional cost to medication prices): ");
            if(!v.isEmpty()){
                try{
                    double extra = Double.parseDouble(v);
                    double medTotal = 0.0;
                    if (tr.getMedicationIds()!=null){
                        for (String mid : tr.getMedicationIds()){
                            if (mid==null||mid.isBlank()) continue;
                            Medication m = control.findMedicationById(mid);
                            if (m!=null && m.getPrice()!=null) medTotal += m.getPrice();
                        }
                    }
                    tr.setCost(medTotal + extra);
                }catch(Exception e){
                    System.out.println("Invalid cost input. Keeping previous cost.");
                }
            }
            // manage medication links
            if (tr.getType()== Treatment.Type.MEDICATION){
                showMedications(control.getAllMedications());
                System.out.println("Manage Medication Links: 1=Add 2=Clear All 3=Skip");
                int c = -1;
                try {
                    c = InputUtil.getIntInput(sc, "Choose: ");
                } catch (Exception e) {
                    System.out.println("Invalid input. Please enter a number.");
                    return;
                }
                if (c==1){
                    while (true){
                        String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                        if (mid.isEmpty()) break; if (control.findMedicationById(mid)==null){ System.out.println("Medication not found"); continue; }
                        tr.addMedicationId(mid); System.out.println("Linked "+mid);
                    }
                } else if (c==2){ tr.setMedicationIds(null); System.out.println("Cleared."); }
            }
            // Confirmation prompt before updating
            String confirm = InputUtil.getInput(sc, "Are you sure you want to update this treatment? (y/n): ").trim().toLowerCase();
            if (!confirm.equals("y")) {
                System.out.println("Update cancelled.");
                return;
            }
            control.updateTreatment(tr);
            displayTreatmentUpdatedMessage(tr);
        } catch (Exception e) {
            System.out.println("An error occurred while updating treatment: " + e.getMessage());
        }
    }

    private void handleDelete(){
        try {
            InputUtil.clearScreen();
            printHeader("Clinic Treatment Maintenance");
            showDeleteHeader();
            showDeleteIntro();
            System.out.println("Delete Options:");
            System.out.println("1. Delete Ongoing Treatment");
            System.out.println("2. Delete Completed Treatment");
            int opt = InputUtil.getIntInput(sc, "Choose option (1-2, 0 to cancel): ");
            if (opt == 0) return;
            if (opt != 1 && opt != 2) { System.out.println("Invalid option."); return; }

            CustomADT<Treatment> treatments = toCustomADT(control.getAllTreatments());
            CustomADT<Treatment> filtered = new CustomADT<>();
            if (opt == 1) {
                // Ongoing: include everything except COMPLETED and DISPENSED
                for (int i = 0; i < treatments.size(); i++) {
                    Treatment t = treatments.get(i);
                    String status = nz(t.getStatus()).toUpperCase();
                    if (!status.equals("COMPLETED") && !status.equals("DISPENSED")) {
                        filtered.add(t);
                    }
                }
                if (filtered.size() == 0) {
                    System.out.println("No ongoing treatments available to delete.");
                    return;
                }
            } else {
                // Completed: include both COMPLETED and DISPENSED
                for (int i = 0; i < treatments.size(); i++) {
                    Treatment t = treatments.get(i);
                    String status = nz(t.getStatus()).toUpperCase();
                    if (status.equals("COMPLETED") || status.equals("DISPENSED")) {
                        filtered.add(t);
                    }
                }
                if (filtered.size() == 0) {
                    System.out.println("No completed or dispensed treatments available to delete.");
                    return;
                }
            }
            System.out.printf("%-8s|%-12s|%-28s|%-10s|%-8s|%-8s\n", "ID", "Type", "Name", "Status", "Severity", "Date");
            System.out.println("-".repeat(80));
            for (int i = 0; i < filtered.size(); i++) {
                Treatment t = filtered.get(i);
                System.out.printf("%-8s|%-12s|%-28s|%-10s|%-8s|%-8s\n", t.getId(), nz(t.getType()), nz(t.getName()), nz(t.getStatus()), nz(t.getDiagnosis()), nz(t.getOrderedDate()));
            }
            System.out.println("-".repeat(80));
            String id = InputUtil.getInput(sc, "Treatment ID to delete ('0' to cancel): ");
            if (id.equals("0")) return;
            // Confirmation prompt before deleting
            String confirm = InputUtil.getInput(sc, "Are you sure you want to delete this treatment? (y/n): ").trim().toLowerCase();
            if (!confirm.equals("y")) {
                System.out.println("Delete cancelled.");
                return;
            }
            if (control.deleteTreatment(id)) displayDeletedMessage(id); else displayNotFoundMessage(id);
        } catch (Exception e) {
            System.out.println("An error occurred while deleting treatment: " + e.getMessage());
        }
    }

    private void handleSort(){
        try {
            InputUtil.clearScreen();
            printHeader("Clinic Treatment Maintenance - Sort");
            System.out.println("Sort by:");
            System.out.println("1. Date (orderedDate)");
            System.out.println("2. Cost (RM)");
            int field = InputUtil.getIntInput(sc, "Choose field (1=Date, 2=Cost, 0=Cancel): ");
            if (field == 0) return;
            if (field != 1 && field != 2) { System.out.println("Invalid field."); return; }
            System.out.println("Order:");
            System.out.println("1. Ascending (earliest/lowest first)");
            System.out.println("2. Descending (latest/highest first)");
            int order = InputUtil.getIntInput(sc, "Choose order (1=Ascending, 2=Descending, 0=Cancel): ");
            if (order == 0) return;
            boolean asc = (order == 1);
            control.sortTreatments(field, asc);
            InputUtil.clearScreen();
            printHeader("Sorted Treatments");
            printTable(rows(control.getAllTreatments()));
        } catch (Exception e) {
            System.out.println("An error occurred during sorting: " + e.getMessage());
        }
    }

    private void handleCompleteTreatment() {
        InputUtil.clearScreen();
        printHeader("Complete Treatment");
        System.out.println("Select a treatment with status 'ORDERED' to mark as 'COMPLETED'.");

        ADTInterface<Treatment> treatments = control.filterByStatus(Treatment.TreatmentStatus.PRESCRIBED);
        if (treatments.isEmpty()) {
            System.out.println("\nNo treatments are currently in the 'ORDERED' status.");
            return;
        }

        printTable(rows(treatments));

        String id = InputUtil.getInput(sc, "Enter Treatment ID to complete ('0' to cancel): ");
        if (id.equals("0")) {
            return;
        }

        if (control.completeTreatment(id)) {
            System.out.println("\nTreatment " + id + " has been marked as COMPLETED.");
        } else {
            System.out.println("\nFailed to complete treatment. Ensure the ID is correct and the status is 'ORDERED'.");
        }
    }

    private String buildConsultationRowsForUI(ADTInterface<Consultation> list){
        if (list == null || list.size() == 0) return "(none)\n";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            entity.Consultation c = list.get(i);
            String dt = c.getDate()==null? "" : c.getDate().toString().replace('T',' ').substring(0, Math.min(16, c.getDate().toString().length()));
            String reason = c.getReason()==null? "" : c.getReason();
            if (reason.length() > 20) reason = reason.substring(0, 19) + "…";
            sb.append(String.format("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
                    c.getId(), dt, nz(c.getPatientId()), nz(c.getDoctorId()), reason, nz(c.getStatus())));
        }
        return sb.toString();
    }

    // Minimal helpers
    private String nz(Object o){ return o==null?"":o.toString(); }

    // ===== Inlined from TreatmentUI =====
    public int menu() {
        System.out.println("Treatment Management");
        System.out.println("1. Add Treatment");
        System.out.println("2. Update Treatment");
        System.out.println("3. Delete Treatment");
        System.out.println("4. View Treatments");
        System.out.println("5. Sort Treatments");
        System.out.println("6. Treatment Reports");
        System.out.println("7. Complete Treatment (Payment Complete)");
            System.out.println("8. Completed Treatment History");
            System.out.println("9. Exit");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

        // Helper to filter out completed treatments
        private CustomADT<Treatment> filterNonCompleted(ADTInterface<Treatment> list) {
            CustomADT<Treatment> result = new CustomADT<>();
            for (int i = 0; i < list.size(); i++) {
                Treatment t = list.get(i);
                String status = t.getStatus() == null ? "" : t.getStatus().toString().toUpperCase();
                if (!status.equals("COMPLETED") && !status.equals("DISPENSED")) {
                    result.add(t);
                }
            }
            return result;
        }

        // Completed Treatment History page
        private void showCompletedHistory() {
            InputUtil.clearScreen();
            printHeader("Completed Treatment History");
            CustomADT<Treatment> completed = new CustomADT<>();
            ADTInterface<Treatment> all = control.getAllTreatments();
            for (int i = 0; i < all.size(); i++) {
                Treatment t = all.get(i);
                String status = t.getStatus() == null ? "" : t.getStatus().toString().toUpperCase();
                if (status.equals("COMPLETED") || status.equals("DISPENSED")) {
                    completed.add(t);
                }
            }
            printTable(rows(completed));
            InputUtil.pauseScreen();
        }

    public void printTable(String rows) {
    System.out.println();
    System.out.println(rows);
    }

    public void showAddIntro() {
        System.out.println("Adding a New Treatment (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showUpdateIntro(String id) {
        System.out.println("Updating Treatment: " + (id==null?"":id));
        System.out.println("─".repeat(50));
    }

    public void showDeleteIntro() {
        System.out.println("Deleting a Treatment (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showAddHeader() {
        System.out.println("\n" + "═".repeat(94));
        System.out.println("ADD TREATMENT");
        System.out.println("" + "═".repeat(94));
    }

    public void showUpdateHeader() {
        System.out.println("\n" + "═".repeat(94));
        System.out.println("UPDATE TREATMENT");
        System.out.println("" + "═".repeat(94));
    }

    public void showDeleteHeader() {
        System.out.println("\n" + "═".repeat(94));
        System.out.println("DELETE TREATMENT");
        System.out.println("" + "═".repeat(94));
    }

    public void displayTreatmentDetails(Treatment t){
        System.out.println("\nTreatment Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", t.getId());
        System.out.printf("Consultation: %s\n", nz(t.getConsultationId()));
        System.out.printf("Diagnosis: %s\n", nz(t.getDiagnosis()));
        System.out.printf("Type: %s\n", nz(t.getType()));
        System.out.printf("Name: %s\n", nz(t.getName()));
        System.out.printf("Status: %s\n", nz(t.getStatus()));
        System.out.printf("Ordered: %s\n", nz(t.getOrderedDate()));
        System.out.printf("Cost: %s\n", nz(t.getCost()));
        System.out.println("-----------------------------------------------");
    }

    public void displayTreatmentAddedMessage(Treatment t){
        System.out.println("Treatment added successfully: " + t.getId());
        displayTreatmentDetails(t);
    }

    public void displayTreatmentUpdatedMessage(Treatment t){
        System.out.println("Treatment updated successfully: " + t.getId());
        displayTreatmentDetails(t);
    }

    public void displayDeletedMessage(String id){
        System.out.println("Treatment with ID: " + id + " deleted successfully.");
    }

    public void displayNotFoundMessage(String id){
        System.out.println("Treatment with ID: " + id + " not found.");
    }

    public int searchMenu(){
        System.out.println("Search By:");
        System.out.println("1. Treatment ID");
        System.out.println("2. Patient Name/ID");
        System.out.println("3. Doctor Name/ID");
        System.out.println("4. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }
    public String promptSearchQuery(){ return InputUtil.getInput(sc, "Enter search text: "); }

    public int sortFieldMenu(){
    System.out.println("Sort Field:");
    System.out.println("1. Type");
    System.out.println("2. Date");
    System.out.println("3. Back");
    return InputUtil.getIntInput(sc, "Choose: ");
    }
    public int sortDirectionMenu(){
        System.out.println("Direction: 1=Ascending 2=Descending");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    // Expose some UI helpers for other modules (Medication)
    public void showMedications(ADTInterface<Medication> meds){
        System.out.println("Available Medications:");
        if (meds==null || meds.size()==0){ System.out.println("(none)"); return; }
        System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s%n","ID","Name","Price","Code","Dose","Freq","Route","Qty");
        for (int i=0;i<meds.size();i++){
            Medication m = meds.get(i);
            String price = m.getPrice()==null? "" : String.format("%.2f", m.getPrice());
            System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s%n", m.getId(), nz(m.getName()), price, nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity()));
        }
    }
    public void printMedicationTable(ADTInterface<Medication> meds){
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<meds.size();i++){
            Medication m = meds.get(i);
            sb.append(String.format("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s%n",
                    m.getId(), nz(m.getName()), nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity())));
        }
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("Medications");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s\n","ID","Name","Price","Code","Dose","Freq","Route","Qty");
        System.out.println("---------------------------------------------------------------");
        System.out.print(sb.length()==0?"(none)\n":sb.toString());
        System.out.println("---------------------------------------------------------------");
    }

    // Helper to convert ADTInterface to CustomADT
    private CustomADT<Treatment> toCustomADT(ADTInterface<Treatment> list) {
        CustomADT<Treatment> result = new CustomADT<>();
        for (int i = 0; i < list.size(); i++) result.add(list.get(i));
        return result;
    }

    public void showReportsMenu() {
        while (true) {
            InputUtil.clearScreen();
            System.out.println("═".repeat(80));
            System.out.println("TREATMENT REPORTS MENU");
            System.out.println("═".repeat(80));
            String timeGenerated = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            System.out.println("Report generated at: " + timeGenerated);
            System.out.println();

            CustomADT<Treatment> treatments = toCustomADT(control.getAllTreatments());

            System.out.println("1. Most Used Treatment Types (with Graph)");
            System.out.println("2. Revenue by Treatment Type (with Graph)");
            System.out.println("3. Back");
            int reportChoice = InputUtil.getIntInput(sc, "Choose: ");
            if (reportChoice == 1) {
                // Most Used Treatment Types with ASCII bar graph
                CustomADT<String> typeList = new CustomADT<>();
                CustomADT<Integer> typeCountList = new CustomADT<>();
                int maxCount = 0;
                for (int i = 0; i < treatments.size(); i++) {
                    Treatment t = treatments.get(i);
                    String type = t.getType() == null ? "Unknown" : t.getType().toString();
                    int idx = -1;
                    for (int j = 0; j < typeList.size(); j++) {
                        if (typeList.get(j).equals(type)) { idx = j; break; }
                    }
                    if (idx == -1) { typeList.add(type); typeCountList.add(1); }
                    else typeCountList.set(idx, typeCountList.get(idx) + 1);
                }
                for (int i = 0; i < typeCountList.size(); i++) {
                    if (typeCountList.get(i) > maxCount) maxCount = typeCountList.get(i);
                }
                System.out.println("\nMost Used Treatment Types:");
                System.out.println("---------------------------------------------------");
                for (int i = 0; i < typeList.size(); i++) {
                    int count = typeCountList.get(i);
                    int barLen = maxCount == 0 ? 0 : (int) ((count * 40.0) / maxCount);
                    String bar = "█".repeat(barLen);
                    System.out.printf("%-20s | %3d | %s\n", typeList.get(i), count, bar);
                }
                System.out.println("---------------------------------------------------");
                InputUtil.pauseScreen();
            } else if (reportChoice == 2) {
                // Revenue by Treatment Type with ASCII bar graph
                CustomADT<String> revTypeList = new CustomADT<>();
                CustomADT<Double> revTypeTotals = new CustomADT<>();
                double maxRev = 0.0;
                for (int i = 0; i < treatments.size(); i++) {
                    Treatment t = treatments.get(i);
                    String type = t.getType() == null ? "Unknown" : t.getType().toString();
                    double cost = t.getCost() == null ? 0.0 : t.getCost();
                    int idx = -1;
                    for (int j = 0; j < revTypeList.size(); j++) {
                        if (revTypeList.get(j).equals(type)) { idx = j; break; }
                    }
                    if (idx == -1) { revTypeList.add(type); revTypeTotals.add(cost); }
                    else revTypeTotals.set(idx, revTypeTotals.get(idx) + cost);
                }
                for (int i = 0; i < revTypeTotals.size(); i++) {
                    if (revTypeTotals.get(i) > maxRev) maxRev = revTypeTotals.get(i);
                }
                System.out.println("\nRevenue by Treatment Type:");
                System.out.println("---------------------------------------------------");
                for (int i = 0; i < revTypeList.size(); i++) {
                    double rev = revTypeTotals.get(i);
                    int barLen = maxRev == 0.0 ? 0 : (int) ((rev * 40.0) / maxRev);
                    String bar = "█".repeat(barLen);
                    System.out.printf("%-20s | RM %8.2f | %s\n", revTypeList.get(i), rev, bar);
                }
                System.out.println("---------------------------------------------------");
                InputUtil.pauseScreen();
            } else if (reportChoice == 3) {
                return;
            } else {
                System.out.println("Invalid");
                InputUtil.pauseScreen();
            }
        }
    }
}

