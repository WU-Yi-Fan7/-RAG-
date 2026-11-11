package cn.edu.hubu.hubuactivities.controller;

import cn.edu.hubu.hubuactivities.service.AuthService;
import cn.edu.hubu.hubuactivities.vo.LoginVO;
import cn.edu.hubu.hubuactivities.vo.RegisterVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public ResultVO<?> login(@Valid @RequestBody LoginVO loginVO,
                             HttpServletRequest request) {
        log.info("收到登录请求，学号: {}", loginVO.getStu_id());
        return authService.login(loginVO, request);
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public ResultVO<?> register(@Valid @RequestBody RegisterVO registerVO) {
        log.info("收到注册请求，学号: {}, 姓名: {}", registerVO.getStu_id(), registerVO.getReal_name());
        return authService.register(registerVO);
    }

    /**
     * 刷新token接口
     */
    @PostMapping("/refresh")
    public ResultVO<?> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("收到刷新token请求");
        return authService.refreshToken(refreshToken);
    }

    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    public ResultVO<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        log.info("收到退出登录请求");
        return authService.logout(token);
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public String test() {
        return "AuthController is working!";
    }
}