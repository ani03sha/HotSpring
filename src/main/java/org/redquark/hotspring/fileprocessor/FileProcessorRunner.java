package org.redquark.hotspring.fileprocessor;

import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.redquark.hotspring.fileprocessor.config.AWSConfig;
import org.redquark.hotspring.fileprocessor.config.CryptoConfig;
import org.redquark.hotspring.fileprocessor.domains.UnzippedFile;
import org.redquark.hotspring.fileprocessor.services.archiver.ZipService;
import org.redquark.hotspring.fileprocessor.services.crypto.CustomKeyPairGenerator;
import org.redquark.hotspring.fileprocessor.services.crypto.EncryptorDecryptor;
import org.redquark.hotspring.fileprocessor.services.s3.S3StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.redquark.hotspring.fileprocessor.config.CryptoConfig.FileConfig;
import static org.redquark.hotspring.fileprocessor.config.CryptoConfig.KeyPairConfig;

@Component
@Slf4j
@ConditionalOnProperty(value = "app.mode", havingValue = "cmd")
public class FileProcessorRunner implements CommandLineRunner {

    private static final String ARCHIVED_FILE = ".archived.zip";

    private final ZipService zipService;
    private final CustomKeyPairGenerator customKeyPairGenerator;
    private final EncryptorDecryptor encryptorDecryptor;
    private final S3StorageService s3StorageService;

    private final AWSConfig awsConfig;

    private final String publicKeyPath;
    private final String privateKeyPath;
    private final String username;
    private final String password;
    private final boolean isArmored;
    private final int keySize;
    private final String inputFileLocation;
    private final String zippedFileLocation;
    private final String cipheredFileLocation;
    private final String decipheredFileLocation;
    private final String unzippedFileLocation;
    private final String s3RetrievedFileLocation;

    public FileProcessorRunner(
            ZipService zipService,
            CustomKeyPairGenerator customKeyPairGenerator,
            EncryptorDecryptor encryptorDecryptor,
            S3StorageService s3StorageService,
            CryptoConfig cryptoConfig, AWSConfig awsConfig
    ) {
        this.zipService = zipService;
        this.customKeyPairGenerator = customKeyPairGenerator;
        this.encryptorDecryptor = encryptorDecryptor;
        this.s3StorageService = s3StorageService;
        this.awsConfig = awsConfig;
        KeyPairConfig keyPairConfig = cryptoConfig.getKeyPairConfig();
        FileConfig fileConfig = cryptoConfig.getFileConfig();
        this.publicKeyPath = keyPairConfig.getPublicKeyPath();
        this.privateKeyPath = keyPairConfig.getPrivateKeyPath();
        this.username = keyPairConfig.getUsername();
        this.password = keyPairConfig.getPassword();
        this.isArmored = keyPairConfig.getIsArmored();
        this.keySize = keyPairConfig.getKeySize();
        this.inputFileLocation = fileConfig.getInputFileLocation();
        this.zippedFileLocation = fileConfig.getZippedFileLocation();
        this.cipheredFileLocation = fileConfig.getCipheredFileLocation();
        this.decipheredFileLocation = fileConfig.getDecipheredFileLocation();
        this.unzippedFileLocation = fileConfig.getUnzippedFileLocation();
        this.s3RetrievedFileLocation = fileConfig.getS3RetrievedFileLocation();
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Key generation starts");
        customKeyPairGenerator.generateKeyPair(privateKeyPath, publicKeyPath, isArmored, username, password, keySize);
        log.info("Key generation ends");
        log.info("Zipping starts");
        String key = UUID.randomUUID() + ARCHIVED_FILE;
        File inputFile = new File(inputFileLocation);
        byte[] zipBytes = zipFile(zippedFileLocation, inputFile);
        String zippedFileName = zippedFileLocation + File.separator + key;
        OutputStream zipOutputStream = new FileOutputStream(zippedFileName);
        zipOutputStream.write(zipBytes);
        log.info("Zipping ends");
        log.info("Encryption starts");
        byte[] cipheredBytes = encryptFile(new File(zippedFileName), publicKeyPath, cipheredFileLocation, isArmored);
        OutputStream cipheredOutputStream = new FileOutputStream(zippedFileName + ".pgp");
        if (cipheredBytes != null) {
            cipheredOutputStream.write(cipheredBytes);
        }
        log.info("Encryption ends");
        log.info("S3 upload starts");
        Map<String, String> optionalMetadata = new HashMap<>();
        optionalMetadata.put("addedBy", "Anirudh Sharma");
        String versionID = uploadToS3(awsConfig.getBucket(), key + ".pgp", optionalMetadata, new ByteArrayInputStream(Objects.requireNonNull(cipheredBytes)));
        log.info("Uploaded file to S3 with version id: {}", versionID);
        log.info("S3 upload ends");
        log.info("Retrieval starts");
        byte[] cipheredBytesFromS3 = retrieveFromS3(awsConfig.getBucket(), key + ".pgp");
        String s3RetrievedFileName = s3RetrievedFileLocation + File.separator + key + ".pgp";
        OutputStream s3FileOutputStream = new FileOutputStream(s3RetrievedFileName);
        if (cipheredBytesFromS3 != null) {
            s3FileOutputStream.write(cipheredBytesFromS3);
        }
        log.info("Retrieval ends");
        String decryptedFileName = decipheredFileLocation + File.separator + key;
        File s3File = new File(s3RetrievedFileName);
        InputStream s3FileStream = FileUtils.openInputStream(s3File);
        byte[] decryptedBytes = decryptFile(privateKeyPath, decryptedFileName, s3FileStream);
        OutputStream decryptedFileOutputStream = new FileOutputStream(decryptedFileName);
        if (decryptedBytes != null) {
            decryptedFileOutputStream.write(decryptedBytes);
        }
        log.info("Decryption ends");
        log.info("Unzipping starts");
        List<UnzippedFile> unzippedFiles = unzipFile(new ByteArrayInputStream(Objects.requireNonNull(decryptedBytes)));
        for (UnzippedFile unzippedFile : unzippedFiles) {
            OutputStream os = new FileOutputStream(unzippedFileLocation + File.separator + unzippedFile.getName());
            os.write(unzippedFile.getBytes());
        }
        log.info("Unzipping ends");
    }

