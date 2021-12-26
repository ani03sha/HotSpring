package org.redquark.hotspring.document.configs;

import lombok.Data;

@Data
public class CryptoDownloaderConfig {

    private String keyPath;
    private String username;
    private String password;
    private Boolean isArmored;
    private Integer keySize;
    private String algorithm;
    private String provider;
}
