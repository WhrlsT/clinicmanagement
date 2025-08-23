package control;

import adt.ADTInterface;
import adt.CustomADT;
import boundary.MedicationUI;
import dao.MedicationDAO;
import entity.Medication;
import utility.InputUtil;

import java.util.Scanner;

public class MedicationMaintenance {
    private final MedicationDAO dao = new MedicationDAO();
    private final MedicationUI ui = new MedicationUI();
    private ADTInterface<Medication> list = new CustomADT<>();
    private final Scanner sc = new Scanner(System.in);

    public MedicationMaintenance() { list = dao.load(); }

    public void run() {
        int c;
        do {
            ui.printTable(rows());
            c = ui.menu();
            switch (c) {
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> ui.printTable(rows());
                case 5 -> {}
                default -> System.out.println("Invalid");
            }
            dao.save(list);
        } while (c != 5);
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
        String name = InputUtil.getNonEmptyInput(sc, "Name: ");
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
        System.out.println("Added: "+m.getId());
    }

    private void update(){
        String id = InputUtil.getInput(sc, "Medication ID: ");
        Medication m = find(id); if (m==null){System.out.println("Not found"); return;}
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
        System.out.println("Updated.");
    }

    private void delete(){
        String id = InputUtil.getInput(sc, "Medication ID to delete: ");
        for (int i=0;i<list.size();i++) if (list.get(i).getId().equals(id)) { list.remove(i); System.out.println("Deleted."); return; }
        System.out.println("Not found.");
    }

    private Medication find(String id){ for (int i=0;i<list.size();i++) if (list.get(i).getId().equals(id)) return list.get(i); return null; }
    private String nz(Object o){ return o==null?"":o.toString(); }
}
