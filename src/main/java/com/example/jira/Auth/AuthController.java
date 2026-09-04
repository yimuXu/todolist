package com.example.jira.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    @Autowired
    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginrequest){
        String result = authService.login(loginrequest);
        if (result.startsWith("ERROR")) {
            // "email not found" is surfaced as its own code so the frontend can offer to
            // send the user straight to signup instead of just showing a generic error.
            String code = result.contains("email not found") ? "EMAIL_NOT_FOUND" : "INVALID_CREDENTIALS";
            return ResponseEntity.badRequest().body(Map.of("error", result, "code", code));
        }
        return ResponseEntity.ok(Map.of("token", result));
    }

    // Signup is a two-step, OTP-verified flow: request a code, then verify it together with
    // the chosen password to actually create the account. There is no direct/unverified signup.
    @PostMapping("/signup/request")
    public ResponseEntity<Map<String, String>> requestSignup(@RequestBody OtpRequest request){
        String result = otpService.requestSignupOtp(request.getEmail());
        if (result.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<Map<String, String>> verifySignup(@RequestBody SignupRequest request){
        String result = otpService.verifySignupOtp(request.getEmail(), request.getCode(), request.getPassword());
        if (result.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
        return ResponseEntity.ok(Map.of("token", result));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody OtpRequest request){
        String result = otpService.requestLoginOtp(request.getEmail());
        if (result.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody OtpVerifyRequest request){
        String result = otpService.verifyLoginOtp(request.getEmail(), request.getCode());
        if (result.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
        return ResponseEntity.ok(Map.of("token", result));
    }

}
