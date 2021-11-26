package org.redquark.hotspring.fileprocessor.config;

import lombok.Data;

@Data
public class CryptoConfig {

    private KeyPair keyPair;
    private File file;

    @Data
    public static class KeyPair {
        private String privateKeyPath;
        private String publicKeyPath;
        private String username;
        private String password;
        private boolean isArmored;
        private int keySize;
    }

    @Data
    public static class File {
        private String inputFileLocation;
        private String zippedFileLocation;
        private String cipheredFileLocation;
        private String decipheredFileLocation;
        private String unzippedFileLocation;
    }
}
