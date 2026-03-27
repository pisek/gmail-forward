package io.pisek.gmailforward.scheduling;

import io.pisek.gmailforward.config.AccountPairConfig;
import io.pisek.gmailforward.config.MailForwardProperties;
import io.pisek.gmailforward.service.MailForwardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class MailForwardScheduler implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MailForwardScheduler.class);

    private final MailForwardProperties properties;
    private final MailForwardService service;
    private final TaskScheduler taskScheduler;

    public MailForwardScheduler(MailForwardProperties properties, MailForwardService service,
                                TaskScheduler taskScheduler) {
        this.properties = properties;
        this.service = service;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void afterPropertiesSet() {
        for (AccountPairConfig account : properties.getAccounts()) {
            log.info("Scheduling mail forwarding for account '{}' every {}", account.getName(),
                    account.getPollingInterval());
            taskScheduler.scheduleWithFixedDelay(
                    () -> processWithErrorHandling(account),
                    account.getPollingInterval()
            );
        }
    }

    private void processWithErrorHandling(AccountPairConfig account) {
        try {
            service.processAccount(account);
        } catch (Exception e) {
            log.error("Unexpected error processing account '{}': {}", account.getName(), e.getMessage(), e);
        }
    }
}
