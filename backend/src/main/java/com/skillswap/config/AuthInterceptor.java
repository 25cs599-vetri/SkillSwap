package com.skillswap.config;

import com.skillswap.service.CurrentStudent;
import com.skillswap.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenService tokens;
    private final CurrentStudent currentStudent;

    public AuthInterceptor(TokenService tokens, CurrentStudent currentStudent) {
        this.tokens = tokens;
        this.currentStudent = currentStudent;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long id = tokens.getStudentId(header.substring(7));
            if (id != null) currentStudent.setId(id);
        }
        return true;
    }
}
