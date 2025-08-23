package boundary;

import utility.InputUtil;
import java.util.Scanner;

public class ConsultationMaintenanceUI {
    private final Scanner scanner = new Scanner(System.in);

    // Main header and menu
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
        System.out.println("7. Back");
        return InputUtil.getIntInput(scanner, "Choose: ");
    }

    public void printInvalidSelection() {
        System.out.println("Invalid");
    }

    // Generic helpers
    public void printBackHint() { System.out.println("(Enter '0' to go back)"); }
    public void pause() { InputUtil.pauseScreen(); }

    // Listing
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
            System.out.printf("%-10s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
                    "ID","Date/Time","Patient","Doctor","Reason","Status","FollowOf");
        } else {
            System.out.printf("%-10s | %-16s | %-20s | %-20s | %-20s | %-10s%n",
                    "ID","Date/Time","Patient","Doctor","Reason","Status");
        }
        System.out.println("-".repeat(100));
        System.out.print(rows);
    }

    public void displayNoConsultations() {
        System.out.println("No consultations found.");
    }

    public void displayNoBookedConsultations() {
        System.out.println("No booked consultations.");
    }

    // Search
    public void displaySearchHeader() {
        System.out.println("═".repeat(100));
        System.out.println("SEARCH RESULTS");
        System.out.println("═".repeat(100));
    }

    public void displayNoMatches() { System.out.println("(no matches)"); }
    public void displayEmptyQuery() { System.out.println("(empty query)"); }

    // Booking
    public void showBookHeader() {
        System.out.println("═".repeat(60));
        System.out.println("BOOK CONSULTATION");
        System.out.println("═".repeat(60));
    }

    public void displayDoctorNotFound() { System.out.println("(X) Doctor not found."); }
    public void displayPatientNotFound() { System.out.println("(X) Patient not found."); }
    public void displaySlotUnavailable() { System.out.println("(X) Selected slot is not available. Please choose one of the listed slots."); }
    public void displayBooked(String id) { System.out.println("✅ Booked: "+id); }

    // Update
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

    // Cancel/Reschedule
    public void displayCancelled() { System.out.println("Cancelled."); }
    public void displayRescheduled() { System.out.println("Rescheduled."); }
    public void displayOnlyBookedAllowed() { System.out.println("Only consultations with status BOOKED are allowed for this action."); }

    // Follow-up
    public void displayCreatingFollowUpFor(String baseId, String patientDisplay, String doctorDisplay) {
        System.out.println("Creating follow-up for: " + baseId);
        System.out.println("  Patient: " + patientDisplay);
        System.out.println("  Doctor : " + doctorDisplay);
    }
    public void displayFollowUpBooked(String id, String baseId) {
        System.out.println("✅ Follow-up booked: "+id+" (for "+baseId+")");
    }

    // Available slots
    public void displayAvailableSlots(String doctorId, String rangeText, String body) {
        System.out.println("\nAvailable slots for doctor " + doctorId + " (" + rangeText + "):");
        if (body == null || body.isBlank()) {
            System.out.println("(No available slots " + rangeText + ")");
        } else {
            System.out.print(body);
        }
        System.out.println();
    }

    // Validation helpers used by read... methods
    public void displayInvalidDateBack() { System.out.println("Invalid date (yyyy-MM-dd). Try again or '0' to go back."); }
    public void displayInvalidHourBack() { System.out.println("Invalid hour (0-23). Try again or '0' to go back."); }

    // Generic not-found
    public void displayNotFound() { System.out.println("Not found."); }
}
