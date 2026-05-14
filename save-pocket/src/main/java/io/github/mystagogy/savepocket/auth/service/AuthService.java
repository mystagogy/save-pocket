package io.github.mystagogy.savepocket.auth.service;

import io.github.mystagogy.savepocket.auth.dto.AuthUserResponse;
import io.github.mystagogy.savepocket.auth.dto.LoginRequest;
import io.github.mystagogy.savepocket.auth.dto.SignupRequest;
import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.auth.session.AuthSessionConstants;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthUserResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SavePocketException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new SavePocketException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(AuthSessionConstants.USER_ID, user.getId());
        session.setAttribute(AuthSessionConstants.USER_EMAIL, user.getEmail());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getId(),
                null,
                Collections.emptyList()
        ));
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return new AuthUserResponse(user.getId(), user.getEmail(), user.getNickname());
    }

    public AuthUserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new SavePocketException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());

        User savedUser = userRepository.save(user);
        return new AuthUserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    public AuthUserResponse me(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new SavePocketException(ErrorCode.UNAUTHORIZED);
        }

        Object userIdValue = session.getAttribute(AuthSessionConstants.USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            throw new SavePocketException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SavePocketException(ErrorCode.UNAUTHORIZED));
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getNickname());
    }

    public void logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null || session.getAttribute(AuthSessionConstants.USER_ID) == null) {
            throw new SavePocketException(ErrorCode.UNAUTHORIZED);
        }

        session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
