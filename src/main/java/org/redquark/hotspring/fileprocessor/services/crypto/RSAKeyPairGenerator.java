package org.redquark.hotspring.fileprocessor.services.crypto;

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
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
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

@Service
@Slf4j
public class RSAKeyPairGenerator implements CustomKeyPairGenerator {

    /**
     * Generates private and public key pair
     *
     * @param privateKeyPath - path where private key will be stored
     * @param publicKeyPath  - path where public key will be stored
     * @param isArmored      - flag to represent if the reader for Base64 armored objects
     * @param username       - username for the keypair
     * @param password       - password for the keypair
     * @param keySize        - size of the key
     */
    @Override
    public void generateKeyPair(
            String privateKeyPath,
            String publicKeyPath,
            boolean isArmored,
            String username,
            String password,
            int keySize) {
        Security.addProvider(new BouncyCastleProvider());
        try {
            // Get the instance of KeyPairGenerator with RSA algorithm and Bouncy Castle provider
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            // Set the size of the key
            keyPairGenerator.initialize(keySize);
            // Generate key pair
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // Private and public key streams
            OutputStream privateKeyStream = new FileOutputStream(privateKeyPath);
            OutputStream publicKeyStream = new FileOutputStream(publicKeyPath);
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
        } catch (NoSuchAlgorithmException | NoSuchProviderException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void export(
            PrivateKey privateKey,
            PublicKey publicKey,
            OutputStream privateKeyStream,
            OutputStream publicKeyStream,
            String username,
            char[] passphrase,
            boolean isArmored) {
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
