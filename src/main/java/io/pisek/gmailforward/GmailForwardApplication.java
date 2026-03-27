package io.pisek.gmailforward;

import io.pisek.gmailforward.config.MailForwardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
@EnableConfigurationProperties(MailForwardProperties.class)
public class GmailForwardApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmailForwardApplication.class, args);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("mail-fwd-");
        return scheduler;
    }
}
