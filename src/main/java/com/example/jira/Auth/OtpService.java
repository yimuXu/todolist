package com.example.jira.Auth;

import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Sends OTP mail through Brevo's HTTPS transactional email API rather than SMTP. Several hosts
 * (Render's free tier among them) throttle or drop outbound SMTP (port 587) as an anti-abuse
 * measure, which left signup hanging indefinitely: the SMTP connect itself never returned. The
 * HTTPS API uses the same outbound path as every other external call this app makes (Canvas
 * included) and is not subject to that restriction.
 */
@Service
public class OtpService {
    private static final int EXPIRY_MINUTES = 10;
    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";
    private final SecureRandom random = new SecureRandom();

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestClient restClient;
    private final String mailFrom;
    private final String brevoApiKey;

    public OtpService(EmailOtpRepository otpRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RestClient.Builder restClientBuilder,
                       @Value("${mail.from}") String mailFrom, @Value("${brevo.api-key}") String brevoApiKey) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.restClient = restClientBuilder.build();
        this.mailFrom = mailFrom;
        this.brevoApiKey = brevoApiKey;
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
        // json style mapping
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", mailFrom),
                "to", java.util.List.of(Map.of("email", email)),
                "subject", "Your TaskFlow verification code",
                "textContent", messagePrefix + code + ". It expires in " + EXPIRY_MINUTES + " minutes."
        );
        restClient.post()
                .uri(BREVO_SEND_URL)
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity();

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
