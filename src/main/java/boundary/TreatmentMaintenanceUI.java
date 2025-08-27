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
    private final TreatmentMaintenance control = new TreatmentMaintenance();
    private final ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    private final Scanner sc = new Scanner(System.in);

    public void run(){
        int c;
        do {
            InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance");
        printTable(rows(control.getAllTreatments()));
        c = menu();
            switch(c){
                case 1 -> handleAdd();
                case 2 -> handleUpdate();
                case 3 -> handleDelete();
                case 4 -> { // view
                    InputUtil.clearScreen();
                    clinicUI.printHeader("Clinic Treatment Maintenance");
            printTable(rows(control.getAllTreatments()));
                    InputUtil.pauseScreen();
                }
                case 5 -> handleSearch();
                case 6 -> handleSort();
                case 7 -> {}
                default -> System.out.println("Invalid");
            }
            if (c != 7 && c != 4) InputUtil.pauseScreen();
        } while (c!=7);
    }

    private String rows(ADTInterface<Treatment> list){
        if (list==null || list.size()==0) return "(none)\n";
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<list.size();i++){
            Treatment t = list.get(i);
            int medCount = t.getMedicationIds()==null?0:t.getMedicationIds().length;
            sb.append(String.format("%-8s|%-10s|%-12s|%-28s|%-10s|%-6s\n",
                t.getId(), nz(t.getConsultationId()), t.getType(), nz(t.getName()), t.getStatus(), medCount));
        }
        return sb.toString();
    }

    private void handleAdd(){
        InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Treatment Maintenance");
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
        while (true) {
            consultId = InputUtil.getInput(sc, "Consultation ID (ONGOING only, '0' to cancel): ").trim();
            if (consultId.equals("0")) return;
            Consultation chosen = control.findConsultationById(consultId);
            if (chosen != null && chosen.getStatus() == Consultation.Status.ONGOING) break;
            System.out.println("Invalid. Please enter an ONGOING consultation ID from the list.");
        }

        String diagnosis = InputUtil.getInput(sc, "Doctor's Diagnosis (blank to skip, '0' to cancel): ");
        if (diagnosis.equals("0")) return;
        System.out.println("Type: 1=MEDICATION 2=PROCEDURE 3=THERAPY 4=IMAGING 5=LAB");
        int t = InputUtil.getIntInput(sc, "Choose (0 to cancel): ");
        if (t==0) return;
        Treatment.Type type = switch(t){
            case 1 -> Treatment.Type.MEDICATION;
            case 2 -> Treatment.Type.PROCEDURE;
            case 3 -> Treatment.Type.THERAPY;
            case 4 -> Treatment.Type.IMAGING;
            case 5 -> Treatment.Type.LAB;
            default -> Treatment.Type.PROCEDURE;
        };
        String name = (type == Treatment.Type.MEDICATION)
            ? InputUtil.getInput(sc, "Plan name (optional, Enter to skip, '0' to cancel): ")
            : InputUtil.getInput(sc, "Name (for non-medication types, '0' to cancel): ");
        if ("0".equals(name)) return;
        Treatment tr = new Treatment(null, consultId, type, name);
        if (!diagnosis.isBlank()) tr.setDiagnosis(diagnosis);
        tr.setOrderedDate(LocalDate.now());
        tr.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
        tr.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
        if (type == Treatment.Type.MEDICATION){
            // allow adding multiple medication IDs
            printMedicationTable(control.getAllMedications());
            while (true){
                String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                if (mid.isEmpty()) break;
                if (control.findMedicationById(mid)==null){ System.out.println("Medication not found"); continue; }
                tr.addMedicationId(mid);
                System.out.println("Linked "+mid);
            }
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
            try{ double extra = Double.parseDouble(extraCostInput); tr.setCost(medTotal + extra); } catch(Exception ignored){ tr.setCost(medTotal); }
        } else if (medTotal>0) {
            tr.setCost(medTotal);
        }

    Treatment added = control.addTreatment(tr);
    displayTreatmentAddedMessage(added);
    }

    private void handleUpdate(){
        InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Treatment Maintenance");
    showUpdateHeader();
    printTable(rows(control.getAllTreatments()));
        String id = InputUtil.getInput(sc, "Treatment ID ('0' to cancel): ");
        if (id.equals("0")) return;
    Treatment tr = control.findTreatmentById(id); if (tr==null){ displayNotFoundMessage(id); return; }
    showUpdateIntro(id);
        String v;
        v = InputUtil.getInput(sc, "Doctor's Diagnosis (blank keep): "); if(!v.isEmpty()) tr.setDiagnosis(v);
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
            }catch(Exception ignored){}
        }
        // manage medication links
        if (tr.getType()== Treatment.Type.MEDICATION){
            showMedications(control.getAllMedications());
            System.out.println("Manage Medication Links: 1=Add 2=Clear All 3=Skip");
            int c = InputUtil.getIntInput(sc, "Choose: ");
            if (c==1){
                while (true){
                    String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                    if (mid.isEmpty()) break; if (control.findMedicationById(mid)==null){ System.out.println("Medication not found"); continue; }
                    tr.addMedicationId(mid); System.out.println("Linked "+mid);
                }
            } else if (c==2){ tr.setMedicationIds(null); System.out.println("Cleared."); }
        }
    control.updateTreatment(tr);
    displayTreatmentUpdatedMessage(tr);
    }

    private void handleDelete(){
        InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Treatment Maintenance");
    showDeleteHeader();
    showDeleteIntro();
        String id = InputUtil.getInput(sc, "Treatment ID to delete ('0' to cancel): ");
        if (id.equals("0")) return;
    if (control.deleteTreatment(id)) displayDeletedMessage(id); else displayNotFoundMessage(id);
    }

    private void handleSearch(){
        InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Treatment Maintenance - Search");
    int opt = searchMenu();
        if (opt==4) return;
    String q = promptSearchQuery().toLowerCase();
        CustomADT<Treatment> results;
        switch (opt){
            case 1 -> results = control.searchById(q);
            case 2 -> results = control.searchByPatient(q);
            case 3 -> results = control.searchByDoctor(q);
            default -> results = new CustomADT<>();
        }
        InputUtil.clearScreen();
        clinicUI.printHeader("Search Results");
    printTable(rows(results));
    }

    private void handleSort(){
        InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Treatment Maintenance - Sort");
    int field = sortFieldMenu(); if (field==5) return;
    int dir = sortDirectionMenu(); boolean asc = dir!=2;
        control.sortTreatments(field, asc);
        InputUtil.clearScreen();
        clinicUI.printHeader("Sorted Treatments");
    printTable(rows(control.getAllTreatments()));
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
        System.out.println("5. Search Treatments");
        System.out.println("6. Sort Treatments");
        System.out.println("7. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void printTable(String rows) {
        System.out.println("\n----------------------------------------------------------------------------------------------");
        System.out.println("Treatments");
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.printf("%-8s|%-10s|%-12s|%-28s|%-10s|%-6s\n","ID","ConsultID","Type","Name","Status","Meds");
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.print(rows == null || rows.isBlank()?"(none)\n":rows);
        System.out.println("----------------------------------------------------------------------------------------------");
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
        System.out.println("1. Treatment ID");
        System.out.println("2. Consultation ID");
        System.out.println("3. Type");
        System.out.println("4. Status");
        System.out.println("5. Back");
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
}
