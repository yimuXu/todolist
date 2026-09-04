package com.example.jira.Auth;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class EmailOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String email;
    private String code;
    private LocalDateTime expiresAt;
    private boolean used;
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    public EmailOtp() {
    }

    public EmailOtp(String email, String code, LocalDateTime expiresAt, OtpPurpose purpose) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.used = false;
        this.purpose = purpose;
    }

    public int getId() {return id;}
    public String getEmail() {return email;}
    public String getCode() {return code;}
    public LocalDateTime getExpiresAt() {return expiresAt;}
    public boolean isUsed() {return used;}
    public void setUsed(boolean used) {this.used = used;}
    public OtpPurpose getPurpose() {return purpose;}
}
