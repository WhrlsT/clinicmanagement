package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.MedicationDAO;
import dao.TreatmentDAO;
import entity.Consultation;
import entity.Medication;
import entity.Treatment;

/**
 * Logic-only Medication maintenance. UI concerns live in boundary.MedicationMaintenanceUI.
 */
public class MedicationMaintenance {
    private final MedicationDAO dao = new MedicationDAO();
    private ADTInterface<Medication> list = new CustomADT<>();

    public MedicationMaintenance() {
        list = dao.load();
    }

    // Queries
    public ADTInterface<Medication> getAllMedications() {
        return list;
    }

    public Medication findById(String id) {
        if (id == null) return null;
        if (list instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Medication> l = (CustomADT<Medication>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Medication>() {
                public boolean test(Medication m) {
                    return m.getId() != null && m.getId().equals(id);
                }
            });
            return idx >= 0 ? l.get(idx) : null;
        }
        for (int i = 0; i < list.size(); i++) if (id.equals(list.get(i).getId())) return list.get(i);
        return null;
    }

    // Commands
    public Medication addMedication(Medication m) {
        if (m == null) return null;
        if (m.getId() == null || m.getId().isBlank()) m.setId(generateNextId());
        list.add(m);
        persist();
        return m;
    }

    public boolean updateMedication(Medication updated) {
        if (updated == null || updated.getId() == null) return false;
        for (int i = 0; i < list.size(); i++) {
            Medication cur = list.get(i);
            if (updated.getId().equals(cur.getId())) {
                list.set(i, updated);
                persist();
                return true;
            }
        }
        return false;
    }

    public boolean deleteMedication(String id) {
        if (id == null) return false;
        if (list instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Medication> l = (CustomADT<Medication>) cadt;
            int idx = l.findIndex(new CustomADT.ADTPredicate<Medication>() {
                public boolean test(Medication m) { return m.getId() != null && m.getId().equals(id); }
            });
            if (idx >= 0) {
                l.remove(idx);
                persist();
                return true;
            }
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                list.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    public CustomADT<Treatment> getPendingMedicationTreatments() {
        TreatmentDAO tdao = new TreatmentDAO();
        ADTInterface<Treatment> treatments = tdao.load();
        CustomADT<Treatment> pending = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            // Only allow dispensing after payment => status must be COMPLETED
            if (t != null && t.getType() == Treatment.Type.MEDICATION && t.getStatus() == Treatment.TreatmentStatus.COMPLETED) {
                pending.add(t);
            }
        }
        return pending;
    }

    public boolean dispenseTreatment(String treatmentId) {
        if (treatmentId == null || treatmentId.isBlank()) return false;
        // Load treatments
        TreatmentDAO tdao = new TreatmentDAO();
        ADTInterface<Treatment> treatments = tdao.load();
        int tIdx = -1; Treatment tr = null;
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && treatmentId.equals(t.getId())) { tIdx = i; tr = t; break; }
        }
    if (tr == null || tr.getType() != Treatment.Type.MEDICATION) return false;
    // Enforce payment first: only dispense when treatment has been paid (COMPLETED)
    if (tr.getStatus() != Treatment.TreatmentStatus.COMPLETED) return false;

        // Deduct stock (if tracked)
        if (tr.getMedicationIds() != null) {
            for (String mid : tr.getMedicationIds()) {
                if (mid == null || mid.isBlank()) continue;
                Medication m = findById(mid);
                if (m != null && m.getQuantity() != null && m.getQuantity() > 0) {
                    m.setQuantity(Math.max(0, m.getQuantity() - 1));
                    updateMedication(m);
                }
            }
        }

    // Mark treatment dispensed and save
    tr.setStatus(Treatment.TreatmentStatus.DISPENSED);
        treatments.set(tIdx, tr);
        tdao.save(treatments);

        // Update linked consultation status to TREATED
        if (tr.getConsultationId() != null && !tr.getConsultationId().isBlank()) {
            ConsultationDAO cdao = new ConsultationDAO();
            ADTInterface<Consultation> consultations = cdao.load();
            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c != null && tr.getConsultationId().equals(c.getId())) {
                    c.setStatus(Consultation.Status.TREATED);
                    consultations.set(i, c);
                    cdao.save(consultations);
                    break;
                }
            }
        }
        return true;
    }

    // Helpers
    private String generateNextId() {
        int max = 0;
        for (int i = 0; i < list.size(); i++) {
            String id = list.get(i).getId();
            if (id != null && id.startsWith("M")) {
                try { int n = Integer.parseInt(id.substring(1)); if (n > max) max = n; } catch (Exception ignored) {}
            }
        }
        return String.format("M%04d", max + 1);
    }

    private void persist() { dao.save(list); }
}
