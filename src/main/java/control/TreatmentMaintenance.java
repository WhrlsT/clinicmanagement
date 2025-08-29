package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.MedicationDAO;
import dao.PatientDAO;
import dao.TreatmentDAO;
import entity.Consultation;
import entity.Medication;
import entity.Patient;
import entity.Treatment;
import java.time.LocalDate;

/**
 * Logic-only Treatment maintenance. UI lives in boundary.TreatmentMaintenanceUI.
 */
public class TreatmentMaintenance {
    // Report: Treatments per Doctor
    public String getTreatmentsPerDoctorReport() {
        CustomADT<String> doctorList = new CustomADT<>();
        CustomADT<Integer> doctorCountList = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            Consultation c = findConsultationById(t.getConsultationId());
            if (c != null && c.getDoctorId() != null) {
                String docId = c.getDoctorId();
                int idx = -1;
                for (int j = 0; j < doctorList.size(); j++) {
                    if (doctorList.get(j).equals(docId)) { idx = j; break; }
                }
                if (idx == -1) { doctorList.add(docId); doctorCountList.add(1); }
                else doctorCountList.set(idx, doctorCountList.get(idx) + 1);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Treatments per Doctor:\n");
        for (int i = 0; i < doctorList.size(); i++) {
            sb.append("  Doctor ").append(doctorList.get(i)).append(": ").append(doctorCountList.get(i)).append("\n");
        }
        return sb.toString();
    }

    // Report: Treatments per Patient
    public String getTreatmentsPerPatientReport() {
        CustomADT<String> patientList = new CustomADT<>();
        CustomADT<Integer> patientCountList = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            Consultation c = findConsultationById(t.getConsultationId());
            if (c != null && c.getPatientId() != null) {
                String patId = c.getPatientId();
                int idx = -1;
                for (int j = 0; j < patientList.size(); j++) {
                    if (patientList.get(j).equals(patId)) { idx = j; break; }
                }
                if (idx == -1) { patientList.add(patId); patientCountList.add(1); }
                else patientCountList.set(idx, patientCountList.get(idx) + 1);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Treatments per Patient:\n");
        for (int i = 0; i < patientList.size(); i++) {
            sb.append("  Patient ").append(patientList.get(i)).append(": ").append(patientCountList.get(i)).append("\n");
        }
        return sb.toString();
    }

    // Report: Cost per Type
    public String getCostPerTypeReport() {
        CustomADT<Treatment.Type> typeList = new CustomADT<>();
        CustomADT<Double> typeCostList = new CustomADT<>();
        CustomADT<Integer> typeCountList = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t.getType() != null && t.getCost() != null) {
                int idx = -1;
                for (int j = 0; j < typeList.size(); j++) {
                    if (typeList.get(j).equals(t.getType())) { idx = j; break; }
                }
                if (idx == -1) {
                    typeList.add(t.getType());
                    typeCostList.add(t.getCost());
                    typeCountList.add(1);
                } else {
                    typeCostList.set(idx, typeCostList.get(idx) + t.getCost());
                    typeCountList.set(idx, typeCountList.get(idx) + 1);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cost per Treatment Type:\n");
        for (int i = 0; i < typeList.size(); i++) {
            double avg = typeCountList.get(i) == 0 ? 0.0 : typeCostList.get(i) / typeCountList.get(i);
            sb.append("  ").append(typeList.get(i)).append(": Total=").append(String.format("%.2f", typeCostList.get(i))).append(", Avg=").append(String.format("%.2f", avg)).append("\n");
        }
        return sb.toString();
    }
    // Advanced filtering
    public CustomADT<Treatment> filterByType(Treatment.Type type) {
        CustomADT<Treatment> result = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getType() == type) result.add(t);
        }
        return result;
    }

    public CustomADT<Treatment> filterByStatus(Treatment.TreatmentStatus status) {
        CustomADT<Treatment> result = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getStatus() == status) result.add(t);
        }
        return result;
    }

    public CustomADT<Treatment> filterByDateRange(LocalDate start, LocalDate end) {
        CustomADT<Treatment> result = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getOrderedDate() != null &&
                !t.getOrderedDate().isBefore(start) && !t.getOrderedDate().isAfter(end)) {
                result.add(t);
            }
        }
        return result;
    }

    public CustomADT<Treatment> filterByKeyword(String keyword) {
        CustomADT<Treatment> result = new CustomADT<>();
        String k = keyword == null ? "" : keyword.toLowerCase();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && (
                (t.getDiagnosis() != null && t.getDiagnosis().toLowerCase().contains(k)) ||
                (t.getNotes() != null && t.getNotes().toLowerCase().contains(k)) ||
                (t.getInstructions() != null && t.getInstructions().toLowerCase().contains(k))
            )) {
                result.add(t);
            }
        }
        return result;
    }

    // Reporting & Exporting
    public void exportToCSV(CustomADT<Treatment> list, String filename) throws java.io.IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(filename)) {
            pw.println("ID,ConsultationID,Type,Name,Diagnosis,Status,OrderedDate,Instructions,Notes,Cost");
            for (int i = 0; i < list.size(); i++) {
                Treatment t = list.get(i);
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    t.getId(),
                    t.getConsultationId(),
                    t.getType(),
                    t.getName(),
                    t.getDiagnosis(),
                    t.getStatus(),
                    t.getOrderedDate(),
                    t.getInstructions(),
                    t.getNotes(),
                    t.getCost()
                );
            }
        }
    }

    public String getSummaryReport(CustomADT<Treatment> list) {
        int total = list.size();
        double totalCost = 0;
        CustomADT<Treatment.Type> typeList = new CustomADT<>();
        CustomADT<Integer> typeCountList = new CustomADT<>();
        CustomADT<Treatment.TreatmentStatus> statusList = new CustomADT<>();
        CustomADT<Integer> statusCountList = new CustomADT<>();
        for (int i = 0; i < list.size(); i++) {
            Treatment t = list.get(i);
            totalCost += t.getCost() != null ? t.getCost() : 0;
            // Type
            int typeIdx = -1;
            for (int j = 0; j < typeList.size(); j++) {
                if (typeList.get(j).equals(t.getType())) { typeIdx = j; break; }
            }
            if (typeIdx == -1) { typeList.add(t.getType()); typeCountList.add(1); }
            else typeCountList.set(typeIdx, typeCountList.get(typeIdx) + 1);
            // Status
            int statusIdx = -1;
            for (int j = 0; j < statusList.size(); j++) {
                if (statusList.get(j).equals(t.getStatus())) { statusIdx = j; break; }
            }
            if (statusIdx == -1) { statusList.add(t.getStatus()); statusCountList.add(1); }
            else statusCountList.set(statusIdx, statusCountList.get(statusIdx) + 1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Total Treatments: ").append(total).append("\n");
        sb.append("Average Cost: ").append(total == 0 ? 0 : String.format("%.2f", totalCost / total)).append("\n");
        sb.append("Type Breakdown:\n");
        for (int i = 0; i < typeList.size(); i++) sb.append("  ").append(typeList.get(i)).append(": ").append(typeCountList.get(i)).append("\n");
        sb.append("Status Breakdown:\n");
        for (int i = 0; i < statusList.size(); i++) sb.append("  ").append(statusList.get(i)).append(": ").append(statusCountList.get(i)).append("\n");
        return sb.toString();
    }

    // Error handling in add/update
    public Treatment addTreatmentWithValidation(Treatment tr) {
        if (tr == null) throw new IllegalArgumentException("Treatment cannot be null.");
        String err = tr.validate();
        if (err != null) throw new IllegalArgumentException(err);
        return addTreatment(tr);
    }
    public boolean updateTreatmentWithValidation(Treatment tr) {
        if (tr == null) throw new IllegalArgumentException("Treatment cannot be null.");
        String err = tr.validate();
        if (err != null) throw new IllegalArgumentException(err);
        return updateTreatment(tr);
    }

    // Analytics
    public int countByType(Treatment.Type type) {
        int count = 0;
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getType() == type) count++;
        }
        return count;
    }
    public int countByStatus(Treatment.TreatmentStatus status) {
        int count = 0;
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getStatus() == status) count++;
        }
        return count;
    }
    public double averageCost() {
        double total = 0;
        int count = 0;
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (t != null && t.getCost() != null) {
                total += t.getCost();
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }
    private final TreatmentDAO tdao = new TreatmentDAO();
    private final ConsultationDAO cdao = new ConsultationDAO();
    private final MedicationDAO mdao = new MedicationDAO();
    private final PatientDAO pdao = new PatientDAO();
    private final DoctorDAO ddao = new DoctorDAO();

    private ADTInterface<Treatment> treatments = new CustomADT<>();

    public TreatmentMaintenance(){
        treatments = tdao.load();
    }

    // Queries
    public ADTInterface<Treatment> getAllTreatments(){ return treatments; }
    public ADTInterface<Consultation> getAllConsultations(){ return cdao.load(); }
    public ADTInterface<Medication> getAllMedications(){ return mdao.load(); }

    public CustomADT<Consultation> getOngoingConsultations(){
        ADTInterface<Consultation> cons = cdao.load();
        CustomADT<Consultation> ongoing = new CustomADT<>();
        for (int i=0;i<cons.size();i++){
            Consultation c = cons.get(i);
            if (c!=null && c.getStatus()== Consultation.Status.ONGOING) ongoing.add(c);
        }
        return ongoing;
    }

    public Treatment findTreatmentById(String id){
        if (id==null) return null;
        if (treatments instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Treatment> list=(CustomADT<Treatment>) cadt;
            int idx = list.findIndex(new CustomADT.ADTPredicate<Treatment>(){ public boolean test(Treatment t){ return t.getId()!=null && t.getId().equals(id);} });
            return idx>=0? list.get(idx): null;
        }
        for (int i=0;i<treatments.size();i++) if (id.equals(treatments.get(i).getId())) return treatments.get(i);
        return null;
    }

    public Consultation findConsultationById(String id){
        ADTInterface<Consultation> cons = cdao.load();
        for (int i=0;i<cons.size();i++) if (id!=null && id.equals(cons.get(i).getId())) return cons.get(i);
        return null;
    }
    public Medication findMedicationById(String id){
        ADTInterface<Medication> meds = mdao.load();
        for (int i=0;i<meds.size();i++) if (id!=null && id.equals(meds.get(i).getId())) return meds.get(i);
        return null;
    }

    // Commands
    public Treatment addTreatment(Treatment tr){
        if (tr==null) return null;
        if (tr.getId()==null || tr.getId().isBlank()) tr.setId(generateNextId());
        treatments.add(tr);
        persist();
        return tr;
    }
    public boolean updateTreatment(Treatment tr){
        if (tr==null || tr.getId()==null) return false;
        for (int i=0;i<treatments.size();i++){
            if (tr.getId().equals(treatments.get(i).getId())){
                treatments.set(i, tr);
                persist();
                return true;
            }
        }
        return false;
    }
    public boolean deleteTreatment(String id){
        if (id==null) return false;
        if (treatments instanceof CustomADT<?> cadt){
            @SuppressWarnings("unchecked") CustomADT<Treatment> list=(CustomADT<Treatment>) cadt;
            int idx = list.findIndex(new CustomADT.ADTPredicate<Treatment>(){ public boolean test(Treatment t){ return t.getId()!=null && t.getId().equals(id);} });
            if (idx>=0){ list.remove(idx); persist(); return true; }
            return false;
        }
        for (int i=0;i<treatments.size();i++) if (id.equals(treatments.get(i).getId())){ treatments.remove(i); persist(); return true; }
        return false;
    }

    // Search helpers
    public CustomADT<Treatment> searchById(String q){
        CustomADT<Treatment> results = new CustomADT<>();
        String s = q==null?"":q.toLowerCase();
        for (int i=0;i<treatments.size();i++){
            Treatment t = treatments.get(i);
            if (t.getId()!=null && t.getId().toLowerCase().contains(s)) results.add(t);
        }
        return results;
    }
    public CustomADT<Treatment> searchByPatient(String q){
        String s=q==null?"":q.toLowerCase();
    // uses cdao in loop via findConsultationById
        ADTInterface<entity.Patient> patients = pdao.retrieveFromFile();
        CustomADT<Treatment> results = new CustomADT<>();
        for (int i=0;i<treatments.size();i++){
            Treatment t = treatments.get(i);
            Consultation c = findConsultationById(t.getConsultationId());
            if (c==null) continue;
            entity.Patient p = findPatientById(patients, c.getPatientId());
            String pid = c.getPatientId()==null?"":c.getPatientId();
            String pname = p==null?"":p.getName()==null?"":p.getName();
            if (pid.toLowerCase().contains(s) || pname.toLowerCase().contains(s)) results.add(t);
        }
        return results;
    }
    public CustomADT<Treatment> searchByDoctor(String q){
        String s=q==null?"":q.toLowerCase();
    // uses cdao in loop via findConsultationById
        ADTInterface<entity.Doctor> doctors = ddao.retrieveFromFile();
        CustomADT<Treatment> results = new CustomADT<>();
        for (int i=0;i<treatments.size();i++){
            Treatment t = treatments.get(i);
            Consultation c = findConsultationById(t.getConsultationId());
            if (c==null) continue;
            entity.Doctor d = findDoctorById(doctors, c.getDoctorId());
            String did = c.getDoctorId()==null?"":c.getDoctorId();
            String dname = d==null?"":d.getName()==null?"":d.getName();
            if (did.toLowerCase().contains(s) || dname.toLowerCase().contains(s)) results.add(t);
        }
        return results;
    }

    // Sort
    public void sortTreatments(int field, boolean asc){
        if (!(treatments instanceof CustomADT<?> cadt)) return;
        @SuppressWarnings("unchecked") CustomADT<Treatment> list=(CustomADT<Treatment>) cadt;
        CustomADT.ADTComparator<Treatment> cmp;
        switch (field){
            case 1 -> cmp = (a,b) -> {
                if (a.getOrderedDate() == null && b.getOrderedDate() == null) return 0;
                if (a.getOrderedDate() == null) return -1;
                if (b.getOrderedDate() == null) return 1;
                return a.getOrderedDate().compareTo(b.getOrderedDate());
            };
            case 2 -> cmp = (a,b) -> {
                if (a.getCost() == null && b.getCost() == null) return 0;
                if (a.getCost() == null) return -1;
                if (b.getCost() == null) return 1;
                return Double.compare(a.getCost(), b.getCost());
            };
            default -> cmp = (a,b) -> 0;
        }
        list.sort(cmp);
        if (!asc) reverse(list);
    }

    // Helpers
    private String generateNextId(){
        int max=0; for (int i=0;i<treatments.size();i++){String id=treatments.get(i).getId(); if(id!=null&&id.startsWith("T"))try{int n=Integer.parseInt(id.substring(1)); if(n>max)max=n;}catch(Exception ignored){}}
        return String.format("T%04d", max+1);
    }
    private void persist(){ tdao.save(treatments); }
    private String safe(String s){ return s==null?"":s; }
    private void reverse(CustomADT<Treatment> list){ int n=list.size(); for (int i=0;i<n/2;i++) list.swap(i, n-1-i); }

    private entity.Patient findPatientById(ADTInterface<entity.Patient> list, String id){
        if (id==null) return null; for (int i=0;i<list.size();i++) if (id.equals(list.get(i).getId())) return list.get(i); return null;
    }
    private entity.Doctor findDoctorById(ADTInterface<entity.Doctor> list, String id){
        if (id==null) return null; for (int i=0;i<list.size();i++) if (id.equals(list.get(i).getId())) return list.get(i); return null;
    }

    public void refreshTreatmentFromFile() {
        ADTInterface<Treatment> fresh = tdao.load();
        treatments.clear();
        for (int i = 0; i < fresh.size(); i++) {
            treatments.add(fresh.get(i));
        }
    }

    /**
     * Marks a treatment as COMPLETED.
     * @param treatmentId The ID of the treatment to complete.
     * @return true if the treatment was successfully updated, false otherwise.
     */
    public boolean completeTreatment(String treatmentId) {
        if (treatmentId == null || treatmentId.isBlank()) {
            return false;
        }
        Treatment treatment = findTreatmentById(treatmentId);
        if (treatment == null) {
            return false; // Treatment not found
        }
        // Only treatments with status ORDERED can be completed
        if (treatment.getStatus() != Treatment.TreatmentStatus.PRESCRIBED) {
            return false; // Already completed or in a different state
        }

        treatment.setStatus(Treatment.TreatmentStatus.COMPLETED);
        return updateTreatment(treatment); // Persist the update
    }
}