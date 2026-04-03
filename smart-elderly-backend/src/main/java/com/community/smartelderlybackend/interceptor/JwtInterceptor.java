package com.community.smartelderlybackend.interceptor;

import com.community.smartelderlybackend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行前端跨域的 OPTIONS 预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 2. 获取请求头里的 Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 3. 校验 Token
        try {
            if (token != null) {
                Claims claims = JwtUtils.parseToken(token);
                // 把解析出的信息挂在 request 上，后面的代码随时可以取
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("role", claims.get("role"));
                return true; // 校验通过，放行
            }
        } catch (Exception e) {
            // Token 过期或被篡改，走到底部拦截
        }

        // 4. 拦截打回
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期，请重新登录\"}");
        return false;
    }
}