package com.redhat.appeng.email;

import static org.bouncycastle.crypto.CryptoServicesRegistrar.isInApprovedOnlyMode;

import java.security.Provider;
import java.util.logging.Logger;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

public class KeycloakFipsSecurityProvider extends Provider {

    protected static final Logger logger = Logger.getLogger(KeycloakFipsSecurityProvider.class.getName());

    private final BouncyCastleFipsProvider bcFipsProvider;

    public KeycloakFipsSecurityProvider(BouncyCastleFipsProvider bcFipsProvider) {
        super("KC(" +
                bcFipsProvider.toString() +
                (isInApprovedOnlyMode() ? " Approved Mode" : "") +
                ", FIPS-JVM: " + isSystemFipsEnabled() +
                ")", 1, "Keycloak pseudo provider");
        this.bcFipsProvider = bcFipsProvider;
    }

    @Override
    public synchronized final Service getService(String type, String algorithm) {
        // Using 'SecureRandom.getInstance("SHA1PRNG")' will delegate to BCFIPS DEFAULT
        // provider instead of returning SecureRandom based on potentially unsecure
        // SHA1PRNG
        if ("SHA1PRNG".equals(algorithm) && "SecureRandom".equals(type)) {
            logger.info("Returning DEFAULT algorithm of BCFIPS provider instead of SHA1PRNG");
            return this.bcFipsProvider.getService("SecureRandom", "DEFAULT");
        } else {
            return null;
        }
    }

    public static String isSystemFipsEnabled() {
        return "enabled";
    }
}
