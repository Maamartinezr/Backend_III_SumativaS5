package cl.duoc.bankxyz.migracion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "bankxyz.security.bff")
public class BffSecurityProperties {

    private boolean enabled = true;
    private String headerName = "X-BFF-Token";
    private String webToken = "web-token-banco-xyz";
    private String mobileToken = "mobile-token-banco-xyz";
    private String atmToken = "atm-token-banco-xyz";

    public String tokenFor(BffChannel channel) {
        return switch (channel) {
            case WEB -> webToken;
            case MOBILE -> mobileToken;
            case ATM -> atmToken;
        };
    }
}
