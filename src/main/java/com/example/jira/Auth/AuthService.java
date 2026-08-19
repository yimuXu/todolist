package com.example.jira.Auth;
import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public String signup(SignupRequest signuprequest) {
        String username = signuprequest.getUsername();
        String password = signuprequest.getPassword();
        String email = signuprequest.getEmail();
//        rule1 no empty field

        if (username == null || password == null || email == null) {
            return "ERROR: All fields are required";
        }
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return "ERROR: Username is already taken";
        }
        //   here add new normal user not manager
        User newuser = new User(username, password, email);
        newuser.setPassword(passwordEncoder.encode(newuser.getPassword()));
        userRepository.save(newuser);
        return "sign up successful!";
    }

    public String login(LoginRequest loginrequest) {
        String username = loginrequest.getUsername();
        String password = loginrequest.getPassword();
        if (username == null || password == null) {
            return "ERROR: All fields are required";
        }
        return userRepository.findByUsername(username).map(user
        ->{if (passwordEncoder.matches(password,user.getPassword())) {return jwtUtil.generateToken(username);}
            else{return "ERROR: password and username do not match";}}).orElse("ERROR: username not found");


    }






}
