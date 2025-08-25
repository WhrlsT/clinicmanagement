/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.*;
import entity.*;
import utility.InputUtil;
import dao.*;
import boundary.PatientMaintenanceUI;
import boundary.ClinicMaintenanceUI;
import java.util.Scanner;
/**
 *
 * @author Whrl
 */
public class PatientMaintenance {
    private ADTInterface<Patient> patientList = new CustomADT<>();
    private PatientDAO patientDAO = new PatientDAO();
    private ConsultationDAO consultationDAO = new ConsultationDAO();
    private TreatmentDAO treatmentDAO = new TreatmentDAO();
    private MedicationDAO medicationDAO = new MedicationDAO();
    private DoctorDAO doctorDAO = new DoctorDAO();
    
    ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
    private Scanner scanner = new Scanner(System.in);

    public PatientMaintenance() {
        patientList = patientDAO.retrieveFromFile();
    }

    public void runPatientMaintenance() {
    InputUtil.clearScreen();
    clinicUI.printHeader("Clinic Patient Maintenance");
    patientUI.displayPatientsTable(getAllPatients());
        int choice;
        do {
            choice = patientUI.getMenuChoice();
            switch (choice) {
                case 1:
            InputUtil.clearScreen();
            addNewPatient();
                    break;
                case 2:
            InputUtil.clearScreen();
            updatePatient();
                    break;
                case 3:
            InputUtil.clearScreen();
            deletePatient();
                    break;
                case 4:
            InputUtil.clearScreen();
            viewPatientDetails();
                    break;
                case 5:
            InputUtil.clearScreen();
            searchPatient();
                    break;
                case 6:
            InputUtil.clearScreen();
            viewPatientVisitRecords();
                    break;
                case 8:
            InputUtil.clearScreen();
            generateAndShowDemographicsReport();
                    break;
                case 7:
            patientUI.printReturningToMainMenu();
                    break;
                default:
            patientUI.printInvalidChoiceMessage();
            }
            if (choice != 7 && choice != 4) {
                InputUtil.pauseScreen();
                InputUtil.clearScreen();
                clinicUI.printHeader("Clinic Patient Maintenance");
                patientUI.displayPatientsTable(getAllPatients());
            }
        } while (choice != 7);
    }

    /**
     * Generate patient demographics and print via UI. Also offer CSV export.
     */
    private void generateAndShowDemographicsReport() {
        clinicUI.printHeader("Clinic Patient Maintenance");
        patientUI.showDemographicsHeader();

        int total = patientList.size();
        double ageSum = 0.0;
    ADTInterface<String> genderCounts = new CustomADT<>();
    ADTInterface<String> nationalityCounts = new CustomADT<>();
    ADTInterface<String> ageGroups = new CustomADT<>();
    // store as "key:count"
    ageGroups.add("0-17:0");
    ageGroups.add("18-35:0");
    ageGroups.add("36-50:0");
    ageGroups.add("51-65:0");
    ageGroups.add("66+:0");

        // Build per-patient CSV lines
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,dateOfBirth,age,gender,phone,email,nationality,visitCount,lastVisitDate,chronicFlag\n");

        // Preload consultations to compute visit counts / last visit
        ADTInterface<Consultation> allConsults = consultationDAO.load();

        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            String ageStr = p.calculateAge(p.getDateOfBirth());
            int age = 0;
            try { age = Integer.parseInt(ageStr); } catch(Exception ex) { age = 0; }
            ageSum += age;

            incrementCount(genderCounts, p.getGender());
            incrementCount(nationalityCounts, p.getNationality());

            if (age <= 17) incrementCount(ageGroups, "0-17");
            else if (age <= 35) incrementCount(ageGroups, "18-35");
            else if (age <= 50) incrementCount(ageGroups, "36-50");
            else if (age <= 65) incrementCount(ageGroups, "51-65");
            else incrementCount(ageGroups, "66+");

            // visit count and last visit
            int visitCount = 0;
            java.time.LocalDate lastVisit = null;
            for (int j = 0; j < allConsults.size(); j++) {
                Consultation c = allConsults.get(j);
                if (c.getPatientId() != null && c.getPatientId().equals(p.getId())) {
                    visitCount++;
                    if (c.getDate() != null) {
                        // consultation.getDate() may be LocalDateTime; convert to LocalDate
                        java.time.LocalDate d;
                        try { d = c.getDate().toLocalDate(); } catch (Exception ex) { d = null; }
                        if (d != null && (lastVisit == null || d.isAfter(lastVisit))) lastVisit = d;
                    }
                }
            }

            String lastVisitStr = lastVisit==null?"":lastVisit.toString();
            boolean chronic = visitCount >= 5; // heuristic

            csv.append(String.format("%s,%s,%s,%d,%s,%s,%s,%s,%d,%s,%b\n",
                    p.getId(), escapeCsv(p.getName()), p.getDateOfBirth(), age, p.getGender(), p.getPhoneNumber(), p.getEmail(), p.getNationality(), visitCount, lastVisitStr, chronic
            ));
        }

