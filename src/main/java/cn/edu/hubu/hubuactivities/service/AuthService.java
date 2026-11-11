package cn.edu.hubu.hubuactivities.service;

import cn.edu.hubu.hubuactivities.vo.LoginVO;
import cn.edu.hubu.hubuactivities.vo.RegisterVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import javax.servlet.http.HttpServletRequest;

public interface AuthService {
    // 登录
    ResultVO<?> login(LoginVO loginVO, HttpServletRequest request);

    // 注册
    ResultVO<?> register(RegisterVO registerVO);

    // 刷新token
    ResultVO<?> refreshToken(String refreshToken);

    // 退出登录
    ResultVO<?> logout(String token);
}