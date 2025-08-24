package control;

import adt.ADTInterface;
import adt.CustomADT;
import boundary.TreatmentUI;
import boundary.ClinicMaintenanceUI;
import dao.TreatmentDAO;
import dao.ConsultationDAO;
import dao.MedicationDAO;
import entity.Treatment;
import entity.Consultation;
import entity.Medication;
import utility.InputUtil;

import java.time.LocalDate;
import java.util.Scanner;

public class TreatmentMaintenance {
    private final TreatmentDAO tdao = new TreatmentDAO();
    private final ConsultationDAO cdao = new ConsultationDAO();
    private final MedicationDAO mdao = new MedicationDAO();
    private final TreatmentUI ui = new TreatmentUI();
    private final ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    private ADTInterface<Treatment> treatments = new CustomADT<>();
    private ADTInterface<Medication> medications = new CustomADT<>();
    private ADTInterface<Consultation> consultations = new CustomADT<>();
    private final Scanner sc = new Scanner(System.in);

    public TreatmentMaintenance(){
        treatments = tdao.load();
    consultations = cdao.load();
        medications = mdao.load();
    }

    public void run(){
        int c;
        do {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Treatment Maintenance");
            ui.printTable(rows());
            c = ui.menu();
            switch(c){
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> {
                    InputUtil.clearScreen();
                    clinicUI.printHeader("Clinic Treatment Maintenance");
                    ui.printTable(rows());
                    InputUtil.pauseScreen();
                }
                case 5 -> search();
                case 6 -> sort();
                case 7 -> {}
                default -> System.out.println("Invalid");
            }
            tdao.save(treatments);
            if (c != 7 && c != 4) InputUtil.pauseScreen();
        } while (c!=7);
    }

    private String nextId(){ int max=0; for (int i=0;i<treatments.size();i++){String id=treatments.get(i).getId(); if(id!=null&&id.startsWith("T"))try{int n=Integer.parseInt(id.substring(1)); if(n>max)max=n;}catch(Exception ignored){}} return String.format("T%04d", max+1);}    

