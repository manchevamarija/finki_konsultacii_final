package mk.ukim.finki.konsultacii.model.custom;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

public class DayMapper {
    private static final Map<DayOfWeek, String> dayOfWeekMap = new HashMap<>();

    static {
        dayOfWeekMap.put(DayOfWeek.MONDAY, "понеделник");
        dayOfWeekMap.put(DayOfWeek.TUESDAY, "вторник");
        dayOfWeekMap.put(DayOfWeek.WEDNESDAY, "среда");
        dayOfWeekMap.put(DayOfWeek.THURSDAY, "четврток");
        dayOfWeekMap.put(DayOfWeek.FRIDAY, "петок");
        dayOfWeekMap.put(DayOfWeek.SATURDAY, "сабота");
        dayOfWeekMap.put(DayOfWeek.SUNDAY, "недела");
    }

    public static String getMacedonianDay(DayOfWeek dayOfWeek) {
        return dayOfWeekMap.get(dayOfWeek);
    }
}