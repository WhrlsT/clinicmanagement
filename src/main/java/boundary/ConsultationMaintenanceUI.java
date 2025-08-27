package boundary;

import adt.ADTInterface;
import control.ConsultationMaintenance;
import control.DoctorMaintenance;
import control.PatientMaintenance;
import entity.Consultation;
import entity.Doctor;
import utility.InputUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

public class ConsultationMaintenanceUI {
    private final Scanner scanner = new Scanner(System.in);
    private final ConsultationMaintenance control = new ConsultationMaintenance();

    public void run() {
        int choice;
        do {
            InputUtil.clearScreen();
            printMainHeader();
            listAllConsultations(); // Show all consultations on the main screen
            choice = getMenuChoice();
            switch (choice) {
                case 1 -> { InputUtil.clearScreen(); handleBook(); pause(); }
                case 2 -> { InputUtil.clearScreen(); handleUpdate(); pause(); }
                case 3 -> { InputUtil.clearScreen(); handleCancel(); pause(); }
                case 4 -> { InputUtil.clearScreen(); handleReschedule(); pause(); }
                case 5 -> { InputUtil.clearScreen(); handleSearch(); pause(); }
                case 6 -> { InputUtil.clearScreen(); handleAddFollowUp(); pause(); }
                case 7 -> { InputUtil.clearScreen(); handleSortByDate(); pause(); }
                case 8 -> { InputUtil.clearScreen(); handleBubbleSortStatusDate(); pause(); }
                case 9 -> { InputUtil.clearScreen(); handleSelectionSortDoctorPatient(); pause(); }
                case 10 -> { InputUtil.clearScreen(); handleBinarySearchById(); pause(); }
                case 11 -> {return;}
                default -> { printInvalidSelection(); pause(); }
            }
        } while (choice != 11);
    }

    private void listAllConsultations() {
        displayConsultationsHeader();
        ADTInterface<Consultation> consultations = control.getAllConsultations();
        if (consultations.isEmpty()) {
            displayNoConsultations();
            return;
        }
        displayConsultationsTable(consultations, true);
    }

    private boolean listBookedConsultations() {
        displayBookedConsultationsHeader();
        ADTInterface<Consultation> bookedConsultations = control.getBookedConsultations();
        if (bookedConsultations.isEmpty()) {
            displayNoBookedConsultations();
            return false;
        }
        displayConsultationsTable(bookedConsultations, true);
        return true;
    }

