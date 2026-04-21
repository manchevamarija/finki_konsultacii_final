package mk.ukim.finki.konsultacii.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "semester-date-range")
@Setter
@Getter
public class SemesterDateRangeProperties {

    private String winterStartMonth;
    private String winterEndMonth;
    private int winterStartDay;
    private int winterEndDay;
    private String summerStartMonth;
    private String summerEndMonth;
    private int summerStartDay;
    private int summerEndDay;
}
