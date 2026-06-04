package com.practice.knowheart.service;

import com.practice.knowheart.dto.AuthResponse;
import com.practice.knowheart.dto.LoginRequest;
import com.practice.knowheart.dto.RegisterRequest;
import com.practice.knowheart.dto.UserCenterResponse;
import com.practice.knowheart.entity.AppUser;
import com.practice.knowheart.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final int TOKEN_VALID_DAYS = 30;

    private final AppUserRepository userRepository;
    private final UserProfileService profileService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository,
                       UserProfileService profileService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        validateUsername(username);
        validatePassword(request.getPassword());

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已被注册");
        }

        AppUser user = new AppUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(resolveNickname(request.getNickname(), username));
        user.setCreatedAt(LocalDateTime.now());

        issueToken(user);
        userRepository.save(user);

        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        issueToken(user);
        userRepository.save(user);

        return toAuthResponse(user);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        userRepository.findByAuthToken(token).ifPresent(user -> {
            user.setAuthToken(null);
            user.setTokenExpiry(null);
            userRepository.save(user);
        });
    }

    public UserCenterResponse getUserCenter(String token) {
        AppUser user = requireValidUser(token);
        var profile = profileService.getProfile(user.getUserId()).orElse(null);
        return new UserCenterResponse(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                profile,
                user.getCreatedAt()
        );
    }

    public AppUser requireValidUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("请先登录");
        }

        AppUser user = userRepository.findByAuthToken(token)
                .orElseThrow(() -> new IllegalArgumentException("登录已失效，请重新登录"));

        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("登录已过期，请重新登录");
        }

        return user;
    }

    private void issueToken(AppUser user) {
        user.setAuthToken(UUID.randomUUID().toString());
        user.setTokenExpiry(LocalDateTime.now().plusDays(TOKEN_VALID_DAYS));
    }

    private AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(user.getAuthToken(), user.getUserId(), user.getUsername(), user.getNickname());
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim();
    }

    private void validateUsername(String username) {
        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("用户名长度需为 3-20 个字符");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new IllegalArgumentException("用户名只能包含中文、字母、数字和下划线");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度至少 6 位");
        }
    }

    private String resolveNickname(String nickname, String username) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        return username;
    }
}
