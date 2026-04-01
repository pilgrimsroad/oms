package com.dbass.oms.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/token",
            "/api/auth/login",
            "/api/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/h2-console"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            sendJsonErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "유효한 JWT 토큰이 필요합니다.", request.getRequestURI());
            return;
        }

        Claims claims = jwtTokenProvider.parseClaims(token);
        String serviceId = claims.getSubject();
        String serviceUrl = claims.get("userUrl", String.class);
        String serviceType = claims.get("userType", String.class);

        if (!isAccessAllowed(serviceType, request.getRequestURI())) {
            sendJsonErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "해당 서비스 타입으로는 이 API에 접근할 수 없습니다.", request.getRequestURI());
            return;
        }

        var principal = new UserPrincipal(serviceId, serviceUrl, serviceType);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private boolean isAccessAllowed(String serviceType, String requestURI) {
        if ("1".equals(serviceType)) {
            return true;
        }
        if ("2".equals(serviceType)) {
            List<String> allowedPathsForWeb = Arrays.asList(
                    "/api/auth",
                    "/api/health",
                    "/api/messages"
            );
            return allowedPathsForWeb.stream().anyMatch(requestURI::startsWith);
        }
        return false;
    }

    private void sendJsonErrorResponse(HttpServletResponse response, int status, String errorMessage, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("path", path);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
