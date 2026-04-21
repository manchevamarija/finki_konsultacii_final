package mk.ukim.finki.konsultacii.model.constants;

import mk.ukim.finki.konsultacii.config.SemesterDateRangeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Month;


@Component
public class ApplicationConstants {

    public static SemesterDateRangeProperties SEMESTER_DATE_RANGE_PROPERTIES;
    public static SemesterDateRange WINTER_SEMESTER_DATE_RANGE;
    public static SemesterDateRange SUMMER_SEMESTER_DATE_RANGE;

    @Autowired
    public ApplicationConstants(SemesterDateRangeProperties semesterDateRangeProperties) {
        SEMESTER_DATE_RANGE_PROPERTIES = semesterDateRangeProperties;
        WINTER_SEMESTER_DATE_RANGE = new SemesterDateRange(
                Month.valueOf(semesterDateRangeProperties.getWinterStartMonth()),
                semesterDateRangeProperties.getWinterStartDay(),
                Month.valueOf(semesterDateRangeProperties.getWinterEndMonth()),
                semesterDateRangeProperties.getWinterEndDay()
        );
        SUMMER_SEMESTER_DATE_RANGE = new SemesterDateRange(
                Month.valueOf(semesterDateRangeProperties.getSummerStartMonth()),
                semesterDateRangeProperties.getSummerStartDay(),
                Month.valueOf(semesterDateRangeProperties.getSummerEndMonth()),
                semesterDateRangeProperties.getSummerEndDay()
        );
    }
}
