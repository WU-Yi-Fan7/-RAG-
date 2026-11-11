// GenerateTestData.java
package cn.edu.hubu.hubuactivities.temp;

import cn.edu.hubu.hubuactivities.utils.PasswordUtils;
import java.time.LocalDateTime;

public class GenerateTestData {
    public static void main(String[] args) {
        System.out.println("========== 生成测试数据 SQL ==========\n");

        // 定义测试数据
        Object[][] users = {
                // 学号, 密码, 盐, 姓名, 手机号, 院系, 专业, 年级, 政治面貌, 角色, 诚信分, 说明
                {"20230000000000000000", "admin123", "admin-salt-123", "系统管理员", "13800000000", "系统管理", "系统管理", "2023", "党员", "admin", 100, "管理员"},
                {"20230123456789012345", "user12345", "user1-salt-123", "张三", "13800138001", "计算机与信息工程学院", "计算机科学与技术", "2023", "团员", "user", 100, "普通用户"},
                {"20240123456789012345", "test1234", "user2-salt-123", "李四", "13800138002", "电子与通信工程学院", "电子信息", "2024", "群众", "user", 98, "普通用户"},
                {"20230234567890123456", "wang1234", "user3-salt-123", "王五", "13800138003", "商学院", "金融学", "2023", "群众", "user", 95, "普通用户"},
                {"20230567890123456789", "hubei123", "black-salt-123", "赵六", "13800138004", "物理与电子科学学院", "物理学", "2025", "群众", "user", 75, "黑名单用户"}
        };

        System.out.println("-- 插入测试数据\n");

        for (Object[] user : users) {
            String stuId = (String) user[0];
            String password = (String) user[1];
            String salt = (String) user[2];
            String realName = (String) user[3];
            String phone = (String) user[4];
            String department = (String) user[5];
            String major = (String) user[6];
            String grade = (String) user[7];
            String politicalStatus = (String) user[8];
            String role = (String) user[9];
            int credit = (int) user[10];
            String desc = (String) user[11];

            // 使用当前后端的加密算法生成密码
            String encodedPassword = PasswordUtils.encode(password, salt);

            System.out.println("-- " + desc + "：" + realName + "（密码：" + password + "）");
            System.out.println("INSERT INTO student (");
            System.out.println("    stu_id, password, salt, real_name, phone,");
            System.out.println("    school_name, department, major, grade, political_status,");
            System.out.println("    credit, signed_count, absent_count, is_black, role, status, create_time");
            System.out.println(") VALUES (");
            System.out.println("    '" + stuId + "',");
            System.out.println("    '" + encodedPassword + "',  -- 加密后密码");
            System.out.println("    '" + salt + "',");
            System.out.println("    '" + realName + "',");
            System.out.println("    '" + phone + "',");
            System.out.println("    '湖北大学',");
            System.out.println("    '" + department + "',");
            System.out.println("    '" + major + "',");
            System.out.println("    '" + grade + "',");
            System.out.println("    '" + politicalStatus + "',");
            System.out.println("    " + credit + ",");
            System.out.println("    0, 0, " + (role.equals("admin") ? 0 : (credit < 80 ? 1 : 0)) + ", ");
            System.out.println("    '" + role + "', 1, NOW()");
            System.out.println(");\n");
        }

        System.out.println("-- 查询验证");
        System.out.println("SELECT ");
        System.out.println("    stu_id as '学号',");
        System.out.println("    password as '加密密码',");
        System.out.println("    salt as '盐值',");
        System.out.println("    real_name as '姓名',");
        System.out.println("    role as '角色',");
        System.out.println("    credit as '诚信分',");
        System.out.println("    CASE WHEN is_black = 1 THEN '是' ELSE '否' END as '黑名单'");
        System.out.println("FROM student;");

        System.out.println("\n\n========== 密码对照表 ==========");
        System.out.println("学号\t\t\t\t\t\t密码\t\t加密密码");
        System.out.println("------------------------------------------------------------------");
        for (Object[] user : users) {
            String stuId = (String) user[0];
            String password = (String) user[1];
            String salt = (String) user[2];
            String realName = (String) user[3];
            String role = (String) user[9];
            String encodedPassword = PasswordUtils.encode(password, salt);
            System.out.println(stuId + "\t" + password + "\t" + role + "\t" + encodedPassword);
        }
    }
}