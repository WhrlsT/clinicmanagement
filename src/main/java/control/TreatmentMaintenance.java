package control;

import adt.ADTInterface;
import adt.CustomADT;
import boundary.TreatmentUI;
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
            ui.printTable(rows());
            c = ui.menu();
            switch(c){
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> ui.printTable(rows());
                case 5 -> {}
                default -> System.out.println("Invalid");
            }
            tdao.save(treatments);
        } while (c!=5);
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
    ui.showConsultations(consultations);
        String consultId = InputUtil.getInput(sc, "Consultation ID (optional): "); if(consultId.isBlank()) consultId=null;
        System.out.println("Type: 1=MEDICATION 2=PROCEDURE 3=THERAPY 4=IMAGING 5=LAB");
        int t = InputUtil.getIntInput(sc, "Choose: ");
        Treatment.Type type = switch(t){
            case 1 -> Treatment.Type.MEDICATION;
            case 2 -> Treatment.Type.PROCEDURE;
            case 3 -> Treatment.Type.THERAPY;
            case 4 -> Treatment.Type.IMAGING;
            case 5 -> Treatment.Type.LAB;
            default -> Treatment.Type.PROCEDURE;
        };
        // If medication, show options now and make name optional
        if (type == Treatment.Type.MEDICATION) {
            new boundary.MedicationUI().showMedications(medications);
        }
        String name = (type == Treatment.Type.MEDICATION)
            ? InputUtil.getInput(sc, "Plan name (optional, Enter to skip): ")
            : InputUtil.getInput(sc, "Name (for non-medication types): ");
        Treatment tr = new Treatment(nextId(), consultId, type, name);
        tr.setOrderedDate(LocalDate.now());
        tr.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
        tr.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
    String cost = InputUtil.getInput(sc, "Cost (opt): "); if(!cost.isEmpty()) try{ tr.setCost(Double.parseDouble(cost)); }catch(Exception ignored){}

        if (type == Treatment.Type.MEDICATION){
            // allow adding multiple medication IDs
            while (true){
                String mid = InputUtil.getInput(sc, "Medication ID to link (blank to stop): ").trim();
                if (mid.isEmpty()) break;
                if (findMedication(mid)==null){ System.out.println("Medication not found"); continue; }
                tr.addMedicationId(mid);
                System.out.println("Linked "+mid);
            }
        }

        treatments.add(tr);
        System.out.println("Added: "+tr.getId());
    }

    private void update(){
        String id = InputUtil.getInput(sc, "Treatment ID: ");
        Treatment tr = findTreatment(id); if (tr==null){ System.out.println("Not found"); return; }
        String v;
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
        System.out.println("Updated.");
    }

    private void delete(){
        String id = InputUtil.getInput(sc, "Treatment ID to delete: ");
        for (int i=0;i<treatments.size();i++) if (treatments.get(i).getId().equals(id)) { treatments.remove(i); System.out.println("Deleted."); return; }
        System.out.println("Not found.");
    }

    private Treatment findTreatment(String id){ for (int i=0;i<treatments.size();i++) if (treatments.get(i).getId().equals(id)) return treatments.get(i); return null; }
    private Medication findMedication(String id){ ADTInterface<Medication> l=medications; for (int i=0;i<l.size();i++) if (l.get(i).getId().equals(id)) return l.get(i); return null; }
    private String nz(Object o){ return o==null?"":o.toString(); }
}
