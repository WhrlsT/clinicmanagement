package boundary;

import java.util.Scanner;
import utility.InputUtil;
import entity.Patient;
import adt.ADTInterface;
import entity.Consultation;
import entity.Treatment;
import entity.Medication;
import entity.Doctor;

/**
 * Console-based UI helper for patient maintenance flows.
 *
 * This class centralises all text output and small input prompts used by the
 * patient maintenance controller. It is responsible for formatting tables,
 * showing section headers, and printing validation or status messages. The
 * methods are presentation-only and do not perform business logic or I/O
 * persistence.
 */
public class PatientMaintenanceUI {
    
    private Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Add Patient");
        System.out.println("2. Update Patient");
        System.out.println("3. Delete Patient");
        System.out.println("4. View Patient Details");
        System.out.println("5. Search Patient");
        System.out.println("6. View Visit Records");
        System.out.println("7. Exit");
        System.out.println("8. Patient Demographics Report");
        System.out.print("Select an option: ");
        return InputUtil.getIntInput(scanner, "Enter your choice: ");
    }

    /**
     * Demographics report UI helpers
     */
    public void showDemographicsHeader() {
        System.out.println("═".repeat(60));
        System.out.println("PATIENT DEMOGRAPHICS REPORT");
        System.out.println("═".repeat(60));
    }

    public void displayDemographicsSummary(int totalPatients, double avgAge, adt.ADTInterface<String> genderCounts, adt.ADTInterface<String> nationalityCounts, int highFreqCount) {
        System.out.println("Summary:");
        System.out.println("Total patients: " + totalPatients);
        System.out.println("Average age: " + String.format("%.1f", avgAge));
        System.out.println("High-frequency patients (>=5 visits): " + highFreqCount);
        System.out.println("Gender distribution:");
        if (genderCounts != null) {
            for (int i = 0; i < genderCounts.size(); i++) {
                String kv = genderCounts.get(i);
                int idx = kv.indexOf(':');
                String k = idx>=0?kv.substring(0,idx):kv;
                String v = idx>=0?kv.substring(idx+1):"0";
                System.out.println("  " + k + ": " + v);
            }
        }
        System.out.println("Nationality distribution:");
        if (nationalityCounts != null) {
            for (int i = 0; i < nationalityCounts.size(); i++) {
                String kv = nationalityCounts.get(i);
                int idx = kv.indexOf(':');
                String k = idx>=0?kv.substring(0,idx):kv;
                String v = idx>=0?kv.substring(idx+1):"0";
                System.out.println("  " + k + ": " + v);
            }
        }
    }

    public void displayAgeGroupTable(adt.ADTInterface<String> ageGroups, int total) {
        System.out.println("\nAge group distribution:");
        System.out.println("Group | Count | %");
        if (ageGroups != null) {
            for (int i = 0; i < ageGroups.size(); i++) {
                String kv = ageGroups.get(i);
                int idx = kv.indexOf(':');
                String key = idx>=0?kv.substring(0,idx):kv;
                int val = 0; try { val = Integer.parseInt(idx>=0?kv.substring(idx+1):"0"); } catch(Exception ex) { val = 0; }
                double pct = total==0?0.0:(val * 100.0 / total);
                System.out.println(String.format("%-6s | %-5d | %4.1f%%", key, val, pct));
            }
        }
    }

    public void displayPerPatientCSV(String csv) {
        System.out.println("\nPer-patient CSV (first lines):");
        System.out.println(csv);
    }

    public boolean promptExportCSV() {
        String ans = InputUtil.getInput(scanner, "Export CSV to 'patient_demographics.csv'? (y/N): ");
        return ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes");
    }

    public void displayExportSaved(String path) { System.out.println("Exported CSV to: " + path); }
    public void displayExportFailed(String msg) { System.out.println("Failed to save CSV: " + msg); }

    public void displayPatientsTable(String outputStr) {
        System.out.println("\n----------------------------------------------------------------------------------------------------------------------");
        System.out.println("Patient List");
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n", "ID", "Name", "Age", "Gender", "Phone", "Email", "Nationality");
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        if (outputStr == null || outputStr.trim().isEmpty()) {
            System.out.println("No records found.\n");
        } else {
            System.out.println(outputStr);
        }
    }

    /**
     * Build and display a patients table from an ADTInterface list.
     * This overload converts the ADT list into a formatted string and delegates
     * to {@link #displayPatientsTable(String)} for printing.
     */
    public void displayPatientsTable(ADTInterface<Patient> patients) {
        StringBuilder sb = new StringBuilder();
        if (patients != null) {
            for (int i = 0; i < patients.size(); i++) {
                Patient p = patients.get(i);
                sb.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n",
                        p.getId(),
                        p.getName(),
                        p.calculateAge(p.getDateOfBirth()),
                        p.getGender(),
                        p.getPhoneNumber(),
                        p.getEmail(),
                        p.getNationality()
                ));
            }
        }
        displayPatientsTable(sb.toString());
    }

    public void printInvalidChoiceMessage() {
        System.out.println("Invalid choice. Please try again.");
        InputUtil.pauseScreen();
    }

    public void printReturningToMainMenu() {
        System.out.println("Returning to Main Menu...");
    }

    public Patient inputPatientDetails() {
        String name = InputUtil.getInput(scanner, "Enter patient name: ");
        String gender = InputUtil.getInput(scanner, "Enter patient gender: ");
        String phoneNumber = InputUtil.getInput(scanner, "Enter patient phone number: ");
        String email = InputUtil.getInput(scanner, "Enter patient email: ");
        String dateOfBirth = InputUtil.getInput(scanner, "Enter patient date of birth (yyyy-MM-dd): ");
        String nationality = InputUtil.getInput(scanner, "Enter patient nationality: ");

    return new Patient(null, name, gender, phoneNumber, email, dateOfBirth, nationality);
    }

    public void displayPatientDetails(Patient patient) {
        System.out.println("\nPatient Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", patient.getId());
        System.out.printf("Name: %s\n", patient.getName());
        System.out.printf("Gender: %s\n", patient.getGender());
        System.out.printf("Phone: %s\n", patient.getPhoneNumber());
        System.out.printf("Email: %s\n", patient.getEmail());
        System.out.printf("Nationality: %s\n", patient.getNationality());
        System.out.printf("Date of Birth: %s\n", patient.getDateOfBirth());
        System.out.printf("Age: %s\n", patient.calculateAge(patient.getDateOfBirth()));
        System.out.println("-----------------------------------------------");
    }

    public void displayPatientAddedMessage(Patient patient){
        System.out.println("Patient : " + patient.getName() + " added successfully:");
        displayPatientDetails(patient);
    }

    public void displayPatientUpdatedMessage(Patient patient){
        System.out.println("Patient : " + patient.getName() + " updated successfully:");
        displayPatientDetails(patient);
    }

    public void displayNotFoundMessage(String patientId) {
        System.out.println("Patient with ID: " + patientId + " not found.");
    }

    public void displayDeletedMessage(String patientId) {
        System.out.println("Patient with ID: " + patientId + " deleted successfully.");
    }
    
    /**
     * UI section helpers for add/update/delete flows and navigation hints.
     */
    public void showAddPatientIntro() {
        System.out.println("Adding a New Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showUpdateIntro(Patient patient) {
        System.out.println("Updating Patient: " + patient.getName() + " (" + patient.getId() + ")");
        System.out.println("─".repeat(50));
        displayPatientDetails(patient);
        System.out.println("Enter new values (leave blank to keep current, '0' to cancel):");
        System.out.println("─".repeat(50));
    }

    public void showDeleteIntro() {
        System.out.println("Deleting a Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showPatientFound() {
        System.out.println("\nPatient found:");
    }

    public void showDeleteCancelled() {
        System.out.println("Delete operation cancelled.");
    }

    public void showSearchIntro() {
        System.out.println("Search for a Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void showSearchResultsHeader(String query) {
        System.out.println("Search Results for: \"" + query + "\"");
        System.out.println("─".repeat(50));
    }

    public void displayQueueSummary(int waiting, int called, int inprog, int skipped, int completed) {
        System.out.println("Queue Summary: WAITING="+waiting+" CALLED="+called+" IN_PROGRESS="+inprog+" SKIPPED="+skipped+" COMPLETED="+completed);
    }

    /**
     * Visit records display helpers. These methods handle listing and detailed
     * views of a patient's consultations, treatments and medications.
     */
    public void displayVisitRecordsIntro() {
        System.out.println("View Patient Visit Records (Enter '0' to go back)");
        System.out.println("─".repeat(50));
    }

    public void displayPatientVisitOverview(Patient patient) {
        System.out.println("\nPatient: " + patient.getName() + " (" + patient.getId() + ")");
        System.out.println("═".repeat(80));
    }

    public void displayVisitRecordsTable(ADTInterface<Consultation> consults, ADTInterface<Doctor> doctors) {
        System.out.println("\nVisit Records:");
        System.out.printf("%-12s | %-12s | %-20s | %-12s | %-10s%n",
                "ConsultationID", "Date", "Reason", "Doctor", "Status");
        System.out.println("─".repeat(80));

        for (int i = 0; i < consults.size(); i++) {
            Consultation c = consults.get(i);
            String doctorName = getDoctorName(doctors, c.getDoctorId());
            System.out.printf("%-12s | %-12s | %-20s | %-12s | %-10s%n",
                    c.getId(),
                    c.getDate().toString(),
                    c.getReason(),
                    doctorName,
                    c.getStatus());
        }
    }

    public String promptConsultationIdForDetails() {
        System.out.println("\nEnter consultation ID to view details (or press Enter to return): ");
        return InputUtil.getInput(scanner, "").trim();
    }

    public void displayNoVisitRecords() {
        System.out.println("No visit records found for this patient.");
    }

    public void displayConsultationDetails(Consultation consultation,
                                           ADTInterface<Treatment> treatments,
                                           ADTInterface<Medication> medications,
                                           ADTInterface<Doctor> doctors) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("CONSULTATION DETAILS");
        System.out.println("═".repeat(60));
        System.out.println("Consultation ID: " + consultation.getId());
        System.out.println("Date: " + consultation.getDate());
        System.out.println("Patient ID: " + consultation.getPatientId());
        System.out.println("Doctor: " + getDoctorName(doctors, consultation.getDoctorId()));
        System.out.println("Reason: " + consultation.getReason());
        if (consultation.getNotes() != null && !consultation.getNotes().isEmpty()) {
            System.out.println("Notes: " + consultation.getNotes());
        }
        System.out.println("Status: " + consultation.getStatus());

        // treatments linked to this consultation (already filtered if caller wishes)
        ADTInterface<Treatment> consultationTreatments = treatments;
        if (consultationTreatments != null && consultationTreatments.size() > 0) {
            System.out.println("\n" + "─".repeat(60));
            System.out.println("TREATMENTS:");
            System.out.println("─".repeat(60));

            for (int i = 0; i < consultationTreatments.size(); i++) {
                Treatment t = consultationTreatments.get(i);
                System.out.println("\nTreatment ID: " + t.getId());
                System.out.println("Type: " + t.getType());
                if (t.getName() != null && !t.getName().isEmpty()) {
                    System.out.println("Name: " + t.getName());
                }
                if (t.getInstructions() != null && !t.getInstructions().isEmpty()) {
                    System.out.println("Instructions: " + t.getInstructions());
                }
                if (t.getNotes() != null && !t.getNotes().isEmpty()) {
                    System.out.println("Notes: " + t.getNotes());
                }
                System.out.println("Status: " + t.getStatus());
                if (t.getOrderedDate() != null) {
                    System.out.println("Ordered Date: " + t.getOrderedDate());
                }
                if (t.getCost() != null) {
                    System.out.println("Cost: $" + String.format("%.2f", t.getCost()));
                }

                if (t.getMedicationIds() != null && t.getMedicationIds().length > 0) {
                    System.out.println("Medications:");
                    for (String medId : t.getMedicationIds()) {
                        Medication med = findMedicationById(medications, medId);
                        if (med != null) {
                            System.out.println("  - " + med.getName() + " (" + medId + ")");
                            if (med.getDosage() != null) {
                                System.out.println("    Dosage: " + med.getDosage());
                            }
                            if (med.getFrequency() != null) {
                                System.out.println("    Frequency: " + med.getFrequency());
                            }
                            if (med.getInstructions() != null) {
                                System.out.println("    Instructions: " + med.getInstructions());
                            }
                        }
                    }
                }
                System.out.println("─".repeat(40));
            }
        } else {
            System.out.println("\nNo treatments recorded for this consultation.");
        }
        InputUtil.pauseScreen();
    }

    public void displayPatientDetailedHeader() {
        System.out.println("═".repeat(60));
        System.out.println("PATIENT DETAILED INFORMATION");
        System.out.println("═".repeat(60));
    }

    public void displayAdditionalInfo(int consultationCount, int treatmentCount) {
        System.out.println("\nAdditional Information:");
        System.out.println("─".repeat(40));
        System.out.println("Total Consultations: " + consultationCount);
        System.out.println("Total Treatments: " + treatmentCount);
        System.out.println("═".repeat(60));
    }

    public void displayAdditionalInfoError(String message) {
        System.out.println("Error loading patient statistics: " + message);
        System.out.println("Total Consultations: Unable to calculate");
        System.out.println("Total Treatments: Unable to calculate");
    }

    /**
     * Validation and prompt error messages used by input helper methods.
     * Keeping these messages in the UI class centralises wording and formatting.
     */
    public void displayEmptyInputOrBackMessage() {
        System.out.println("Input cannot be empty. Please try again or enter '0' to go back.");
    }

    public void displayInvalidGenderOrBackMessage() {
        System.out.println("Please enter M, F, or 0 to go back.");
    }

    public void displayInvalidGenderMessage() {
        System.out.println("Please enter only M or F.");
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

    public void displayInvalidDateMessage() {
        System.out.println("Invalid date (yyyy-MM-dd). Try again.");
    }

    public void displayInvalidDateWithBackMessage() {
        System.out.println("Invalid date format (use yyyy-MM-dd). Please try again or enter '0' to go back.");
    }

    /**
     * Small helper methods used by the UI to lookup and format related data.
     */
    private String getDoctorName(ADTInterface<Doctor> doctors, String doctorId) {
        if (doctorId == null || doctorId.equals("UNASSIGNED")) {
            return "UNASSIGNED";
        }
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            if (d.getId().equals(doctorId)) {
                return d.getName();
            }
        }
        return doctorId;
    }

    private Medication findMedicationById(ADTInterface<Medication> medications, String medicationId) {
        for (int i = 0; i < medications.size(); i++) {
            Medication m = medications.get(i);
            if (m.getId().equals(medicationId)) {
                return m;
            }
        }
        return null;
    }
    
}