package com.example.jira.Auth;
import com.example.jira.User.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(LoginRequest loginrequest) {
        String email = loginrequest.getEmail();
        String password = loginrequest.getPassword();
        if (email == null || password == null) {
            return "ERROR: All fields are required";
        }
        return userRepository.findByEmail(email).map(user
        ->{if (passwordEncoder.matches(password,user.getPassword())) {return jwtUtil.generateToken(email);}
            else{return "ERROR: password and email do not match";}}).orElse("ERROR: email not found");


    }






}
