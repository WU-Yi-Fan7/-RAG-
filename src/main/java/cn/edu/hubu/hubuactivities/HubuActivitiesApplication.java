package cn.edu.hubu.hubuactivities;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.edu.hubu.hubuactivities.mapper")  // 添加这行，扫描mapper包
public class HubuActivitiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubuActivitiesApplication.class, args);
    }
}