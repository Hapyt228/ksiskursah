package by.bsuir.filmcatalog.dto;

/**
 * Ответ на успешную аутентификацию.
 *
 * Пример JSON:
 * {
 *   "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "a3f1c4d2-...",
 *   "tokenType":    "Bearer",
 *   "username":     "john_doe",
 *   "userId":       42,
 *   "role":         "ROLE_USER"
 * }
 */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String username;
    private Long userId;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken,
                        String username, Long userId, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.userId = userId;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getTokenType() { return tokenType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
