package control;

import adt.ADTInterface;
import adt.CustomADT;
import entity.Consultation;
import entity.Treatment;
import entity.Patient;
import entity.Doctor;
import entity.Medication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Lightweight controller for patient-side actions using fixed patient ID P0001.
 * Delegates to existing maintenance classes for business logic.
 */
public class userSideMaintenance {
    public static final String FIXED_PATIENT_ID = "P0001"; // default

    private final String patientId;

    private final ConsultationMaintenance consultationCtrl = new ConsultationMaintenance();
    private final PatientMaintenance patientCtrl = new PatientMaintenance();
    private final TreatmentMaintenance treatmentCtrl = new TreatmentMaintenance();
    private final MedicationMaintenance medicationCtrl = new MedicationMaintenance();

    public userSideMaintenance() {
        this.patientId = FIXED_PATIENT_ID;
    }

    public userSideMaintenance(String patientId) {
        this.patientId = (patientId == null || patientId.isBlank()) ? FIXED_PATIENT_ID : patientId;
    }

    // Booking
    public Consultation bookConsultation(String doctorId, LocalDateTime dateTime, String reason) {
        return consultationCtrl.book(patientId, doctorId, dateTime, reason);
    }

    // Past consultations (by date before now or status TREATED)
    public ADTInterface<Consultation> getPastConsultations() {
    ADTInterface<Consultation> allForPatient = patientCtrl.getConsultationsByPatient(patientId);
        ADTInterface<Consultation> past = new CustomADT<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < allForPatient.size(); i++) {
            Consultation c = allForPatient.get(i);
            if (c == null) continue;
            if ((c.getDate() != null && c.getDate().isBefore(now)) || c.getStatus() == Consultation.Status.TREATED) {
                past.add(c);
            }
        }
        return past;
    }

    // Treatments for this patient's consultations
    public ADTInterface<Treatment> getOutstandingTreatments() {
    ADTInterface<Consultation> cons = patientCtrl.getConsultationsByPatient(patientId);
        ADTInterface<Treatment> out = new CustomADT<>();
        for (int i = 0; i < cons.size(); i++) {
            Consultation c = cons.get(i);
            ADTInterface<Treatment> ts = patientCtrl.getTreatmentsByConsultation(c.getId());
            for (int j = 0; j < ts.size(); j++) {
                Treatment t = ts.get(j);
                if (t == null) continue;
                // Only include treatments that are payable (exclude COMPLETED and DISPENSED)
                if (t.getStatus() != Treatment.TreatmentStatus.COMPLETED
                        && t.getStatus() != Treatment.TreatmentStatus.DISPENSED) {
                    out.add(t);
                }
            }
        }
        return out;
    }


    // "Payment" = mark treatment(s) as COMPLETED
    public boolean payForTreatment(String treatmentId) {
        Treatment t = treatmentCtrl.findTreatmentById(treatmentId);
        if (t == null) return false;
        // Block payment for DISPENSED or already COMPLETED treatments
        if (t.getStatus() == Treatment.TreatmentStatus.DISPENSED
                || t.getStatus() == Treatment.TreatmentStatus.COMPLETED) {
            return false;
        }
        t.setStatus(Treatment.TreatmentStatus.COMPLETED);
        return treatmentCtrl.updateTreatment(t);
    }

    // Dispense medicines: show pending MEDICATION treatments for this patient and allow dispensing
    public ADTInterface<Treatment> getPendingMedicationTreatmentsForPatient() {
        ADTInterface<Treatment> pendingAll = medicationCtrl.getPendingMedicationTreatments();
        ADTInterface<Treatment> result = new CustomADT<>();
        // Keep only those belonging to P0001
        for (int i = 0; i < pendingAll.size(); i++) {
            Treatment t = pendingAll.get(i);
            if (t == null) continue;
            Consultation c = treatmentCtrl.findConsultationById(t.getConsultationId());
            if (c != null && patientId.equals(c.getPatientId())) result.add(t);
        }
        return result;
    }

    public boolean dispenseMedication(String treatmentId) {
        return medicationCtrl.dispenseTreatment(treatmentId);
    }

    // Patient profile helpers
    public Patient getCurrentPatient() {
        return patientCtrl.findPatientById(patientId);
    }

    public boolean updatePatient(Patient p) {
        return patientCtrl.updatePatient(p);
    }

    // Next upcoming consultation (future date, earliest)
    public LocalDateTime getNextConsultationDate() {
        ADTInterface<Consultation> cons = patientCtrl.getConsultationsByPatient(patientId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = null;
        for (int i = 0; i < cons.size(); i++) {
            Consultation c = cons.get(i);
            if (c == null || c.getDate() == null) continue;
            if (c.getDate().isAfter(now)) {
                if (next == null || c.getDate().isBefore(next)) next = c.getDate();
            }
        }
        return next;
    }

    // Detail helpers for UI
    public ADTInterface<Treatment> getTreatmentsByConsultation(String consultationId) {
        return patientCtrl.getTreatmentsByConsultation(consultationId);
    }
    public ADTInterface<Doctor> getAllDoctors() { return patientCtrl.getAllDoctors(); }
    public ADTInterface<Medication> getAllMedications() { return patientCtrl.getAllMedications(); }

    // Booked consultations only
    public ADTInterface<Consultation> getBookedConsultations() {
        ADTInterface<Consultation> allForPatient = patientCtrl.getConsultationsByPatient(patientId);
        ADTInterface<Consultation> booked = new CustomADT<>();
        for (int i = 0; i < allForPatient.size(); i++) {
            Consultation c = allForPatient.get(i);
            if (c != null && c.getStatus() == Consultation.Status.BOOKED) booked.add(c);
        }
        return booked;
    }

    // Slot availability wrappers
    public boolean isSlotAvailable(Doctor doctor, LocalDate dateOnly, int hour) {
        return consultationCtrl.isSlotAvailable(doctor, dateOnly, hour);
    }
    public Map<LocalDate, ADTInterface<Integer>> getAvailableSlots(Doctor doctor, int days, LocalDate startDate) {
        return consultationCtrl.getAvailableSlots(doctor, days, startDate);
    }
}
