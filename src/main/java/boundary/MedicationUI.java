package boundary;

import utility.InputUtil;
import java.util.Scanner;
import entity.Medication;

public class MedicationUI {
    private final Scanner sc = new Scanner(System.in);

    public int menu() {
        System.out.println("Medication Management");
        System.out.println("1. Add Medication");
        System.out.println("2. Update Medication");
        System.out.println("3. Delete Medication");
    System.out.println("4. View Medications");
    System.out.println("5. Dispense Medications");
    System.out.println("6. Back");
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

    // New UI helpers for consistent UX
    public void showAddIntro(){
        System.out.println("Adding a New Medication (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }
    public void showUpdateIntro(String id){
        System.out.println("Updating Medication (Enter '0' to go back): " + (id==null?"":id));
        System.out.println("─".repeat(50));
    }
    public void showDeleteIntro(){
        System.out.println("Deleting a Medication (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }
    public void displayMedicationDetails(Medication m){
        System.out.println("\nMedication Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", m.getId());
        System.out.printf("Name: %s\n", nz(m.getName()));
        System.out.printf("Code: %s\n", nz(m.getCode()));
        System.out.printf("Dosage: %s\n", nz(m.getDosage()));
        System.out.printf("Frequency: %s\n", nz(m.getFrequency()));
        System.out.printf("Route: %s\n", nz(m.getRoute()));
        System.out.printf("Quantity: %s\n", nz(m.getQuantity()));
        System.out.printf("Duration (days): %s\n", nz(m.getDurationDays()));
        System.out.printf("Instructions: %s\n", nz(m.getInstructions()));
        System.out.printf("Notes: %s\n", nz(m.getNotes()));
        System.out.println("-----------------------------------------------");
    }
    public void displayMedicationAddedMessage(Medication m){
        System.out.println("Medication added successfully: " + m.getId());
        displayMedicationDetails(m);
    }
    public void displayMedicationUpdatedMessage(Medication m){
        System.out.println("Medication updated successfully: " + m.getId());
        displayMedicationDetails(m);
    }
    public void displayDeletedMessage(String id){
        System.out.println("Medication with ID: " + id + " deleted successfully.");
    }
    public void displayNotFoundMessage(String id){
        System.out.println("Medication with ID: " + id + " not found.");
    }

    // Dispense UI under Medication module
    public void showDispenseIntro(){
        System.out.println("Dispense Medications for Prescribed Treatments (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void printPendingTreatmentsTable(String rows){
        System.out.println("\n----------------------------------------------------------------------------------------------");
        System.out.println("Pending Prescribed Medication Treatments");
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.printf("%-8s|%-10s|%-28s|%-10s|%-6s\n","ID","ConsultID","Name","Status","Meds");
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.print(rows == null || rows.isBlank()?"(none)\n":rows);
        System.out.println("----------------------------------------------------------------------------------------------");
    }

    public void displayDispenseSuccess(String treatmentId){
        System.out.println("Dispensed medications for Treatment " + treatmentId + ".");
    }
}
