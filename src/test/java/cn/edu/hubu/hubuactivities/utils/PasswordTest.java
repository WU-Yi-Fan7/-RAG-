package cn.edu.hubu.hubuactivities.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PasswordTest {

    @Test
    public void testPassword() {
        // 测试管理员密码
        String password = "admin123";
        String salt = "admin-salt-123";
        String encoded = encode(password, salt);
        System.out.println("========== 密码测试 ==========");
        System.out.println("密码: " + password);
        System.out.println("盐值: " + salt);
        System.out.println("加密后: " + encoded);
        System.out.println("应该等于: 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
        System.out.println("匹配: " + encoded.equals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"));
        System.out.println("==============================");
    }

    private String encode(String password, String salt) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hash = digest.digest(password.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}