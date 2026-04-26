package com.community.smartelderlybackend.interceptor;

import com.community.smartelderlybackend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Pattern USER_PROFILE_PATH = Pattern.compile("^/api/user/(\\d+)$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            if (token != null) {
                Claims claims = JwtUtils.parseToken(token);
                Long userId = toLong(claims.get("userId"));
                Integer role = toInt(claims.get("role"));
                request.setAttribute("userId", userId);
                request.setAttribute("role", role);

                String path = request.getServletPath();
                String method = request.getMethod();

                if (!isApiAllowed(path, method, userId, role)) {
                    writeJson(response, 403, "无权限访问该接口");
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            // fall through to 401
        }

        writeJson(response, 401, "未登录或Token已过期，请重新登录");
        return false;
    }

    private static void writeJson(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":" + httpStatus + ",\"message\":\"" + escaped + "\"}");
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    /**
     * 按路径前缀限制角色，防止越权调用接口。
     */
    private static boolean isApiAllowed(String path, String method, Long tokenUserId, Integer role) {
        if (role == null || tokenUserId == null) {
            return false;
        }

        Matcher m = USER_PROFILE_PATH.matcher(path);
        if ("GET".equalsIgnoreCase(method) && m.matches()) {
            long pathUserId = Long.parseLong(m.group(1));
            return pathUserId == tokenUserId;
        }

        if (path.startsWith("/api/user/testToken")) {
            return true;
        }

        if (path.startsWith("/api/doctor/")) {
            return role == 2;
        }
        if (path.startsWith("/api/community/")) {
            return role == 3;
        }
        if (path.startsWith("/api/family/")) {
            return role == 1;
        }
        if (path.startsWith("/api/elderly/")) {
            return role == 0;
        }
        if (path.startsWith("/api/health/")) {
            return role == 1 || role == 2;
        }
        if (path.startsWith("/api/message/")) {
            return role == 1;
        }

        return false;
    }
}
