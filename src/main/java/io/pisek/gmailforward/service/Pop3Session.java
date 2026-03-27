package io.pisek.gmailforward.service;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Pop3Session implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Pop3Session.class);

    private final Store store;
    private final Folder folder;
    private final List<MessageData> messages;
    private final Map<Integer, Message> originalMessages;

    public Pop3Session(Store store, Folder folder, List<MessageData> messages,
                       Map<Integer, Message> originalMessages) {
        this.store = store;
        this.folder = folder;
        this.messages = messages;
        this.originalMessages = originalMessages;
    }

    public List<MessageData> getMessages() {
        return messages;
    }

    public void deleteMessages(Set<String> messageIds) throws MessagingException {
        for (MessageData data : messages) {
            if (messageIds.contains(data.messageId())) {
                Message original = originalMessages.get(data.messageNumber());
                if (original != null) {
                    original.setFlag(Flags.Flag.DELETED, true);
                    log.debug("Marked message {} for deletion", data.messageId());
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(true);
            }
        } catch (MessagingException e) {
            log.warn("Failed to close POP3 folder: {}", e.getMessage());
        }
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            log.warn("Failed to close POP3 store: {}", e.getMessage());
        }
    }
}
