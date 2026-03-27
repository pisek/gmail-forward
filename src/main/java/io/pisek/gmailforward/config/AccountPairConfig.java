package io.pisek.gmailforward.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public class AccountPairConfig {

    @NotBlank
    private String name;

    @Valid
    @NotNull
    private ServerConfig pop3;

    @Valid
    @NotNull
    private ImapServerConfig imap;

    private Duration pollingInterval = Duration.ofMinutes(5);

    private boolean deleteAfterCopy = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServerConfig getPop3() {
        return pop3;
    }

    public void setPop3(ServerConfig pop3) {
        this.pop3 = pop3;
    }

    public ImapServerConfig getImap() {
        return imap;
    }

    public void setImap(ImapServerConfig imap) {
        this.imap = imap;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public boolean isDeleteAfterCopy() {
        return deleteAfterCopy;
    }

    public void setDeleteAfterCopy(boolean deleteAfterCopy) {
        this.deleteAfterCopy = deleteAfterCopy;
    }

    public static class ServerConfig {

        @NotBlank
        private String host;

        private int port;

        @NotBlank
        private String username;

        @NotBlank
        private String password;

        private boolean ssl = true;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }
    }

    public static class ImapServerConfig extends ServerConfig {

        private String folder = "INBOX";

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }
    }
}
