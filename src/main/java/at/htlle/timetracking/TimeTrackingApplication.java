package at.htlle.timetracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimeTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeTrackingApplication.class, args);
        System.out.println("Application running on http://localhost:8080/");
    }

}
