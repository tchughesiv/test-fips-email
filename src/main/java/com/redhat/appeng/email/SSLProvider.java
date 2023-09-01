package com.redhat.appeng.email;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

class SSLProvider {

    static void initProviders() {
        BouncyCastleFipsProvider bcFipsProvider = new BouncyCastleFipsProvider();
        Security.insertProviderAt(new KeycloakFipsSecurityProvider(bcFipsProvider), 1);

        // providers.put(CryptoConstants.A128KW, new FIPSAesKeyWrapAlgorithmProvider());
        // providers.put(CryptoConstants.RSA1_5, new
        // FIPSRsaKeyEncryptionJWEAlgorithmProvider(FipsRSA.WRAP_PKCS1v1_5));
        // providers.put(CryptoConstants.RSA_OAEP, new
        // FIPSRsaKeyEncryptionJWEAlgorithmProvider(FipsRSA.WRAP_OAEP));
        // providers.put(CryptoConstants.RSA_OAEP_256, new
        // FIPSRsaKeyEncryptionJWEAlgorithmProvider(FipsRSA.WRAP_OAEP.withDigest(FipsSHS.Algorithm.SHA256)));

        SSLProvider.checkSecureRandom(() -> Security.insertProviderAt(bcFipsProvider, 2));
        Provider bcJsseProvider = new BouncyCastleJsseProvider("fips:BCFIPS");
        Security.insertProviderAt(bcJsseProvider, 3);
        // force the key and trust manager factories if default values not present in
        // BCJSSE
        SSLProvider.modifyKeyTrustManagerSecurityProperties(bcJsseProvider);
    }

    static void modifyKeyTrustManagerSecurityProperties(Provider bcJsseProvider) {
        boolean setKey = bcJsseProvider.getService(KeyManagerFactory.class.getSimpleName(),
                KeyManagerFactory.getDefaultAlgorithm()) == null;
        boolean setTrust = bcJsseProvider.getService(TrustManagerFactory.class.getSimpleName(),
                TrustManagerFactory.getDefaultAlgorithm()) == null;
        if (!setKey && !setTrust) {
            return;
        }
        Set<Provider.Service> services = bcJsseProvider.getServices();
        if (services != null) {
            for (Provider.Service service : services) {
                if (setKey && KeyManagerFactory.class.getSimpleName().equals(service.getType())) {
                    Security.setProperty("ssl.KeyManagerFactory.algorithm", service.getAlgorithm());
                    setKey = false;
                    if (!setTrust) {
                        return;
                    }
                } else if (setTrust && TrustManagerFactory.class.getSimpleName().equals(service.getType())) {
                    Security.setProperty("ssl.TrustManagerFactory.algorithm", service.getAlgorithm());
                    setTrust = false;
                    if (!setKey) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Provider " + bcJsseProvider.getName()
                + " does not provide KeyManagerFactory or TrustManagerFactory algorithms for TLS");
    }

    static void checkSecureRandom(Runnable insertBcFipsProvider) {
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            EmailService.logger.info(
                    String.format("Strong secure random available. Algorithm: %s, Provider: %s", sr.getAlgorithm(),
                            sr.getProvider()));
            insertBcFipsProvider.run();
        } catch (NoSuchAlgorithmException nsae) {

            // Fallback to regular SecureRandom
            SecureRandom secRandom = new SecureRandom();
            String origStrongAlgs = Security.getProperty("securerandom.strongAlgorithms");
            String usedAlg = secRandom.getAlgorithm() + ":" + secRandom.getProvider().getName();
            EmailService.logger.info(String.format(
                    "Strong secure random not available. Tried algorithms: %s. Using algorithm as a fallback for strong secure random: %s",
                    origStrongAlgs, usedAlg));

            String strongAlgs = origStrongAlgs == null ? usedAlg : usedAlg + "," + origStrongAlgs;
            Security.setProperty("securerandom.strongAlgorithms", strongAlgs);

            try {
                // Need to insert BCFIPS provider to security providers with "strong algorithm"
                // available
                insertBcFipsProvider.run();
                SecureRandom.getInstance("DEFAULT", "BCFIPS");
                EmailService.logger.info("Initialized BCFIPS secured random");
            } catch (NoSuchAlgorithmException | NoSuchProviderException nsaee) {
                throw new IllegalStateException("Not possible to initiate BCFIPS secure random", nsaee);
            } finally {
                Security.setProperty("securerandom.strongAlgorithms", origStrongAlgs != null ? origStrongAlgs : "");
            }
        }
    }

    static String getSupportedSslProtocols() {
        try {
            String[] protocols = SSLContext.getDefault().getSupportedSSLParameters().getProtocols();
            if (protocols != null) {
                return String.join(" ", protocols);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}