package mk.ukim.finki.konsultacii.model.constants;

import lombok.Getter;

import java.time.LocalDate;
import java.time.Month;


@Getter
public class SemesterDateRange {

    private final Month startMonth;
    private final int startDay;
    private final Month endMonth;
    private final int endDay;

    public SemesterDateRange(Month startMonth, int startDay, Month endMonth, int endDay) {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.endMonth = endMonth;
        this.endDay = endDay;
    }

    public boolean isDateInSemesterRange(LocalDate dateToCheck, int year) {
        LocalDate startDate = LocalDate.of(year, startMonth, startDay);

        // Winter semesters span two years
        if (dateToCheck.isAfter(
                LocalDate.of(startDate.getYear(),
                        Month.valueOf(ApplicationConstants.SEMESTER_DATE_RANGE_PROPERTIES.getWinterStartMonth()),
                        ApplicationConstants.SEMESTER_DATE_RANGE_PROPERTIES.getWinterStartDay()))
        ||
            dateToCheck.equals(
                LocalDate.of(startDate.getYear(),
                        Month.valueOf(ApplicationConstants.SEMESTER_DATE_RANGE_PROPERTIES.getWinterStartMonth()),
                        ApplicationConstants.SEMESTER_DATE_RANGE_PROPERTIES.getWinterStartDay()))
        )
            year++;

        LocalDate endDate = LocalDate.of(year, endMonth, endDay);

        return !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate);
    }
}
