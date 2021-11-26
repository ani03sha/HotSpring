package org.redquark.hotspring.fileprocessor.services.crypto;

public interface CustomKeyPairGenerator {

    /**
     * Generates private and public key pair
     *
     * @param privateKey - path where private key will be stored
     * @param publicKey  - path where public key will be stored
     * @param isArmored  - flag to represent if the reader for Base64 armored objects
     * @param username   - username for the keypair
     * @param password   - password for the keypair
     * @param keySize    - size of the key
     */
    void generateKeyPair(
            String privateKey,
            String publicKey,
            boolean isArmored,
            String username,
            String password,
            int keySize
    );
}
