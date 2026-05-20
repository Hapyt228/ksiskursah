/**
 * CineBase — Модуль авторизации
 * Управляет JWT токенами, состоянием пользователя, refresh логикой.
 */

const Auth = (() => {
    const ACCESS_KEY  = 'cinebase_access';
    const REFRESH_KEY = 'cinebase_refresh';
    const USER_KEY    = 'cinebase_user';

    // ========================
    // Хранение токенов
    // ========================

    function saveTokens(data) {
        localStorage.setItem(ACCESS_KEY,  data.accessToken);
        localStorage.setItem(REFRESH_KEY, data.refreshToken);
        localStorage.setItem(USER_KEY, JSON.stringify({
            username: data.username,
            userId:   data.userId,
            role:     data.role
        }));
    }

    function clearTokens() {
        localStorage.removeItem(ACCESS_KEY);
        localStorage.removeItem(REFRESH_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function getAccessToken()  { return localStorage.getItem(ACCESS_KEY); }
    function getRefreshToken() { return localStorage.getItem(REFRESH_KEY); }
    function getCurrentUser()  {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    }
    function isLoggedIn() { return !!getAccessToken(); }

    // ========================
    // HTTP с автоматическим refresh
    // ========================

    /**
     * Выполняет fetch с Bearer токеном.
     * Если 401 — пробует обновить access token через refresh и повторяет запрос.
     */
    async function authFetch(url, options = {}) {
        const token = getAccessToken();
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(options.headers || {}),
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        };

        let res = await fetch(url, { ...options, headers });

        // Пробуем обновить токен и повторить запрос
        if (res.status === 401 && getRefreshToken()) {
            const refreshed = await tryRefresh();
            if (refreshed) {
                const newToken = getAccessToken();
                headers['Authorization'] = `Bearer ${newToken}`;
                res = await fetch(url, { ...options, headers });
            } else {
                clearTokens();
                updateNavbar();
                return res;
            }
        }
        return res;
    }

    /**
     * Пробует обновить access token.
     * Возвращает true при успехе, false при провале.
     */
    async function tryRefresh() {
        const refreshToken = getRefreshToken();
        if (!refreshToken) return false;
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });
            if (res.ok) {
                const data = await res.json();
                saveTokens(data);
                return true;
            }
        } catch (_) {}
        return false;
    }

    // ========================
    // Регистрация / Логин / Выход
    // ========================

    async function register(username, email, password) {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Ошибка регистрации');
        saveTokens(data);
        return data;
    }

    async function login(username, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Неверные данные');
        saveTokens(data);
        return data;
    }

    async function logout() {
        const token = getAccessToken();
        if (token) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            }).catch(() => {});
        }
        clearTokens();
    }

    /**
     * Декодирует payload JWT (base64url) без верификации подписи.
     * Используется только для проверки срока жизни `exp`.
     */
    function decodeJwtPayload(token) {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;
            const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            return JSON.parse(atob(payload));
        } catch (_) { return null; }
    }

    /**
     * Проверяет сессию при старте.
     * - Если access token есть и не истёк — OK.
     * - Если истёк / недействителен — пробует refresh.
     * - Если refresh тоже провалился — clearTokens().
     * Возвращает true если пользователь авторизован после проверки.
     */
    async function initAuth() {
        const accessToken = getAccessToken();
        if (!accessToken) return false;

        const payload = decodeJwtPayload(accessToken);
        const nowSec  = Math.floor(Date.now() / 1000);
        // Если токен действителен (+ 30 сек запас на задержку сети) — OK
        if (payload && payload.exp && payload.exp > nowSec + 30) return true;

        // Токен истёк или не декодируется — пробуем refresh
        if (getRefreshToken()) {
            const refreshed = await tryRefresh();
            if (refreshed) return true;
        }
        clearTokens();
        return false;
    }

    return { saveTokens, clearTokens, getAccessToken, getRefreshToken,
             getCurrentUser, isLoggedIn, authFetch, login, register, logout, initAuth };
})();

// ========================
// Глобальные обработчики авторизации
// ========================

function updateNavbar() {
    const user = Auth.getCurrentUser();
    const isAuth = Auth.isLoggedIn() && user;

    document.getElementById('navAuthGuest').classList.toggle('d-none', !!isAuth);
    document.getElementById('navAuthUser').classList.toggle('d-none', !isAuth);
    document.querySelectorAll('.nav-auth-only').forEach(el =>
        el.classList.toggle('d-none', !isAuth)
    );

    if (isAuth) {
        document.getElementById('navUsername').textContent = user.username;
    } else {
        document.getElementById('recommendationsSection')?.classList.add('d-none');
    }
}

function showAuthModal(mode) {
    const modal = new bootstrap.Modal(document.getElementById('authModal'));
    const isLogin = mode === 'login';
    document.getElementById('authModalTitle').textContent = isLogin ? 'Вход' : 'Регистрация';
    document.getElementById('loginForm').classList.toggle('d-none', !isLogin);
    document.getElementById('registerForm').classList.toggle('d-none', isLogin);
    document.getElementById('authError').classList.add('d-none');
    modal.show();
}

async function doLogin() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errEl = document.getElementById('authError');
    errEl.classList.add('d-none');
    try {
        await Auth.login(username, password);
        bootstrap.Modal.getInstance(document.getElementById('authModal'))?.hide();
        updateNavbar();
        showToast('Добро пожаловать, ' + username + '!', 'success');
        showPage('catalog');
    } catch(e) {
        errEl.textContent = e.message;
        errEl.classList.remove('d-none');
    }
}

async function doRegister() {
    const username = document.getElementById('regUsername').value.trim();
    const email    = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const errEl = document.getElementById('authError');
    errEl.classList.add('d-none');
    try {
        await Auth.register(username, email, password);
        bootstrap.Modal.getInstance(document.getElementById('authModal'))?.hide();
        updateNavbar();
        showToast('Аккаунт создан! Добро пожаловать, ' + username, 'success');
        showPage('catalog');
    } catch(e) {
        errEl.textContent = e.message;
        errEl.classList.remove('d-none');
    }
}

async function logout() {
    await Auth.logout();
    updateNavbar();
    showToast('Вы вышли из системы');
    showPage('catalog');
}

// ========================
// Рекомендации
// ========================

async function loadRecommendations() {
    if (!Auth.isLoggedIn()) return;
    try {
        const res = await Auth.authFetch('/api/user/recommendations?limit=8');
        if (!res.ok) return;
        const films = await res.json();

        const section   = document.getElementById('recommendationsSection');
        const container = document.getElementById('recommendationsContainer');
        if (!section || !container) return;

        if (films.length > 0) {
            section.classList.remove('d-none');
            // filmCardHtml определяет правильный обработчик (tmdbId или localId)
            container.innerHTML = films.map((f, i) => filmCardHtml(f, i)).join('');
        }
    } catch(_) {}
}

// Инициализация происходит в app.js через Auth.initAuth() + updateNavbar()
