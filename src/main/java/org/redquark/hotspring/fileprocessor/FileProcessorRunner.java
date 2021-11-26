package org.redquark.hotspring.fileprocessor;

import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.fileprocessor.config.AWSConfig;
import org.redquark.hotspring.fileprocessor.config.CryptoConfig;
import org.redquark.hotspring.fileprocessor.services.archiver.ZipService;
import org.redquark.hotspring.fileprocessor.services.crypto.CustomKeyPairGenerator;
import org.redquark.hotspring.fileprocessor.services.crypto.EncryptorDecryptor;
import org.redquark.hotspring.fileprocessor.services.s3.S3StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class FileProcessorRunner implements CommandLineRunner {

    private final ZipService zipService;
    private final CustomKeyPairGenerator customKeyPairGenerator;
    private final EncryptorDecryptor encryptorDecryptor;
    private final S3StorageService s3StorageService;

    private final CryptoConfig cryptoConfig;
    private final AWSConfig awsConfig;

    public FileProcessorRunner(
            ZipService zipService,
            CustomKeyPairGenerator customKeyPairGenerator,
            EncryptorDecryptor encryptorDecryptor,
            S3StorageService s3StorageService,
            CryptoConfig cryptoConfig, AWSConfig awsConfig) {
        this.zipService = zipService;
        this.customKeyPairGenerator = customKeyPairGenerator;
        this.encryptorDecryptor = encryptorDecryptor;
        this.s3StorageService = s3StorageService;
        this.cryptoConfig = cryptoConfig;
        this.awsConfig = awsConfig;
    }

    @Override
    public void run(String... args) throws FileNotFoundException {
        zipService.zip(new File(cryptoConfig.getFile().getInputFileLocation()), cryptoConfig.getFile().getZippedFileLocation() + File.separator + "archive.zip");
        customKeyPairGenerator.generateKeyPair(
                cryptoConfig.getKeyPair().getPrivateKeyPath(),
                cryptoConfig.getKeyPair().getPublicKeyPath(),
                cryptoConfig.getKeyPair().isArmored(),
                cryptoConfig.getKeyPair().getUsername(),
                cryptoConfig.getKeyPair().getPassword(),
                cryptoConfig.getKeyPair().getKeySize()
        );
        encryptorDecryptor.encrypt(
                cryptoConfig.getKeyPair().getPublicKeyPath(),
                cryptoConfig.getFile().getCipheredFileLocation() + "/archive.zip.pgp",
                cryptoConfig.getFile().getZippedFileLocation() + "/archive.zip",
                true);
        Map<String, String> metadata = new HashMap<>();
        s3StorageService.upload(awsConfig.getBucket(),
                "archive.zip.pgp",
                metadata,
                new FileInputStream(cryptoConfig.getFile().getCipheredFileLocation() + "/archive.zip.pgp"));
        byte[] s3ObjectBytes = s3StorageService.retrieve(awsConfig.getBucket(), "archive.zip.pgp");
        try (OutputStream s3ObjectStream = new FileOutputStream(awsConfig.getS3Retrieved() + File.separator + "archive.zip.pgp")) {
            s3ObjectStream.write(s3ObjectBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        encryptorDecryptor.decrypt(
                cryptoConfig.getFile().getCipheredFileLocation() + "/archive.zip.pgp",
                cryptoConfig.getKeyPair().getPrivateKeyPath(),
                cryptoConfig.getFile().getDecipheredFileLocation() + "/archive.zip",
                cryptoConfig.getKeyPair().getPassword());
        zipService.unzip(cryptoConfig.getFile().getDecipheredFileLocation() + "/archive.zip",
                cryptoConfig.getFile().getUnzippedFileLocation());
    }
}