    private byte[] zipFile(String destinationDirectory, File inputFile) {
        return zipService.zip(destinationDirectory, inputFile);
    }

    private List<UnzippedFile> unzipFile(InputStream zippedIs) {
        return zipService.unzip(zippedIs);
    }

    private byte[] encryptFile(File file, String publicKeyPath, String cipheredFileLocation, boolean isArmored) {
        try (
                InputStream publicKey = new FileInputStream(publicKeyPath);
                FileOutputStream cipheredFile = new FileOutputStream(cipheredFileLocation + File.separator + file.getName() + ".pgp")
        ) {
            encryptorDecryptor.encrypt(file, publicKey, cipheredFile, isArmored);
            return IOUtils.toByteArray(new FileInputStream(cipheredFileLocation + File.separator + file.getName() + ".pgp"));
        } catch (IOException e) {
            log.error("Exception occurred while encrypting the file: {}", e.getMessage(), e);
        }
        return null;
    }

    private byte[] decryptFile(String privateKeyPath, String decipheredFileLocation, InputStream cipheredFileStream) {
        try (
                InputStream privateKeyStream = new FileInputStream(privateKeyPath);
                FileOutputStream decipheredFileStream = new FileOutputStream(decipheredFileLocation)) {
            return encryptorDecryptor.decrypt(
                    cipheredFileStream,
                    decipheredFileStream,
                    privateKeyStream,
                    password.toCharArray()
            );
        } catch (IOException e) {
            log.error("Exception occurred while decrypting file: {}", e.getMessage(), e);
        }
        return null;
    }

    private String uploadToS3(String bucket, String key, Map<String, String> optionalMetadata, InputStream cipheredFileStream) {
        PutObjectResult putObjectResult = s3StorageService.upload(bucket, key, optionalMetadata, cipheredFileStream);
        return putObjectResult.getVersionId();
    }

    private byte[] retrieveFromS3(String bucket, String encryptedFileName) {
        return s3StorageService.retrieve(bucket, encryptedFileName);
    }
}
