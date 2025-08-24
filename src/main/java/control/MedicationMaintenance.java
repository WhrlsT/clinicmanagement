package control;

import adt.ADTInterface;
import adt.CustomADT;
import boundary.MedicationUI;
import boundary.ClinicMaintenanceUI;
import dao.MedicationDAO;
import entity.Medication;
import utility.InputUtil;

import java.util.Scanner;

public class MedicationMaintenance {
    private final MedicationDAO dao = new MedicationDAO();
    private final MedicationUI ui = new MedicationUI();
    private final ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    private ADTInterface<Medication> list = new CustomADT<>();
    private final Scanner sc = new Scanner(System.in);

    public MedicationMaintenance() { list = dao.load(); }

    public void run() {
        int c;
        do {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Medication Maintenance");
            ui.printTable(rows());
            c = ui.menu();
            switch (c) {
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> {
                    InputUtil.clearScreen();
                    clinicUI.printHeader("Clinic Medication Maintenance");
                    ui.printTable(rows());
                    InputUtil.pauseScreen();
                }
                case 5 -> {
                    InputUtil.clearScreen();
                    dispense();
                    InputUtil.pauseScreen();
                }
                case 6 -> {}
                default -> System.out.println("Invalid");
            }
            dao.save(list);
            if (c != 6 && c != 4 && c != 5) InputUtil.pauseScreen();
        } while (c != 6);
    }

    private String nextId(){ int max=0; for (int i=0;i<list.size();i++){String id=list.get(i).getId(); if(id!=null&&id.startsWith("M"))try{int n=Integer.parseInt(id.substring(1)); if(n>max)max=n;}catch(Exception ignored){}} return String.format("M%04d", max+1);}    

