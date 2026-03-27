package io.pisek.gmailforward.service;

import io.pisek.gmailforward.config.AccountPairConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MailForwardService {

    private static final Logger log = LoggerFactory.getLogger(MailForwardService.class);

    private final Pop3Fetcher pop3Fetcher;
    private final ImapAppender imapAppender;
    private final DuplicateTracker duplicateTracker;

    public MailForwardService(Pop3Fetcher pop3Fetcher, ImapAppender imapAppender,
                              DuplicateTracker duplicateTracker) {
        this.pop3Fetcher = pop3Fetcher;
        this.imapAppender = imapAppender;
        this.duplicateTracker = duplicateTracker;
    }

    public void processAccount(AccountPairConfig account) {
        log.info("Processing account '{}'", account.getName());

        try (Pop3Session pop3Session = pop3Fetcher.connect(account.getPop3())) {
            List<MessageData> allMessages = pop3Session.getMessages();
            if (allMessages.isEmpty()) {
                log.info("Account '{}': no messages on POP3 server", account.getName());
                return;
            }

            List<MessageData> newMessages = duplicateTracker.filterUnseen(account.getName(), allMessages);
            if (newMessages.isEmpty()) {
                log.info("Account '{}': no new messages", account.getName());
                return;
            }

            log.info("Account '{}': {} new message(s) to forward", account.getName(), newMessages.size());

            Set<String> successfullyAppended = new HashSet<>();
            for (MessageData message : newMessages) {
                try {
                    imapAppender.append(account.getImap(), message);
                    duplicateTracker.markSeen(account.getName(), message);
                    successfullyAppended.add(message.messageId());
                    log.debug("Account '{}': forwarded message {}", account.getName(), message.messageId());
                } catch (Exception e) {
                    log.error("Account '{}': failed to forward message {}: {}",
                            account.getName(), message.messageId(), e.getMessage(), e);
                }
            }

            if (account.isDeleteAfterCopy() && !successfullyAppended.isEmpty()) {
                try {
                    pop3Session.deleteMessages(successfullyAppended);
                    log.info("Account '{}': deleted {} message(s) from POP3",
                            account.getName(), successfullyAppended.size());
                } catch (Exception e) {
                    log.error("Account '{}': failed to delete messages from POP3: {}",
                            account.getName(), e.getMessage(), e);
                }
            }

            log.info("Account '{}': forwarded {}/{} message(s)",
                    account.getName(), successfullyAppended.size(), newMessages.size());

        } catch (Exception e) {
            log.error("Account '{}': failed to process: {}", account.getName(), e.getMessage(), e);
        }
    }
}
