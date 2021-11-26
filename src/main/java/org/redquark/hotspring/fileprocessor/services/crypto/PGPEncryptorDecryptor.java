package org.redquark.hotspring.fileprocessor.services.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import java.util.Objects;

@Service
@Slf4j
public class PGPEncryptorDecryptor implements EncryptorDecryptor {

    @Override
    public void encrypt(String publicKeyPath, String cipheredFileLocation, String inputFileName, boolean isArmored) {
        log.info("Encrypting file={} using PGP", inputFileName);
        try (
                InputStream publicKey = new FileInputStream(publicKeyPath);
                OutputStream cipheredFile = new FileOutputStream(cipheredFileLocation)
        ) {
            encryptFile(inputFileName, readPublicKey(publicKey), cipheredFile, isArmored);
        } catch (IOException e) {
            log.error("Exception occurred while encrypting the file: {}", e.getMessage(), e);
        }
    }

    @Override
    public void decrypt(String cipheredFileLocation, String privateKeyPath, String decryptedFileLocation, String password) {
        try (
                InputStream cipheredFileStream = new FileInputStream(cipheredFileLocation);
                InputStream privateKeyStream = new FileInputStream(privateKeyPath);
                OutputStream decryptedFileStream = new FileOutputStream(decryptedFileLocation)) {
            decryptFile(cipheredFileStream, decryptedFileStream, privateKeyStream, password.toCharArray());
        } catch (IOException e) {
            log.error("Exception occurred while decrypting file: {}", e.getMessage(), e);
        }
    }

    private void decryptFile(InputStream cipheredFileStream, OutputStream decryptedFileStream, InputStream privateKeyStream, char[] passphrase) {
        Security.addProvider(new BouncyCastleProvider());
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
                    .setProvider("BC")
                    .setContentProvider("BC")
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
                int c;
                while ((c = unclear.read()) >= 0) {
                    decryptedFileStream.write(c);
                }
            } else if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("Encryption message doesn't contain literal data but signed data");
            } else {
                throw new PGPException("Unknown type of message");
            }
            if (pgpPublicKeyEncryptedData.isIntegrityProtected() && !pgpPublicKeyEncryptedData.verify()) {
                throw new PGPException("Integrity check of message is failed");

            }
        } catch (IOException | PGPException e) {
            e.printStackTrace();
        }
    }

    private PGPPrivateKey findSecretKey(InputStream privateKeyStream, long keyID, char[] passphrase) {
        try {
            PGPSecretKeyRingCollection ringCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyStream), new JcaKeyFingerprintCalculator());
            PGPSecretKey pgpSecretKey = ringCollection.getSecretKey(keyID);
            if (pgpSecretKey == null) {
                return null;
            }
            PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").setProvider("BC").build()).setProvider("BC").build(passphrase);
            return pgpSecretKey.extractPrivateKey(decryptor);
        } catch (PGPException | IOException e) {
            log.error("Exception occurred while finding the secret key: {}", e.getMessage(), e);
        }
        return null;
    }

    private void encryptFile(String inputFileName, PGPPublicKey pgpPublicKey, OutputStream cipheredFileStream, boolean isArmored) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            if (isArmored) {
                cipheredFileStream = new ArmoredOutputStream(cipheredFileStream);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PGPCompressedDataGenerator compressor = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
                PGPUtil.writeFileToLiteralData(compressor.open(byteArrayOutputStream), PGPLiteralData.BINARY, new File(inputFileName));
                compressor.close();
                JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                        .setWithIntegrityPacket(true)
                        .setSecureRandom(new SecureRandom())
                        .setProvider("BC");
                PGPEncryptedDataGenerator generator = new PGPEncryptedDataGenerator(encryptorBuilder);
                JcePublicKeyKeyEncryptionMethodGenerator encryptionMethodGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(pgpPublicKey)
                        .setProvider(new BouncyCastleProvider())
                        .setSecureRandom(new SecureRandom());
                generator.addMethod(encryptionMethodGenerator);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                OutputStream outputStream = generator.open(cipheredFileStream, bytes.length);
                outputStream.write(bytes);
                outputStream.close();
                cipheredFileStream.close();
            }
        } catch (IOException | PGPException e) {
            log.error("Exception occurred while encrypting the file: {}", e.getMessage(), e);
        }
    }

    private PGPPublicKey readPublicKey(InputStream publicKeyStream) {
        try {
            publicKeyStream = PGPUtil.getDecoderStream(publicKeyStream);
            PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(publicKeyStream, new JcaKeyFingerprintCalculator());
            PGPPublicKey pgpPublicKey = null;
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
            return pgpPublicKey;
        } catch (IOException | PGPException e) {
            log.error("Exception occurred while reading public key: {}", e.getMessage(), e);
        }
        return null;
    }
}
