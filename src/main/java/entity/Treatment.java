package entity;

import java.time.LocalDate;

public class Treatment {
	private String id;                  // T0001
	private String consultationId;      // Link to Consultation

	private Type type;                  // MEDICATION / PROCEDURE / THERAPY / IMAGING / LAB
	private String name;                // procedure/therapy name (for non-medication)
	private String[] medicationIds;     // links to Medication entities when type == MEDICATION

	private TreatmentStatus status = TreatmentStatus.PLANNED;
	private LocalDate orderedDate;      // date ordered/prescribed
	private String instructions;        // patient instructions
	private String notes;               // clinician notes
	private Double cost;                // optional cost estimate

	public Treatment() {}

	public Treatment(String id, String consultationId, Type type, String name) {
		this.id = id;
		this.consultationId = consultationId;
		this.type = type;
		this.name = name;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getConsultationId() { return consultationId; }
	public void setConsultationId(String consultationId) { this.consultationId = consultationId; }
	public Type getType() { return type; }
	public void setType(Type type) { this.type = type; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String[] getMedicationIds() { return medicationIds; }
	public void setMedicationIds(String[] medicationIds) { this.medicationIds = medicationIds; }
	public void addMedicationId(String medicationId) {
		if (medicationId == null || medicationId.isEmpty()) return;
		int n = medicationIds == null ? 0 : medicationIds.length;
		String[] next = new String[n+1];
		if (n>0) System.arraycopy(medicationIds, 0, next, 0, n);
		next[n] = medicationId;
		medicationIds = next;
	}
	public TreatmentStatus getStatus() { return status; }
	public void setStatus(TreatmentStatus status) { this.status = status; }
	public LocalDate getOrderedDate() { return orderedDate; }
	public void setOrderedDate(LocalDate orderedDate) { this.orderedDate = orderedDate; }
	public String getInstructions() { return instructions; }
	public void setInstructions(String instructions) { this.instructions = instructions; }
	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }
	public Double getCost() { return cost; }
	public void setCost(Double cost) { this.cost = cost; }

	public enum Type { MEDICATION, PROCEDURE, THERAPY, IMAGING, LAB }
	public enum TreatmentStatus { PLANNED, PRESCRIBED, DISPENSED, ADMINISTERED, COMPLETED, CANCELLED }
}

