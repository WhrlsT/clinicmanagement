package entity;

public class DoctorSchedule {
    private SlotStatus[][] availability = new SlotStatus[7][24];

    public DoctorSchedule() {
        for (int d=0; d<7; d++) {
            for (int h=0; h<24; h++) {
                availability[d][h] = SlotStatus.NOT_AVAILABLE;
            }
        }
    }

    public boolean isAvailable(int day, int hour) {
    return availability[day][hour] == SlotStatus.AVAILABLE;
    }

    public void setStatus(int day, int hour, SlotStatus status) {
        availability[day][hour] = status;
    }

    public boolean book(int day, int hour) {
    // Deprecated: booking no longer mutates weekly template.
    return isAvailable(day,hour);
    }

    public boolean cancelBooking(int day, int hour) {
    // Deprecated: no booking state stored.
    return true;
    }

    public SlotStatus[][] getAvailability() {
        return availability;
    }

    public void setAvailabilityMatrix(SlotStatus[][] matrix) {
        if (matrix != null && matrix.length == 7 && matrix[0].length == 24) {
            this.availability = matrix;
        }
    }

    public void setAvailabilityRange(int day, int startHour, int endHour, SlotStatus status) {
        if (day < 0 || day > 6) return;
        if (startHour < 0) startHour = 0;
        if (endHour > 24) endHour = 24;
        for (int h = startHour; h < endHour; h++) {
            availability[day][h] = status;
        }
    }

    public void printScheduleTable() {
        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        System.out.print("       ");
        for (int h=0; h<24; h++) System.out.printf("%02d:00 ", h);
        System.out.println();
        for (int d=0; d<7; d++) {
            System.out.printf("%-5s  ", days[d]);
            for (int h=0; h<24; h++) {
                SlotStatus s = availability[d][h];
                char c = (s==SlotStatus.AVAILABLE)? 'A' : '.';
                System.out.print(c+"      ");
            }
            System.out.println();
        }
    }

    // More compact representation; if crop=true, only show hour range that has at least one non-NOT_AVAILABLE slot.
    public void printCompactScheduleTable(boolean crop) {
        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        int start = 0, end = 23;
        if (crop) {
            start = 24; end = -1;
            for (int d=0; d<7; d++) {
                for (int h=0; h<24; h++) {
                    if (availability[d][h] != SlotStatus.NOT_AVAILABLE) {
                        if (h < start) start = h;
                        if (h > end) end = h;
                    }
                }
            }
            if (end == -1) { // No availability at all
                System.out.println("(All slots NOT_AVAILABLE)");
                return;
            }
        }
        System.out.print("Day  ");
        for (int h=start; h<=end; h++) System.out.printf("%02d ", h);
        System.out.println();
        for (int d=0; d<7; d++) {
            System.out.printf("%-4s ", days[d]);
            for (int h=start; h<=end; h++) {
                SlotStatus s = availability[d][h];
                char c = (s==SlotStatus.AVAILABLE)? 'A' : '.';
                System.out.print(c + "  ");
            }
            System.out.println();
        }
    System.out.println("Legend: A=AVAILABLE .=NOT_AVAILABLE (bookings are date-based, not shown here)");
    }
}