    private void handleBook() {
        showBookHeader();

        // 1. Select Patient
        PatientMaintenance patientMaintenance = new PatientMaintenance();
        PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
        patientUI.displayPatientsTable(patientMaintenance.getAllPatients());
        printBackHint();
        String patientId = InputUtil.getInput(scanner, "Patient ID: ");
        if (patientId.equals("0")) return;
        if (control.findPatient(patientId) == null) {
            displayPatientNotFound();
            return;
        }

        // 2. Select Doctor
        showBookHeader();
        DoctorMaintenance doctorMaintenance = new DoctorMaintenance();
        DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
        doctorUI.displayDoctorsTable(doctorMaintenance.getAllDoctors());
        printBackHint();
        String doctorId = InputUtil.getInput(scanner, "Doctor ID: ");
        if (doctorId.equals("0")) return;
        Doctor doctor = control.findDoctor(doctorId);
        if (doctor == null) {
            displayDoctorNotFound();
            return;
        }

        // 3. Pick a date and time
        System.out.println();
        showAvailableSlots(doctor, 14, LocalDate.now());

        LocalDate dateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to go back): ");
        if (dateOnly == null) return;
        Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to go back): ");
        if (hour == null) return;
        LocalDateTime date = dateOnly.atTime(hour, 0);

        if (!control.isSlotAvailable(doctor, dateOnly, hour)) {
            displaySlotUnavailable();
            return;
        }

        // 4. Reason & booking
        String reason = InputUtil.getInput(scanner, "Reason (enter '0' to go back): ");
        if (reason.equals("0")) return;

        Consultation newConsultation = control.book(patientId, doctorId, date, reason);
        if (newConsultation != null) {
            displayBooked(newConsultation.getId());
        } else {
            // Generic error, specific errors are handled above
            System.out.println("(X) Booking failed.");
        }
    }

    private void handleUpdate() {
        if (!listBookedConsultations()) return;
        String id = InputUtil.getInput(scanner, "Consultation ID to update ('0' to go back): ");
        if (id.equals("0")) return;

        Consultation c = control.getConsultationById(id);
        if (c == null) {
            displayNotFound();
            return;
        }
        if (c.getStatus() != Consultation.Status.BOOKED) {
            displayOnlyBookedAllowed();
            return;
        }

        displayCurrentConsultationDetails(c);

        String newReason = InputUtil.getInput(scanner, "New Reason (blank=keep): ");
        String newNotes = InputUtil.getInput(scanner, "New Notes (blank=keep): ");
        String newStatus = InputUtil.getInput(scanner, "New Status [BOOKED/ONGOING/TREATED] (blank=keep): ").toUpperCase();
        
        LocalDateTime newDate = null;
        String changeDt = InputUtil.getInput(scanner, "Change Date/Time? (Y/N): ").trim().toUpperCase();
        if (changeDt.equals("Y")) {
            LocalDate nd = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to cancel): ");
            if (nd == null) {
                displayDateChangeCancelled();
            } else {
                Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to cancel): ");
                if (hour == null) {
                    displayTimeChangeCancelled();
                } else {
                    newDate = nd.atTime(hour, 0);
                }
            }
        }

        String newDoctorId = null;
        String changeDoc = InputUtil.getInput(scanner, "Change Doctor? (Y/N): ").trim().toUpperCase();
        if (changeDoc.equals("Y")) {
            DoctorMaintenance dm = new DoctorMaintenance();
            DoctorMaintenanceUI dui = new DoctorMaintenanceUI();
            dui.displayDoctorsTable(dm.getAllDoctors());
            printBackHint();
            newDoctorId = InputUtil.getInput(scanner, "New Doctor ID ('0' to cancel): ");
            if (newDoctorId.equals("0")) newDoctorId = null; // Cancelled
        }

        boolean success = control.updateConsultation(c, newReason, newNotes, newStatus, newDate, newDoctorId);
        if (success) {
            displayUpdated();
        } else {
            System.out.println("(X) Update failed. The new doctor or time slot may not be available.");
        }
    }

    private void handleSearch() {
        String q = InputUtil.getInput(scanner, "Search text (ID/Patient/Doctor/Reason/Status/Date, '0' to go back): ");
        if (q.equals("0")) return;
        if (q.isEmpty()) {
            displayEmptyQuery();
            return;
        }

        displaySearchHeader();
        ADTInterface<Consultation> results = control.searchConsultations(q);
        if (results.isEmpty()) {
            displayNoMatches();
        } else {
            displayConsultationsTable(results, true);
        }
    }

    private void handleCancel() {
        if (!listBookedConsultations()) return;
        String id = InputUtil.getInput(scanner, "Consultation ID to cancel ('0' to go back): ");
        if (id.equals("0")) return;

        if (control.cancel(id)) {
            displayCancelled();
        } else {
            displayNotFound(); // Or not cancellable
        }
    }

    private void handleReschedule() {
        if (!listBookedConsultations()) return;
        String id = InputUtil.getInput(scanner, "Consultation ID to reschedule ('0' to go back): ");
        if (id.equals("0")) return;

        Consultation c = control.getConsultationById(id);
        if (c == null || c.getStatus() != Consultation.Status.BOOKED) {
            displayOnlyBookedAllowed();
            return;
        }

        Doctor d = control.findDoctor(c.getDoctorId());
        if (d != null) {
            showAvailableSlots(d, 14, LocalDate.now());
        }

        LocalDate newDateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "New Date (YYYY-MM-DD, '0' to go back): ");
        if (newDateOnly == null) return;
        Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "New Hour (0-23, '0' to go back): ");
        if (hour == null) return;
        LocalDateTime newDate = newDateOnly.atTime(hour, 0);

        if (control.reschedule(id, newDate)) {
            displayRescheduled();
        } else {
            displaySlotUnavailable();
        }
    }

    private void handleAddFollowUp() {
        listAllConsultations();
        if (control.getAllConsultations().isEmpty()) return;

        String baseId = InputUtil.getInput(scanner, "Base Consultation ID for follow-up ('0' to go back): ");
        if (baseId.equals("0")) return;

        Consultation base = control.getConsultationById(baseId);
        if (base == null) {
            displayNotFound();
            return;
        }

        Doctor doctor = control.findDoctor(base.getDoctorId());
        if (doctor == null) {
            displayDoctorNotFound();
            return;
        }

        displayCreatingFollowUpFor(base.getId(), getPatientDisplay(base.getPatientId()), getDoctorDisplay(base.getDoctorId()));
        
        LocalDate startDate = (base.getDate() == null ? LocalDate.now() : base.getDate().toLocalDate().plusWeeks(1));
        System.out.println();
        showAvailableSlots(doctor, 21, startDate);

        LocalDate dateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to go back): ");
        if (dateOnly == null) return;
        Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to go back): ");
        if (hour == null) return;
        LocalDateTime dt = dateOnly.atTime(hour, 0);

        if (!control.isSlotAvailable(doctor, dateOnly, hour)) {
            displaySlotUnavailable();
            return;
        }

        String reason = InputUtil.getInput(scanner, "Reason (blank=Follow-up, '0' to go back): ");
        if (reason.equals("0")) return;

        Consultation followUp = control.addFollowUp(baseId, dt, reason);
        if (followUp != null) {
            displayFollowUpBooked(followUp.getId(), base.getId());
        } else {
            System.out.println("(X) Failed to book follow-up.");
        }
    }

    // --- Display Methods ---

    private String getDoctorDisplay(String doctorId) {
        return control.getDoctorDisplay(doctorId);
    }

    private String getPatientDisplay(String patientId) {
        return control.getPatientDisplay(patientId);
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public void displayConsultationsTable(ADTInterface<Consultation> consultations, boolean includeFollowUp) {
        StringBuilder rows = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            String dt = c.getDate() == null ? "" : c.getDate().format(fmt);
            String patient = getPatientDisplay(c.getPatientId());
            String doctor = getDoctorDisplay(c.getDoctorId());
            String reason = c.getReason() == null ? "" : c.getReason();
            String status = c.getStatus() == null ? "" : c.getStatus().toString();
            String followOf = (c.getFollowUpOfId() == null ? "" : c.getFollowUpOfId());

            if (includeFollowUp) {
                rows.append(String.format("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
                    c.getId(), dt, patient, doctor, truncate(reason, 20), status, followOf));
            } else {
                rows.append(String.format("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
                    c.getId(), dt, patient, doctor, truncate(reason, 20), status));
            }
        }
        
        if (includeFollowUp) {
            System.out.printf("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
                "ConsultID", "Date/Time", "Patient", "Doctor", "Reason", "Status", "FollowOf");
        } else {
            System.out.printf("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
                "ConsultID", "Date/Time", "Patient", "Doctor", "Reason", "Status");
        }
        System.out.println("-".repeat(120));
        System.out.print(rows);
    }

    public void displayCurrentConsultationDetails(Consultation c) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        System.out.println("Current: ");
        System.out.println("  Date/Time: " + (c.getDate() == null ? "" : c.getDate().format(fmt)));
        System.out.println("  Doctor:    " + getDoctorDisplay(c.getDoctorId()));
        System.out.println("  Patient:   " + getPatientDisplay(c.getPatientId()));
        System.out.println("  Status:    " + c.getStatus());
        System.out.println("  Reason:    " + safe(c.getReason()));
        System.out.println("  Notes:     " + safe(c.getNotes()));
    }

    public void showAvailableSlots(Doctor doctor, int days, LocalDate startDate) {
        String rangeText = "from " + startDate + " for " + days + " days";
        Map<LocalDate, ADTInterface<Integer>> slots = control.getAvailableSlots(doctor, days, startDate);
        
        StringBuilder body = new StringBuilder();
        if (slots.isEmpty()) {
            body.append("(No available slots ").append(rangeText).append(")");
        } else {
            for (Map.Entry<LocalDate, ADTInterface<Integer>> entry : slots.entrySet()) {
                StringBuilder hours = new StringBuilder();
                ADTInterface<Integer> hourList = entry.getValue();
                for (int i = 0; i < hourList.size(); i++) {
                    hours.append(String.format("%02d:00 ", hourList.get(i)));
                }
                body.append(String.format("%s : %s%n", entry.getKey(), hours.toString().trim()));
            }
        }
        
        System.out.println("\nAvailable slots for doctor " + doctor.getId() + " (" + rangeText + "):");
        System.out.print(body);
        System.out.println();
    }
    
    /**
     * Prints the main header for the consultation maintenance section.
     */
    public void printMainHeader() {
        System.out.println("═".repeat(100));
        System.out.println("CLINIC CONSULTATION MAINTENANCE");
        System.out.println("═".repeat(100));
    }

    public int getMenuChoice() {
        System.out.println();
        System.out.println("1. Book Consultation");
        System.out.println("2. Update Consultation");
        System.out.println("3. Cancel Consultation");
        System.out.println("4. Reschedule Consultation");
        System.out.println("5. Search Consultations");
    System.out.println("6. Add Follow-up Consultation");
    System.out.println("7. Sort by Date/Time (merge sort)");
    System.out.println("8. Sort by Status then Date (bubble sort)");
    System.out.println("9. Sort by Doctor then Patient (selection sort)");
    System.out.println("10. Binary Search by ID");
    System.out.println("11. Back");
        return InputUtil.getIntInput(scanner, "Choose: ");
    }

    public void printInvalidSelection() {
        System.out.println("Invalid");
    }

    /**
     * Prints a hint for going back and pauses the screen.
     */
    public void printBackHint() { System.out.println("(Enter '0' to go back)"); }
    public void pause() { InputUtil.pauseScreen(); }

    /**
     * Displays headers and tables for consultations and booked consultations.
     */
    public void displayConsultationsHeader() {
        System.out.println("═".repeat(100));
        System.out.println("CONSULTATIONS");
        System.out.println("═".repeat(100));
    }

    public void displayBookedConsultationsHeader() {
        System.out.println("═".repeat(100));
        System.out.println("BOOKED CONSULTATIONS");
        System.out.println("═".repeat(100));
    }

    public void displayConsultationsTable(String rows, boolean includeFollowUp) {
    if (includeFollowUp) {
        System.out.printf("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
            "ConsultID","Date/Time","Patient","Doctor","Reason","Status","FollowOf");
    } else {
        System.out.printf("%-12s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
            "ConsultID","Date/Time","Patient","Doctor","Reason","Status");
        }
        System.out.println("-".repeat(100));
        System.out.print(rows);
    }

    // === Handlers for new sort/search actions ===
    private void handleSortByDate() {
        displayConsultationsHeader();
        ADTInterface<Consultation> sorted = control.sortByDateTime();
        if (sorted.isEmpty()) displayNoConsultations();
        else displayConsultationsTable(sorted, true);
    }

    private void handleBubbleSortStatusDate() {
        displayConsultationsHeader();
        ADTInterface<Consultation> sorted = control.bubbleSortByStatusThenDate();
        if (sorted.isEmpty()) displayNoConsultations();
        else displayConsultationsTable(sorted, true);
    }

    private void handleSelectionSortDoctorPatient() {
        displayConsultationsHeader();
        ADTInterface<Consultation> sorted = control.selectionSortByDoctorThenPatient();
        if (sorted.isEmpty()) displayNoConsultations();
        else displayConsultationsTable(sorted, true);
    }

    private void handleBinarySearchById() {
        String id = InputUtil.getInput(scanner, "Enter Consultation ID to search: ");
        if (id == null || id.isBlank()) { displayEmptyQuery(); return; }
        Consultation found = control.binarySearchByIdGet(id);
        if (found == null) { displayNoMatches(); return; }
        adt.CustomADT<Consultation> single = new adt.CustomADT<>();
        single.add(found);
        displayConsultationsHeader();
        displayConsultationsTable(single, true);
    }

    public void displayNoConsultations() {
        System.out.println("No consultations found.");
    }

    public void displayNoBookedConsultations() {
        System.out.println("No booked consultations.");
    }

    /**
     * Displays search results headers and messages for search operations.
     */
    public void displaySearchHeader() {
        System.out.println("═".repeat(100));
        System.out.println("SEARCH RESULTS");
        System.out.println("═".repeat(100));
    }

    public void displayNoMatches() { System.out.println("(no matches)"); }
    public void displayEmptyQuery() { System.out.println("(empty query)"); }

    /**
     * Displays booking headers, status, and error messages for booking consultations.
     */
    public void showBookHeader() {
        InputUtil.clearScreen();
        System.out.println("═".repeat(60));
        System.out.println("BOOK CONSULTATION");
        System.out.println("═".repeat(60));
    }

    public void displayDoctorNotFound() { System.out.println("(X) Doctor not found."); }
    public void displayPatientNotFound() { System.out.println("(X) Patient not found."); }
    public void displaySlotUnavailable() { System.out.println("(X) Selected slot is not available. Please choose one of the listed slots."); }
    public void displayBooked(String id) { System.out.println("(/) Booked: "+id); }

    /**
     * Displays details and status messages for updating consultations.
     */
    public void displayCurrentConsultationDetails(String dateTime, String doctor, String patient,
                                                  String status, String reason, String notes) {
        System.out.println("Current: ");
        System.out.println("  Date/Time: " + dateTime);
        System.out.println("  Doctor:    " + doctor);
        System.out.println("  Patient:   " + patient);
        System.out.println("  Status:    " + status);
        System.out.println("  Reason:    " + reason);
        System.out.println("  Notes:     " + notes);
    }
    public void displayInvalidStatusKeepOld() { System.out.println("Invalid status; keeping old."); }
    public void displayDateChangeCancelled() { System.out.println("Date change cancelled."); }
    public void displayTimeChangeCancelled() { System.out.println("Time change cancelled."); }
    public void displayDoctorNotAvailable() { System.out.println("(X) Doctor not available at that time."); }
    public void displayDoctorChangeCancelled() { System.out.println("Doctor change cancelled."); }
    public void displayNewDoctorNotAvailable() { System.out.println("(X) New doctor not available at this time."); }
    public void displayUpdated() { System.out.println("Updated."); }

    /**
     * Displays messages for cancelling and rescheduling consultations.
     */
    public void displayCancelled() { System.out.println("Cancelled."); }
    public void displayRescheduled() { System.out.println("Rescheduled."); }
    public void displayOnlyBookedAllowed() { System.out.println("Only consultations with status BOOKED are allowed for this action."); }

    /**
     * Displays messages for creating and booking follow-up consultations.
     */
    public void displayCreatingFollowUpFor(String baseId, String patientDisplay, String doctorDisplay) {
        System.out.println("Creating follow-up for: " + baseId);
        System.out.println("  Patient: " + patientDisplay);
        System.out.println("  Doctor : " + doctorDisplay);
    }
    public void displayFollowUpBooked(String id, String baseId) {
        System.out.println("(/) Follow-up booked: "+id+" (for "+baseId+")");
    }

    /**
     * Displays available slots for a doctor within a given range.
     */
    public void displayAvailableSlots(String doctorId, String rangeText, String body) {
        System.out.println("\nAvailable slots for doctor " + doctorId + " (" + rangeText + "):");
        if (body == null || body.isBlank()) {
            System.out.println("(No available slots " + rangeText + ")");
        } else {
            System.out.print(body);
        }
        System.out.println();
    }

    /**
     * Displays validation error messages for date and hour input.
     */
    public void displayInvalidDateBack() { System.out.println("Invalid date (yyyy-MM-dd). Try again or '0' to go back."); }
    public void displayInvalidHourBack() { System.out.println("Invalid hour (0-23). Try again or '0' to go back."); }

    /**
     * Displays a generic not found message.
     */
    public void displayNotFound() { System.out.println("Not found."); }
}
