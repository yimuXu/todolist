package com.example.jira.Auth;

public class SignupRequest {
    private String password;
    private String email;
    private String code;

    public SignupRequest(String password, String email) {
        this.password = password;
        this.email = email;
    }

    public SignupRequest() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
