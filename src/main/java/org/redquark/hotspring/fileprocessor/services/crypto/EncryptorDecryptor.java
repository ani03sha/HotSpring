package org.redquark.hotspring.fileprocessor.services.crypto;

public interface EncryptorDecryptor {

    void encrypt(
            String publicKeyPath,
            String cipheredFileLocation,
            String inputFileName,
            boolean isArmored
    );

    void decrypt(
            String cipheredFileLocation,
            String privateKeyPath,
            String decryptedFileLocation,
            String password
    );
}
