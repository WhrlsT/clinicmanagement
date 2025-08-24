package boundary;

import java.util.Scanner;
import utility.InputUtil;
import entity.Doctor;
import adt.ADTInterface;

public class DoctorMaintenanceUI {

    private Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Add Doctor");
        System.out.println("2. Update Doctor");
        System.out.println("3. Delete Doctor");
        System.out.println("4. View Doctor Details");
        System.out.println("5. Search Doctor");
    System.out.println("6. View Overall Duty Schedule");
    System.out.println("7. Set Doctor Availability Range");
    System.out.println("8. View Doctor's Consultations");
    System.out.println("9. View Duty Dashboard");
    System.out.println("10. Exit");
        System.out.print("Select an option: ");
        return InputUtil.getIntInput(scanner, "Enter your choice: ");
    }

    public void displayDoctorsTable(String outputStr) {
        System.out.println("\n----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Doctor List");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
    System.out.printf("%-10s|%-20s|%-15s|%-20s|%-20s\n", "ID", "Name", "Specialty", "Phone", "Email");
    System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
        if (outputStr == null || outputStr.trim().isEmpty()) {
            System.out.println("No records found.\n");
        } else {
            System.out.println(outputStr);
        }
    }

    // Overload: build and display table from list of doctors
    public void displayDoctorsTable(ADTInterface<Doctor> doctors) {
        StringBuilder sb = new StringBuilder();
        if (doctors != null) {
            for (int i = 0; i < doctors.size(); i++) {
                Doctor d = doctors.get(i);
                sb.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s\n",
                        d.getId(),
                        d.getName(),
                        d.getSpecialization(),
                        d.getPhoneNumber(),
                        d.getEmail()
                ));
            }
        }
        displayDoctorsTable(sb.toString());
    }

    public Doctor inputDoctorDetails() {
        String name = InputUtil.getInput(scanner, "Enter doctor name: ");
        String specialty = InputUtil.getInput(scanner, "Enter doctor specialty: ");
        String phoneNumber = InputUtil.getInput(scanner, "Enter doctor phone number: ");
    // Address field removed from Doctor entity and listing; omit address input.
        String email = InputUtil.getInput(scanner, "Enter doctor email: ");
        // Add other fields as needed

        return new Doctor(null, name, specialty, phoneNumber, email);
    }

    public void displayDoctorDetails(Doctor doctor) {
        System.out.println("\nDoctor Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", doctor.getId());
        System.out.printf("Name: %s\n", doctor.getName());
        System.out.printf("Specialty: %s\n", doctor.getSpecialization());
        System.out.printf("Phone: %s\n", doctor.getPhoneNumber());
        System.out.printf("Email: %s\n", doctor.getEmail());
        System.out.println("-----------------------------------------------");
    }

    public void displayDoctorAddedMessage(Doctor doctor){
        System.out.println("Doctor : " + doctor.getName() + " added successfully:");
        displayDoctorDetails(doctor);
    }

    public void displayDoctorUpdatedMessage(Doctor doctor){
        System.out.println("Doctor : " + doctor.getName() + " updated successfully:");
        displayDoctorDetails(doctor);
    }

    public void displayNotFoundMessage(String doctorId) {
        System.out.println("Doctor with ID: " + doctorId + " not found.");
    }

    public void displayDeletedMessage(String doctorId) {
        System.out.println("Doctor with ID: " + doctorId + " deleted successfully.");
    }

    public void displayConsultations(String doctorName, String consultationsOutput) {
        System.out.println("\n=================================================================");
        System.out.println("Consultations for Dr. " + doctorName);
        System.out.println("=================================================================");
        if (consultationsOutput == null || consultationsOutput.trim().isEmpty()) {
            System.out.println("No consultations found for this doctor.");
        } else {
            System.out.printf("%-12s %-10s %-12s %-10s %-30s%n", "ConsultID", "Patient", "Date", "Status", "Reason");
            System.out.println("-----------------------------------------------------------------");
            System.out.println(consultationsOutput);
        }
        System.out.println("=================================================================\n");
    }

    // Generic menu/flow messages
    public void printInvalidChoiceMessage() {
        System.out.println("Invalid choice. Please try again.");
        InputUtil.pauseScreen();
    }

    public void printReturningToMainMenu() {
        System.out.println("Returning to Main Menu...");
    }

    // Dashboard display
    public void displayDutyDashboard(String content) {
        System.out.println("Clinic Duty Schedule Dashboard");
        System.out.println("\u2550".repeat(63));
        System.out.print(content);
        System.out.println("\u2550".repeat(63));
    }

    // Build and show dashboard from doctor list
    public void showDutyDashboard(ADTInterface<Doctor> doctorList) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        int dow = today.getDayOfWeek().getValue() - 1; // 0=Mon

        int totalDoctors = doctorList.size();
        int activeToday = 0;
        for (int i=0;i<doctorList.size();i++) if (isDoctorAvailableOnDay(doctorList.get(i), dow)) activeToday++;

        int hour = now.getHour();
        int availableNow = 0;
        for (int i=0;i<doctorList.size();i++) if (isDoctorAvailableAtTime(doctorList.get(i), dow, hour)) availableNow++;
        int coverageNow = totalDoctors==0?0:(int)Math.round(availableNow * 100.0 / totalDoctors);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total Doctors: %d    |    Active Today: %d    |    Coverage: %d%%%n%n", totalDoctors, activeToday, coverageNow));

        // Current On Duty
        String currentTimeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        sb.append(String.format("Current On Duty (%s):%n", currentTimeStr));
        for (int i=0;i<doctorList.size();i++){
            Doctor d = doctorList.get(i);
            if (!isDoctorAvailableAtTime(d, dow, hour)) continue;
            Integer until = findNextOffHour(d, dow, hour);
            String untilStr = until==null?"--:--":String.format("%02d:00", until);
            sb.append(String.format("\u2713 Dr. %s (%s)     - Until %s%n", safe(d.getName()), safe(d.getSpecialization()), untilStr));
        }
        if (availableNow==0) sb.append("(none)\n");
        sb.append('\n');

        // Next Shift Changes
        sb.append("Next Shift Changes:\n");
        boolean anyChange=false;
        for (int h=hour+1; h<24; h++){
            java.util.List<String> changes = new java.util.ArrayList<>();
            for (int i=0;i<doctorList.size();i++){
                Doctor d = doctorList.get(i);
                boolean prev = isDoctorAvailableAtTime(d, dow, h-1);
                boolean cur = isDoctorAvailableAtTime(d, dow, h);
                if (prev!=cur){ changes.add(String.format("Dr. %s %s", safe(d.getName()), cur?"starts":"ends")); }
            }
            if (!changes.isEmpty()){
                anyChange=true;
                sb.append(String.format("• %02d:00 - %s%n", h, String.join(", ", changes)));
                if (sb.length()>2000) break; // guard against overly long output
            }
        }
        if (!anyChange) sb.append("(no changes)\n");
        sb.append('\n');

        // Weekly Coverage Summary
        sb.append("Weekly Coverage Summary:\n");
        String[] dayNamesShort = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for (int d=0; d<7; d+=2){
            int cover1 = coveragePercentForDay(doctorList, d);
            String bar1 = coverageBar(cover1);
            if (d+1 < 7){
                int cover2 = coveragePercentForDay(doctorList, d+1);
                String bar2 = coverageBar(cover2);
                sb.append(String.format("%-3s: %s %3d%%    ", dayNamesShort[d], bar1, cover1));
                sb.append(String.format("%-3s: %s %3d%%%n", dayNamesShort[d+1], bar2, cover2));
            } else {
                sb.append(String.format("%-3s: %s %3d%%%n", dayNamesShort[d], bar1, cover1));
            }
        }

        displayDutyDashboard(sb.toString());
    }

    private boolean isDoctorAvailableAtTime(Doctor doctor, int dayIndex, int hour) {
        try { return doctor != null && doctor.getSchedule() != null && doctor.getSchedule().isAvailable(dayIndex, hour); }
        catch (Exception e) { return false; }
    }

    private boolean isDoctorAvailableOnDay(Doctor doctor, int dayIndex) {
        try { for (int hour = 0; hour < 24; hour++) if (isDoctorAvailableAtTime(doctor, dayIndex, hour)) return true; }
        catch (Exception ignored) {}
        return false;
    }

    private Integer findNextOffHour(Doctor d, int day, int startHour){
        for (int h=startHour+1; h<=24; h++){
            int check = (h==24)?23:h;
            if (h==24 || !isDoctorAvailableAtTime(d, day, check)) return (h==24)?24:h;
        }
        return null;
    }

    private int coveragePercentForDay(ADTInterface<Doctor> doctorList, int day){
        if (doctorList == null || doctorList.size()==0) return 0;
        int coveredSlots=0;
        for (int h=0; h<24; h++){
            int avail=0;
            for (int i=0;i<doctorList.size();i++) if (isDoctorAvailableAtTime(doctorList.get(i), day, h)) avail++;
            if (avail>0) coveredSlots++;
        }
        return (int)Math.round(coveredSlots * 100.0 / 24.0);
    }

    private String coverageBar(int percent){
        int filled = Math.max(0, Math.min(10, (int)Math.round(percent/10.0)));
        StringBuilder bar = new StringBuilder();
        for (int i=0;i<filled;i++) bar.append('█');
        for (int i=filled;i<10;i++) bar.append('░');
        return bar.toString();
    }

    private String safe(String s){ return s==null?"":s; }

    // Section intros and headers
    public void showAddDoctorIntro() {
        System.out.println("Adding a New Doctor (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showUpdateIntro(Doctor doctor) {
        System.out.println("Updating Doctor: " + doctor.getName() + " (" + doctor.getId() + ")");
        System.out.println("─".repeat(50));
        displayDoctorDetails(doctor);
        System.out.println("Enter new values (leave blank to keep current, '0' to cancel):");
        System.out.println("─".repeat(50));
    }

    public void showDeleteIntro() {
        System.out.println("Deleting a Doctor (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showDoctorFound() {
        System.out.println("\nDoctor found:");
    }

    public void showDeleteCancelled() {
        System.out.println("Delete operation cancelled.");
    }

    public void showSearchIntro() {
        System.out.println("Search for a Doctor (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showSearchResultsHeader(String query) {
        System.out.println("Search Results for: \"" + query + "\"");
        System.out.println("─".repeat(50));
    }

    public void displayConsultationsIntro() {
        System.out.println("View Doctor's Consultations (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void displayDoctorDetailedHeader() {
        System.out.println("═".repeat(60));
        System.out.println("DOCTOR DETAILED INFORMATION");
        System.out.println("═".repeat(60));
    }

    public void displayAdditionalInfo(int consultationCount) {
        System.out.println("Total Consultations: " + consultationCount);
    }

    public void displayAdditionalInfoHeader() {
        System.out.println("\nAdditional Information:");
        System.out.println("─".repeat(40));
    }

    public void displayScheduleHeader() {
        System.out.println("\nDoctor's Schedule:");
        System.out.println("─".repeat(40));
    }

    public void displayAdditionalInfoError(String message) {
        System.out.println("Error loading doctor statistics: " + message);
        System.out.println("Total Consultations: Unable to calculate");
        System.out.println("Schedule: Unable to display");
    }

    // Validation messages
    public void displayEmptyInputOrBackMessage() {
        System.out.println("Input cannot be empty. Please try again or enter '0' to go back.");
    }

    public void displayInvalidPhoneMessage() {
        System.out.println("Invalid phone (digits 7-15). Try again.");
    }

    public void displayInvalidPhoneWithBackMessage() {
        System.out.println("Invalid phone number (7-15 digits only). Please try again or enter '0' to go back.");
    }

    public void displayInvalidEmailMessage() {
        System.out.println("Invalid email format. Try again.");
    }

    public void displayInvalidEmailWithBackMessage() {
        System.out.println("Invalid email format. Please try again or enter '0' to go back.");
    }

    // Availability messages
    public void displayAvailabilityStatusDefaulted() {
        System.out.println("Invalid status, defaulting to AVAILABLE");
    }

    public void displayEndHourMustBeGreater() {
        System.out.println("End hour must be > start hour");
    }

    public void displayAvailabilityUpdated() {
        System.out.println("Availability updated.");
    }

    // Overall duty schedule headers
    public void displayOverallDutyHeader() {
        System.out.println("Overall Duty Schedule");
        System.out.println("─".repeat(50));
    }

    public void displayWeeklySummary(String summary) {
        System.out.println("Weekly Duty Overview:");
        System.out.println("═".repeat(60));
        if (summary != null && !summary.isBlank()) {
            System.out.print(summary);
        }
        System.out.println("═".repeat(60));
    }

    public void displayDayMenu() {
        System.out.println("\nView Day Details:");
        System.out.println("0. Go Back");
        System.out.println("1. Monday");
        System.out.println("2. Tuesday");
        System.out.println("3. Wednesday");
        System.out.println("4. Thursday");
        System.out.println("5. Friday");
        System.out.println("6. Saturday");
        System.out.println("7. Sunday");
    }

    public void displayDayDetailsHeader(String dayName) {
        System.out.println("Detailed Schedule for " + dayName);
        System.out.println("─".repeat(50));
    }

    public void displayDayDetails(String content) {
        if (content != null && !content.isBlank()) {
            System.out.print(content);
        }
        System.out.println("─".repeat(70));
    }

    public void displayCoverageLegend() {
        System.out.println("⚠️  = Low coverage (1 or fewer doctors)");
        System.out.println("Press Enter to continue...");
    }

    public void showViewDoctorDetailsIntro() {
        System.out.println("View Doctor Details (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }
}
