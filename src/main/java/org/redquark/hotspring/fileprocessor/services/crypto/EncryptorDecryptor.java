package org.redquark.hotspring.fileprocessor.services.crypto;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface EncryptorDecryptor {

    byte[] encrypt(
            File inputFile,
            InputStream publicKeyStream,
            OutputStream cipheredFileStream,
            boolean isArmored
    );

    byte[] decrypt(
            InputStream cipheredFileStream,
            OutputStream decryptedFileStream,
            InputStream privateKeyStream,
            char[] passphrase
    );
}