    private String rows(){
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<treatments.size();i++){
            Treatment t = treatments.get(i);
            int medCount = t.getMedicationIds()==null?0:t.getMedicationIds().length;
            sb.append(String.format("%-8s|%-10s|%-12s|%-28s|%-10s|%-6s\n",
                t.getId(), nz(t.getConsultationId()), t.getType(), nz(t.getName()), t.getStatus(), medCount));
        }
        return sb.toString();
    }

    private void add(){
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance");
        ui.showAddHeader();
        ui.showAddIntro();
    // Refresh consultations to get latest statuses
    consultations = cdao.load();
        // Filter consultations to ONGOING only and display
        ADTInterface<Consultation> ongoing = new CustomADT<>();
        if (consultations != null) {
            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                try {
                    if (c != null && c.getStatus() == Consultation.Status.ONGOING) ongoing.add(c);
                } catch (Exception ignored) {}
            }
        }
        if (ongoing.size() == 0) {
            System.out.println("No ONGOING consultations available. You can only add treatments to ONGOING consultations.");
            return;
        }
        boundary.ConsultationMaintenanceUI cUI = new boundary.ConsultationMaintenanceUI();
        String consultRows = buildConsultationRowsForUI(ongoing);
        cUI.displayConsultationsTable(consultRows, false);

        // Require a valid ONGOING consultation ID
        String consultId;
        while (true) {
            consultId = InputUtil.getInput(sc, "Consultation ID (ONGOING only, '0' to cancel): ").trim();
            if (consultId.equals("0")) return;
            Consultation chosen = findConsultationLocal(consultId);
            if (chosen != null && chosen.getStatus() == Consultation.Status.ONGOING) break;
            System.out.println("Invalid. Please enter an ONGOING consultation ID from the list.");
        }

    // Ask for Doctor's Diagnosis before choosing type
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
    Treatment tr = new Treatment(nextId(), consultId, type, name);
    if (!diagnosis.isBlank()) tr.setDiagnosis(diagnosis);
        tr.setOrderedDate(LocalDate.now());
        tr.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
        tr.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
    String cost = InputUtil.getInput(sc, "Cost (opt): "); if(!cost.isEmpty()) try{ tr.setCost(Double.parseDouble(cost)); }catch(Exception ignored){}

    if (type == Treatment.Type.MEDICATION){
            // allow adding multiple medication IDs
            new boundary.MedicationUI().printTable(buildMedicationRowsForUI());
            while (true){
                String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                if (mid.isEmpty()) break;
                if (findMedication(mid)==null){ System.out.println("Medication not found"); continue; }
                tr.addMedicationId(mid);
                System.out.println("Linked "+mid);
            }
        }

        treatments.add(tr);
        ui.displayTreatmentAddedMessage(tr);
    }

    // Build rows string for ConsultationMaintenanceUI table (no FollowOf col)
    @SuppressWarnings("unused")
    private String buildConsultationRowsForUI(){
        if (consultations == null || consultations.size() == 0) return "(none)\n";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < consultations.size(); i++) {
            entity.Consultation c = consultations.get(i);
            String dt = c.getDate()==null? "" : c.getDate().toString().replace('T',' ').substring(0, Math.min(16, c.getDate().toString().length()));
            String reason = c.getReason()==null? "" : c.getReason();
            if (reason.length() > 20) reason = reason.substring(0, 19) + "…";
            sb.append(String.format("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
                    c.getId(),
                    dt,
                    nz(c.getPatientId()),
                    nz(c.getDoctorId()),
                    reason,
                    nz(c.getStatus())
            ));
        }
        return sb.toString();
    }

    // Overload: build rows for a provided list (e.g., ONGOING only)
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

    // Build rows for MedicationUI table
    private String buildMedicationRowsForUI(){
        if (medications == null || medications.size() == 0) return "(none)\n";
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<medications.size();i++){
            entity.Medication m = medications.get(i);
            sb.append(String.format("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s%n",
                    m.getId(), nz(m.getName()), nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity())));
        }
        return sb.toString();
    }

    private void update(){
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance");
        ui.showUpdateHeader();
        ui.printTable(rows());
        String id = InputUtil.getInput(sc, "Treatment ID ('0' to cancel): ");
        if (id.equals("0")) return;
        Treatment tr = findTreatment(id); if (tr==null){ ui.displayNotFoundMessage(id); return; }
        ui.showUpdateIntro(id);
        String v;
    v = InputUtil.getInput(sc, "Doctor's Diagnosis (blank keep): "); if(!v.isEmpty()) tr.setDiagnosis(v);
        v = InputUtil.getInput(sc, "Name (blank keep): "); if(!v.isEmpty()) tr.setName(v);
        v = InputUtil.getInput(sc, "Instructions (blank keep): "); if(!v.isEmpty()) tr.setInstructions(v);
        v = InputUtil.getInput(sc, "Notes (blank keep): "); if(!v.isEmpty()) tr.setNotes(v);
        v = InputUtil.getInput(sc, "Cost (blank keep): "); if(!v.isEmpty()) try{ tr.setCost(Double.parseDouble(v)); }catch(Exception ignored){}
        // manage medication links
        if (tr.getType()== Treatment.Type.MEDICATION){
            new boundary.MedicationUI().showMedications(medications);
            System.out.println("Manage Medication Links: 1=Add 2=Clear All 3=Skip");
            int c = InputUtil.getIntInput(sc, "Choose: ");
            if (c==1){
                while (true){
                    String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                    if (mid.isEmpty()) break; if (findMedication(mid)==null){ System.out.println("Medication not found"); continue; }
                    tr.addMedicationId(mid); System.out.println("Linked "+mid);
                }
            } else if (c==2){ tr.setMedicationIds(null); System.out.println("Cleared."); }
        }
        ui.displayTreatmentUpdatedMessage(tr);
    }

    private void search(){
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance - Search");
        int opt = ui.searchMenu();
        if (opt==4) return;
        String q = ui.promptSearchQuery().toLowerCase();
        CustomADT<Treatment> results = new CustomADT<>();
        switch (opt){
            case 1 -> { // by Treatment ID
                if (treatments instanceof CustomADT<?> cadt){
                    @SuppressWarnings("unchecked") CustomADT<Treatment> list=(CustomADT<Treatment>) cadt;
                    results = list.filter((java.util.function.Predicate<Treatment>) (t -> t.getId()!=null && t.getId().toLowerCase().contains(q)));
                }
            }
            case 2 -> { // by Patient Name/ID -> via consultations
                ADTInterface<Consultation> cons = cdao.load();
                ADTInterface<entity.Patient> patients = new dao.PatientDAO().retrieveFromFile();
                for (int i=0;i<treatments.size();i++){
                    Treatment t = treatments.get(i);
                    Consultation c = findConsultationById(cons, t.getConsultationId());
                    if (c==null) continue;
                    entity.Patient p = findPatientById(patients, c.getPatientId());
                    String pid = c.getPatientId()==null?"":c.getPatientId();
                    String pname = p==null?"":p.getName()==null?"":p.getName();
                    if (pid.toLowerCase().contains(q) || pname.toLowerCase().contains(q)) results.add(t);
                }
            }
            case 3 -> { // by Doctor Name/ID -> via consultations
                ADTInterface<Consultation> cons = cdao.load();
                ADTInterface<entity.Doctor> doctors = new dao.DoctorDAO().retrieveFromFile();
                for (int i=0;i<treatments.size();i++){
                    Treatment t = treatments.get(i);
                    Consultation c = findConsultationById(cons, t.getConsultationId());
                    if (c==null) continue;
                    entity.Doctor d = findDoctorById(doctors, c.getDoctorId());
                    String did = c.getDoctorId()==null?"":c.getDoctorId();
                    String dname = d==null?"":d.getName()==null?"":d.getName();
                    if (did.toLowerCase().contains(q) || dname.toLowerCase().contains(q)) results.add(t);
                }
            }
            default -> {}
        }
        InputUtil.clearScreen();
        clinicUI.printHeader("Search Results");
        ui.printTable(rowsFor(results));
    }

    private void sort(){
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance - Sort");
        int field = ui.sortFieldMenu(); if (field==5) return;
        int dir = ui.sortDirectionMenu(); boolean asc = dir!=2;
        if (treatments instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Treatment> list=(CustomADT<Treatment>) cadt;
            CustomADT.ADTComparator<Treatment> cmp;
            switch (field){
                case 1 -> cmp = (a,b) -> safeStr(a.getId()).compareToIgnoreCase(safeStr(b.getId()));
                case 2 -> cmp = (a,b) -> safeStr(a.getConsultationId()).compareToIgnoreCase(safeStr(b.getConsultationId()));
                case 3 -> cmp = (a,b) -> safeStr(a.getType()==null?null:a.getType().name()).compareToIgnoreCase(safeStr(b.getType()==null?null:b.getType().name()));
                case 4 -> cmp = (a,b) -> safeStr(a.getStatus()==null?null:a.getStatus().name()).compareToIgnoreCase(safeStr(b.getStatus()==null?null:b.getStatus().name()));
                default -> cmp = (a,b) -> safeStr(a.getId()).compareToIgnoreCase(safeStr(b.getId()));
            }
            // Sort ascending, then reverse if needed (stable approach)
            list.sort(cmp);
            if (!asc) reverse(list);
        }
        InputUtil.clearScreen();
        clinicUI.printHeader("Sorted Treatments");
        ui.printTable(rows());
    }

    private void reverse(CustomADT<Treatment> list){
        int n = list.size();
        for (int i=0;i<n/2;i++) list.swap(i, n-1-i);
    }

    private String rowsFor(ADTInterface<Treatment> list){
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

    private String safeStr(String s){ return s==null?"":s; }
    private Consultation findConsultationById(ADTInterface<Consultation> list, String id){
        if (id==null) return null;
        for (int i=0;i<list.size();i++) if (id.equals(list.get(i).getId())) return list.get(i); return null;
    }
    private entity.Patient findPatientById(ADTInterface<entity.Patient> list, String id){
        if (id==null) return null;
        for (int i=0;i<list.size();i++) if (id.equals(list.get(i).getId())) return list.get(i); return null;
    }
    private entity.Doctor findDoctorById(ADTInterface<entity.Doctor> list, String id){
        if (id==null) return null;
        for (int i=0;i<list.size();i++) if (id.equals(list.get(i).getId())) return list.get(i); return null;
    }

    // Dispense moved under MedicationMaintenance

    private void delete(){
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Treatment Maintenance");
        ui.showDeleteHeader();
        ui.showDeleteIntro();
        String id = InputUtil.getInput(sc, "Treatment ID to delete ('0' to cancel): ");
        if (id.equals("0")) return;
        for (int i=0;i<treatments.size();i++) if (treatments.get(i).getId().equals(id)) { treatments.remove(i); ui.displayDeletedMessage(id); return; }
        ui.displayNotFoundMessage(id);
    }

    private Treatment findTreatment(String id){
        if (treatments instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Treatment> l=(CustomADT<Treatment>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Treatment>(){ public boolean test(Treatment t){ return t.getId()!=null && t.getId().equals(id);} });
            return idx>=0? l.get(idx): null;
        }
        for (int i=0;i<treatments.size();i++) if (treatments.get(i).getId().equals(id)) return treatments.get(i); return null;
    }

    // Helper to find a consultation by ID
    private Consultation findConsultationLocal(String id){
        ADTInterface<Consultation> l = consultations;
        if (l instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Consultation> cl=(CustomADT<Consultation>) cadt;
            int idx = cl.findIndex(new CustomADT.ADTPredicate<Consultation>(){ public boolean test(Consultation c){ return c.getId()!=null && c.getId().equals(id);} });
            return idx>=0? cl.get(idx): null;
        }
        for (int i=0;i<l.size();i++) if (l.get(i).getId().equals(id)) return l.get(i); return null;
    }
    private Medication findMedication(String id){
        ADTInterface<Medication> l=medications;
        if (l instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Medication> cl=(CustomADT<Medication>) cadt;
            int idx = cl.findIndex(new CustomADT.ADTPredicate<Medication>(){ public boolean test(Medication m){ return m.getId()!=null && m.getId().equals(id);} });
            return idx>=0? cl.get(idx): null;
        }
        for (int i=0;i<l.size();i++) if (l.get(i).getId().equals(id)) return l.get(i); return null;
    }
    private String nz(Object o){ return o==null?"":o.toString(); }
}
