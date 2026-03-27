package io.pisek.gmailforward.service;

import java.util.Date;

public record MessageData(String messageId, byte[] rawContent, Date sentDate, int messageNumber) {
}
