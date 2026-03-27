# Gmail Forward

A Spring Boot application that periodically fetches emails from POP3 servers and copies them to IMAP servers, similar to how Gmail fetches mail from external accounts. The original sender (FROM) headers are preserved.

## Features

- Multiple POP3-to-IMAP account pairs with independent polling intervals
- Configurable delete-after-copy or leave-on-server mode per account
- Duplicate detection using H2 database (persists across restarts)
- Original email headers (FROM, Reply-To, etc.) preserved via IMAP APPEND
- YAML configuration with environment variable support for passwords

## Requirements

- Java 21+
- Maven 3.9+ (for building)

## Building

```bash
mvn package
```

This produces an executable JAR at `target/gmail-forward-1.0.0-SNAPSHOT.jar`.

## Configuration

Place an `application.yml` file next to the JAR:

```yaml
mail-forward:
  accounts:
    - name: "personal-gmail"
      pop3:
        host: pop.gmail.com
        port: 995
        username: user@gmail.com
        password: ${GMAIL_APP_PASSWORD}
        ssl: true
      imap:
        host: imap.destination.com
        port: 993
        username: user@destination.com
        password: ${DEST_PASSWORD}
        ssl: true
        folder: INBOX
      polling-interval: 5m
      delete-after-copy: false

    - name: "work-account"
      pop3:
        host: pop.work.com
        port: 995
        username: worker@work.com
        password: ${WORK_POP_PASS}
        ssl: true
      imap:
        host: imap.work.com
        port: 993
        username: worker@work.com
        password: ${WORK_IMAP_PASS}
        ssl: true
        folder: Forwarded
      polling-interval: 10m
      delete-after-copy: true

spring:
  datasource:
    url: jdbc:h2:file:./data/mailforward;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
```

### Configuration reference

| Property | Description | Default |
|---|---|---|
| `name` | Unique account identifier | required |
| `pop3.host` | POP3 server hostname | required |
| `pop3.port` | POP3 server port | required |
| `pop3.username` | POP3 login username | required |
| `pop3.password` | POP3 login password | required |
| `pop3.ssl` | Enable SSL/TLS | `true` |
| `imap.host` | IMAP server hostname | required |
| `imap.port` | IMAP server port | required |
| `imap.username` | IMAP login username | required |
| `imap.password` | IMAP login password | required |
| `imap.ssl` | Enable SSL/TLS | `true` |
| `imap.folder` | Target IMAP folder | `INBOX` |
| `polling-interval` | How often to check for new mail (`5m`, `1h`, `30s`) | `5m` |
| `delete-after-copy` | Delete messages from POP3 after copying to IMAP | `false` |

### Passwords

Avoid storing passwords in plain text. Use environment variables instead:

```bash
export GMAIL_APP_PASSWORD=your-app-password
export DEST_PASSWORD=your-imap-password
java -jar gmail-forward-1.0.0-SNAPSHOT.jar
```

Alternatively, point to an external config file with restricted permissions:

```bash
java -jar gmail-forward-1.0.0-SNAPSHOT.jar --spring.config.import=file:/etc/gmail-forward/secrets.yml
```

## Running

```bash
java -jar gmail-forward-1.0.0-SNAPSHOT.jar
```

The application will:
1. Start polling each configured POP3 account at the specified interval
2. Fetch new messages and copy them to the corresponding IMAP server
3. Track processed message IDs in an H2 database at `./data/mailforward.mv.db`

To use a custom config location:

```bash
java -jar gmail-forward-1.0.0-SNAPSHOT.jar --spring.config.location=/path/to/application.yml
```

## How it works

1. Connects to the POP3 server and downloads raw message bytes
2. Filters out already-seen messages using the H2 database
3. Appends each new message to the IMAP server using IMAP APPEND, which stores the literal RFC 822 message data — all original headers (FROM, Reply-To, Date, etc.) are preserved byte-for-byte
4. Marks messages as seen in the database
5. Optionally deletes messages from the POP3 server if `delete-after-copy` is enabled
