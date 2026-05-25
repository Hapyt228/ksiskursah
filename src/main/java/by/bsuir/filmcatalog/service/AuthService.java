package by.bsuir.filmcatalog.service;

import by.bsuir.filmcatalog.dto.AuthRequest;
import by.bsuir.filmcatalog.dto.AuthResponse;
import by.bsuir.filmcatalog.dto.RegisterRequest;
import by.bsuir.filmcatalog.model.RefreshToken;
import by.bsuir.filmcatalog.model.User;
import by.bsuir.filmcatalog.repository.RefreshTokenRepository;
import by.bsuir.filmcatalog.repository.UserRepository;
import by.bsuir.filmcatalog.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Сервис аутентификации: регистрация, вход, обновление токенов, выход
@Service
@Transactional
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authManager;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    // Регистрация — проверяем что имя и email не заняты, сохраняем пользователя
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email уже используется");
        }

        User user = new User(
            req.getUsername(),
            req.getEmail(),
            passwordEncoder.encode(req.getPassword())
        );
        user = userRepository.save(user);

        return buildTokens(user);
    }

    // Вход — проверяем логин/пароль через Spring Security, выдаём токены
    public AuthResponse login(AuthRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        User user = userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        // Отзываем старые refresh токены (только одна активная сессия)
        refreshTokenRepository.revokeAllByUser(user);

        return buildTokens(user);
    }

    // Обновление токена — проверяем refresh token и выдаём новую пару токенов
    public AuthResponse refreshToken(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new IllegalArgumentException("Refresh token не найден"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new IllegalArgumentException("Refresh token истёк или отозван");
        }

        User user = stored.getUser();

        // Помечаем использованный токен как отозванный
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokens(user);
    }

    // Выход — отзываем все refresh токены пользователя
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user ->
            refreshTokenRepository.revokeAllByUser(user)
        );
    }

    // Создаём access token + refresh token и сохраняем refresh token в БД
    private AuthResponse buildTokens(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtils.generateRefreshToken();

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshExpirationDays);
        refreshTokenRepository.save(new RefreshToken(refreshToken, user, expiresAt));

        return new AuthResponse(
            accessToken,
            refreshToken,
            user.getUsername(),
            user.getId(),
            user.getRole()
        );
    }
}
