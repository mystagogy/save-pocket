package io.github.mystagogy.savepocket.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setNickname("절약러");
        userRepository.save(user);
    }

    // 유효한 회원가입 요청은 201과 사용자 응답을 반환하고 비밀번호를 해시로 저장해야 한다.
    @Test
    void signupReturns201AndStoresEncodedPassword() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "new@example.com", "password", "Password123!", "nickname", "신규유저")
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("신규유저"))
                .andExpect(jsonPath("$.error").doesNotExist());

        User savedUser = userRepository.findByEmail("new@example.com").orElseThrow();
        Assertions.assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password123!");
        Assertions.assertThat(passwordEncoder.matches("Password123!", savedUser.getPasswordHash())).isTrue();
    }

    // 이미 존재하는 이메일로 회원가입하면 409 중복 이메일 응답을 반환해야 한다.
    @Test
    void signupReturns409WhenEmailAlreadyExists() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "user@example.com", "password", "Password123!", "nickname", "중복유저")
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    // 대소문자만 다른 동일 이메일로 회원가입하면 409 중복 이메일 응답을 반환해야 한다.
    @Test
    void signupReturns409WhenEmailAlreadyExistsIgnoringCase() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "User@Example.com", "password", "Password123!", "nickname", "중복유저")
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    // 비밀번호 정책을 만족하지 못하는 회원가입 요청은 400 유효성 검증 실패를 반환해야 한다.
    @Test
    void signupReturns400WhenPasswordPolicyIsInvalid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "invalid-signup@example.com", "password", "1234", "nickname", "유효성실패")
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // 올바른 자격증명으로 로그인하면 200과 사용자 응답을 반환해야 한다.
    @Test
    void loginReturns200WhenCredentialsAreValid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "user@example.com", "password", "Password123!")
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("절약러"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // 로그인 시 이메일 대소문자가 달라도 인증에 성공해야 한다.
    @Test
    void loginReturns200WhenEmailCaseIsDifferent() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "User@Example.Com", "password", "Password123!")
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    // 로그인 후 동일 세션으로 보호 경로 접근 시 401이 아닌 응답(인증 통과)을 반환해야 한다.
    @Test
    void protectedEndpointIsNot401AfterLogin() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "user@example.com", "password", "Password123!")
        );

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/test/protected").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk());
    }

    // 잘못된 비밀번호로 로그인하면 401과 인증 실패 코드를 반환해야 한다.
    @Test
    void loginReturns401WhenPasswordIsInvalid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "user@example.com", "password", "WrongPassword123!")
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    // 이메일 형식이 잘못된 로그인 요청은 400 유효성 검증 실패를 반환해야 한다.
    @Test
    void loginReturns400WhenEmailFormatIsInvalid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("email", "invalid-email", "password", "Password123!")
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // 인증 없이 보호된 엔드포인트에 접근하면 401을 반환해야 한다.
    @Test
    void protectedEndpointReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/wishes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // 로그인된 세션으로 로그아웃하면 204를 반환하고 세션이 만료되어야 한다.
    @Test
    void logoutReturns204AndInvalidatesSessionWhenAuthenticated() throws Exception {
        String loginRequestBody = objectMapper.writeValueAsString(
                Map.of("email", "user@example.com", "password", "Password123!")
        );

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        mockMvc.perform(post("/auth/logout").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/wishes").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // 인증되지 않은 상태로 로그아웃하면 401 인증 필요 응답을 반환해야 한다.
    @Test
    void logoutReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @TestConfiguration
    static class TestControllerConfig {
        @Bean
        TestProtectedController testProtectedController() {
            return new TestProtectedController();
        }
    }

    @RestController
    static class TestProtectedController {
        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "ok";
        }
    }
}
