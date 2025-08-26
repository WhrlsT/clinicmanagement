package entity;

public class Medication {
    private String id;           // M0001
    private String name;         // drug name
    private String code;         // optional local/NDC/ATC code
    private String dosage;       // e.g., "500 mg"
    private String frequency;    // e.g., "2x/day"
    private String route;        // e.g., "Oral", "IV"
    private Double price;        // optional unit price
    private Integer durationDays;// e.g., 7
    private Integer quantity;    // e.g., 14
    private String instructions; // patient instructions
    private String notes;        // clinician notes

    public Medication() {}

    public Medication(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
