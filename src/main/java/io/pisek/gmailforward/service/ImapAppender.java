package io.pisek.gmailforward.service;

import io.pisek.gmailforward.config.AccountPairConfig.ImapServerConfig;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Properties;

@Component
public class ImapAppender {

    private static final Logger log = LoggerFactory.getLogger(ImapAppender.class);

    public void append(ImapServerConfig config, MessageData messageData) throws MessagingException {
        String protocol = config.isSsl() ? "imaps" : "imap";
        Properties props = new Properties();
        props.setProperty("mail." + protocol + ".host", config.getHost());
        props.setProperty("mail." + protocol + ".port", String.valueOf(config.getPort()));
        props.setProperty("mail." + protocol + ".connectiontimeout", "10000");
        props.setProperty("mail." + protocol + ".timeout", "30000");

        Session session = Session.getInstance(props);
        try (Store store = session.getStore(protocol)) {
            store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());

            Folder folder = store.getFolder(config.getFolder());
            if (!folder.exists()) {
                folder.create(Folder.HOLDS_MESSAGES);
                log.info("Created IMAP folder '{}'", config.getFolder());
            }
            folder.open(Folder.READ_WRITE);

            // Reconstruct MimeMessage from raw bytes - preserves all original headers including FROM
            MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(messageData.rawContent()));
            // Do NOT call message.saveChanges() - it would rewrite headers

            folder.appendMessages(new Message[]{message});

            folder.close(false);
            log.debug("Appended message {} to IMAP folder '{}'", messageData.messageId(), config.getFolder());
        }
    }
}
