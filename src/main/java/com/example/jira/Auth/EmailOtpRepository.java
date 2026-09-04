package com.example.jira.Auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Integer> {
    Optional<EmailOtp> findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(String email, String code, OtpPurpose purpose);
}
