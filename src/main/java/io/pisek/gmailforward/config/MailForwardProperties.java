package io.pisek.gmailforward.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "mail-forward")
@Validated
public class MailForwardProperties {

    @NotEmpty
    @Valid
    private List<AccountPairConfig> accounts = new ArrayList<>();

    public List<AccountPairConfig> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountPairConfig> accounts) {
        this.accounts = accounts;
    }
}
