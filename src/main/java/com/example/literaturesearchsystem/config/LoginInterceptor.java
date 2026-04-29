package com.example.literaturesearchsystem.config;

import com.example.literaturesearchsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String path = request.getRequestURI();

        // 放行登录、注册、搜索、搜索建议、文献列表等公开接口
        if (path.contains("/user/login") || path.contains("/user/register") ||
                path.contains("/literature/search") || path.contains("/literature/list") ||
                path.contains("/search/suggest") || path.contains("/search/test") ||
                path.contains("/literature/") && !path.contains("/add") && !path.contains("/update") && !path.contains("/delete")) {
            return true;
        }

        // 获取 token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        return true;
    }
}