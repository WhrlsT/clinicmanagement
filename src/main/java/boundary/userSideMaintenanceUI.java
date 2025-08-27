package boundary;

import adt.ADTInterface;
import control.userSideMaintenance;
import control.DoctorMaintenance;
import entity.Consultation;
import entity.Doctor;
import entity.Treatment;
import entity.Patient;
import utility.InputUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Console UI for a patient using the system (fixed ID P0001).
 */
public class userSideMaintenanceUI {
    private final Scanner sc = new Scanner(System.in);
    private userSideMaintenance control;
    private final DoctorMaintenance doctorCtrl = new DoctorMaintenance();

    public void run() {
        if (this.control == null) this.control = new userSideMaintenance();
        int choice;
        do {
            printUserHome();
            choice = InputUtil.getIntInput(sc, "Enter your choice: ");
            InputUtil.clearScreen();
            switch (choice) {
                case 1 -> handleBook();
                case 2 -> handleViewPast();
                case 3 -> handleViewBooked();
                case 4 -> handlePayment();
                case 5 -> handleDispense();
                case 6 -> handleChangeDetails();
                case 7 -> {return;}
                default -> {
                    System.out.println("Invalid choice.");
                    InputUtil.pauseScreen();
                }
            }
        } while (choice != 7);
    }

    private void printUserHome() {
        String pid = getPatientIdLabel();
        Patient p = control.getCurrentPatient();
        String name = p == null ? "" : nz(p.getName());
        String age = p == null ? "" : nz(p.calculateAge(p.getDateOfBirth()));
        String nationality = p == null ? "" : nz(p.getNationality());
        String gender = p == null ? "" : nz(p.getGender());
        String phone = p == null ? "" : nz(p.getPhoneNumber());
        String email = p == null ? "" : nz(p.getEmail());
        LocalDateTime next = control.getNextConsultationDate();
        String nextStr = next == null ? "-" : next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        System.out.println("-----------------------------------------------");
        System.out.println("Use System as Patient " + pid);
        System.out.println("-----------------------------------------------");
        System.out.println("Name        : " + name);
        System.out.println("Age         : " + age);
        System.out.println("Nationality : " + nationality);
        System.out.println("Gender      : " + gender);
        System.out.println("Next Consultation Date: " + nextStr);
        System.out.println("Phone Contact         : " + phone);
        System.out.println("Email                 : " + email);
        System.out.println();
        System.out.println("1. Book a consultation");
        System.out.println("2. View past consultations");
        System.out.println("3. View booked consultations");
        System.out.println("4. Make payment for treatments");
        System.out.println("5. Dispense medications (for prescriptions)");
        System.out.println("6. Change Details ");
        System.out.println("7. Back to main menu");
        System.out.println("-----------------------------------------------");
    }

    private void handleViewBooked() {
        printHeader("Booked Consultations (" + getPatientIdLabel() + ")");
        ADTInterface<Consultation> booked = control.getBookedConsultations();
        if (booked.size() == 0) {
            System.out.println("No booked consultations.");
        } else {
            System.out.printf("%-8s  %-8s  %-16s  %-20s\n", "ID", "Doctor", "Date", "Reason");
            for (int i = 0; i < booked.size(); i++) {
                Consultation c = booked.get(i);
                String dt = c.getDate() == null ? "" : c.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                System.out.printf("%-8s  %-8s  %-16s  %-20s\n", nz(c.getId()), nz(c.getDoctorId()), dt, truncate(nz(c.getReason()), 20));
            }
        }
        InputUtil.pauseScreen();
    }

    private void handleBook() {
        // Mirror ConsultationMaintenanceUI.handleBook but for the current patient only
        // 1. Pick doctor
        InputUtil.clearScreen();
        System.out.println("═".repeat(60));
        System.out.println("BOOK CONSULTATION");
        System.out.println("═".repeat(60));

        ADTInterface<Doctor> doctors = doctorCtrl.getAllDoctors();
        System.out.println("Available Doctors:");
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            System.out.printf("- %s | %s | %s\n", d.getId(), nz(d.getName()), nz(d.getSpecialization()));
        }
        System.out.println("(Enter '0' to go back)");
        String doctorId = InputUtil.getInput(sc, "Doctor ID: ");
        if (doctorId.equals("0")) return;
        Doctor doctor = null;
        for (int i = 0; i < doctors.size(); i++) if (doctorId.equals(doctors.get(i).getId())) { doctor = doctors.get(i); break; }
        if (doctor == null) {
            System.out.println("(X) Doctor not found.");
            InputUtil.pauseScreen();
            return;
        }

