package com.example.jira.Auth;

import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {
    private static final int EXPIRY_MINUTES = 10;
    private final SecureRandom random = new SecureRandom();

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final String mailFrom;

    public OtpService(EmailOtpRepository otpRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil, JavaMailSender mailSender,
                       @Value("${mail.from}") String mailFrom) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    // ------------------------------ login OTP ------------------------------

    public String requestLoginOtp(String email) {
        if (email == null || email.isBlank()) {
            return "ERROR: Email is required";
        }
        if (userRepository.findByEmail(email).isEmpty()) {
            return "ERROR: No account with that email";
        }
        return send(email, OtpPurpose.LOGIN, "Your login code is ");
    }

    public String verifyLoginOtp(String email, String code) {
        if (email == null || code == null) {
            return "ERROR: All fields are required";
        }
        return consume(email, code, OtpPurpose.LOGIN)
                .map(otp -> jwtUtil.generateToken(email))
                .orElse("ERROR: Invalid or expired code");
    }

    // ------------------------------ signup OTP ------------------------------

    /** A signup code is only useful before the account exists, so it does not leak whether an email is already registered beyond what signup itself already reveals. */
    public String requestSignupOtp(String email) {
        if (email == null || email.isBlank()) {
            return "ERROR: Email is required";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return "ERROR: Email is already taken";
        }
        return send(email, OtpPurpose.SIGNUP, "Your sign up verification code is ");
    }

    /** Verifies the signup code and creates the account in the same step, so an email is never registered without proving it can receive mail there. */
    public String verifySignupOtp(String email, String code, String password) {
        if (email == null || code == null || password == null) {
            return "ERROR: All fields are required";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return "ERROR: Email is already taken";
        }
        Optional<EmailOtp> otp = consume(email, code, OtpPurpose.SIGNUP);
        if (otp.isEmpty()) {
            return "ERROR: Invalid or expired code";
        }
        User newUser = new User(email, passwordEncoder.encode(password));
        userRepository.save(newUser);
        return jwtUtil.generateToken(email);
    }

    // ------------------------------ shared ------------------------------

    private String send(String email, OtpPurpose purpose, String messagePrefix) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        otpRepository.save(new EmailOtp(email, code, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES), purpose));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Your TaskFlow verification code");
        message.setText(messagePrefix + code + ". It expires in " + EXPIRY_MINUTES + " minutes.");
        mailSender.send(message);

        return "OTP sent";
    }

    private Optional<EmailOtp> consume(String email, String code, OtpPurpose purpose) {
        return otpRepository.findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(email, code, purpose)
                .filter(otp -> otp.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(otp -> {
                    otp.setUsed(true);
                    otpRepository.save(otp);
                    return otp;
                });
    }
}
