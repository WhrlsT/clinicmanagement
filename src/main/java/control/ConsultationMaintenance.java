package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import entity.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConsultationMaintenance {
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ADTInterface<Consultation> consultations;
    private final ADTInterface<Doctor> doctors;
    private final ADTInterface<Patient> patients;

    public ConsultationMaintenance() {
        consultations = consultationDAO.load();
        doctors = doctorDAO.retrieveFromFile();
        patients = patientDAO.retrieveFromFile();
    }

    public ADTInterface<Consultation> getAllConsultations() {
    refreshConsultationsFromFile();
        return consultations;
    }
    
    public ADTInterface<Doctor> getAllDoctors() {
        return doctors;
    }

    public ADTInterface<Patient> getAllPatients() {
        return patients;
    }

    public Consultation getConsultationById(String id) {
    refreshConsultationsFromFile();
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId().equals(id)) {
                return consultations.get(i);
            }
        }
        return null;
    }
    
    public ADTInterface<Consultation> getBookedConsultations() {
    refreshConsultationsFromFile();
        ADTInterface<Consultation> booked = new CustomADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getStatus() == Consultation.Status.BOOKED) {
                booked.add(c);
            }
        }
        return booked;
    }

    public Consultation book(String patientId, String doctorId, LocalDateTime date, String reason) {
    refreshConsultationsFromFile();
        refreshDoctorsAndPatients();
        Doctor doctor = findDoctor(doctorId);
        if (doctor == null || findPatient(patientId) == null) {
            return null; // Or throw exception
        }

        if (!isSlotAvailable(doctor, date.toLocalDate(), date.getHour())) {
            return null;
        }

        String id = nextConsultationId();
        Consultation c = new Consultation(id, patientId, doctorId, date, reason, "", Consultation.Status.BOOKED);
        try {
            utility.GoogleCalendarService gcal = utility.GoogleCalendarService.getInstance();
            if (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty()) {
                String evId = gcal.addConsultationEvent(doctor, date, 60, "Consultation - " + patientId, reason);
                c.setCalendarEventId(evId);
            }
        } catch (Exception ex) {
            // Non-fatal
        }
        consultations.add(c);
        persist();
        return c;
    }

    public boolean updateConsultation(Consultation c, String newReason, String newNotes, String newStatus, LocalDateTime newDate, String newDoctorId) {
    refreshConsultationsFromFile();
        int idx = -1;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId().equals(c.getId())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        boolean changedDateTime = false;
        boolean changedDoctor = false;

        if (newReason != null && !newReason.isEmpty()) c.setReason(newReason);
        if (newNotes != null && !newNotes.isEmpty()) c.setNotes(newNotes);
        if (newStatus != null && !newStatus.isEmpty()) {
            try {
                c.setStatus(Consultation.Status.valueOf(newStatus.toUpperCase()));
            } catch (Exception ignored) {}
        }

        if (newDate != null) {
            Doctor docForCheck = (newDoctorId != null) ? findDoctor(newDoctorId) : findDoctor(c.getDoctorId());
            if (docForCheck != null && !isSlotAvailable(docForCheck, newDate.toLocalDate(), newDate.getHour())) {
                return false; // Slot not available
            }
            c.setDate(newDate);
            changedDateTime = true;
        }

        if (newDoctorId != null && !newDoctorId.equals(c.getDoctorId())) {
            Doctor newDoc = findDoctor(newDoctorId);
            if (newDoc == null) return false;
            if (c.getDate() != null && !isSlotAvailable(newDoc, c.getDate().toLocalDate(), c.getDate().getHour())) {
                return false;
            }
            // Calendar updates
            try {
                if (c.getCalendarEventId() != null) {
                    Doctor oldDoc = findDoctor(c.getDoctorId());
                    if (oldDoc != null) utility.GoogleCalendarService.getInstance().removeConsultationEvent(oldDoc, c.getCalendarEventId());
                    c.setCalendarEventId(null);
                }
                if (newDoc.getCalendarId() != null && !newDoc.getCalendarId().isEmpty() && c.getDate() != null) {
                    String evId = utility.GoogleCalendarService.getInstance().addConsultationEvent(newDoc, c.getDate(), 60, "Consultation - " + c.getPatientId(), c.getReason());
                    c.setCalendarEventId(evId);
                }
            } catch (Exception ignored) {}
            c.setDoctorId(newDoctorId);
            changedDoctor = true;
        }

        if (changedDateTime && !changedDoctor && c.getCalendarEventId() != null) {
            try {
                Doctor d = findDoctor(c.getDoctorId());
                if (d != null) utility.GoogleCalendarService.getInstance().updateConsultationEvent(d, c.getCalendarEventId(), c.getDate(), 60);
            } catch (Exception ignored) {}
        }

        consultations.set(idx, c);
        persist();
        return true;
    }

    public ADTInterface<Consultation> searchConsultations(String q) {
    refreshConsultationsFromFile();
        ADTInterface<Consultation> results = new CustomADT<>();
        if (q == null || q.isEmpty()) return results;
        String lowerQ = q.toLowerCase();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            String patient = getPatientDisplay(c.getPatientId());
            String doctor = getDoctorDisplay(c.getDoctorId());
            String dt = c.getDate() == null ? "" : c.getDate().format(fmt);
            String status = c.getStatus() == null ? "" : c.getStatus().toString();
            String reason = c.getReason() == null ? "" : c.getReason();
            String hay = String.join(" ",
                safe(c.getId()), safe(c.getPatientId()), patient,
                safe(c.getDoctorId()), doctor,
                dt, status, reason, safe(c.getFollowUpOfId())).toLowerCase();
            if (hay.contains(lowerQ)) {
                results.add(c);
            }
        }
        return results;
    }

    private String safe(String s) { return s == null ? "" : s; }

    public boolean cancel(String id) {
    refreshConsultationsFromFile();
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getId().equals(id)) {
                if (c.getStatus() != Consultation.Status.BOOKED) return false;
                try {
                    Doctor d = findDoctor(c.getDoctorId());
                    if (d != null && c.getCalendarEventId() != null) {
                        utility.GoogleCalendarService.getInstance().removeConsultationEvent(d, c.getCalendarEventId());
                    }
                } catch (Exception ignored) {}
                consultations.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    public boolean reschedule(String id, LocalDateTime newDate) {
    refreshConsultationsFromFile();
        Consultation c = getConsultationById(id);
        if (c == null || c.getStatus() != Consultation.Status.BOOKED) return false;
        
        Doctor d = findDoctor(c.getDoctorId());
        if (d != null && !isSlotAvailable(d, newDate.toLocalDate(), newDate.getHour())) {
            return false; // Slot unavailable
        }

        try {
            if (d != null && c.getCalendarEventId() != null) {
                utility.GoogleCalendarService.getInstance().updateConsultationEvent(d, c.getCalendarEventId(), newDate, 60);
            }
        } catch (Exception ignored) {}
        c.setDate(newDate);
        persist();
        return true;
    }

    public Consultation addFollowUp(String baseId, LocalDateTime dt, String reason) {
    refreshConsultationsFromFile();
        Consultation base = getConsultationById(baseId);
        if (base == null) return null;

        Doctor doctor = findDoctor(base.getDoctorId());
        if (doctor == null || !isSlotAvailable(doctor, dt.toLocalDate(), dt.getHour())) {
            return null;
        }

        String finalReason = (reason == null || reason.isEmpty()) ? "Follow-up for " + base.getId() : reason;
        String id = nextConsultationId();
        Consultation c = new Consultation(id, base.getPatientId(), base.getDoctorId(), dt, finalReason, "", Consultation.Status.BOOKED);
        c.setFollowUpOfId(base.getId());
        try {
            utility.GoogleCalendarService gcal = utility.GoogleCalendarService.getInstance();
            if (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty()) {
                String evId = gcal.addConsultationEvent(doctor, dt, 60, "Follow-up - " + getPatientDisplay(c.getPatientId()), finalReason);
                c.setCalendarEventId(evId);
            }
        } catch (Exception ignored) {}
        consultations.add(c);
        persist();
        return c;
    }

    public Doctor findDoctor(String id) {
        if (id == null) return null;
        for (int i = 0; i < doctors.size(); i++) {
            if (id.equals(doctors.get(i).getId())) return doctors.get(i);
        }
        return null;
    }

    public Patient findPatient(String id) {
        if (id == null) return null;
        for (int i = 0; i < patients.size(); i++) {
            if (id.equals(patients.get(i).getId())) return patients.get(i);
        }
        return null;
    }

    private String nextConsultationId() {
        int max = 0;
        for (int i = 0; i < consultations.size(); i++) {
            String id = consultations.get(i).getId();
            if (id != null && id.startsWith("C")) {
                try {
                    int n = Integer.parseInt(id.substring(1));
                    if (n > max) max = n;
                } catch (Exception ignored) {}
            }
        }
        return String.format("C%04d", max + 1);
    }

    private void persist() {
        consultationDAO.save(consultations);
    }
    
    private void refreshConsultationsFromFile() {
        ADTInterface<Consultation> fresh = consultationDAO.load();
        consultations.clear();
        for (int i = 0; i < fresh.size(); i++) {
            consultations.add(fresh.get(i));
        }
    }
    
    private void refreshDoctorsAndPatients() {
        doctors.clear();
        patients.clear();
        ADTInterface<Doctor> freshDocs = doctorDAO.retrieveFromFile();
        ADTInterface<Patient> freshPatients = patientDAO.retrieveFromFile();
        for(int i=0; i<freshDocs.size(); i++) doctors.add(freshDocs.get(i));
        for(int i=0; i<freshPatients.size(); i++) patients.add(freshPatients.get(i));
    }

    public Map<LocalDate, ADTInterface<Integer>> getAvailableSlots(Doctor doctor, int days, LocalDate startDate) {
    refreshConsultationsFromFile();
        Map<LocalDate, ADTInterface<Integer>> availableSlots = new LinkedHashMap<>();
        if (doctor == null) return availableSlots;

        utility.GoogleCalendarService gcal = null;
        boolean useCalendar = false;
        try {
            gcal = utility.GoogleCalendarService.getInstance();
            useCalendar = (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty());
        } catch (Exception ex) {
            useCalendar = false;
        }

        for (int d = 0; d < days; d++) {
            LocalDate date = startDate.plusDays(d);
            ADTInterface<Integer> hours = new CustomADT<>();
            for (int h = 0; h < 24; h++) {
                if (isSlotAvailable(doctor, date, h, useCalendar, gcal)) {
                    hours.add(h);
                }
            }
            if (!hours.isEmpty()) {
                availableSlots.put(date, hours);
            }
        }
        return availableSlots;
    }

    public boolean isSlotAvailable(Doctor doctor, LocalDate date, int hour) {
    refreshConsultationsFromFile();
        utility.GoogleCalendarService gcal = null;
        boolean useCalendar = false;
        try {
            gcal = utility.GoogleCalendarService.getInstance();
            useCalendar = (doctor.getCalendarId() != null && !doctor.getCalendarId().isEmpty());
        } catch (Exception ex) {
            useCalendar = false;
        }
        return isSlotAvailable(doctor, date, hour, useCalendar, gcal);
    }

    private boolean isSlotAvailable(Doctor doctor, LocalDate date, int hour, boolean useCalendar, utility.GoogleCalendarService gcal) {
        if (doctor == null || date == null) return false;
        int dayIdx = date.getDayOfWeek().getValue() - 1; // 0 = Monday

        if (doctor.getSchedule() == null || !doctor.getSchedule().isAvailable(dayIdx, hour)) {
            return false;
        }

        if (useCalendar) {
            try {
                if (!gcal.isDutyHour(doctor, date, hour)) {
                    return false;
                }
            } catch (Exception ignored) {
                // Fallback to local if gcal fails
            }
        }

        LocalDateTime slotDateTime = date.atTime(hour, 0);
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getDoctorId().equals(doctor.getId()) && c.getDate() != null && c.getDate().equals(slotDateTime)) {
                return false;
            }
        }
        return true;
    }
    
    public String getDoctorDisplay(String doctorId) {
        Doctor d = findDoctor(doctorId);
        return (d == null) ? doctorId : String.format("%s (%s)", d.getName(), d.getId());
    }

    public String getPatientDisplay(String patientId) {
        Patient p = findPatient(patientId);
        return (p == null) ? patientId : String.format("%s (%s)", p.getName(), p.getId());
    }
}
