package org.redquark.hotspring.document.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.redquark.hotspring.document.configs.CryptoDownloaderConfig;
import org.redquark.hotspring.document.exceptions.DecryptionException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

@Component
@Slf4j
@RequiredArgsConstructor
public class PGPDecryptor {

    private static final String PRIVATE_KEY = "private.asc";

    private final CryptoDownloaderConfig cryptoConfig;

    public byte[] decrypt(InputStream cipheredStream) {
        try (InputStream privateKeyStream = new FileInputStream(cryptoConfig.getKeyPath() + "/" + PRIVATE_KEY)) {
            return decrypt(cipheredStream, privateKeyStream, cryptoConfig.getPassword().toCharArray());
        } catch (IOException e) {
            log.error("Exception while decrypting the file: {}", e.getMessage(), e);
            throw new DecryptionException("Could not decrypt file", e);
        }
    }

    private byte[] decrypt(
            InputStream cipheredFileStream,
            InputStream privateKeyStream,
            char[] passphrase
    ) {
        Security.addProvider(new BouncyCastleProvider());
        ByteArrayOutputStream decryptedBytes = new ByteArrayOutputStream();
        try {
            cipheredFileStream = PGPUtil.getDecoderStream(cipheredFileStream);
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(cipheredFileStream, new JcaKeyFingerprintCalculator());
            PGPEncryptedDataList pgpEncryptedData;
            Object o = pgpObjectFactory.nextObject();
            if (o instanceof PGPEncryptedDataList) {
                pgpEncryptedData = (PGPEncryptedDataList) o;
            } else {
                pgpEncryptedData = (PGPEncryptedDataList) pgpObjectFactory.nextObject();
            }
            Iterator<PGPEncryptedData> encryptedDataIterator = pgpEncryptedData.getEncryptedDataObjects();
            PGPPrivateKey secretKey = null;
            PGPPublicKeyEncryptedData pgpPublicKeyEncryptedData = null;
            while (secretKey == null && encryptedDataIterator.hasNext()) {
                pgpPublicKeyEncryptedData = (PGPPublicKeyEncryptedData) encryptedDataIterator.next();
                secretKey = findSecretKey(privateKeyStream, pgpPublicKeyEncryptedData.getKeyID(), passphrase);
            }
            if (secretKey == null) {
                throw new IllegalArgumentException("Secret key for message not found");
            }
            PublicKeyDataDecryptorFactory publicKeyDataDecryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(cryptoConfig.getProvider())
                    .setContentProvider(cryptoConfig.getProvider())
                    .build(secretKey);
            InputStream clear = pgpPublicKeyEncryptedData.getDataStream(publicKeyDataDecryptorFactory);
            PGPObjectFactory plainFactory = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
            Object message = plainFactory.nextObject();
            if (message instanceof PGPCompressedData pgpCompressedData) {
                PGPObjectFactory pgpFactory = new PGPObjectFactory(pgpCompressedData.getDataStream(), new JcaKeyFingerprintCalculator());
                message = pgpFactory.nextObject();
            }
            if (message instanceof PGPLiteralData literalData) {
                InputStream unclear = literalData.getInputStream();
                unclear.transferTo(decryptedBytes);
            } else if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("Encryption message doesn't contain literal data but signed data");
            } else {
                throw new PGPException("Unknown type of message");
            }
            if (pgpPublicKeyEncryptedData.isIntegrityProtected() && !pgpPublicKeyEncryptedData.verify()) {
                throw new PGPException("Integrity check of message is failed");
            }
            return decryptedBytes.toByteArray();
        } catch (IOException | PGPException e) {
            log.error("Exception occurred while decrypting file: {}", e.getMessage(), e);
            throw new DecryptionException("Could not decrypt file", e);
        }
    }

    private PGPPrivateKey findSecretKey(InputStream privateKeyStream, long keyID, char[] passphrase) {
        try {
            PGPSecretKeyRingCollection ringCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyStream), new JcaKeyFingerprintCalculator());
            PGPSecretKey pgpSecretKey = ringCollection.getSecretKey(keyID);
            if (pgpSecretKey == null) {
                return null;
            }
            PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(cryptoConfig.getProvider())
                    .setProvider(cryptoConfig.getProvider())
                    .build())
                    .setProvider(cryptoConfig.getProvider()).build(passphrase);
            return pgpSecretKey.extractPrivateKey(decryptor);
        } catch (PGPException | IOException e) {
            log.error("Exception occurred while finding the secret key: {}", e.getMessage(), e);
            throw new DecryptionException("Could not find private key", e);
        }
    }
}
