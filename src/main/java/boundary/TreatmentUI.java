package boundary;

import utility.InputUtil;
import java.util.Scanner;
import entity.Treatment;

/**
 * Console UI helper for treatment management flows.
 *
 * This class centralises all text output and small input prompts used by the
 * treatment controller. It focuses on presentation: printing headers, menus,
 * tables and short status/validation messages. No business logic or persistence
 * is performed here.
 */
public class TreatmentUI {
    private final Scanner sc = new Scanner(System.in);

    public void printHeader(String title) {
        System.out.println("\n" + "═".repeat(94));
        System.out.println(title.toUpperCase());
        System.out.println("═".repeat(94));
    }

    public int menu() {
    /**
     * Display the treatment management menu and return the user's choice.
     * Note: callers may print a module header before invoking this menu.
     */
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

    public void showConsultations(adt.ADTInterface<entity.Consultation> consultations){
        System.out.println("Available Consultations:");
        
        if (consultations==null || consultations.size()==0){ System.out.println("(none)"); return; }
    System.out.printf("%-12s|%-10s|%-10s|%-10s|%-10s%n","ConsultID","Patient","Doctor","Date","Reason");
        for (int i=0;i<consultations.size();i++){
            entity.Consultation c = consultations.get(i);
            System.out.printf("%-8s|%-10s|%-10s|%-10s|%-10s%n", c.getId(), nz(c.getPatientId()), nz(c.getDoctorId()), nz(c.getDate()==null?null:c.getDate().toString()), nz(c.getReason()));
        }
    }

    private String nz(Object o){ return o==null?"":o.toString(); }
    /**
     * Header helpers used by add/update/delete flows 
     */
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

    public void printBackHint() { System.out.println("(Enter '0' to go back)"); }
    /**
     * Treatment detail and status messages. 
     */
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
    /**
     * Search-related prompts and helpers.
     */
    public int searchMenu(){
        System.out.println("Search By:");
        System.out.println("1. Treatment ID");
        System.out.println("2. Patient Name/ID");
        System.out.println("3. Doctor Name/ID");
        System.out.println("4. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }
    public String promptSearchQuery(){ return InputUtil.getInput(sc, "Enter search text: "); }

    /**
     * Sort field and direction prompts used by callers that support sorting.
     */
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
}
