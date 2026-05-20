package com.skyways.user.service;

import com.skyways.common.exception.auth.AuthenticationException;
import com.skyways.user.dto.LoginRequest;
import com.skyways.user.dto.LoginResponse;
import com.skyways.user.dto.RegisterRequest;
import com.skyways.user.dto.UserProfileDto;
import com.skyways.user.entity.AuditLog;
import com.skyways.user.entity.User;
import com.skyways.user.kafka.UserEventProducer;
import com.skyways.user.repository.AuditLogRepository;
import com.skyways.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserEventProducer userEventProducer;

    public UserService(UserRepository userRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userEventProducer = userEventProducer;
    }

    @Transactional
    public User register(RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AuthenticationException("Email already registered: " + request.getEmail());
            }

            User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();

            user = userRepository.save(user);
            log.info("New user registered [userId={}, email={}]", user.getUserId(), user.getEmail());

            userEventProducer.publishUserRegistered(user);

            return user;
        } catch (AuthenticationException e) {
            log.warn("Registration rejected for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration for email {}", request.getEmail(), e);
            throw e;
        }
    }

    public UserProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));
        return toProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UserProfileDto dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        user = userRepository.save(user);
        log.info("Profile updated [userId={}]", userId);
        return toProfileDto(user);
    }

    private UserProfileDto toProfileDto(User user) {
        return UserProfileDto.builder()
            .userId(user.getUserId().toString())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

            if (!user.isActive()) {
                throw new AuthenticationException("Account is deactivated. Contact support.");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.warn("Failed login attempt for email: {}", request.getEmail());
                throw new AuthenticationException("Invalid email or password");
            }

            String token = jwtService.generateToken(user);

            auditLogRepository.save(AuditLog.builder()
                .userId(user.getUserId())
                .action("LOGIN")
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build());

            log.info("User logged in [userId={}]", user.getUserId());

            return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getTokenExpirySeconds())
                .userId(user.getUserId().toString())
                .role(user.getRole().name())
                .build();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected login error for email {}", request.getEmail(), e);
            throw e;
        }
    }
}
