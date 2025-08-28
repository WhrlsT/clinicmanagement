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

/**
 * Logic-only Treatment maintenance. UI lives in boundary.TreatmentMaintenanceUI.
 */
public class TreatmentMaintenance {
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
            case 1 -> cmp = (a,b) -> safe(a.getId()).compareToIgnoreCase(safe(b.getId()));
            case 2 -> cmp = (a,b) -> safe(a.getConsultationId()).compareToIgnoreCase(safe(b.getConsultationId()));
            case 3 -> cmp = (a,b) -> safe(a.getType()==null?null:a.getType().name()).compareToIgnoreCase(safe(b.getType()==null?null:b.getType().name()));
            case 4 -> cmp = (a,b) -> safe(a.getStatus()==null?null:a.getStatus().name()).compareToIgnoreCase(safe(b.getStatus()==null?null:b.getStatus().name()));
            default -> cmp = (a,b) -> safe(a.getId()).compareToIgnoreCase(safe(b.getId()));
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
}