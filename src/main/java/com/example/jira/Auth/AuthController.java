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
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    // send sign up request to service
    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest signuprequest){
        return authService.signup(signuprequest);
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginrequest){
        String result = authService.login(loginrequest);
        if (result.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(Map.of("error", result));
        }
        return ResponseEntity.ok(Map.of("token", result));
    }


}
