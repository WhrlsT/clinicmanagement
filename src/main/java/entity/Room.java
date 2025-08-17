package entity;

public class Room {
    private String id; // R1, R2
    private String name;
    private String calendarId; // optional Google Calendar for the room
    private RoomSchedule schedule = new RoomSchedule();

    public Room() {}
    public Room(String id, String name) {
        this.id = id; this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCalendarId() { return calendarId; }
    public void setCalendarId(String calendarId) { this.calendarId = calendarId; }
    public RoomSchedule getSchedule() { return schedule; }
    public void setSchedule(RoomSchedule schedule) { this.schedule = schedule; }
}