        // 2. Show available slots for next 14 days from today
        var slots = control.getAvailableSlots(doctor, 14, LocalDate.now());
        StringBuilder body = new StringBuilder();
        if (slots.isEmpty()) {
            body.append("(No available slots from ").append(LocalDate.now()).append(" for 14 days)");
        } else {
            for (int i = 0; i < slots.size(); i++) {
                var sd = slots.get(i);
                var hours = new StringBuilder();
                var hourList = sd.hours;
                for (int j = 0; j < hourList.size(); j++) hours.append(String.format("%02d:00 ", hourList.get(j)));
                body.append(String.format("%s : %s%n", sd.date, hours.toString().trim()));
            }
        }
        System.out.println("\nAvailable slots for doctor " + doctor.getId() + " (from " + LocalDate.now() + " for 14 days):");
        System.out.print(body);
        System.out.println();

        // 3. Pick date and time
        LocalDate dateOnly = InputUtil.getValidatedLocalDateWithBackOption(sc, "Date (YYYY-MM-DD, '0' to go back): ");
        if (dateOnly == null) return;
        Integer hour = InputUtil.getValidatedHourWithBackOption(sc, "Hour (0-23, '0' to go back): ");
        if (hour == null) return;
        if (!control.isSlotAvailable(doctor, dateOnly, hour)) {
            System.out.println("(X) Selected slot is not available. Please choose one of the listed slots.");
            InputUtil.pauseScreen();
            return;
        }
        LocalDateTime date = dateOnly.atTime(hour, 0);

        // 4. Reason & book
        String reason = InputUtil.getInput(sc, "Reason (enter '0' to go back): ");
        if (reason.equals("0")) return;

