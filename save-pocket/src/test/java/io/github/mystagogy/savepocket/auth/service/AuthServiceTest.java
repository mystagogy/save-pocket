package io.github.mystagogy.savepocket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.auth.dto.AuthUserResponse;
import io.github.mystagogy.savepocket.auth.dto.LoginRequest;
import io.github.mystagogy.savepocket.auth.dto.SignupRequest;
import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.auth.session.AuthSessionConstants;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    // 로그인 성공 시 사용자 정보를 반환하고 세션에 인증 정보를 저장해야 한다.
    @Test
    void loginSuccessStoresSessionAndReturnsUser() {
        User user = createUser(1L, "user@example.com", "encoded-password", "절약러");
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);

        AuthUserResponse response = authService.login(request, servletRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("절약러");

        HttpSession session = servletRequest.getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(AuthSessionConstants.USER_ID)).isEqualTo(1L);
        assertThat(session.getAttribute(AuthSessionConstants.USER_EMAIL)).isEqualTo("user@example.com");
    }

    // 이메일로 사용자를 찾지 못하면 인증 실패 예외를 던져야 한다.
    @Test
    void loginFailsWhenUserNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "Password123!");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, servletRequest))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // 비밀번호가 일치하지 않으면 인증 실패 예외를 던져야 한다.
    @Test
    void loginFailsWhenPasswordMismatch() {
        User user = createUser(1L, "user@example.com", "encoded-password", "절약러");
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword123!");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, servletRequest))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    // 회원가입 성공 시 비밀번호를 해시해 저장하고 생성된 사용자 정보를 반환해야 한다.
    @Test
    void signupSuccessEncodesPasswordAndReturnsUser() {
        SignupRequest request = new SignupRequest("new@example.com", "Password123!", "신규유저");
        User savedUser = createUser(10L, "new@example.com", "encoded-password", "신규유저");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthUserResponse response = authService.signup(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.nickname()).isEqualTo("신규유저");
        verify(passwordEncoder).encode("Password123!");
    }

    // 회원가입 시 이메일이 이미 존재하면 중복 이메일 예외를 던져야 한다.
    @Test
    void signupFailsWhenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("user@example.com", "Password123!", "절약러");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    // 로그인된 세션으로 로그아웃하면 세션이 무효화되어야 한다.
    @Test
    void logoutInvalidatesSessionWhenAuthenticated() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSessionConstants.USER_ID, 1L);
        session.setAttribute(AuthSessionConstants.USER_EMAIL, "user@example.com");
        servletRequest.setSession(session);

        authService.logout(servletRequest);

        assertThat(session.isInvalid()).isTrue();
    }

    // 세션이 없거나 인증 정보가 없으면 로그아웃에서 인증 필요 예외를 던져야 한다.
    @Test
    void logoutFailsWhenUnauthenticated() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertThatThrownBy(() -> authService.logout(servletRequest))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private User createUser(Long id, String email, String passwordHash, String nickname) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        return user;
    }
}
