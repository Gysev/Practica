package ru.mtuci.coursemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.mtuci.coursemanagement.config.JwtProperties;
import ru.mtuci.coursemanagement.license.config.LicenseProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, LicenseProperties.class})
public class CourseManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseManagementApplication.class, args);
    }

}
