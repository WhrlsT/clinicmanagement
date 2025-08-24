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
    System.out.println("9. Exit");
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
