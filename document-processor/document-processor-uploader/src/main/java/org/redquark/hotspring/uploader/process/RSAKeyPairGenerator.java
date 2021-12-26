package org.redquark.hotspring.uploader.process;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.RSASecretBCPGKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.redquark.hotspring.uploader.configs.CryptoConfig;
import org.redquark.hotspring.uploader.configs.DocumentUploaderConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Date;

@Component
@Lazy
@Slf4j
public class RSAKeyPairGenerator {

    private static final String PRIVATE_KEY = "private.asc";
    private static final String PUBLIC_KEY = "public.asc";
    private final CryptoConfig cryptoConfig;

    public RSAKeyPairGenerator(DocumentUploaderConfig documentUploaderConfig) {
        this.cryptoConfig = documentUploaderConfig.getCryptoConfig();
    }

    public void generateKeyPair() {
        try (OutputStream privateKeyStream = new FileOutputStream(cryptoConfig.getKeyPath() + File.separator + PRIVATE_KEY);
             OutputStream publicKeyStream = new FileOutputStream(cryptoConfig.getKeyPath() + File.separator + PUBLIC_KEY)) {
            generateKeyPair(
                    privateKeyStream,
                    publicKeyStream,
                    cryptoConfig.getIsArmored(),
                    cryptoConfig.getUsername(),
                    cryptoConfig.getPassword(),
                    cryptoConfig.getKeySize(),
                    cryptoConfig.getAlgorithm(),
                    cryptoConfig.getProvider()
            );
        } catch (IOException e) {
            log.info("Exception occurred while generating the key pair");
        }
    }

    private void generateKeyPair(
            OutputStream privateKeyStream,
            OutputStream publicKeyStream,
            boolean isArmored,
            String username,
            String password,
            int keySize,
            String algorithm,
            String provider
    ) {
        Security.addProvider(new BouncyCastleProvider());
        try {
            // Get the instance of KeyPairGenerator with RSA algorithm and Bouncy Castle provider
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, provider);
            // Set the size of the key
            keyPairGenerator.initialize(keySize);
            // Generate key pair
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // Export the key pair
            export(
                    keyPair.getPrivate(),
                    keyPair.getPublic(),
                    privateKeyStream,
                    publicKeyStream,
                    username,
                    password.toCharArray(),
                    isArmored
            );
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("Exception occurred while generating key pair: {}", e.getMessage(), e);
        }
    }

    private void export(
            PrivateKey privateKey,
            PublicKey publicKey,
            OutputStream privateKeyStream,
            OutputStream publicKeyStream,
            String username,
            char[] passphrase,
            boolean isArmored
    ) {
        if (isArmored) {
            privateKeyStream = new ArmoredOutputStream(privateKeyStream);
        }
        try {
            PGPPublicKey pgpPublicKey = (new JcaPGPKeyConverter()).getPGPPublicKey(1, publicKey, new Date());
            RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey) privateKey;
            RSASecretBCPGKey rsaSecretBCPGKey = new RSASecretBCPGKey(
                    rsaPrivateCrtKey.getPrivateExponent(),
                    rsaPrivateCrtKey.getPrimeP(),
                    rsaPrivateCrtKey.getPrimeQ()
            );
            PGPPrivateKey pgpPrivateKey = new PGPPrivateKey(
                    pgpPublicKey.getKeyID(),
                    pgpPublicKey.getPublicKeyPacket(),
                    rsaSecretBCPGKey
            );
            PGPDigestCalculator sha1Calculator = (new JcaPGPDigestCalculatorProviderBuilder()).build().get(2);
            PGPKeyPair pgpKeyPair = new PGPKeyPair(pgpPublicKey, pgpPrivateKey);
            PGPSecretKey pgpSecretKey = new PGPSecretKey(
                    16,
                    pgpKeyPair,
                    username,
                    sha1Calculator,
                    null,
                    null,
                    new JcaPGPContentSignerBuilder(pgpKeyPair.getPublicKey().getAlgorithm(), 2),
                    (new JcePBESecretKeyEncryptorBuilder(3, sha1Calculator)).setProvider("BC").build(passphrase)
            );
            pgpSecretKey.encode(privateKeyStream);
            privateKeyStream.close();
            if (isArmored) {
                publicKeyStream = new ArmoredOutputStream(publicKeyStream);
            }
            PGPPublicKey key = pgpSecretKey.getPublicKey();
            key.encode(publicKeyStream);
            publicKeyStream.close();
        } catch (PGPException | IOException e) {
            log.error("Exception occurred while exporting key pair: {}", e.getMessage(), e);
        }
    }
}