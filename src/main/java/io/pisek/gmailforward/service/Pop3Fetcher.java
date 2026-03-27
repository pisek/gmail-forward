package io.pisek.gmailforward.service;

import io.pisek.gmailforward.config.AccountPairConfig.ServerConfig;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class Pop3Fetcher {

    private static final Logger log = LoggerFactory.getLogger(Pop3Fetcher.class);

    public Pop3Session connect(ServerConfig config) throws MessagingException {
        String protocol = config.isSsl() ? "pop3s" : "pop3";
        Properties props = new Properties();
        props.setProperty("mail." + protocol + ".host", config.getHost());
        props.setProperty("mail." + protocol + ".port", String.valueOf(config.getPort()));
        props.setProperty("mail." + protocol + ".connectiontimeout", "10000");
        props.setProperty("mail." + protocol + ".timeout", "30000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());

        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        Message[] rawMessages = folder.getMessages();
        List<MessageData> messageDataList = new ArrayList<>();
        Map<Integer, Message> originalMessages = new HashMap<>();

        for (int i = 0; i < rawMessages.length; i++) {
            Message msg = rawMessages[i];
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                msg.writeTo(baos);
                byte[] rawContent = baos.toByteArray();

                String messageId = extractMessageId(msg, rawContent);
                int messageNumber = i + 1;

                messageDataList.add(new MessageData(messageId, rawContent, msg.getSentDate(), messageNumber));
                originalMessages.put(messageNumber, msg);
            } catch (IOException | MessagingException e) {
                log.warn("Failed to read message {}: {}", i + 1, e.getMessage());
            }
        }

        log.debug("Fetched {} messages from POP3 {}:{}", messageDataList.size(), config.getHost(), config.getPort());
        return new Pop3Session(store, folder, messageDataList, originalMessages);
    }

    private String extractMessageId(Message msg, byte[] rawContent) throws MessagingException {
        String[] messageIdHeaders = msg.getHeader("Message-ID");
        if (messageIdHeaders != null && messageIdHeaders.length > 0 && messageIdHeaders[0] != null
                && !messageIdHeaders[0].isBlank()) {
            return messageIdHeaders[0].trim();
        }
        return computeHash(rawContent);
    }

    private String computeHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
