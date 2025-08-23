package boundary;

import utility.InputUtil;
import java.util.Scanner;

public class TreatmentUI {
    private final Scanner sc = new Scanner(System.in);

    public int menu() {
        System.out.println("Treatment Management");
        System.out.println("1. Add Treatment");
        System.out.println("2. Update Treatment");
        System.out.println("3. Delete Treatment");
        System.out.println("4. View Treatments");
        System.out.println("5. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void printTable(String rows) {
        System.out.println("\n----------------------------------------------------------------------------------------------");
        System.out.println("Treatments");
    System.out.println("----------------------------------------------------------------------------------------------");
    System.out.printf("%-8s|%-10s|%-12s|%-28s|%-10s|%-6s\n","ID","Consult","Type","Name","Status","Meds");
    System.out.println("----------------------------------------------------------------------------------------------");
        System.out.print(rows == null || rows.isBlank()?"(none)\n":rows);
        System.out.println("----------------------------------------------------------------------------------------------");
    }

    public void showConsultations(adt.ADTInterface<entity.Consultation> consultations){
        System.out.println("Available Consultations:");
        if (consultations==null || consultations.size()==0){ System.out.println("(none)"); return; }
        System.out.printf("%-8s|%-10s|%-10s|%-10s|%-10s%n","ID","Patient","Doctor","Date","Reason");
        for (int i=0;i<consultations.size();i++){
            entity.Consultation c = consultations.get(i);
            System.out.printf("%-8s|%-10s|%-10s|%-10s|%-10s%n", c.getId(), nz(c.getPatientId()), nz(c.getDoctorId()), nz(c.getDate()==null?null:c.getDate().toString()), nz(c.getReason()));
        }
    }

    private String nz(Object o){ return o==null?"":o.toString(); }
}
