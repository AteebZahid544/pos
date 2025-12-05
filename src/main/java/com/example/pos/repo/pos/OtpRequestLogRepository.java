package com.example.pos.repo.pos;

import com.example.pos.entity.pos.OtpRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRequestLogRepository extends JpaRepository<OtpRequestLog, Long> {

    // Count OTP requests in last 1 minute
    @Query("SELECT COUNT(o) FROM OtpRequestLog o WHERE o.email = :email AND o.requestTime >= :after")
    int countRecentRequests(@Param("email") String email, @Param("after") LocalDateTime after);

    // Get last request time
    Optional<OtpRequestLog> findTopByEmailOrderByRequestTimeDesc(String email);
}

