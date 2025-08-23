package boundary;

import utility.InputUtil;
import java.util.Scanner;

public class MedicationUI {
    private final Scanner sc = new Scanner(System.in);

    public int menu() {
        System.out.println("Medication Management");
        System.out.println("1. Add Medication");
        System.out.println("2. Update Medication");
        System.out.println("3. Delete Medication");
        System.out.println("4. View Medications");
        System.out.println("5. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void printTable(String rows) {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("Medications");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s\n","ID","Name","Code","Dose","Freq","Route","Qty");
        System.out.println("---------------------------------------------------------------");
        System.out.print(rows == null || rows.isBlank()?"(none)\n":rows);
        System.out.println("---------------------------------------------------------------");
    }

    public <T> void showMedications(adt.ADTInterface<entity.Medication> meds){
        System.out.println("Available Medications:");
        if (meds==null || meds.size()==0){ System.out.println("(none)"); return; }
        System.out.printf("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s%n","ID","Name","Code","Dose","Freq","Route","Qty");
        for (int i=0;i<meds.size();i++){
            entity.Medication m = meds.get(i);
            System.out.printf("%-8s|%-24s|%-10s|%-8s|%-8s|%-10s|%-8s%n", m.getId(), nz(m.getName()), nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity()));
        }
    }

    private String nz(Object o){ return o==null?"":o.toString(); }
}
