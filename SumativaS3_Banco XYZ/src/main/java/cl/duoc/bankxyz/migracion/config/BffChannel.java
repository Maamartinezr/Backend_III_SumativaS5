package cl.duoc.bankxyz.migracion.config;

import java.util.Arrays;
import java.util.Optional;

public enum BffChannel {
    WEB("/api/bff/web", "WEB"),
    MOBILE("/api/bff/mobile", "MOBILE"),
    ATM("/api/bff/atm", "ATM");

    private final String pathPrefix;
    private final String role;

    BffChannel(String pathPrefix, String role) {
        this.pathPrefix = pathPrefix;
        this.role = role;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public String getRole() {
        return role;
    }

    public static Optional<BffChannel> fromPath(String path) {
        return Arrays.stream(values())
                .filter(channel -> path.startsWith(channel.pathPrefix))
                .findFirst();
    }
}
