package control;

import adt.*;
import entity.Doctor;
import entity.Consultation;
import utility.InputUtil;
import dao.DoctorDAO;
import dao.ConsultationDAO;
import boundary.DoctorMaintenanceUI;
import boundary.ClinicMaintenanceUI;
import java.util.Scanner;

public class DoctorMaintenance {
    private ADTInterface<Doctor> doctorList = new CustomADT<>();
    private DoctorDAO doctorDAO = new DoctorDAO();
    private ConsultationDAO consultationDAO = new ConsultationDAO();
    DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
    ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    private Scanner scanner = new Scanner(System.in);

    public DoctorMaintenance() {
        doctorList = doctorDAO.retrieveFromFile();
    }

    public void runDoctorMaintenance() {
        InputUtil.clearScreen();
    System.out.println("Doctor Maintenance");
    System.out.println("─".repeat(50));
        doctorUI.displayDoctorsTable(getAllDoctors());
        int choice;
        do {
            choice = doctorUI.getMenuChoice();
            switch (choice) {
                case 1:
                    InputUtil.clearScreen();
                    addNewDoctor();
                    break;
                case 2:
                    InputUtil.clearScreen();
                    updateDoctor();
                    break;
                case 3:
                    InputUtil.clearScreen();
                    deleteDoctor();
                    break;
                case 4:
                    InputUtil.clearScreen();
                    viewDoctorDetails();
                    break;
                case 5:
                    InputUtil.clearScreen();
                    searchDoctor();
                    break;
                case 6:
                    InputUtil.clearScreen();
                    viewOverallDutySchedule();
                    break;
                case 7:
                    InputUtil.clearScreen();
                    setDoctorAvailabilityRange();
                    break;
                case 8:
                    InputUtil.clearScreen();
                    viewDoctorConsultations();
                    break;
                case 9:
                    doctorUI.printReturningToMainMenu();
                    break;
                default:
                    doctorUI.printInvalidChoiceMessage();
            }
            if (choice != 9 && choice != 4) {
                InputUtil.pauseScreen();
                InputUtil.clearScreen();
                System.out.println("Doctor Maintenance");
                System.out.println("─".repeat(50));
                doctorUI.displayDoctorsTable(getAllDoctors());
            }
        } while (choice != 9);
    }

