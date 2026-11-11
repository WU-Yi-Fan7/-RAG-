package cn.edu.hubu.hubuactivities.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    // 生成随机盐
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // 密码加密 - 修复版
    public static String encode(String password, String salt) {
        try {
            // 每次调用都创建新的 MessageDigest 实例
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // 明确指定 UTF-8 编码
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    // 验证密码
    public static boolean matches(String rawPassword, String encodedPassword, String salt) {
        String newEncoded = encode(rawPassword, salt);
        return newEncoded.equals(encodedPassword);
    }

    // 字节转十六进制
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 测试方法
    public static void main(String[] args) {
        // 测试管理员密码
        String password = "admin123";
        String salt = "admin-salt-123";
        String expected = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

        String encoded = encode(password, salt);
        System.out.println("密码: " + password);
        System.out.println("盐值: " + salt);
        System.out.println("加密结果: " + encoded);
        System.out.println("预期结果: " + expected);
        System.out.println("是否匹配: " + encoded.equals(expected));
    }
}