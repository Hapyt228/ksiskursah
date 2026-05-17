package by.bsuir.filmcatalog.controller;

import by.bsuir.filmcatalog.dto.AuthRequest;
import by.bsuir.filmcatalog.dto.AuthResponse;
import by.bsuir.filmcatalog.dto.RegisterRequest;
import by.bsuir.filmcatalog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-контроллер для аутентификации.
 *
 * POST /api/auth/register    — регистрация
 * POST /api/auth/login       — вход
 * POST /api/auth/refresh     — обновление access token
 * POST /api/auth/logout      — выход (отзыв refresh token)
 * GET  /api/auth/me          — информация о текущем пользователе
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Регистрация нового пользователя.
     *
     * Request:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "secret123"
     * }
     *
     * Response 201:
     * {
     *   "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "a3f1c4d2-9e72-4b8f-...",
     *   "tokenType":    "Bearer",
     *   "username":     "john_doe",
     *   "userId":       1,
     *   "role":         "ROLE_USER"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Вход в систему.
     *
     * Request:  { "username": "john_doe", "password": "secret123" }
     * Response: { "accessToken": "...", "refreshToken": "...", ... }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверное имя пользователя или пароль"));
        }
    }

    /**
     * Обновление access token через refresh token.
     *
     * Request:  { "refreshToken": "a3f1c4d2-..." }
     * Response: { "accessToken": "new_token...", "refreshToken": "new_uuid...", ... }
     *
     * Refresh token одноразовый — после использования выдаётся новый.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken обязателен"));
        }
        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Выход — отзывает все refresh токены пользователя.
     * Требует валидный access token в заголовке.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth != null) {
            authService.logout(auth.getName());
        }
        return ResponseEntity.ok(Map.of("message", "Выход выполнен успешно"));
    }

    /**
     * Информация о текущем пользователе.
     * Требует валидный access token.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Не авторизован"));
        }
        return ResponseEntity.ok(Map.of(
            "username", auth.getName(),
            "roles",    auth.getAuthorities().stream()
                            .map(Object::toString)
                            .toList()
        ));
    }
}