    public String getAllDoctors() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
                outputStr.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s\n",
                d.getId(),
                d.getName(),
                d.getSpecialization(),
                d.getPhoneNumber(),
                d.getEmail()
            ));
        }
        return outputStr.toString();
    }

    private String generateNextDoctorId() {
        int maxId = 0;
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            String id = d.getId();
            if (id != null && id.startsWith("D")) {
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
        return String.format("D%04d", maxId + 1);
    }

    public void addNewDoctor() {
        InputUtil.clearScreen();
        System.out.println("Doctor Maintenance");
        System.out.println("─".repeat(50));
    doctorUI.showAddDoctorIntro();
        
    String name = InputUtil.getInputWithBackOption(scanner, "Enter doctor name: ");
        if (name == null) return; // User chose to go back
        
    String specialization = InputUtil.getInputWithBackOption(scanner, "Enter doctor specialty: ");
        if (specialization == null) return;
        
    String phone = InputUtil.getValidatedPhoneWithBackOption(scanner, "Enter doctor phone number (digits 7-15): ");
        if (phone == null) return;
        
    String email = InputUtil.getValidatedEmailWithBackOption(scanner, "Enter doctor email: ");
        if (email == null) return;
        
        Doctor newDoctor = new Doctor(null, name, specialization, phone, email);
        newDoctor.setId(generateNextDoctorId());
        doctorList.add(newDoctor);
        doctorDAO.saveToFile(doctorList);
        doctorUI.displayDoctorAddedMessage(newDoctor);
    }

    public void updateDoctor() {
    System.out.println("Doctor Maintenance");
    System.out.println("─".repeat(50));
    System.out.println("Updating Doctor Details (Enter '0' to go back)");
    System.out.println("─".repeat(50));
        
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to update: ");
        if (doctorId.equals("0")) {
            return; // Go back
        }
        
        Doctor doctor = findDoctorById(doctorId);

        if (doctor != null) {
            InputUtil.clearScreen();
            System.out.println("Doctor Maintenance");
            System.out.println("─".repeat(50));
            doctorUI.showUpdateIntro(doctor);

            // Update fields with optional blank to keep & validation
            doctor.setName(promptOptionalNonEmpty("Name", doctor.getName()));
            doctor.setSpecialization(promptOptionalNonEmpty("Specialty", doctor.getSpecialization()));
            doctor.setPhoneNumber(promptOptionalValidatedPhone("Phone Number", doctor.getPhoneNumber()));
            doctor.setEmail(promptOptionalValidatedEmail("Email", doctor.getEmail()));

            doctorDAO.saveToFile(doctorList);
            doctorUI.displayDoctorUpdatedMessage(doctor);
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
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

    private String promptOptionalValidatedPhone(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[0-9]{7,15}$")) return inp;
            doctorUI.displayInvalidPhoneMessage();
        }
    }

    private String promptOptionalValidatedEmail(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return inp;
            doctorUI.displayInvalidEmailMessage();
        }
    }

    private Doctor findDoctorById(String doctorId) {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> list = (CustomADT<Doctor>) cadt;
            int idx = list.findIndex(new CustomADT.ADTPredicate<Doctor>(){
                public boolean test(Doctor d){ return d.getId()!=null && d.getId().equals(doctorId); }
            });
            return idx>=0? list.get(idx) : null;
        }
        for (int i = 0; i < doctorList.size(); i++) if (doctorList.get(i).getId().equals(doctorId)) return doctorList.get(i);
        return null;
    }

    private void setDoctorAvailabilityRange() {
        doctorUI.displayDoctorsTable(doctorList);
        System.out.println("Doctor Maintenance");
        
        System.out.println("─".repeat(50));
        System.out.println("Set Doctor Availability Range (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID: ");
        if (doctorId.equals("0")) {
            return; // Go back
        }
        
        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            doctorUI.displayNotFoundMessage(doctorId);
            return;
        }
        System.out.println("Select day (0=Mon .. 6=Sun)");
        int day = InputUtil.getIntInput(scanner, "Day index: ");
        int startHour = InputUtil.getIntInput(scanner, "Start hour (0-23): ");
        int endHour = InputUtil.getIntInput(scanner, "End hour (1-24): ");
        System.out.println("Status: 1=AVAILABLE, 2=NOT_AVAILABLE");
        int statusChoice = InputUtil.getIntInput(scanner, "Choose status: ");
        entity.SlotStatus status = entity.SlotStatus.AVAILABLE;
        switch (statusChoice) {
            case 1: status = entity.SlotStatus.AVAILABLE; break;
            case 2: status = entity.SlotStatus.NOT_AVAILABLE; break;
            default: doctorUI.displayAvailabilityStatusDefaulted();
        }
        if (startHour < 0) startHour = 0;
        if (endHour > 24) endHour = 24;
        if (endHour <= startHour) { doctorUI.displayEndHourMustBeGreater(); return; }
        // If marking NOT_AVAILABLE, ensure no booked slots would be overridden
    // BOOKED state removed; no need to guard against overriding bookings in template
        doctor.getSchedule().setAvailabilityRange(day, startHour, endHour, status);
        doctorDAO.saveToFile(doctorList);
    doctorUI.displayAvailabilityUpdated();
        InputUtil.pauseScreen();
        
        // Show the doctor list again after viewing details
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Doctor Maintenance");
        doctorUI.displayDoctorsTable(getAllDoctors());
    }

    public ADTInterface<Doctor> findDoctorByIdOrName(String query) {
        ADTInterface<Doctor> results = new CustomADT<>();
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doctor = doctorList.get(i);
            if (doctor.getId().equalsIgnoreCase(query) ||
                doctor.getName().toLowerCase().contains(lowerQuery)) {
                results.add(doctor);
            }
        }
        return results;
    }

    public void searchDoctor() {
    System.out.println("Doctor Maintenance");
    System.out.println("─".repeat(50));
    doctorUI.showSearchIntro();
        
        String query = InputUtil.getInput(scanner, "Enter doctor ID or name to search: ");
        if (query.equals("0")) {
            return; // Go back
        }
        
        ADTInterface<Doctor> foundDoctors = findDoctorByIdOrName(query);
        if (foundDoctors.size() > 0) {
            InputUtil.clearScreen();
            System.out.println("Doctor Maintenance");
            System.out.println("─".repeat(50));
            doctorUI.showSearchResultsHeader(query);
            
            // Build table string similar to getAllDoctors()
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < foundDoctors.size(); i++) {
                Doctor d = foundDoctors.get(i);
                sb.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s\n",
                        d.getId(),
                        d.getName(),
                        d.getSpecialization(),
                        d.getPhoneNumber(),
                        d.getEmail()
                ));
            }
            doctorUI.displayDoctorsTable(sb.toString());
        } else {
            doctorUI.displayNotFoundMessage(query);
        }
    }

    public void deleteDoctor() {
    System.out.println("Doctor Maintenance");
    System.out.println("─".repeat(50));
    doctorUI.showDeleteIntro();
        
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to delete: ");
        if (doctorId.equals("0")) {
            return; // Go back
        }
        
        Doctor doctor = findDoctorById(doctorId);

        if (doctor != null) {
            doctorUI.showDoctorFound();
            doctorUI.displayDoctorDetails(doctor);
            System.out.println("─".repeat(50));
            
            String confirmation = InputUtil.getInput(scanner, "Are you sure you want to delete this doctor? (y/N): ");
            if (confirmation.equalsIgnoreCase("y") || confirmation.equalsIgnoreCase("yes")) {
                doctorList.remove(doctor);
                reassignDoctorIds();
                doctorDAO.saveToFile(doctorList);
                doctorUI.displayDeletedMessage(doctorId);
            } else {
                doctorUI.showDeleteCancelled();
            }
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
        }
    }

    // Reassign IDs in sequential order after deletion
    private void reassignDoctorIds() {
        ADTInterface<Doctor> tempList = new CustomADT<>();
        for (int i = 0; i < doctorList.size(); i++) {
            tempList.add(doctorList.get(i));
        }

        // Sort by ID numeric part using CustomADT comparator
        if (tempList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> list = (CustomADT<Doctor>) cadt;
            list.sort(new CustomADT.ADTComparator<Doctor>(){
                public int compare(Doctor a, Doctor b){
                    try { int n1=Integer.parseInt(a.getId().substring(1)); int n2=Integer.parseInt(b.getId().substring(1)); return Integer.compare(n1,n2);}catch(Exception e){return 0;}
                }
            });
        }

    for (int i = 0; i < tempList.size(); i++) { tempList.get(i).setId(String.format("D%04d", i + 1)); doctorList.set(i, tempList.get(i)); }
    }

    private void viewDoctorConsultations() {
        System.out.println("Doctor Maintenance");
        System.out.println("─".repeat(50));
        doctorUI.displayDoctorsTable(getAllDoctors());
    doctorUI.displayConsultationsIntro();
        
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to view consultations: ");
        if (doctorId.equals("0")) {
            return; // Go back
        }
        
        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            doctorUI.displayNotFoundMessage(doctorId);
            return;
        }

        // Load consultations and filter by doctor ID
        ADTInterface<Consultation> allConsultations = consultationDAO.load();
        StringBuilder consultationsOutput = new StringBuilder();
        
        for (int i = 0; i < allConsultations.size(); i++) {
            Consultation consultation = allConsultations.get(i);
            if (consultation.getDoctorId().equals(doctorId)) {
                consultationsOutput.append(String.format("%-10s %-10s %-20s %-10s %-30s\n",
                    consultation.getId(),
                    consultation.getPatientId(),
                    consultation.getDate().toString(),
                    consultation.getStatus(),
                    consultation.getReason()
                ));
            }
        }

        doctorUI.displayConsultations(doctor.getName(), consultationsOutput.toString());
    }

    private void viewDoctorDetails() {
    System.out.println("View Doctor Details (Enter '0' to go back)");
    System.out.println("─".repeat(50));
        
        // Show all doctors first
        doctorUI.displayDoctorsTable(getAllDoctors());
        
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to view details: ");
        if (doctorId.equals("0")) {
            return; // Go back
        }
        
        Doctor doctor = findDoctorById(doctorId);
        if (doctor != null) {
            InputUtil.clearScreen();
            doctorUI.displayDoctorDetailedHeader();
            doctorUI.displayDoctorDetails(doctor);
            
            // Show additional statistics
            doctorUI.displayAdditionalInfoHeader();
            
            try {
                // Count consultations
                ADTInterface<Consultation> allConsultations = consultationDAO.load();
                int consultationCount = 0;
                for (int i = 0; i < allConsultations.size(); i++) {
                    Consultation consultation = allConsultations.get(i);
                    if (consultation != null && 
                        consultation.getDoctorId() != null && 
                        consultation.getDoctorId().equals(doctorId)) {
                        consultationCount++;
                    }
                }
                doctorUI.displayAdditionalInfo(consultationCount);
                
                // Show doctor's schedule
                doctorUI.displayScheduleHeader();
                doctor.getSchedule().printCompactScheduleTable(false);
                
            } catch (Exception e) {
                doctorUI.displayAdditionalInfoError(e.getMessage());
            }
            System.out.println("═".repeat(60));
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
        }
        utility.InputUtil.pauseScreen();
        
        // Show the doctor list again after viewing details
        InputUtil.clearScreen();
        System.out.println("Doctor Maintenance");
        System.out.println("─".repeat(50));
        doctorUI.displayDoctorsTable(getAllDoctors());
    }

    private void viewOverallDutySchedule() {
        System.out.println("Doctor Maintenance");
        System.out.println("─".repeat(50));
    doctorUI.displayOverallDutyHeader();
        
    // Display weekly summary via UI
    String weeklySummary = buildWeeklySummary();
    doctorUI.displayWeeklySummary(weeklySummary);
        
        // Prompt for day details
    doctorUI.displayDayMenu();
        
        int dayChoice = InputUtil.getIntInput(scanner, "Select day (0-7): ");
        
        if (dayChoice == 0) {
            return; // Go back
        }
        
        if (dayChoice >= 1 && dayChoice <= 7) {
            InputUtil.clearScreen();
            displayDayDetails(dayChoice - 1); // Convert to 0-based index
            InputUtil.pauseScreen();
            
            // Show the overall schedule again
            InputUtil.clearScreen();
            viewOverallDutySchedule();
        } else {
            doctorUI.printInvalidChoiceMessage();
            InputUtil.clearScreen();
            viewOverallDutySchedule();
        }
    }
    
    private String buildWeeklySummary() {
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        StringBuilder sb = new StringBuilder();
        for (int day = 0; day < 7; day++) {
            StringBuilder doctorsOnDuty = new StringBuilder();
            int doctorCount = 0;
            for (int i = 0; i < doctorList.size(); i++) {
                Doctor doctor = doctorList.get(i);
                if (isDoctorAvailableOnDay(doctor, day)) {
                    if (doctorCount > 0) {
                        doctorsOnDuty.append(", ");
                    }
                    doctorsOnDuty.append("Dr. ").append(doctor.getName());
                    doctorCount++;
                }
            }
            if (doctorCount == 0) {
                doctorsOnDuty.append("No doctors on duty");
            }
            sb.append(String.format("%-10s: [%s] (%d doctor%s)\n",
                    dayNames[day],
                    doctorsOnDuty.toString(),
                    doctorCount,
                    doctorCount == 1 ? "" : "s"));
        }
        return sb.toString();
    }
    
    private void displayDayDetails(int dayIndex) {
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        
        System.out.println("Doctor Maintenance");
        System.out.println("─".repeat(50));
    doctorUI.displayDayDetailsHeader(dayNames[dayIndex]);
        
        // Create hourly breakdown
    StringBuilder content = new StringBuilder();
    for (int hour = 0; hour < 24; hour++) { // 24-hour view (00:00 to 23:00)
            StringBuilder availableDoctors = new StringBuilder();
            int doctorCount = 0;
            
            // Check each doctor for this specific time slot
            for (int i = 0; i < doctorList.size(); i++) {
                Doctor doctor = doctorList.get(i);
                if (isDoctorAvailableAtTime(doctor, dayIndex, hour)) {
                    if (doctorCount > 0) {
                        availableDoctors.append(", ");
                    }
                    availableDoctors.append("Dr. ").append(doctor.getName());
                    doctorCount++;
                }
            }
            
            String timeSlot = String.format("%02d:00-%02d:00", hour, (hour + 1) % 24);
            String warning = (doctorCount <= 1) ? "  ⚠️" : "";
            
            if (doctorCount == 0) {
                availableDoctors.append("No doctors available");
            }
            
        content.append(String.format("%-12s Available: %-40s (%d doctor%s)%s\n",
            timeSlot,
            availableDoctors.toString(),
            doctorCount,
            doctorCount == 1 ? "" : "s",
            warning));
        }
    doctorUI.displayDayDetails(content.toString());
    doctorUI.displayCoverageLegend();
    }
    
    private boolean isDoctorAvailableOnDay(Doctor doctor, int dayIndex) {
        // Check if doctor has any availability on this day
        // This is a simplified check - you might need to adjust based on your Schedule implementation
        try {
            for (int hour = 0; hour < 24; hour++) { // Check all 24 hours
                if (isDoctorAvailableAtTime(doctor, dayIndex, hour)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Handle any schedule access errors
            return false;
        }
        return false;
    }
    
    private boolean isDoctorAvailableAtTime(Doctor doctor, int dayIndex, int hour) {
        // Check if doctor is available at specific day and hour
        // This is a simplified implementation - adjust based on your Schedule class methods
        try {
            // Assuming your Schedule class has a method to check availability
            // You'll need to adjust this based on your actual Schedule implementation
            return doctor.getSchedule().isAvailable(dayIndex, hour);
        } catch (Exception e) {
            // Handle any schedule access errors gracefully
            return false;
        }
    }

    // Helper methods for input with back option
    // Removed local back-option helpers; using InputUtil versions
}
