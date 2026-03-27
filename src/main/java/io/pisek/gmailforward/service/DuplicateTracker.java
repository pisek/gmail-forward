package io.pisek.gmailforward.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DuplicateTracker {

    private final JdbcTemplate jdbcTemplate;

    public DuplicateTracker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MessageData> filterUnseen(String accountName, List<MessageData> messages) {
        if (messages.isEmpty()) {
            return messages;
        }

        List<String> messageIds = messages.stream()
                .map(MessageData::messageId)
                .toList();

        String placeholders = String.join(",", Collections.nCopies(messageIds.size(), "?"));
        String sql = "SELECT message_id FROM seen_messages WHERE account_name = ? AND message_id IN (" + placeholders + ")";

        Object[] params = new Object[messageIds.size() + 1];
        params[0] = accountName;
        for (int i = 0; i < messageIds.size(); i++) {
            params[i + 1] = messageIds.get(i);
        }

        Set<String> seenIds = new HashSet<>(jdbcTemplate.queryForList(sql, String.class, params));

        return messages.stream()
                .filter(m -> !seenIds.contains(m.messageId()))
                .toList();
    }

    public void markSeen(String accountName, MessageData message) {
        jdbcTemplate.update(
                "MERGE INTO seen_messages (account_name, message_id) VALUES (?, ?)",
                accountName, message.messageId()
        );
    }
}