        Consultation newConsultation = control.bookConsultation(doctorId, date, reason);
        if (newConsultation != null) {
            System.out.println("(/) Booked: " + newConsultation.getId());
        } else {
            System.out.println("(X) Booking failed.");
        }
        InputUtil.pauseScreen();
    }

    private void handleViewPast() {
        printHeader("Past Consultations (" + getPatientIdLabel() + ")");
        ADTInterface<Consultation> past = control.getPastConsultations();
        if (past.size() == 0) {
            System.out.println("No past consultations.");
        } else {
            // Simple table
            System.out.printf("%-8s  %-8s  %-16s  %-20s\n", "ID", "Doctor", "Date", "Reason");
            for (int i = 0; i < past.size(); i++) {
                Consultation c = past.get(i);
                String dt = c.getDate() == null ? "" : c.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                System.out.printf("%-8s  %-8s  %-16s  %-20s\n", nz(c.getId()), nz(c.getDoctorId()), dt, truncate(nz(c.getReason()), 20));
            }

            // Prompt to view details
            String id = InputUtil.getInput(sc, "Enter consultation ID to view details (Enter to return): ").trim();
            if (!id.isEmpty()) {
                Consultation sel = findConsultationById(past, id);
                if (sel == null) {
                    System.out.println("Consultation not found.");
                } else {
                    showConsultationDetails(sel);
                }
            }
        }
        InputUtil.pauseScreen();
    }

    private Consultation findConsultationById(ADTInterface<Consultation> list, String id) {
        if (id == null) return null;
        for (int i = 0; i < list.size(); i++) {
            Consultation c = list.get(i);
            if (c != null && id.equalsIgnoreCase(c.getId())) return c;
        }
        return null;
    }

    private void showConsultationDetails(Consultation c) {
    var treatments = control.getTreatmentsByConsultation(c.getId());
    var medications = control.getAllMedications();

        System.out.println("\n" + "═".repeat(60));
        System.out.println("CONSULTATION DETAILS");
        System.out.println("═".repeat(60));
        System.out.println("Consultation ID: " + nz(c.getId()));
        System.out.println("Date: " + (c.getDate()==null?"":c.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        System.out.println("Patient ID: " + nz(c.getPatientId()));
        System.out.println("Doctor: " + nz(c.getDoctorId()));
        System.out.println("Reason: " + nz(c.getReason()));
        if (c.getNotes() != null && !c.getNotes().isEmpty()) System.out.println("Notes: " + c.getNotes());
        System.out.println("Status: " + (c.getStatus()==null?"":c.getStatus().name()));

        if (treatments != null && treatments.size() > 0) {
            System.out.println("\n" + "─".repeat(60));
            System.out.println("TREATMENTS:");
            System.out.println("─".repeat(60));
            for (int i = 0; i < treatments.size(); i++) {
                entity.Treatment t = treatments.get(i);
                System.out.println("- [" + (t.getType()==null?"":t.getType().name()) + "] " + nz(t.getName()) +
                        " | Status: " + (t.getStatus()==null?"":t.getStatus().name()) +
                        (t.getCost()==null?"":" | Cost: " + String.format("%.2f", t.getCost())));
                if (t.getType() == entity.Treatment.Type.MEDICATION && t.getMedicationIds() != null) {
                    System.out.println("  Medications:");
                    for (String mid : t.getMedicationIds()) {
                        String mname = findMedicationNameById(medications, mid);
                        System.out.println("   • " + mid + " - " + mname);
                    }
                }
            }
        } else {
            System.out.println("\nNo treatments recorded for this consultation.");
        }
    }

    private String findMedicationNameById(ADTInterface<entity.Medication> meds, String id) {
        if (meds == null || id == null) return "";
        for (int i = 0; i < meds.size(); i++) {
            entity.Medication m = meds.get(i);
            if (m != null && id.equalsIgnoreCase(m.getId())) return nz(m.getName());
        }
        return "";
    }

    private void handlePayment() {
        printHeader("Make Payment (" + getPatientIdLabel() + ")");
        ADTInterface<Treatment> outstanding = control.getOutstandingTreatments();
        if (outstanding.size() == 0) {
            System.out.println("No outstanding treatments.");
            InputUtil.pauseScreen();
            return;
        }
        System.out.printf("%-8s  %-12s  %-18s  %-8s  %-10s\n", "ID", "Type", "Name", "Status", "Cost");
        for (int i = 0; i < outstanding.size(); i++) {
            Treatment t = outstanding.get(i);
            System.out.printf("%-8s  %-12s  %-18s  %-8s  %-10s\n",
                nz(t.getId()),
                t.getType() == null ? "" : t.getType().name(),
                truncate(nz(t.getName()), 18),
                t.getStatus() == null ? "" : t.getStatus().name(),
                t.getCost() == null ? "-" : String.format("%.2f", t.getCost())
            );
        }
        String id = InputUtil.getInput(sc, "Enter Treatment ID to pay: ").trim();
        if (id.isEmpty()) { InputUtil.pauseScreen(); return; }
        // UI guard: block paying for DISPENSED
        Treatment selected = null;
        for (int i = 0; i < outstanding.size(); i++) {
            Treatment t = outstanding.get(i);
            if (t != null && id.equalsIgnoreCase(nz(t.getId()))) { selected = t; break; }
        }
        if (selected != null && selected.getStatus() == Treatment.TreatmentStatus.DISPENSED) {
            System.out.println("Payment is not allowed for DISPENSED treatments.");
            InputUtil.pauseScreen();
            return;
        }
        boolean ok = control.payForTreatment(id);
        System.out.println(ok ? "Payment successful. Treatment marked completed." : "Payment failed. Check ID or status.");
    InputUtil.pauseScreen();
    }

    private void handleDispense() {
        printHeader("Dispense Medications (" + getPatientIdLabel() + ")");
        ADTInterface<Treatment> pending = control.getPendingMedicationTreatmentsForPatient();
        if (pending.size() == 0) {
            System.out.println("No medications ready for dispensing. Pay first to mark treatment COMPLETED.");
            InputUtil.pauseScreen();
            return;
        }
        System.out.printf("%-8s  %-12s  %-20s  %-10s\n", "ID", "Type", "Name", "Status");
        for (int i = 0; i < pending.size(); i++) {
            Treatment t = pending.get(i);
            System.out.printf("%-8s  %-12s  %-20s  %-10s\n",
                nz(t.getId()),
                t.getType() == null ? "" : t.getType().name(),
                truncate(nz(t.getName()), 20),
                t.getStatus() == null ? "" : t.getStatus().name());
        }
        String id = InputUtil.getInput(sc, "Enter Treatment ID to dispense: ");
        boolean ok = control.dispenseMedication(id);
        System.out.println(ok ? "Dispensed successfully." : "Dispense failed. Check ID or status.");
        InputUtil.pauseScreen();
    }

    // Change Details uses the same validations semantics as PatientMaintenanceUI update flow
    private void handleChangeDetails() {
        printHeader("Change My Details (" + getPatientIdLabel() + ")");
        Patient p = control.getCurrentPatient();
        if (p == null) {
            System.out.println("No patient record found.");
            InputUtil.pauseScreen();
            return;
        }
        // Show current
        System.out.println("Current details:");
        System.out.println("Name        : " + nz(p.getName()));
        System.out.println("Gender      : " + nz(p.getGender()));
        System.out.println("Phone       : " + nz(p.getPhoneNumber()));
        System.out.println("Email       : " + nz(p.getEmail()));
        System.out.println("Date of Birth (yyyy-MM-dd): " + nz(p.getDateOfBirth()));
        System.out.println("Nationality : " + nz(p.getNationality()));
        System.out.println("-----------------------------------------------");

        // Prompts (blank to keep)
        String name = promptOptionalNonEmpty("Name", p.getName());
        String gender = promptOptionalMF("Gender", p.getGender(), "Male", "Female");
        String phone = promptOptionalValidatedPhone("Phone Number", p.getPhoneNumber());
        String email = promptOptionalValidatedEmail("Email", p.getEmail());
        String dob = promptOptionalDate("Date of Birth (yyyy-MM-dd)", p.getDateOfBirth());
        String nationality = promptOptionalMF("Nationality (M=Malaysian,F=Foreigner)", p.getNationality(), "Malaysian", "Foreigner");

        p.setName(name);
        p.setGender(gender);
        p.setPhoneNumber(phone);
        p.setEmail(email);
        p.setDateOfBirth(dob);
        p.setNationality(nationality);

        boolean ok = control.updatePatient(p);
        System.out.println(ok ? "Details updated." : "Failed to update.");
        InputUtil.pauseScreen();
    }

    private String promptOptionalNonEmpty(String label, String current) {
        String inp = InputUtil.getInput(sc, label + " (leave blank to keep '" + nz(current) + "'): ");
        return inp.isEmpty() ? current : inp;
    }

    private String promptOptionalMF(String label, String current, String mMeaning, String fMeaning) {
        while (true) {
            String inp = InputUtil.getInput(sc, label + " (M=" + mMeaning + ", F=" + fMeaning + ", blank keep): ").trim();
            if (inp.isEmpty()) return current;
            if (inp.equalsIgnoreCase("M")) return mMeaning;
            if (inp.equalsIgnoreCase("F")) return fMeaning;
            System.out.println("Please enter only M or F.");
        }
    }

    private String promptOptionalValidatedPhone(String label, String current) {
        while (true) {
            String inp = InputUtil.getInput(sc, label + " (7-15 digits, blank keep '" + nz(current) + "'): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[0-9]{7,15}$")) return inp;
            System.out.println("Invalid phone (digits 7-15). Try again.");
        }
    }

    private String promptOptionalValidatedEmail(String label, String current) {
        while (true) {
            String inp = InputUtil.getInput(sc, label + " (blank keep '" + nz(current) + "'): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$")) return inp;
            System.out.println("Invalid email format. Try again.");
        }
    }

    private String promptOptionalDate(String label, String current) {
        while (true) {
            String inp = InputUtil.getInput(sc, label + " (blank keep '" + nz(current) + "'): ");
            if (inp.isEmpty()) return current;
            try {
                LocalDate.parse(inp, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return inp;
            } catch (Exception e) {
                System.out.println("Invalid date (yyyy-MM-dd). Try again.");
            }
        }
    }

    private String nz(Object s) { return s == null ? "" : s.toString(); }
    private String truncate(String s, int len) { return s.length() <= len ? s : s.substring(0, len-1) + "…"; }

    // Allow external caller to provide patient ID
    public void setPatientId(String patientId) {
        this.control = new userSideMaintenance(patientId);
    }
    private String getPatientIdLabel() {
        // reflect configured patient id if possible
        try {
            java.lang.reflect.Field f = userSideMaintenance.class.getDeclaredField("patientId");
            f.setAccessible(true);
            Object v = f.get(this.control);
            return v == null ? userSideMaintenance.FIXED_PATIENT_ID : v.toString();
        } catch (Exception e) {
            return userSideMaintenance.FIXED_PATIENT_ID;
        }
    }
    
    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }
}
