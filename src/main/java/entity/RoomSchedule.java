package entity;

public class RoomSchedule {
    private SlotStatus[][] slots = new SlotStatus[7][24];

    public RoomSchedule() {
        for (int d=0; d<7; d++)
            for (int h=0; h<24; h++)
                slots[d][h] = SlotStatus.AVAILABLE; // rooms default to available
    }

    public boolean isAvailable(int day, int hour) {
        return slots[day][hour] == SlotStatus.AVAILABLE;
    }

    public boolean book(int day, int hour) {
        // Booking no longer mutates template; consultations carry bookings.
        return isAvailable(day,hour);
    }

    public boolean cancel(int day, int hour) {
        // Always succeed; nothing to revert in template.
        return true;
    }

    public void block(int day, int hour) { slots[day][hour] = SlotStatus.NOT_AVAILABLE; }

    public SlotStatus[][] getSlots() { return slots; }

    public void printTable() {
        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        System.out.print("       ");
        for (int h=0; h<24; h++) System.out.printf("%02d:00 ", h);
        System.out.println();
        for (int d=0; d<7; d++) {
            System.out.printf("%-5s  ", days[d]);
            for (int h=0; h<24; h++) {
                SlotStatus s = slots[d][h];
                char c = (s==SlotStatus.AVAILABLE)? 'A' : '.';
                System.out.print(c+"      ");
            }
            System.out.println();
        }
    }
}
