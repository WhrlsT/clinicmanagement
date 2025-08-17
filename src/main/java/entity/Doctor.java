package entity;

public class Doctor {
    private String id;
    private String name;
    private String specialization;
    private String phoneNumber;
    private String email;
    // Google Calendar ID storing duty schedule calendar for this doctor
    private String calendarId;
    // Local weekly availability / duty schedule (7 x 24)
    private DoctorSchedule schedule = new DoctorSchedule();

    public Doctor(String id, String name, String specialization, String phoneNumber, String email) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.specialization = specialization;
    }
    
    public Doctor() {
        // Default constructor for serialization/deserialization
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public DoctorSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(DoctorSchedule schedule) {
        this.schedule = schedule;
    }
}
