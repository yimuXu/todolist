package com.example.jira.Auth;

import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OtpServiceTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final EmailOtpRepository otpRepository = mock(EmailOtpRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);

    private MockRestServiceServer brevo;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        brevo = MockRestServiceServer.bindTo(builder).build();
        otpService = new OtpService(otpRepository, userRepository, passwordEncoder, jwtUtil, builder,
                "noreply@example.com", "test-brevo-key");
    }

    @Test
    void requestSignupOtpRefusesAnEmailThatIsTaken() {
        when(userRepository.findByEmail("dd@example.com"))
                .thenReturn(Optional.of(new User("dd@example.com", "hash")));

        assertTrue(otpService.requestSignupOtp("dd@example.com").startsWith("ERROR"));
        // No request expectation was registered on `brevo`, so any call it made would fail the
        // test on its own — this assertion just documents that no send was expected.
    }

    @Test
    void requestSignupOtpSendsACodeForANewEmail() {
        when(userRepository.findByEmail("dd@example.com")).thenReturn(Optional.empty());
        brevo.expect(requestTo(BREVO_URL)).andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess());

        assertEquals("OTP sent", otpService.requestSignupOtp("dd@example.com"));
        verify(otpRepository).save(any(EmailOtp.class));
        brevo.verify();
    }

    @Test
    void verifySignupOtpCreatesTheAccountOnAValidCode() {
        EmailOtp otp = new EmailOtp("dd@example.com", "123456",
                LocalDateTime.now().plusMinutes(5), OtpPurpose.SIGNUP);
        when(userRepository.findByEmail("dd@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(
                "dd@example.com", "123456", OtpPurpose.SIGNUP)).thenReturn(Optional.of(otp));
        when(passwordEncoder.encode("hunter2")).thenReturn("$2a$10$hashed");
        when(jwtUtil.generateToken("dd@example.com")).thenReturn("a.jwt.token");

        assertEquals("a.jwt.token",
                otpService.verifySignupOtp("dd@example.com", "123456", "hunter2"));

        verify(userRepository).save(any(User.class));
        assertTrue(otp.isUsed());
    }

    @Test
    void verifySignupOtpRejectsAnExpiredCode() {
        EmailOtp otp = new EmailOtp("dd@example.com", "123456",
                LocalDateTime.now().minusMinutes(1), OtpPurpose.SIGNUP);
        when(userRepository.findByEmail("dd@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(
                "dd@example.com", "123456", OtpPurpose.SIGNUP)).thenReturn(Optional.of(otp));

        assertTrue(otpService.verifySignupOtp("dd@example.com", "123456", "hunter2")
                .startsWith("ERROR"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void requestLoginOtpRefusesAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertTrue(otpService.requestLoginOtp("nobody@example.com").startsWith("ERROR"));
        // No request expectation was registered on `brevo`, so any call it made would fail the
        // test on its own.
    }

    @Test
    void verifyLoginOtpHandsBackATokenOnAValidCode() {
        EmailOtp otp = new EmailOtp("dd@example.com", "654321",
                LocalDateTime.now().plusMinutes(5), OtpPurpose.LOGIN);
        when(otpRepository.findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(
                "dd@example.com", "654321", OtpPurpose.LOGIN)).thenReturn(Optional.of(otp));
        when(jwtUtil.generateToken("dd@example.com")).thenReturn("a.jwt.token");

        assertEquals("a.jwt.token", otpService.verifyLoginOtp("dd@example.com", "654321"));
        assertTrue(otp.isUsed());
    }

    @Test
    void aSignupCodeCannotBeUsedToLogIn() {
        when(otpRepository.findFirstByEmailAndCodeAndPurposeAndUsedFalseOrderByIdDesc(
                "dd@example.com", "123456", OtpPurpose.LOGIN)).thenReturn(Optional.empty());

        assertTrue(otpService.verifyLoginOtp("dd@example.com", "123456").startsWith("ERROR"));
    }
}
