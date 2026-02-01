package com.example.pos.config;

import com.example.pos.entity.central.Session;
import com.example.pos.repo.central.SessionRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Transactional
public class SessionInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionRepo sessionRepo;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            return true; // public endpoints
        }

        Optional<Session> sessionOpt = sessionRepo.findByToken(token);

        if (sessionOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Session session = sessionOpt.get();

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepo.delete(session);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // ✅ EXTEND SESSION
        session.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        sessionRepo.save(session);

        return true;
    }
}



