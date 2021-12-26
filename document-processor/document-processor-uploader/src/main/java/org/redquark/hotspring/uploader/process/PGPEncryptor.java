package org.redquark.hotspring.uploader.process;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.redquark.hotspring.uploader.configs.CryptoConfig;
import org.redquark.hotspring.uploader.configs.DocumentUploaderConfig;
import org.redquark.hotspring.uploader.domains.Document;
import org.redquark.hotspring.uploader.exceptions.DocumentEncryptionException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import java.util.Objects;

@Component
@Slf4j
public class PGPEncryptor {

    private static final String PUBLIC_KEY = "public.asc";

    private final RSAKeyPairGenerator keyPairGenerator;
    private final CryptoConfig cryptoConfig;

    public PGPEncryptor(RSAKeyPairGenerator keyPairGenerator, DocumentUploaderConfig documentUploaderConfig) {
        this.keyPairGenerator = keyPairGenerator;
        cryptoConfig = documentUploaderConfig.getCryptoConfig();
    }

    public byte[] encrypt(Document document) {
        String publicKeyPath = cryptoConfig.getKeyPath() + File.separator + PUBLIC_KEY;
        if (Files.notExists(Paths.get(publicKeyPath))) {
            try {
                Files.createDirectories(Paths.get(cryptoConfig.getKeyPath()));
            } catch (IOException e) {
                log.error("Exception occurred while creating directory: {}", e.getMessage());
                throw new DocumentEncryptionException("Could not generate key pair", e);
            }
        }
        if (Files.notExists(Paths.get(publicKeyPath))) {
            keyPairGenerator.generateKeyPair();
        }
        try (InputStream publicKeyStream = new FileInputStream(Paths.get(publicKeyPath).toFile());
             OutputStream cipheredFileStream = new FileOutputStream(document.getName() + ".pgp")) {
            File inputFile = new File(document.getName());
            FileUtils.writeByteArrayToFile(inputFile, document.getContents());
            return encrypt(inputFile, publicKeyStream, cipheredFileStream, cryptoConfig.getIsArmored());
        } catch (IOException e) {
            log.error("Exception occurred while encrypting the zip: {}", e.getMessage(), e);
            throw new DocumentEncryptionException("Could not encrypt file", e);
        }
    }

    private byte[] encrypt(
            File inputFile,
            InputStream publicKeyStream,
            OutputStream cipheredFileStream,
            boolean isArmored
    ) {
        byte[] bytes = new byte[0];
        try {
            PGPPublicKey pgpPublicKey = readPublicKey(publicKeyStream);
            Security.addProvider(new BouncyCastleProvider());
            if (isArmored) {
                cipheredFileStream = new ArmoredOutputStream(cipheredFileStream);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PGPCompressedDataGenerator compressor = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
                PGPUtil.writeFileToLiteralData(compressor.open(byteArrayOutputStream), PGPLiteralData.BINARY, inputFile);
                compressor.close();
                JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                        .setWithIntegrityPacket(true)
                        .setSecureRandom(new SecureRandom())
                        .setProvider(cryptoConfig.getProvider());
                PGPEncryptedDataGenerator generator = new PGPEncryptedDataGenerator(encryptorBuilder);
                JcePublicKeyKeyEncryptionMethodGenerator encryptionMethodGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(Objects.requireNonNull(pgpPublicKey))
                        .setProvider(new BouncyCastleProvider())
                        .setSecureRandom(new SecureRandom());
                generator.addMethod(encryptionMethodGenerator);
                bytes = byteArrayOutputStream.toByteArray();
                OutputStream outputStream = generator.open(cipheredFileStream, bytes.length);
                outputStream.write(bytes);
                outputStream.close();
                cipheredFileStream.close();
            }
            return bytes;
        } catch (IOException | PGPException e) {
            log.error("Exception occurred while encrypting the file: {}", e.getMessage(), e);
            throw new DocumentEncryptionException("Could not encrypt file", e);
        }
    }

    private PGPPublicKey readPublicKey(InputStream publicKeyStream) {
        PGPPublicKey pgpPublicKey = null;
        try {
            publicKeyStream = PGPUtil.getDecoderStream(publicKeyStream);
            PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(publicKeyStream, new JcaKeyFingerprintCalculator());
            Iterator<PGPPublicKeyRing> ringIterator = keyRingCollection.getKeyRings();
            while (Objects.nonNull(ringIterator) && ringIterator.hasNext()) {
                PGPPublicKeyRing keyRing = ringIterator.next();
                Iterator<PGPPublicKey> publicKeys = keyRing.getPublicKeys();
                while (publicKeys != null && publicKeys.hasNext()) {
                    PGPPublicKey publicKey = publicKeys.next();
                    if (publicKey.isEncryptionKey()) {
                        pgpPublicKey = publicKey;
                    }
                }
            }
            if (Objects.isNull(pgpPublicKey)) {
                throw new IllegalArgumentException("No encryption key found");
            }
        } catch (IOException | PGPException e) {
            log.error("Exception occurred while reading public key: {}", e.getMessage(), e);
        }
        return pgpPublicKey;
    }
}