    private String rows(){
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<list.size();i++){
            Medication m=list.get(i);
            sb.append(String.format("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s\n",
                m.getId(), m.getName(), nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity())));
        }
        return sb.toString();
    }

    private void add(){
    InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Medication Maintenance");
    ui.showAddIntro();
    String name = InputUtil.getInput(sc, "Name ('0' to cancel): ");
    if (name.equals("0")) return;
    if (name.isBlank()) { System.out.println("Name cannot be empty."); return; }
        Medication m = new Medication(nextId(), name);
        m.setCode(InputUtil.getInput(sc, "Code (opt): "));
        m.setDosage(InputUtil.getInput(sc, "Dosage (opt): "));
        m.setFrequency(InputUtil.getInput(sc, "Frequency (opt): "));
        m.setRoute(InputUtil.getInput(sc, "Route (opt): "));
        String dur = InputUtil.getInput(sc, "Duration days (opt): "); if(!dur.isEmpty()) try{ m.setDurationDays(Integer.parseInt(dur)); }catch(Exception ignored){}
        String qty = InputUtil.getInput(sc, "Quantity (opt): "); if(!qty.isEmpty()) try{ m.setQuantity(Integer.parseInt(qty)); }catch(Exception ignored){}
        m.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
        m.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
        list.add(m);
    ui.displayMedicationAddedMessage(m);
    }

    private void update(){
    InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Medication Maintenance");
    ui.showUpdateIntro(null);
    ui.printTable(rows());
    String id = InputUtil.getInput(sc, "Medication ID ('0' to cancel): ");
    if (id.equals("0")) return;
    Medication m = find(id); if (m==null){ ui.displayNotFoundMessage(id); return; }
        String v;
        v = InputUtil.getInput(sc, "Name (blank keep): "); if(!v.isEmpty()) m.setName(v);
        v = InputUtil.getInput(sc, "Code (blank keep): "); if(!v.isEmpty()) m.setCode(v);
        v = InputUtil.getInput(sc, "Dosage (blank keep): "); if(!v.isEmpty()) m.setDosage(v);
        v = InputUtil.getInput(sc, "Frequency (blank keep): "); if(!v.isEmpty()) m.setFrequency(v);
        v = InputUtil.getInput(sc, "Route (blank keep): "); if(!v.isEmpty()) m.setRoute(v);
        v = InputUtil.getInput(sc, "Duration days (blank keep): "); if(!v.isEmpty()) try{ m.setDurationDays(Integer.parseInt(v)); }catch(Exception ignored){}
        v = InputUtil.getInput(sc, "Quantity (blank keep): "); if(!v.isEmpty()) try{ m.setQuantity(Integer.parseInt(v)); }catch(Exception ignored){}
        v = InputUtil.getInput(sc, "Instructions (blank keep): "); if(!v.isEmpty()) m.setInstructions(v);
        v = InputUtil.getInput(sc, "Notes (blank keep): "); if(!v.isEmpty()) m.setNotes(v);
    ui.displayMedicationUpdatedMessage(m);
    }

    private void delete(){
    InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Medication Maintenance");
    ui.showDeleteIntro();
    String id = InputUtil.getInput(sc, "Medication ID to delete ('0' to cancel): ");
    if (id.equals("0")) return;
    for (int i=0;i<list.size();i++) if (list.get(i).getId().equals(id)) { list.remove(i); ui.displayDeletedMessage(id); return; }
    ui.displayNotFoundMessage(id);
    }

    private Medication find(String id){
        if (list instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Medication> l=(CustomADT<Medication>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Medication>(){ public boolean test(Medication m){ return m.getId()!=null && m.getId().equals(id);} });
            return idx>=0? l.get(idx): null;
        }
        for (int i=0;i<list.size();i++) if (list.get(i).getId().equals(id)) return list.get(i);
        return null;
    }
    private String nz(Object o){ return o==null?"":o.toString(); }

    // Move dispense flow under Medication module
    private void dispense(){
        ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Medication Maintenance - Dispense");
        ui.showDispenseIntro();

        // Load treatments and medications
        dao.TreatmentDAO tdao = new dao.TreatmentDAO();
        ADTInterface<entity.Treatment> treatments = tdao.load();
        ADTInterface<entity.Medication> medications = dao.load();

        // Build list of PRESCRIBED medication-type treatments
        CustomADT<entity.Treatment> pending = new CustomADT<>();
        for (int i=0;i<treatments.size();i++){
            entity.Treatment t = treatments.get(i);
            if (t.getType()==entity.Treatment.Type.MEDICATION && t.getStatus()==entity.Treatment.TreatmentStatus.PRESCRIBED) pending.add(t);
        }
        if (pending.size()==0){ System.out.println("No prescribed medication treatments pending."); return; }

        // Render a simple rows table for pending
        StringBuilder rows = new StringBuilder();
        for (int i=0;i<pending.size();i++){
            entity.Treatment t = pending.get(i);
            int medCount = t.getMedicationIds()==null?0:t.getMedicationIds().length;
            rows.append(String.format("%-8s|%-10s|%-28s|%-10s|%-6s\n",
                t.getId(), nz(t.getConsultationId()), nz(t.getName()), t.getStatus(), medCount));
        }
        ui.printPendingTreatmentsTable(rows.toString());

        String id = InputUtil.getInput(sc, "Treatment ID to dispense ('0' to cancel): ");
        if (id.equals("0")) return;
        // Find treatment by id
        entity.Treatment tr = null; int idx=-1;
        for (int i=0;i<treatments.size();i++){ if (treatments.get(i).getId().equals(id)){ tr=treatments.get(i); idx=i; break; } }
        if (tr==null || tr.getType()!=entity.Treatment.Type.MEDICATION){ System.out.println("Treatment not found or not a medication treatment."); return; }

        // Optional: deduct stock if medication quantity tracked
        if (tr.getMedicationIds()!=null){
            for (String mid : tr.getMedicationIds()){
                if (mid==null||mid.isBlank()) continue;
                entity.Medication m = null; int midx=-1;
                for (int i=0;i<medications.size();i++){ if (mid.equals(medications.get(i).getId())){ m=medications.get(i); midx=i; break; } }
                if (m!=null && m.getQuantity()!=null && m.getQuantity()>0){
                    m.setQuantity(Math.max(0, m.getQuantity()-1));
                    // ensure save to in-memory list
                    medications.set(midx, m);
                }
            }
            // Save medications after deduction
            dao.save(medications);
        }
        tr.setStatus(entity.Treatment.TreatmentStatus.DISPENSED);
        treatments.set(idx, tr);
        tdao.save(treatments);
        ui.displayDispenseSuccess(tr.getId());
    }
}
