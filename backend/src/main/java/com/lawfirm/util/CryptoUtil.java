package com.lawfirm.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于敏感字段（手机号、邮箱等）的加密存储
 */
@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${crypto.secret-key:lawfirm-secret-key-2024-aes-256-encryption}")
    private String secretKeyStr;

    private SecretKey secretKey;

    /**
     * 获取密钥（从配置的密钥字符串派生）
     */
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            try {
                // 使用SHA-256将字符串密钥转换为固定长度的密钥
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = sha.digest(secretKeyStr.getBytes(StandardCharsets.UTF_8));
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            } catch (Exception e) {
                log.error("生成密钥失败", e);
                throw new RuntimeException("加密密钥初始化失败", e);
            }
        }
        return secretKey;
    }

    /**
     * 生成随机IV
     */
    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 加密字符串
     * @param plainText 明文
     * @return Base64编码的密文（包含IV）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 生成随机IV
            byte[] iv = generateIv();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文合并：IV(12字节) + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            // 返回Base64编码的结果
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /**
     * 解密字符串
     * @param encryptedText Base64编码的密文（包含IV）
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 解码Base64
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // 分离IV和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据解密失败", e);
        }
    }

    /**
     * 生成新的AES密钥（用于初始化系统时生成密钥）
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            log.error("生成新密钥失败", e);
            throw new RuntimeException("生成加密密钥失败", e);
        }
    }

    /**
     * 验证加密功能是否正常工作
     */
    public boolean testEncryption() {
        try {
            String testText = "test123456";
            String encrypted = encrypt(testText);
            String decrypted = decrypt(encrypted);
            return testText.equals(decrypted);
        } catch (Exception e) {
            log.error("加密测试失败", e);
            return false;
        }
    }
}
