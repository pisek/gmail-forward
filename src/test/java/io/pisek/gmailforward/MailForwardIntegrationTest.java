package io.pisek.gmailforward;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.pisek.gmailforward.config.AccountPairConfig;
import io.pisek.gmailforward.service.DuplicateTracker;
import io.pisek.gmailforward.service.ImapAppender;
import io.pisek.gmailforward.service.MailForwardService;
import io.pisek.gmailforward.service.Pop3Fetcher;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class MailForwardIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.ALL);

    @Autowired
    private Pop3Fetcher pop3Fetcher;

    @Autowired
    private ImapAppender imapAppender;

    @Autowired
    private DuplicateTracker duplicateTracker;

    @Test
    void shouldForwardMessagePreservingFromHeader() throws Exception {
        // Set up GreenMail users
        greenMail.setUser("pop3user@localhost", "pop3user", "pop3pass");
        greenMail.setUser("imapuser@localhost", "imapuser", "imappass");

        // Deliver a test message to POP3 user's inbox
        deliverTestMessage("sender@external.com", "pop3user@localhost",
                "Test Subject", "Test body content");

        // Configure account pair pointing at GreenMail
        AccountPairConfig account = createTestAccountConfig();

        // Run the forwarder
        MailForwardService service = new MailForwardService(pop3Fetcher, imapAppender, duplicateTracker);
        service.processAccount(account);

        // Verify message in IMAP
        Message[] imapMessages = fetchImapMessages(account);
        assertEquals(1, imapMessages.length, "Should have exactly 1 message in IMAP");

        MimeMessage forwarded = (MimeMessage) imapMessages[0];
        assertEquals("Test Subject", forwarded.getSubject());

        // Verify FROM is preserved
        InternetAddress from = (InternetAddress) forwarded.getFrom()[0];
        assertEquals("sender@external.com", from.getAddress(),
                "FROM header must be the original sender, not the POP3/IMAP user");

        // Run again - should NOT duplicate
        service.processAccount(account);
        Message[] afterSecondRun = fetchImapMessages(account);
        assertEquals(1, afterSecondRun.length, "Should still have exactly 1 message after second run");
    }

    @Test
    void shouldHandleDeleteAfterCopy() throws Exception {
        greenMail.setUser("pop3del@localhost", "pop3del", "pop3pass");
        greenMail.setUser("imapdel@localhost", "imapdel", "imappass");

        deliverTestMessage("sender@external.com", "pop3del@localhost",
                "Delete Test", "Body");

        AccountPairConfig account = createTestAccountConfig();
        account.setName("delete-test");
        account.getPop3().setUsername("pop3del");
        account.getPop3().setPassword("pop3pass");
        account.getImap().setUsername("imapdel");
        account.getImap().setPassword("imappass");
        account.setDeleteAfterCopy(true);

        MailForwardService service = new MailForwardService(pop3Fetcher, imapAppender, duplicateTracker);
        service.processAccount(account);

        // Verify message in IMAP
        Message[] imapMessages = fetchImapMessages(account);
        assertEquals(1, imapMessages.length);
    }

    private AccountPairConfig createTestAccountConfig() {
        AccountPairConfig config = new AccountPairConfig();
        config.setName("test-account");

        AccountPairConfig.ServerConfig pop3 = new AccountPairConfig.ServerConfig();
        pop3.setHost("localhost");
        pop3.setPort(greenMail.getPop3().getPort());
        pop3.setUsername("pop3user");
        pop3.setPassword("pop3pass");
        pop3.setSsl(false);
        config.setPop3(pop3);

        AccountPairConfig.ImapServerConfig imap = new AccountPairConfig.ImapServerConfig();
        imap.setHost("localhost");
        imap.setPort(greenMail.getImap().getPort());
        imap.setUsername("imapuser");
        imap.setPassword("imappass");
        imap.setSsl(false);
        imap.setFolder("INBOX");
        config.setImap(imap);

        return config;
    }

    private void deliverTestMessage(String from, String to, String subject, String body) throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();
        greenMail.getUserManager().getUserByEmail(to).deliver(message);
    }

    private Message[] fetchImapMessages(AccountPairConfig account) throws Exception {
        Properties props = new Properties();
        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");
        store.connect("localhost", greenMail.getImap().getPort(),
                account.getImap().getUsername(), account.getImap().getPassword());
        Folder folder = store.getFolder(account.getImap().getFolder());
        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.getMessages();
        // Force fetch to avoid lazy loading after close
        for (Message m : messages) {
            m.getContent();
        }
        return messages;
    }
}
