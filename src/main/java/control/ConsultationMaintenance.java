package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import entity.*;
import utility.InputUtil;
import boundary.DoctorMaintenanceUI;
import boundary.PatientMaintenanceUI;
import boundary.ConsultationMaintenanceUI;

import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ConsultationMaintenance {
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ADTInterface<Consultation> consultations;
    private final ADTInterface<Doctor> doctors;
    private final ADTInterface<Patient> patients;
    // Rooms removed

    private final Scanner scanner = new Scanner(System.in);
    private final ConsultationMaintenanceUI consultUI = new ConsultationMaintenanceUI();

    public ConsultationMaintenance() {
        consultations = consultationDAO.load();
        doctors = doctorDAO.retrieveFromFile();
        patients = patientDAO.retrieveFromFile();
    }

    public void run() {
        int choice;
        do {
            InputUtil.clearScreen();
            consultUI.printMainHeader();
            list();
            choice = consultUI.getMenuChoice();
            switch (choice) {
                case 1 -> { InputUtil.clearScreen(); book(); consultUI.pause(); }
                case 2 -> { InputUtil.clearScreen(); updateConsultation(); consultUI.pause(); }
                case 3 -> { InputUtil.clearScreen(); cancel(); consultUI.pause(); }
                case 4 -> { InputUtil.clearScreen(); reschedule(); consultUI.pause(); }
                case 5 -> { InputUtil.clearScreen(); searchConsultations(); consultUI.pause(); }
                case 6 -> { InputUtil.clearScreen(); addFollowUp(); consultUI.pause(); }
                case 7 -> {}
                default -> { consultUI.printInvalidSelection(); consultUI.pause(); }
            }
        } while (choice != 7);
    }

    private void book() {
    // Always refresh in case doctors/patients changed since this instance was created
    refreshDoctorsAndPatients();

    // Header similar to Queue Enqueue
    consultUI.showBookHeader();

    // 1. Show doctor options
    // Use DoctorMaintenance UI table for consistency
    DoctorMaintenance doctorMaintenance = new DoctorMaintenance();
    DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
    doctorUI.displayDoctorsTable(doctorMaintenance.getAllDoctors());
    consultUI.printBackHint();
    String doctorId = InputUtil.getInput(scanner, "Doctor ID: ");
    if (doctorId.equals("0")) return;
        Doctor doctor = findDoctor(doctorId);
    if (doctor==null){consultUI.displayDoctorNotFound();return;}

        // 2. Show patient options
        // Use PatientMaintenance UI table for consistency
        PatientMaintenance patientMaintenance = new PatientMaintenance();
        PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
        patientUI.displayPatientsTable(patientMaintenance.getAllPatients());
    consultUI.printBackHint();
    String patientId = InputUtil.getInput(scanner, "Patient ID: ");
    if (patientId.equals("0")) return;
        Patient patient = findPatient(patientId);
    if (patient==null){consultUI.displayPatientNotFound();return;}

    // 3. Pick a date and time
    // Show available dates and time slots (next 14 days) for the chosen doctor
    System.out.println();
    showAvailableDatesAndTimeslots(doctor, 14);

    java.time.LocalDate dateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to go back): "); if (dateOnly==null) return;
    Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to go back): "); if (hour==null) return;
    java.time.LocalDateTime date = dateOnly.atTime(hour, 0);

        // Validate selected slot using schedule, existing consultations and Google Calendar (if configured)
    if (!isSlotAvailable(doctor, dateOnly, hour)) { consultUI.displaySlotUnavailable(); return; }

    // 4. Reason & booking
    String reason = InputUtil.getInput(scanner, "Reason (enter '0' to go back): ");
    if (reason.equals("0")) return;
        String id = nextConsultationId();
    Consultation c = new Consultation(id, patientId, doctorId, date, reason, "", Consultation.Status.BOOKED);
    // Create corresponding Google Calendar event if doctor has calendar configured
    try {
        utility.GoogleCalendarService gcal = utility.GoogleCalendarService.getInstance();
        Doctor doc = findDoctor(doctorId);
        if (doc != null && doc.getCalendarId() != null && !doc.getCalendarId().isEmpty()) {
            String evId = gcal.addConsultationEvent(doc, date, 60, "Consultation - " + patientId, reason);
            c.setCalendarEventId(evId);
        }
    } catch (Exception ex) {
        // Non-fatal: proceed without calendar event
    }
        consultations.add(c);
        persist();
    consultUI.displayBooked(id);
    }

    private void updateConsultation() {
        // Show only BOOKED consultations
        if (!listBooked()) { consultUI.displayNoBookedConsultations(); return; }
    String id = InputUtil.getInput(scanner, "Consultation ID to update ('0' to go back): ");
    if (id.equals("0")) return;
        Consultation c = null; int idx=-1;
        for (int i=0;i<consultations.size();i++){ if (consultations.get(i).getId().equals(id)){ c=consultations.get(i); idx=i; break; } }
    if (c==null){ consultUI.displayNotFound(); return; }
        if (c.getStatus() != Consultation.Status.BOOKED) { consultUI.displayOnlyBookedAllowed(); return; }

        // Show current details
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        consultUI.displayCurrentConsultationDetails(
            (c.getDate()==null?"":c.getDate().format(fmt)),
            getDoctorDisplay(c.getDoctorId()),
            getPatientDisplay(c.getPatientId()),
            String.valueOf(c.getStatus()),
            (c.getReason()==null?"":c.getReason()),
            (c.getNotes()==null?"":c.getNotes())
        );

        // Update basic fields (blank = keep)
        String newReason = InputUtil.getInput(scanner, "New Reason (blank=keep): ");
        if (!newReason.isEmpty()) c.setReason(newReason);
        String newNotes = InputUtil.getInput(scanner, "New Notes (blank=keep): ");
        if (!newNotes.isEmpty()) c.setNotes(newNotes);

        // Update status (optional)
        String st = InputUtil.getInput(scanner, "New Status [BOOKED/ONGOING/TREATED] (blank=keep): ").toUpperCase();
        if (!st.isEmpty()) {
            try { c.setStatus(Consultation.Status.valueOf(st)); } catch(Exception e){ consultUI.displayInvalidStatusKeepOld(); }
        }

        // Change date/time?
    boolean changedDateTime = false;
        String changeDt = InputUtil.getInput(scanner, "Change Date/Time? (Y/N): ").trim().toUpperCase();
        if (changeDt.equals("Y")) {
            java.time.LocalDate nd = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to cancel): "); if (nd==null) { consultUI.displayDateChangeCancelled(); }
            else {
                Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to cancel): "); if (hour==null) { consultUI.displayTimeChangeCancelled(); }
                else {
                // Validate slot availability for current doctor if not UNASSIGNED
                if (!"UNASSIGNED".equals(c.getDoctorId()) && !isSlotAvailable(findDoctor(c.getDoctorId()), nd, hour)) {
                    consultUI.displayDoctorNotAvailable();
                } else {
                    c.setDate(nd.atTime(hour,0));
                    changedDateTime = true;
                }
                }
            }
        }

        // Change doctor?
        boolean changedDoctor = false;
        String changeDoc = InputUtil.getInput(scanner, "Change Doctor? (Y/N): ").trim().toUpperCase();
        if (changeDoc.equals("Y")) {
            // Show doctor table from DoctorMaintenance for consistency
            DoctorMaintenance dm2 = new DoctorMaintenance();
            DoctorMaintenanceUI dui2 = new DoctorMaintenanceUI();
        dui2.displayDoctorsTable(dm2.getAllDoctors());
        consultUI.printBackHint();
        String newDocId = InputUtil.getInput(scanner, "New Doctor ID: ");
        if (newDocId.equals("0")) { /* no-op */ }
            Doctor newDoc = findDoctor(newDocId);
            if (newDoc == null) { consultUI.displayDoctorNotFound(); }
            else {
                // Ensure date/time set; if missing, prompt
                if (c.getDate() == null) {
            java.time.LocalDate nd = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to cancel): "); if (nd==null){ consultUI.displayDoctorChangeCancelled(); }
            else { Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to cancel): "); if (hour==null){ consultUI.displayDoctorChangeCancelled(); } else { c.setDate(nd.atTime(hour,0)); changedDateTime = true; } }
                }
                if (c.getDate()!=null) {
                    if (!isSlotAvailable(newDoc, c.getDate().toLocalDate(), c.getDate().getHour())) {
                        consultUI.displayNewDoctorNotAvailable();
                    } else {
                        // Calendar: if event existed, remove old, create new
                        try {
                            if (c.getCalendarEventId()!=null) {
                                Doctor oldDoc = findDoctor(c.getDoctorId());
                                if (oldDoc!=null) utility.GoogleCalendarService.getInstance().removeConsultationEvent(oldDoc, c.getCalendarEventId());
                                c.setCalendarEventId(null);
                            }
                            // create new event for new doctor if has calendar
                            if (newDoc.getCalendarId()!=null && !newDoc.getCalendarId().isEmpty()) {
                                String evId = utility.GoogleCalendarService.getInstance().addConsultationEvent(newDoc, c.getDate(), 60, "Consultation - "+c.getPatientId(), c.getReason());
                                c.setCalendarEventId(evId);
                            }
                        } catch(Exception ignored){}
                        c.setDoctorId(newDocId);
                        changedDoctor = true;
                    }
                }
            }
        }

        // Calendar update for date/time change with same doctor
        if (changedDateTime && !changedDoctor && c.getCalendarEventId()!=null) {
            try {
                Doctor d = findDoctor(c.getDoctorId());
                if (d!=null) utility.GoogleCalendarService.getInstance().updateConsultationEvent(d, c.getCalendarEventId(), c.getDate(), 60);
            } catch(Exception ignored){}
        }

        consultations.set(idx, c);
        persist();
    consultUI.displayUpdated();
    }

    private void searchConsultations() {
    String q = InputUtil.getInput(scanner, "Search text (ID/Patient/Doctor/Reason/Status/Date, '0' to go back): ").toLowerCase();
    if (q.equals("0")) { return; }
    if (q.isEmpty()) { consultUI.displayEmptyQuery(); return; }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    consultUI.displaySearchHeader();
    boolean any=false;
    StringBuilder rows = new StringBuilder();
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            String patient = getPatientDisplay(c.getPatientId());
            String doctor = getDoctorDisplay(c.getDoctorId());
            String dt = c.getDate()==null? "" : c.getDate().format(fmt);
            String status = c.getStatus()==null? "" : c.getStatus().toString();
            String reason = c.getReason()==null? "" : c.getReason();
        String hay = String.join(" ",
            safe(c.getId()), safe(c.getPatientId()), patient,
            safe(c.getDoctorId()), doctor,
            dt, status, reason, safe(c.getFollowUpOfId())).toLowerCase();
            if (hay.contains(q)) {
                any=true;
        rows.append(String.format("%-10s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
            c.getId(), dt, patient, doctor, truncate(reason,20), status, safe(c.getFollowUpOfId())));
            }
        }
    if (!any) { consultUI.displayNoMatches(); }
    else { consultUI.displayConsultationsTable(rows.toString(), true); }
    }

    private String safe(String s){ return s==null?"":s; }

    private void list() {
        consultUI.displayConsultationsHeader();
        if (consultations.size()==0){ consultUI.displayNoConsultations(); return;}
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder rows = new StringBuilder();
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            String dt = c.getDate()==null? "" : c.getDate().format(fmt);
            String patient = getPatientDisplay(c.getPatientId());
            String doctor = getDoctorDisplay(c.getDoctorId());
            String reason = c.getReason()==null? "" : c.getReason();
            String status = c.getStatus()==null? "" : c.getStatus().toString();
            String followOf = (c.getFollowUpOfId()==null? "" : c.getFollowUpOfId());
            rows.append(String.format("%-10s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
                c.getId(), dt, patient, doctor, truncate(reason,20), status, followOf));
        }
        consultUI.displayConsultationsTable(rows.toString(), true);
    }

    // List only consultations with status BOOKED; returns false if none
    private boolean listBooked() {
        consultUI.displayBookedConsultationsHeader();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder rows = new StringBuilder();
        int count = 0;
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            if (c.getStatus() == Consultation.Status.BOOKED) {
                count++;
                String dt = c.getDate()==null? "" : c.getDate().format(fmt);
                String patient = getPatientDisplay(c.getPatientId());
                String doctor = getDoctorDisplay(c.getDoctorId());
                String reason = c.getReason()==null? "" : c.getReason();
                String status = c.getStatus().toString();
                String followOf = (c.getFollowUpOfId()==null? "" : c.getFollowUpOfId());
                rows.append(String.format("%-10s | %-16s | %-20s | %-20s | %-20s | %-10s | %-10s%n",
                    c.getId(), dt, patient, doctor, truncate(reason,20), status, followOf));
            }
        }
        if (count == 0) return false;
        consultUI.displayConsultationsTable(rows.toString(), true);
        return true;
    }

    private void cancel() {
    // Show only BOOKED consultations
    if (!listBooked()) { consultUI.displayNoBookedConsultations(); return; }
    String id = InputUtil.getInput(scanner, "Consultation ID ('0' to go back): ");
    if (id.equals("0")) return;
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
        if (c.getStatus() != Consultation.Status.BOOKED) continue; // enforce booked only
            if (c.getId().equals(id)) {
                // Remove calendar event if present
                try {
                    Doctor d = findDoctor(c.getDoctorId());
                    if (d != null && c.getCalendarEventId()!=null) {
                        utility.GoogleCalendarService.getInstance().removeConsultationEvent(d, c.getCalendarEventId());
                    }
                } catch (Exception ignored) {}
                // No room schedule to update
                consultations.remove(i);
                persist();
        consultUI.displayCancelled();
                return;
            }
        }
    consultUI.displayNotFound();
    }

    private void reschedule() {
        // Show only BOOKED consultations
        if (!listBooked()) { consultUI.displayNoBookedConsultations(); return; }
    String id = InputUtil.getInput(scanner, "Consultation ID ('0' to go back): ");
    if (id.equals("0")) return;
    Consultation c = null;
    for (int i=0;i<consultations.size();i++) if (consultations.get(i).getId().equals(id)){c=consultations.get(i);break;}
    if (c==null){consultUI.displayNotFound();return;}
        if (c.getStatus() != Consultation.Status.BOOKED) { consultUI.displayOnlyBookedAllowed(); return; }
    // Reschedule date and hour
    java.time.LocalDate newDateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to go back): "); if (newDateOnly==null) return;
    Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to go back): "); if (hour==null) return;
    java.time.LocalDateTime newDate = newDateOnly.atTime(hour,0);
    // Update calendar event if present
    try {
        Doctor d = findDoctor(c.getDoctorId());
        if (d != null && c.getCalendarEventId()!=null) {
            utility.GoogleCalendarService.getInstance().updateConsultationEvent(d, c.getCalendarEventId(), newDate, 60);
        }
    } catch (Exception ignored) {}
    c.setDate(newDate);
        persist();
    consultUI.displayRescheduled();
    }

    // Create a follow-up consultation linked to an existing one
    private void addFollowUp() {
    if (consultations.size()==0){ consultUI.displayNoConsultations(); return; }
        list();
    String baseId = InputUtil.getInput(scanner, "Base Consultation ID for follow-up ('0' to go back): ");
    if (baseId.equals("0")) return;
        Consultation base = null;
        for (int i=0;i<consultations.size();i++) if (consultations.get(i).getId().equals(baseId)) { base = consultations.get(i); break; }
    if (base == null) { consultUI.displayNotFound(); return; }

        Doctor doctor = findDoctor(base.getDoctorId());
        Patient patient = findPatient(base.getPatientId());
    if (doctor == null || patient == null) { consultUI.displayNotFound(); return; }

    consultUI.displayCreatingFollowUpFor(base.getId(), getPatientDisplay(base.getPatientId()), getDoctorDisplay(base.getDoctorId()));
        // Suggest starting from one week after the base date
        java.time.LocalDate startDate = (base.getDate()==null? java.time.LocalDate.now() : base.getDate().toLocalDate().plusWeeks(1));
        System.out.println();
    showAvailableDatesAndTimeslotsFrom(doctor, 21, startDate);

    java.time.LocalDate dateOnly = InputUtil.getValidatedLocalDateWithBackOption(scanner, "Date (YYYY-MM-DD, '0' to go back): "); if (dateOnly==null) return;
    Integer hour = InputUtil.getValidatedHourWithBackOption(scanner, "Hour (0-23, '0' to go back): "); if (hour==null) return;
    if (!isSlotAvailable(doctor, dateOnly, hour)) { consultUI.displaySlotUnavailable(); return; }
    String reason = InputUtil.getInput(scanner, "Reason (blank=Follow-up, '0' to go back): ");
    if (reason.equals("0")) return;
        if (reason.isEmpty()) reason = "Follow-up for " + base.getId();

        String id = nextConsultationId();
        java.time.LocalDateTime dt = dateOnly.atTime(hour,0);
        Consultation c = new Consultation(id, base.getPatientId(), base.getDoctorId(), dt, reason, "", Consultation.Status.BOOKED);
        c.setFollowUpOfId(base.getId());
        try {
            utility.GoogleCalendarService gcal = utility.GoogleCalendarService.getInstance();
            if (doctor.getCalendarId()!=null && !doctor.getCalendarId().isEmpty()) {
                String evId = gcal.addConsultationEvent(doctor, dt, 60, "Follow-up - " + patient.getName(), reason);
                c.setCalendarEventId(evId);
            }
        } catch(Exception ignored){}
        consultations.add(c);
        persist();
    consultUI.displayFollowUpBooked(id, base.getId());
    }


    private Doctor findDoctor(String id){
        if (doctors instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Doctor> l=(CustomADT<Doctor>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Doctor>(){ public boolean test(Doctor d){ return d.getId()!=null && d.getId().equals(id);} });
            return idx>=0? l.get(idx): null;
        }
        for (int i=0;i<doctors.size();i++) if (doctors.get(i).getId().equals(id)) return doctors.get(i); return null;
    }
    private Patient findPatient(String id){
        if (patients instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Patient> l=(CustomADT<Patient>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Patient>(){ public boolean test(Patient p){ return p.getId()!=null && p.getId().equals(id);} });
            return idx>=0? l.get(idx): null;
        }
        for (int i=0;i<patients.size();i++) if (patients.get(i).getId().equals(id)) return patients.get(i); return null;
    }

    private String nextConsultationId() {
        int max=0; for (int i=0;i<consultations.size();i++){String id=consultations.get(i).getId(); if (id!=null&&id.startsWith("C")) {try{int n=Integer.parseInt(id.substring(1)); if(n>max)max=n;}catch(Exception ignored){}}}
        return String.format("C%04d", max+1);
    }

    // Input helpers moved to InputUtil

    private void persist() {
        consultationDAO.save(consultations);
        doctorDAO.saveToFile(doctors);
        patientDAO.saveToFile(patients);
    }

    // ---- Helper presentation methods for improved booking flow ----

    // availability helpers removed due to date-only consultations

    /**
     * Print available dates and timeslots for the provided doctor for the next `days` days.
     * Only dates/hours where the doctor's weekly schedule marks AVAILABLE and there's no
     * existing consultation for that doctor on that date are printed.
     */
    private void showAvailableDatesAndTimeslots(Doctor doctor, int days) {
        if (doctor == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean any = false;
        StringBuilder body = new StringBuilder();
        utility.GoogleCalendarService gcal = null;
        boolean useCalendar = false;
        try {
            gcal = utility.GoogleCalendarService.getInstance();
            useCalendar = (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty());
        } catch (Exception ex) {
            // If calendar not configured/available, silently ignore and fall back to local schedule
            useCalendar = false;
        }
        for (int d = 0; d < days; d++) {
            java.time.LocalDate date = today.plusDays(d);
            int dayIdx = date.getDayOfWeek().getValue() - 1; // 0 = Monday

            // If doctor has no availability on that weekday, skip
            boolean dayHasAvail = false;
            StringBuilder hours = new StringBuilder();
            for (int h = 0; h < 24; h++) {
                if (doctor.getSchedule() != null && doctor.getSchedule().isAvailable(dayIdx, h)) {
                    // If Google Calendar is configured for duty hours, require a duty event at this hour
                    if (useCalendar) {
                        try {
                            if (!gcal.isDutyHour(doctor, date, h)) {
                                continue; // not on duty this hour per Google Calendar
                            }
                        } catch (Exception ignored) {
                            // If call fails, fall back to local schedule for this iteration
                        }
                    }
                    // Ensure no consultation already exists for this doctor at this date+hour
                    boolean alreadyBooked = false;
                    java.time.LocalDateTime slotDateTime = date.atTime(h, 0);
                    for (int i = 0; i < consultations.size(); i++) {
                        Consultation c = consultations.get(i);
                        if (c.getDoctorId().equals(doctor.getId()) && c.getDate().equals(slotDateTime)) {
                            alreadyBooked = true;
                            break;
                        }
                    }
                    if (!alreadyBooked) { dayHasAvail = true; hours.append(String.format("%02d:00 ", h)); }
                }
            }

            if (dayHasAvail) {
                any = true;
                body.append(String.format("%s : %s%n", date, hours.toString().trim()));
            }
        }
        String range = "next " + days + " days";
        consultUI.displayAvailableSlots(doctor.getId(), range, any ? body.toString() : "");
    }

    // Print available slots starting from a specific date for a given range
    private void showAvailableDatesAndTimeslotsFrom(Doctor doctor, int days, java.time.LocalDate startDate) {
        if (doctor == null) return;
        java.time.LocalDate today = startDate;
        boolean any = false;
        StringBuilder body = new StringBuilder();
        utility.GoogleCalendarService gcal = null;
        boolean useCalendar = false;
        try {
            gcal = utility.GoogleCalendarService.getInstance();
            useCalendar = (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty());
        } catch (Exception ex) {
            // Fall back to local schedule
            useCalendar = false;
        }
        for (int d = 0; d < days; d++) {
            java.time.LocalDate date = today.plusDays(d);
            int dayIdx = date.getDayOfWeek().getValue() - 1; // 0 = Monday
            boolean dayHasAvail = false;
            StringBuilder hours = new StringBuilder();
            for (int h = 0; h < 24; h++) {
                if (doctor.getSchedule() != null && doctor.getSchedule().isAvailable(dayIdx, h)) {
                    if (useCalendar) {
                        try { if (!gcal.isDutyHour(doctor, date, h)) { continue; } } catch (Exception ignored) {}
                    }
                    boolean alreadyBooked = false;
                    java.time.LocalDateTime slotDateTime = date.atTime(h, 0);
                    for (int i = 0; i < consultations.size(); i++) {
                        Consultation c = consultations.get(i);
                        if (c.getDoctorId().equals(doctor.getId()) && c.getDate().equals(slotDateTime)) {
                            alreadyBooked = true; break;
                        }
                    }
                    if (!alreadyBooked) { dayHasAvail = true; hours.append(String.format("%02d:00 ", h)); }
                }
            }
            if (dayHasAvail) { any = true; body.append(String.format("%s : %s%n", date, hours.toString().trim())); }
        }
        String range = "from " + startDate + " next " + days + " days";
        consultUI.displayAvailableSlots(doctor.getId(), range, any ? body.toString() : "");
    }

    // Validate a slot is available per weekly schedule, Google Calendar duty (if configured), and not already booked
    private boolean isSlotAvailable(Doctor doctor, java.time.LocalDate date, int hour) {
        if (doctor == null || doctor.getSchedule() == null) return false;
        int dayIdx = date.getDayOfWeek().getValue() - 1;
        if (!doctor.getSchedule().isAvailable(dayIdx, hour)) return false;
        // If calendar configured, require duty hour
        if (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty()) {
            try {
                utility.GoogleCalendarService gcal = utility.GoogleCalendarService.getInstance();
                if (!gcal.isDutyHour(doctor, date, hour)) return false;
            } catch (Exception ignored) {
                // If calendar fails, do not hard fail the booking; fall back to local schedule
            }
        }
        java.time.LocalDateTime dt = date.atTime(hour, 0);
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (doctor.getId().equals(c.getDoctorId()) && dt.equals(c.getDate())) return false;
        }
        return true;
    }

    // Reload latest doctor & patient lists from file into existing ADT instances
    private void refreshDoctorsAndPatients() {
        ADTInterface<Doctor> latestDoctors = doctorDAO.retrieveFromFile();
        doctors.clear();
        for (int i=0;i<latestDoctors.size();i++) doctors.add(latestDoctors.get(i));
        ADTInterface<Patient> latestPatients = patientDAO.retrieveFromFile();
        patients.clear();
        for (int i=0;i<latestPatients.size();i++) patients.add(latestPatients.get(i));
    }

    // --- Display helpers ---
    private String getDoctorDisplay(String doctorId) {
        Doctor d = findDoctor(doctorId);
        if (d == null) return doctorId==null?"":doctorId;
        String name = d.getName()==null?"":d.getName();
        return (name.isEmpty()? d.getId() : name+" ("+d.getId()+")");
    }

    private String getPatientDisplay(String patientId) {
        Patient p = findPatient(patientId);
        if (p == null) return patientId==null?"":patientId;
        String name = p.getName()==null?"":p.getName();
        return (name.isEmpty()? p.getId() : name+" ("+p.getId()+")");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max-1)) + "…";
    }
}
