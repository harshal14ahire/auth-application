₹1package passway.example.personal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import passway.example.personal.config.AppProperties;
import passway.example.personal.config.MailProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, MailProperties.class})
public class PersonalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalApplication.class, args);
    }

}
