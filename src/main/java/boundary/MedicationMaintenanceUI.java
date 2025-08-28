package boundary;

import adt.ADTInterface;
import adt.CustomADT;
import control.MedicationMaintenance;
import entity.Medication;
import entity.Treatment;
import utility.InputUtil;

import java.util.Scanner;

/**
 * Console UI runner for Medication module. Uses control.MedicationMaintenance for logic.
 */
public class MedicationMaintenanceUI {
    private final MedicationMaintenance control = new MedicationMaintenance();
    private final Scanner sc = new Scanner(System.in);

    private boolean sortAscending = true;
    private boolean sortQuantityAscending = true;
    public void run() {
        int c;
        do {
            InputUtil.clearScreen();
            printHeader("Clinic Medication Maintenance");
            printTable(buildRows((ADTInterface<Medication>) control.getAllMedications()));
            c = menu();
            switch (c) {
                case 1 -> handleAdd();
                case 2 -> handleUpdate();
                case 3 -> handleDelete();
                case 4 -> { // view
                    InputUtil.clearScreen();
                    printHeader("Clinic Medication Maintenance");
                    printTable(buildRows((ADTInterface<Medication>) control.getAllMedications()));
                    InputUtil.pauseScreen();
                }
                case 5 -> { // dispense
                    InputUtil.clearScreen();
                    handleDispense();
                    InputUtil.pauseScreen();
                }
                case 6 -> {
                    if (sortAscending) {
                        control.sortMedicationsByName();
                        System.out.println("Medications sorted by name (ascending).");
                    } else {
                        control.sortMedicationsByNameDesc();
                        System.out.println("Medications sorted by name (descending).");
                    }
                    sortAscending = !sortAscending;
                }
                case 7 -> {
                    if (sortQuantityAscending) {
                        control.sortMedicationsByQuantity();
                        System.out.println("Medications sorted by quantity (ascending).");
                    } else {
                        control.sortMedicationsByQuantityDesc();
                        System.out.println("Medications sorted by quantity (descending).");
                    }
                    sortQuantityAscending = !sortQuantityAscending;
                }
                case 8 -> handleSearchByName();
                case 9 -> handleReport();
                case 10 -> {return;}
                default -> System.out.println("Invalid");
            }
            if (c != 10 && c != 4 && c != 5) InputUtil.pauseScreen();
        } while (c != 10);
    }

