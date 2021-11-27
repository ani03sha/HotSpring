package org.redquark.hotspring.fileprocessor.config;

import lombok.Data;

@Data
public class CryptoConfig {

    private KeyPairConfig keyPairConfig;
    private FileConfig fileConfig;

    @Data
    public static class KeyPairConfig {
        private String privateKeyPath;
        private String publicKeyPath;
        private String username;
        private String password;
        private Boolean isArmored;
        private Integer keySize;
    }

    @Data
    public static class FileConfig {
        private String inputFileLocation;
        private String zippedFileLocation;
        private String cipheredFileLocation;
        private String decipheredFileLocation;
        private String s3RetrievedFileLocation;
        private String unzippedFileLocation;
    }
}
