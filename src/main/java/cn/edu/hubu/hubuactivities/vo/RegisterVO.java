package cn.edu.hubu.hubuactivities.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegisterVO {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^202\\d{17}$", message = "学号必须是20位，以202开头")
    private String stu_id;

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[\u4e00-\u9fa5]{2,10}$", message = "姓名必须是2-10个汉字")
    private String real_name;

    @NotBlank(message = "院校不能为空")
    private String school_name;

    @NotBlank(message = "院系不能为空")
    private String department;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String political_status;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String grade;  // 年级，由学号前4位自动生成
}