        double avgAge = total==0?0.0:ageSum / total;

        // Count high-frequency patients
        int highFreq = 0;
        // Quick scan of CSV lines (or recompute) — recompute for clarity
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            int visits = 0;
            for (int j = 0; j < allConsults.size(); j++) if (allConsults.get(j).getPatientId().equals(p.getId())) visits++;
            if (visits >= 5) highFreq++;
        }

    patientUI.displayDemographicsSummary(total, avgAge, genderCounts, nationalityCounts, highFreq);
    patientUI.displayAgeGroupTable(ageGroups, total);
        patientUI.displayPerPatientCSV(csv.toString().split("\n", 20)[0] + "\n... (truncated) ...\n");

        if (patientUI.promptExportCSV()) {
            try {
                java.nio.file.Path out = java.nio.file.Paths.get("patient_demographics.csv");
                java.nio.file.Files.writeString(out, csv.toString());
                patientUI.displayExportSaved(out.toAbsolutePath().toString());
            } catch (Exception e) {
                patientUI.displayExportFailed(e.getMessage());
            }
        }
    }

    private static String escapeCsv(String s) { if (s==null) return ""; return s.contains(",")?('"'+s.replace("\"","\"\"")+'"'):s; }

    // Helper: increment a 'key:count' stored in an ADTInterface<String>
    private void incrementCount(ADTInterface<String> list, String key) {
        if (key == null) key = "";
        for (int i = 0; i < list.size(); i++) {
            String kv = list.get(i);
            int idx = kv.indexOf(':');
            String k = idx>=0?kv.substring(0,idx):kv;
            int v = 0; try { v = Integer.parseInt(idx>=0?kv.substring(idx+1):"0"); } catch(Exception ex) { v = 0; }
            if (k.equals(key)) { list.set(i, k + ":" + (v+1)); return; }
        }
        // not found -> add
        list.add(key + ":1");
    }

    // New hub flow as requested
    public void runPatientManagementHub() {
        int choice;
        do {
            InputUtil.clearScreen();
            clinicUI.printHeader("Patient Management Hub");
            patientUI.displayPatientsTable(getAllPatients());
            displayQueueSummary();
            System.out.println("1. Manage Patient Details");
            System.out.println("2. Manage Patient Queue");
            System.out.println("3. Back to Main Menu");
            choice = InputUtil.getIntInput(scanner, "Select option: ");
            switch (choice) {
                case 1 -> runPatientMaintenance();
                case 2 -> {
                    InputUtil.clearScreen();
                    new QueueMaintenance().run();
                }
                case 3 -> patientUI.printReturningToMainMenu();
                default -> {
                    patientUI.printInvalidChoiceMessage();
                }
            }
        } while (choice != 3);
    }

    private void displayQueueSummary() {
        // Basic summary: counts by status
        try {
            // Access queue through a fresh QueueMaintenance for up-to-date view
            dao.QueueDAO qdao = new dao.QueueDAO();
            adt.ADTInterface<entity.PatientQueueEntry> q = qdao.load();
            int waiting=0, called=0, inprog=0, skipped=0, completed=0;
            for (int i=0;i<q.size();i++) {
                entity.QueueStatus st = q.get(i).getStatus();
                switch(st){
                    case WAITING -> waiting++;
                    case CALLED -> called++;
                    case IN_PROGRESS -> inprog++;
                    case SKIPPED -> skipped++;
                    case COMPLETED -> completed++;
                }
            }
            patientUI.displayQueueSummary(waiting, called, inprog, skipped, completed);
        } catch(Exception e) {
            patientUI.displayQueueSummary(0,0,0,0,0);
        }
    }

    public String getAllPatients() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            outputStr.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n",
                p.getId(),
                p.getName(),
                p.calculateAge(p.getDateOfBirth()),
                p.getGender(),
                p.getPhoneNumber(),
                p.getEmail(),
                p.getNationality()
            ));
        }
        return outputStr.toString();
    }

    public void viewAllPatients() {
        String outputStr = getAllPatients();
        patientUI.displayPatientsTable(outputStr);
    }    

    private String generateNextPatientId() {
        int maxId = 0;
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            String id = p.getId();
            if (id != null && id.startsWith("P")) {
                try {
                    int num = Integer.parseInt(id.substring(1));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // ignore invalid format
                }
            }
        }
        return String.format("P%04d", maxId + 1);
    }

    public void addNewPatient() {
    clinicUI.printHeader("Clinic Patient Maintenance");
    patientUI.showAddPatientIntro();
        
    String name = InputUtil.getInputWithBackOption(scanner, "Enter patient name: ");
        if (name == null) return; // User chose to go back
        
    String gender = InputUtil.getMFChoiceWithBackOption(scanner, "Enter patient gender", "Male", "Female");
        if (gender == null) return;
        
    String phone = InputUtil.getValidatedPhoneWithBackOption(scanner, "Enter patient phone number (digits 7-15): ");
        if (phone == null) return;
        
    String email = InputUtil.getValidatedEmailWithBackOption(scanner, "Enter patient email: ");
        if (email == null) return;
        
    String dob = InputUtil.getValidatedDateWithBackOption(scanner, "Enter patient date of birth (yyyy-MM-dd): ");
        if (dob == null) return;
        
    String nationality = InputUtil.getMFChoiceWithBackOption(scanner, "Enter patient nationality (M=Malaysian, F=Foreigner)", "Malaysian", "Foreigner");
        if (nationality == null) return;
        
        Patient newPatient = new Patient(null, name, gender, phone, email, dob, nationality);
        newPatient.setId(generateNextPatientId());
        patientList.add(newPatient);
        patientDAO.saveToFile(patientList);
        patientUI.displayPatientAddedMessage(newPatient);
    }   

    public void updatePatient() {
    clinicUI.printHeader("Clinic Patient Maintenance");
    System.out.println("Updating Patient Details (Enter '0' to go back)");
    System.out.println("─".repeat(50));
        
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to update: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        Patient patient = findPatientById(patientId);
        
        if (patient != null) {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Patient Maintenance");
            patientUI.showUpdateIntro(patient);
            
            // For each field: show current, allow blank to keep
            patient.setName(promptOptionalNonEmpty("Name", patient.getName()));
            patient.setGender(promptOptionalMF("Gender", patient.getGender(), "Male", "Female"));
            patient.setPhoneNumber(promptOptionalValidatedPhone("Phone Number", patient.getPhoneNumber()));
            patient.setEmail(promptOptionalValidatedEmail("Email", patient.getEmail()));
            patient.setDateOfBirth(promptOptionalDate("Date of Birth (yyyy-MM-dd)", patient.getDateOfBirth()));
            patient.setNationality(promptOptionalMF("Nationality (M=Malaysian,F=Foreigner)", patient.getNationality(), "Malaysian", "Foreigner"));

            patientDAO.saveToFile(patientList);
            patientUI.displayPatientUpdatedMessage(patient);
        } else {
            patientUI.displayNotFoundMessage(patientId);
        }
    }

    private String promptOptionalNonEmpty(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (!inp.isEmpty()) return inp;
        }
    }

    private String promptOptionalMF(String label, String current, String mMeaning, String fMeaning) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " (M/F, blank keep): ").trim();
            if (inp.isEmpty()) return current;
            if (inp.equalsIgnoreCase("M")) return mMeaning;
            if (inp.equalsIgnoreCase("F")) return fMeaning;
            patientUI.displayInvalidGenderMessage();
        }
    }

    private String promptOptionalValidatedPhone(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[0-9]{7,15}$")) return inp;
            patientUI.displayInvalidPhoneMessage();
        }
    }

    private String promptOptionalValidatedEmail(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return inp;
            patientUI.displayInvalidEmailMessage();
        }
    }

    private String promptOptionalDate(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            try { java.time.LocalDate.parse(inp); return inp; } catch(Exception e){ patientUI.displayInvalidDateMessage(); }
        }
    }

    private Patient findPatientById(String patientId) {
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            int idx = list.findIndex(new CustomADT.ADTPredicate<Patient>(){
                public boolean test(Patient p){ return p.getId()!=null && p.getId().equals(patientId); }
            });
            return idx>=0? list.get(idx) : null;
        }
        for (int i = 0; i < patientList.size(); i++) if (patientList.get(i).getId().equals(patientId)) return patientList.get(i);
        return null;
    }    

    public ADTInterface<Patient> findPatientByIdOrName(String query) {
        ADTInterface<Patient> results = new CustomADT<>();
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < patientList.size(); i++) {
            Patient patient = patientList.get(i);
            if (patient.getId().equalsIgnoreCase(query) ||
                patient.getName().toLowerCase().contains(lowerQuery)) {
                results.add(patient);
            }
        }
        return results;
    }

    public void searchPatient() {
        clinicUI.printHeader("Clinic Patient Maintenance");
        patientUI.showSearchIntro();
        
        String query = InputUtil.getInput(scanner, "Enter patient ID or name to search: ");
        if (query.equals("0")) {
            return; // Go back
        }
        
        ADTInterface<Patient> foundPatients = findPatientByIdOrName(query);
        if (foundPatients.size() > 0) {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Patient Maintenance");
            patientUI.showSearchResultsHeader(query);
            
            // Build table string similar to getAllPatients()
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < foundPatients.size(); i++) {
                Patient p = foundPatients.get(i);
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
            patientUI.displayPatientsTable(sb.toString());
        } else {
            patientUI.displayNotFoundMessage(query);
        }
    }

    public void deletePatient() {
    clinicUI.printHeader("Clinic Patient Maintenance");
    patientUI.showDeleteIntro();
        
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to delete: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        Patient patient = findPatientById(patientId);

        if (patient != null) {
            patientUI.showPatientFound();
            patientUI.displayPatientDetails(patient);
            System.out.println("─".repeat(50));
            
            String confirmation = InputUtil.getInput(scanner, "Are you sure you want to delete this patient? (y/N): ");
            if (confirmation.equalsIgnoreCase("y") || confirmation.equalsIgnoreCase("yes")) {
                patientList.remove(patient);
                reassignPatientIds();
                patientDAO.saveToFile(patientList);
                patientUI.displayDeletedMessage(patientId);
            } else {
                patientUI.showDeleteCancelled();
            }
        } else {
            patientUI.displayNotFoundMessage(patientId);
        }
    }

    // Reassign IDs in sequential order after deletion
    private void reassignPatientIds() {
        // Copy to a new CustomADT for sorting
        ADTInterface<Patient> tempList = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) tempList.add(patientList.get(i));
        // Use CustomADT's insertion sort with an ID numeric comparator
        if (tempList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.sort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b){
                    try { int n1=Integer.parseInt(a.getId().substring(1)); int n2=Integer.parseInt(b.getId().substring(1)); return Integer.compare(n1,n2);}catch(Exception e){return 0;}
                }
            });
        }

        // Reassign IDs and update patientList
    for (int i = 0; i < tempList.size(); i++) { tempList.get(i).setId(String.format("P%04d", i + 1)); patientList.set(i, tempList.get(i)); }
    }
    
    private void viewPatientVisitRecords() {
    clinicUI.printHeader("Clinic Patient Maintenance");
    patientUI.displayVisitRecordsIntro();
        
        // Show available patients
        patientUI.displayPatientsTable(getAllPatients());
        
        String patientId = InputUtil.getInput(scanner, "Enter Patient ID to view visit records: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        // Find patient
        Patient patient = findPatientById(patientId);
        if (patient == null) {
            patientUI.displayNotFoundMessage(patientId);
            return;
        }
        
        InputUtil.clearScreen();
        
        // Load consultations and treatments
        ADTInterface<Consultation> consultations = consultationDAO.load();
        ADTInterface<Treatment> treatments = treatmentDAO.load();
        ADTInterface<Medication> medications = medicationDAO.load();
        ADTInterface<Doctor> doctors = doctorDAO.retrieveFromFile();
        
    // Display patient info
    patientUI.displayPatientVisitOverview(patient);
        
        // Find consultations for this patient
        ADTInterface<Consultation> patientConsultations = new CustomADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getPatientId().equals(patientId)) {
                patientConsultations.add(c);
            }
        }
        
        if (patientConsultations.size() == 0) {
            patientUI.displayNoVisitRecords();
            return;
        }
        
        // Display visit records table
        patientUI.displayVisitRecordsTable(patientConsultations, doctors);
        
        // Prompt for detailed view
        String consultationId = patientUI.promptConsultationIdForDetails();
        
        if (!consultationId.isEmpty()) {
            InputUtil.clearScreen();
            viewConsultationDetails(consultationId, consultations, treatments, medications, doctors);
        }
    }
    
    private void viewConsultationDetails(String consultationId, ADTInterface<Consultation> consultations, 
                                        ADTInterface<Treatment> treatments, ADTInterface<Medication> medications,
                                        ADTInterface<Doctor> doctors) {
        
        // Find consultation
        Consultation consultation = null;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId().equals(consultationId)) {
                consultation = consultations.get(i);
                break;
            }
        }
        
        if (consultation == null) {
            patientUI.displayNotFoundMessage(consultationId);
            return;
        }
        
        // Find and display treatments for this consultation
        ADTInterface<Treatment> consultationTreatments = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (consultationId.equals(t.getConsultationId())) {
                consultationTreatments.add(t);
            }
        }

        patientUI.displayConsultationDetails(consultation, consultationTreatments, medications, doctors);
    }

    private void viewPatientDetails() {
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("View Patient Details (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        // Show all patients first
        patientUI.displayPatientsTable(getAllPatients());
        
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to view details: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        Patient patient = findPatientById(patientId);
        if (patient != null) {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Patient Maintenance");
            System.out.println("═".repeat(60));
            System.out.println("PATIENT DETAILED INFORMATION");
            System.out.println("═".repeat(60));
            patientUI.displayPatientDetails(patient);
            
            // Show additional statistics
            System.out.println("\nAdditional Information:");
            System.out.println("─".repeat(40));
            
            try {
                // Count consultations for this patient
                ADTInterface<Consultation> allConsultations = consultationDAO.load();
                int consultationCount = 0;
                for (int i = 0; i < allConsultations.size(); i++) {
                    Consultation consultation = allConsultations.get(i);
                    if (consultation != null && 
                        consultation.getPatientId() != null && 
                        consultation.getPatientId().equals(patientId)) {
                        consultationCount++;
                    }
                }
                System.out.println("Total Consultations: " + consultationCount);
                
                // Count treatments for this patient
                ADTInterface<Treatment> allTreatments = treatmentDAO.load();
                int treatmentCount = 0;
                for (int i = 0; i < allTreatments.size(); i++) {
                    Treatment treatment = allTreatments.get(i);
                    // Check if treatment has a valid consultation ID
                    if (treatment != null && treatment.getConsultationId() != null) {
                        // Treatments are linked to consultations, so we need to find consultations for this patient first
                        for (int j = 0; j < allConsultations.size(); j++) {
                            Consultation consultation = allConsultations.get(j);
                            if (consultation != null && 
                                consultation.getPatientId() != null && 
                                consultation.getId() != null &&
                                consultation.getPatientId().equals(patientId) && 
                                treatment.getConsultationId().equals(consultation.getId())) {
                                treatmentCount++;
                                break;
                            }
                        }
                    }
                }
                System.out.println("Total Treatments: " + treatmentCount);
            } catch (Exception e) {
                System.out.println("Error loading patient statistics: " + e.getMessage());
                System.out.println("Total Consultations: Unable to calculate");
                System.out.println("Total Treatments: Unable to calculate");
            }
            System.out.println("═".repeat(60));
        } else {
            patientUI.displayNotFoundMessage(patientId);
        }
        utility.InputUtil.pauseScreen();
        
        // Show the patient list again after viewing details
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Patient Maintenance");
        patientUI.displayPatientsTable(getAllPatients());
    }

}
