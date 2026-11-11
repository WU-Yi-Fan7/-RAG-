package cn.edu.hubu.hubuactivities.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;  // ✅ 新增 import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()  // ✅ 关键：启用 CORS
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // ✅ 关键：放行所有 OPTIONS 预检请求
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 登录和注册接口放行
                .antMatchers("/auth/login", "/auth/register", "/auth/test").permitAll()
                // 活动相关接口需要认证
                .antMatchers("/activity/**").authenticated()
                // 用户相关接口需要认证
                .antMatchers("/user/**").authenticated()
                // 其他所有请求也需要认证
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}