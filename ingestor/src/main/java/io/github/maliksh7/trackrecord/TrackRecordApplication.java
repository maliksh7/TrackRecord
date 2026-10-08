package io.github.maliksh7.trackrecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class TrackRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackRecordApplication.class, args);
    }
}
