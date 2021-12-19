package org.redquark.hotspring.uploader.configs;

import lombok.Data;

@Data
public class CryptoConfig {

    private String keyPath;
    private String username;
    private String password;
    private Boolean isArmored;
    private Integer keySize;
    private String algorithm;
    private String provider;
}
