package cn.edu.hubu.hubuactivities.config;

import cn.edu.hubu.hubuactivities.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ✅ 新增：OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("OPTIONS 预检请求，直接放行");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // 1. 检查是否有 Authorization 头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("没有Authorization头，放行");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 提取 token
            String token = authHeader.substring(7);
            log.debug("提取到token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");

            // 3. 验证 token
            if (jwtUtils.validateToken(token)) {
                String stuId = jwtUtils.getStuIdFromToken(token);
                log.debug("token验证成功，学号: {}", stuId);

                // 4. 创建认证信息
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(stuId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. 设置到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("已设置认证信息");
            } else {
                log.warn("token验证失败");
            }
        } catch (Exception e) {
            log.error("JWT认证异常: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}