    private String buildRows(ADTInterface<Medication> list){
        if (list == null || list.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<list.size();i++){
            Medication m=list.get(i);
            String price = m.getPrice()==null? "" : String.format("%.2f", m.getPrice());
            sb.append(String.format("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s\n",
                nz(m.getId()), nz(m.getName()), price, nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity())));
        }
        return sb.toString();
    }

    private void handleAdd(){
        InputUtil.clearScreen();
    printHeader("Clinic Medication Maintenance");
    showAddIntro();
        String name = InputUtil.getInput(sc, "Name ('0' to cancel): ");
        if (name.equals("0")) return;
        if (name.isBlank()) { System.out.println("Name cannot be empty."); return; }
        Medication m = new Medication();
        m.setName(name);
        // Price (required)
        while (true) {
            String priceInput = InputUtil.getInput(sc, "Price (required, '0' to cancel): ");
            if (priceInput.equals("0")) return;
            if (priceInput.isBlank()) { System.out.println("Price is required."); continue; }
            try { m.setPrice(Double.parseDouble(priceInput)); break; } catch (Exception e) { System.out.println("Invalid price. Enter a numeric value."); }
        }
        m.setCode(InputUtil.getInput(sc, "Code (opt): "));
        // Dosage (required)
        while (true) {
            String d = InputUtil.getInput(sc, "Dosage (required, '0' to cancel): ");
            if (d.equals("0")) return;
            if (d.isBlank()) { System.out.println("Dosage is required."); continue; }
            m.setDosage(d); break;
        }
        // Frequency (required)
        while (true) {
            String f = InputUtil.getInput(sc, "Frequency (required, '0' to cancel): ");
            if (f.equals("0")) return;
            if (f.isBlank()) { System.out.println("Frequency is required."); continue; }
            m.setFrequency(f); break;
        }
        // Route (required)
        while (true) {
            String r = InputUtil.getInput(sc, "Route (required, '0' to cancel): ");
            if (r.equals("0")) return;
            if (r.isBlank()) { System.out.println("Route is required."); continue; }
            m.setRoute(r); break;
        }
        String dur = InputUtil.getInput(sc, "Duration days (opt): "); if(!dur.isEmpty()) try{ m.setDurationDays(Integer.parseInt(dur)); }catch(Exception ignored){}
        // Quantity (required)
        while (true) {
            String qty = InputUtil.getInput(sc, "Quantity (required, '0' to cancel): ");
            if (qty.equals("0")) return;
            if (qty.isBlank()) { System.out.println("Quantity is required."); continue; }
            try { m.setQuantity(Integer.parseInt(qty)); break; } catch (Exception e) { System.out.println("Invalid quantity. Enter an integer."); }
        }
        m.setInstructions(InputUtil.getInput(sc, "Instructions (opt): "));
    m.setNotes(InputUtil.getInput(sc, "Notes (opt): "));
    Medication added = control.addMedication(m);
    if (added != null) displayMedicationAddedMessage(added);
    }

    private void handleUpdate(){
        InputUtil.clearScreen();
     printHeader("Clinic Medication Maintenance");
    showUpdateIntro(null);
    printTable(buildRows(control.getAllMedications()));
        String id = InputUtil.getInput(sc, "Medication ID ('0' to cancel): ");
        if (id.equals("0")) return;
        Medication m = control.findById(id);
    if (m==null){ displayNotFoundMessage(id); return; }
        String v;
        v = InputUtil.getInput(sc, "Name (blank keep): "); if(!v.isEmpty()) m.setName(v);
        v = InputUtil.getInput(sc, "Code (blank keep): "); if(!v.isEmpty()) m.setCode(v);
        v = InputUtil.getInput(sc, "Dosage (blank keep): "); if(!v.isEmpty()) m.setDosage(v);
        v = InputUtil.getInput(sc, "Frequency (blank keep): "); if(!v.isEmpty()) m.setFrequency(v);
        v = InputUtil.getInput(sc, "Route (blank keep): "); if(!v.isEmpty()) m.setRoute(v);
        v = InputUtil.getInput(sc, "Duration days (blank keep): "); if(!v.isEmpty()) try{ m.setDurationDays(Integer.parseInt(v)); }catch(Exception ignored){}
        v = InputUtil.getInput(sc, "Quantity (blank keep): "); if(!v.isEmpty()) try{ m.setQuantity(Integer.parseInt(v)); }catch(Exception ignored){}
        v = InputUtil.getInput(sc, "Price (blank keep): "); if(!v.isEmpty()) try{ m.setPrice(Double.parseDouble(v)); }catch(Exception ignored){}
        v = InputUtil.getInput(sc, "Instructions (blank keep): "); if(!v.isEmpty()) m.setInstructions(v);
        v = InputUtil.getInput(sc, "Notes (blank keep): "); if(!v.isEmpty()) m.setNotes(v);
    if (control.updateMedication(m)) displayMedicationUpdatedMessage(m);
    }

    private void handleDelete(){
        InputUtil.clearScreen();
      printHeader("Clinic Medication Maintenance");
    showDeleteIntro();
        String id = InputUtil.getInput(sc, "Medication ID to delete ('0' to cancel): ");
        if (id.equals("0")) return;
    if (control.deleteMedication(id)) displayDeletedMessage(id); else displayNotFoundMessage(id);
    }

    private void handleDispense(){
        ClinicMaintenanceUI header = new ClinicMaintenanceUI();
        InputUtil.clearScreen();
    header.printHeader("Clinic Medication Maintenance - Dispense");
    showDispenseIntro();

        CustomADT<Treatment> pending = control.getPendingMedicationTreatments();
        if (pending.size()==0){ System.out.println("No prescribed medication treatments pending."); return; }

        StringBuilder rows = new StringBuilder();
        for (int i=0;i<pending.size();i++){
            Treatment t = pending.get(i);
            int medCount = t.getMedicationIds()==null?0:t.getMedicationIds().length;
            rows.append(String.format("%-8s|%-10s|%-28s|%-10s|%-6s\n",
                nz(t.getId()), nz(t.getConsultationId()), nz(t.getName()), t.getStatus(), medCount));
        }
    printPendingTreatmentsTable(rows.toString());

        String id = InputUtil.getInput(sc, "Treatment ID to dispense ('0' to cancel): ");
        if (id.equals("0")) return;
        boolean ok = control.dispenseTreatment(id);
        if (ok) displayDispenseSuccess(id); else System.out.println("Treatment not found or not a medication treatment.");
    }

    // ===== Moved over from MedicationUI =====
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
        System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s\n","ID","Name","Price","Code","Dose","Freq","Route","Qty");
        System.out.println("---------------------------------------------------------------");
        System.out.print(rows == null || rows.isBlank()?"(none)\n":rows);
        System.out.println("---------------------------------------------------------------");
    }

    public void showMedications(adt.ADTInterface<entity.Medication> meds){
        System.out.println("Available Medications:");
        if (meds==null || meds.size()==0){ System.out.println("(none)"); return; }
        System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s%n","ID","Name","Price","Code","Dose","Freq","Route","Qty");
        for (int i=0;i<meds.size();i++){
            entity.Medication m = meds.get(i);
            String price = m.getPrice()==null? "" : String.format("%.2f", m.getPrice());
            System.out.printf("%-8s|%-24s|%-10s|%-10s|%-8s|%-8s|%-10s|%-8s%n", m.getId(), nz(m.getName()), price, nz(m.getCode()), nz(m.getDosage()), nz(m.getFrequency()), nz(m.getRoute()), nz(m.getQuantity()));
        }
    }

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
        String price = m.getPrice()==null? "" : String.format("%.2f", m.getPrice());
        System.out.printf("Price: %s\n", price);
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

    private String nz(Object o){ return o==null?"":o.toString(); }

    
    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }

    private void handleReport() {
        ADTInterface<Medication> meds = control.getAllMedications();
        int total = meds.size();
        int totalQty = 0;
        int lowStock = 0;
        int lowStockThreshold = 5; // You can adjust this threshold
        CustomADT<control.MedicationMaintenance.DispensedCount> soldCounts = control.getMedicationDispensedCounts();
        // Helper to get sold count by medication ID
        java.util.function.Function<String, Integer> getSoldCount = (id) -> {
            for (int i = 0; i < soldCounts.size(); i++) {
                control.MedicationMaintenance.DispensedCount dc = soldCounts.get(i);
                if (dc.getMedicationId().equals(id)) return dc.getCount();
            }
            return 0;
        };
        // Clear screen (works for most terminals)
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Ignore if clear fails
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String timeGenerated = now.format(formatter);
        System.out.println("\n--- Medication Report ---");
        System.out.println("Report generated at: " + timeGenerated);
        for (int i = 0; i < meds.size(); i++) {
            Medication m = meds.get(i);
            Integer qty = m.getQuantity();
            if (qty != null) {
                totalQty += qty;
                if (qty <= lowStockThreshold) lowStock++;
            }
        }
        System.out.println("Total medications: " + total);
        System.out.println("Total quantity in stock: " + totalQty);
        System.out.println("Medications low in stock (≤ " + lowStockThreshold + "): " + lowStock);
        System.out.println("\nStock, Quantity Sold, and Price Sold per Medication:");
        System.out.printf("%-8s | %-24s | %-8s | %-8s | %-10s\n", "ID", "Name", "Stock", "Sold", "Price Sold");
        System.out.println("---------------------------------------------------------------------");
        double totalPriceSold = 0.0;
        for (int i = 0; i < meds.size(); i++) {
            Medication m = meds.get(i);
            int sold = getSoldCount.apply(m.getId());
            Integer stockQty = m.getQuantity();
            String stock = stockQty == null ? "-" : stockQty.toString();
            Double price = m.getPrice();
            double priceSold = (price == null ? 0.0 : price) * sold;
            totalPriceSold += priceSold;
            String priceSoldStr = String.format("%.2f", priceSold);
            // Red color for low stock (<= threshold)
            boolean isLow = stockQty != null && stockQty <= lowStockThreshold;
            String RED = "\u001B[31m";
            String RESET = "\u001B[0m";
            String line = String.format("%-8s | %-24s | %-8s | %-8d | %-10s", m.getId(), nz(m.getName()), stock, sold, priceSoldStr);
            if (isLow) {
                System.out.println(RED + line + RESET);
            } else {
                System.out.println(line);
            }
        }
        System.out.println("------------------------");
        System.out.printf("Total price for all sold: RM %.2f\n", totalPriceSold);
    }

    private void handleSearchByName() {
        String query = InputUtil.getInput(sc, "Enter medication name to search: ");
        if (query == null || query.isBlank()) {
            System.out.println("No search term entered.");
            return;
        }
        ADTInterface<Medication> results = new CustomADT<>();
        ADTInterface<Medication> all = control.getAllMedications();
        for (int i = 0; i < all.size(); i++) {
            Medication m = all.get(i);
            if (m.getName() != null && m.getName().toLowerCase().contains(query.toLowerCase())) {
                results.add(m);
            }
        }
        if (results.size() == 0) {
            System.out.println("No medications found matching: " + query);
            return;
        }
        System.out.println("Search Results:");
        printTable(buildRows(results));
        String id = InputUtil.getInput(sc, "Enter Medication ID to view details ('0' to cancel): ");
        if (id.equals("0")) return;
        Medication found = null;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getId().equalsIgnoreCase(id)) {
                found = results.get(i);
                break;
            }
        }
        if (found != null) {
            displayMedicationDetails(found);
        } else {
            System.out.println("Medication with ID '" + id + "' not found in search results.");
        }
    }


}
