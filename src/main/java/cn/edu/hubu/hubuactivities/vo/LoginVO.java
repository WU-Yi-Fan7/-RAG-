package cn.edu.hubu.hubuactivities.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginVO {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^202\\d{17}$", message = "学号必须是20位，以202开头")
    private String stu_id;  // 改为 stu_id，和其他实体类一致

    @NotBlank(message = "密码不能为空")
    private String password;
}