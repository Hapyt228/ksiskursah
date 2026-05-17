/**
 * CineBase — Основной клиентский модуль
 * Использует TMDb API как источник фильмов.
 * Пользовательские данные (статус, рейтинг, история, рекомендации) — локальный backend.
 */

const API_LOCAL  = '/api/films';
const API_TMDB   = '/api/tmdb';
const API_USER   = '/api/user';

let allFilms     = [];
let currentView  = 'grid';
let isLoading    = false;
let previousPage = 'catalog'; // откуда пришли на страницу деталей

// Жанры TMDb (id → name), заполняется при loadFilters()
let genreMap = {};

// ========================
// Инициализация
// ========================
document.addEventListener('DOMContentLoaded', () => {
    loadFilters();
    showPage('catalog');
    document.getElementById('btnGrid')?.classList.add('active');
});

// ========================
// Навигация
// ========================
function showPage(name) {
    // Запоминаем страницу, с которой уходим (не сохраняем 'details' как предыдущую)
    const currentVisible = ['catalog','search','top','history'].find(p => {
        const el = document.getElementById(`page-${p}`);
        return el && !el.classList.contains('d-none');
    });
    if (currentVisible && currentVisible !== 'details') {
        previousPage = currentVisible;
    }

    document.querySelectorAll('.page').forEach(p => p.classList.add('d-none'));
    document.getElementById(`page-${name}`)?.classList.remove('d-none');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    switch (name) {
        case 'catalog': loadAllFilms();  break;
        case 'top':     loadTopFilms();  break;
        case 'history': loadHistory();   break;
    }

    // При возврате с деталей обновляем рекомендации
    if (name !== 'details' && Auth.isLoggedIn()) {
        loadRecommendations();
    }
}

// ========================
// Загрузка фильмов — сразу 3 страницы (60 фильмов)
// ========================
async function loadAllFilms() {
    if (isLoading) return;
    isLoading = true;
    showLoading(true);
    // НЕ сбрасываем значения фильтров при переключении вида

    try {
        // Параллельно запрашиваем страницы 1, 2, 3 → 60 фильмов сразу
        const [p1, p2, p3] = await Promise.all([
            apiFetch(`${API_TMDB}/popular?page=1`),
            apiFetch(`${API_TMDB}/popular?page=2`),
            apiFetch(`${API_TMDB}/popular?page=3`)
        ]);
        allFilms = [...(p1||[]), ...(p2||[]), ...(p3||[])];
        renderFilms(allFilms, 'filmsContainer');
        updateResultsCount(allFilms.length, 'resultsCount');

        // Подгружаем ещё 3 страницы фоном (4-6), чтобы было 120 фильмов для фильтрации
        loadMoreBackground(4, 6);
    } catch (e) {
        showError('Не удалось загрузить фильмы. Проверьте TMDb API ключ.');
    } finally {
        showLoading(false);
        isLoading = false;
    }
}

// Фоновая подгрузка страниц для расширения базы фильтрации
async function loadMoreBackground(fromPage, toPage) {
    try {
        const requests = [];
        for (let p = fromPage; p <= toPage; p++) {
            requests.push(apiFetch(`${API_TMDB}/popular?page=${p}`));
        }
        const results = await Promise.all(requests);
        const extra = results.flat().filter(Boolean);
        if (extra.length) {
            allFilms = [...allFilms, ...extra];
            // Обновляем счётчик но не перерисовываем — чтобы не мешать пользователю
            updateResultsCount(allFilms.length, 'resultsCount');
        }
    } catch (_) { /* фоновая загрузка, игнорируем */ }
}

// ========================
// Топ фильмов — несколько страниц, сортировка по рейтингу
// ========================
async function loadTopFilms() {
    const container = document.getElementById('topFilmsContainer');
    if (container) container.innerHTML = `<div class="text-center py-5"><div class="spinner-border text-accent"></div></div>`;
    try {
        // Используем реальный TMDb top_rated endpoint.
        // Загружаем 15 страниц (300 фильмов) — обеспечивает порядочное покрытие фильмов с 9+
        const pages = await Promise.all(
            Array.from({length: 15}, (_, i) => i + 1)
                 .map(p => apiFetch(`${API_TMDB}/top_rated?page=${p}`))
        );
        const films = pages.flat().filter(Boolean);
        // Убираем дубликаты
        const seen = new Set();
        const top = films.filter(f => {
            const key = f.tmdbId || f.id;
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        }).slice(0, 250);
        renderFilmsWithRank(top, 'topFilmsContainer');
    } catch (e) {
        if (container) container.innerHTML = errorHtml('Ошибка загрузки. Проверьте TMDb API ключ.');
    }
}

async function loadHistory() {
    const container = document.getElementById('historyContainer');
    if (!Auth.isLoggedIn()) {
        container.innerHTML = errorHtml('Войдите, чтобы видеть историю просмотров');
        return;
    }
    container.innerHTML = `<div class="text-center py-5"><div class="spinner-border text-accent"></div></div>`;
    try {
        const res = await Auth.authFetch(`${API_USER}/history`);
        if (!res.ok) { container.innerHTML = errorHtml('Ошибка загрузки истории'); return; }
        const films = await res.json();
        if (!films.length) {
            container.innerHTML = errorHtml('История пуста — откройте любой фильм');
            return;
        }
        renderHistoryGrouped(films, 'historyContainer');
    } catch (_) {
        container.innerHTML = errorHtml('Ошибка загрузки истории');
    }
}

// Рендер истории с группировкой по датам
function renderHistoryGrouped(films, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Группируем фильмы по дате просмотра
    const groups = {};
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    films.forEach(f => {
        let label;
        if (f.viewedAt) {
            const d = new Date(f.viewedAt);
            d.setHours(0, 0, 0, 0);
            if (d.getTime() === today.getTime()) label = 'Сегодня';
            else if (d.getTime() === yesterday.getTime()) label = 'Вчера';
            else label = d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' });
        } else {
            label = 'Раньше';
        }
        if (!groups[label]) groups[label] = [];
        groups[label].push(f);
    });

    let html = '';
    for (const [label, groupFilms] of Object.entries(groups)) {
        html += `
            <div class="history-group mb-5">
                <h5 class="history-date-label mb-3">
                    <i class="bi bi-calendar3 me-2 text-accent"></i>${escapeHtml(label)}
                    <span class="badge bg-secondary ms-2">${groupFilms.length}</span>
                </h5>
                <div class="films-grid">
                    ${groupFilms.map((f, i) => filmCardHtml(f, i)).join('')}
                </div>
            </div>`;
    }
    container.innerHTML = html;
}

// ========================
// Фильтры — жанры из TMDb, фильтрация по всем загруженным
// ========================
async function loadFilters() {
    try {
        const genres = await apiFetch(`${API_TMDB}/genres`);
        genres.forEach(g => { genreMap[g.id] = g.name; });

        const gs = document.getElementById('filterGenre');
        if (gs) {
            genres.forEach(g => {
                const o = document.createElement('option');
                o.value = g.name;
                o.textContent = g.name;
                gs.appendChild(o);
            });
        }
    } catch (_) {}
}

function resetFiltersUI() {
    ['filterGenre','filterYearFrom','filterYearTo','filterRating']
        .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
}

async function applyFilters() {
    const genre    = document.getElementById('filterGenre')?.value;
    const yearFrom = document.getElementById('filterYearFrom')?.value;
    const yearTo   = document.getElementById('filterYearTo')?.value;
    const rating   = document.getElementById('filterRating')?.value;
    const currentYear = new Date().getFullYear();

    // Валидация года
    if (yearFrom && parseInt(yearFrom) > currentYear) {
        showToast(`Год ОТ не может быть больше ${currentYear}`, 'error');
        document.getElementById('filterYearFrom').value = currentYear;
        return;
    }
    if (yearTo && parseInt(yearTo) > currentYear) {
        showToast(`Год ДО не может быть больше ${currentYear}`, 'error');
        document.getElementById('filterYearTo').value = currentYear;
        return;
    }

    const hasFilter = genre || yearFrom || yearTo || rating;
    if (!hasFilter) {
        renderFilms(allFilms, 'filmsContainer');
        updateResultsCount(allFilms.length, 'resultsCount');
        return;
    }

    showLoading(true);
    try {
        // Всегда используем TMDb Discover API — фильтры выполняются на сервере
        // Находим genre_id если жанр выбран
        const genreId = genre ? (Object.entries(genreMap).find(([, n]) => n === genre)?.[0] || null) : null;

        // Строим URL с параметрами
        const buildUrl = (page) => {
            const params = new URLSearchParams({ page });
            if (genreId)  params.set('genreId', genreId);
            if (yearFrom) params.set('yearFrom', yearFrom);
            if (yearTo)   params.set('yearTo',   yearTo);
            if (rating)   params.set('rating',   rating);
            return `${API_TMDB}/discover?${params}`;
        };

        // Загружаем 3 страницы параллельно
        const [r1, r2, r3] = await Promise.all([
            apiFetch(buildUrl(1)),
            apiFetch(buildUrl(2)),
            apiFetch(buildUrl(3))
        ]);
        const results = [...(r1||[]), ...(r2||[]), ...(r3||[])];

        // Убираем дубликаты
        const seen = new Set();
        const unique = results.filter(f => {
            const key = f.tmdbId || f.id;
            if (seen.has(key)) return false;
            seen.add(key); return true;
        });

        renderFilms(unique, 'filmsContainer');
        updateResultsCount(unique.length, 'resultsCount');
    } catch (_) {
        showToast('Ошибка при фильтрации', 'error');
    } finally {
        showLoading(false);
    }
}

function resetFilters() {
    resetFiltersUI();
    renderFilms(allFilms, 'filmsContainer');
    updateResultsCount(allFilms.length, 'resultsCount');
}

// ========================
// Поиск
// ========================
function handleNavSearch(e) {
    e.preventDefault();
    const q = document.getElementById('navSearchInput')?.value.trim();
    if (q) performSearch(q);
}
function handleHeroSearch(e) {
    e.preventDefault();
    const q = document.getElementById('heroSearchInput')?.value.trim();
    if (q) performSearch(q);
}

async function performSearch(query) {
    // Запоминаем страницу, с которой идёт поиск
    const visiblePage = ['catalog','search','top','history'].find(p => {
        const el = document.getElementById(`page-${p}`);
        return el && !el.classList.contains('d-none');
    });
    if (visiblePage && visiblePage !== 'search') previousPage = visiblePage;

    // Переключаем на страницу поиска сразу
    document.querySelectorAll('.page').forEach(p => p.classList.add('d-none'));
    document.getElementById('page-search')?.classList.remove('d-none');
    const label = document.getElementById('searchQueryLabel');
    if (label) label.textContent = `"${query}"`;

    const container = document.getElementById('searchResultsContainer');
    if (container) container.innerHTML = `<div class="text-center py-5"><div class="spinner-border text-accent"></div><p class="mt-2 text-muted">Поиск...</p></div>`;

    try {
        // Параллельно загружаем 2 страницы результатов поиска
        const [r1, r2] = await Promise.all([
            apiFetch(`${API_TMDB}/search?q=${encodeURIComponent(query)}&page=1`),
            apiFetch(`${API_TMDB}/search?q=${encodeURIComponent(query)}&page=2`).catch(() => [])
        ]);
        const films = [...(r1||[]), ...(r2||[])];
        // Убираем дубликаты по tmdbId
        const seen = new Set();
        const unique = films.filter(f => {
            const key = f.tmdbId || f.id;
            if (seen.has(key)) return false;
            seen.add(key); return true;
        });
        renderFilms(unique, 'searchResultsContainer');
        updateResultsCount(unique.length, 'searchResultsCount');
    } catch (_) {
        if (container) container.innerHTML = errorHtml('Ошибка поиска. Попробуйте снова.');
    }
}

// ========================
// Детальная страница фильма
// ========================
async function openTmdbFilmDetails(tmdbId) {
    try {
        // Запоминаем текущую страницу перед переходом на детали
        const visiblePage = ['catalog','search','top','history'].find(p => {
            const el = document.getElementById(`page-${p}`);
            return el && !el.classList.contains('d-none');
        });
        if (visiblePage) previousPage = visiblePage;

        document.querySelectorAll('.page').forEach(p => p.classList.add('d-none'));
        document.getElementById('page-details')?.classList.remove('d-none');
        document.getElementById('filmDetailsContent').innerHTML =
            `<div class="text-center py-5"><div class="spinner-border text-accent"></div></div>`;

        const film = await apiFetch(`${API_TMDB}/movie/${tmdbId}`);

        if (Auth.isLoggedIn()) {
            Auth.authFetch(`${API_USER}/tmdb/${tmdbId}/view`, {
                method: 'POST', body: JSON.stringify(film)
            }).catch(() => {});
        }

        const recommendations = await apiFetch(`${API_TMDB}/movie/${tmdbId}/similar`).catch(() => []);
        await renderFilmDetails(film, recommendations, tmdbId);
    } catch (e) {
        showToast('Ошибка загрузки фильма', 'error');
    }
}

async function openFilmDetails(filmId) {
    const cached = allFilms.find(f => String(f.id) === String(filmId) || String(f.tmdbId) === String(filmId));
    if (cached && cached.tmdbId) return openTmdbFilmDetails(cached.tmdbId);

    try {
        // Запоминаем текущую страницу
        const visiblePage = ['catalog','search','top','history'].find(p => {
            const el = document.getElementById(`page-${p}`);
            return el && !el.classList.contains('d-none');
        });
        if (visiblePage) previousPage = visiblePage;

        document.querySelectorAll('.page').forEach(p => p.classList.add('d-none'));
        document.getElementById('page-details')?.classList.remove('d-none');

        if (Auth.isLoggedIn()) {
            Auth.authFetch(`${API_USER}/films/${filmId}/view`, { method: 'POST' }).catch(() => {});
        }
        const [film, recommendations] = await Promise.all([
            apiFetch(`${API_LOCAL}/${filmId}`),
            apiFetch(`${API_LOCAL}/${filmId}/recommendations`).catch(() => [])
        ]);
        await renderFilmDetails(film, recommendations, null);
    } catch (e) {
        showToast('Ошибка загрузки фильма', 'error');
    }
}

async function renderFilmDetails(film, recommendations, tmdbId) {
    const posterSrc = film.posterUrl || '';
    const recHtml = (recommendations || []).length > 0
        ? recommendations.slice(0, 6).map(r => filmCardHtml(r)).join('')
        : '<p class="text-muted">Нет похожих фильмов</p>';
    const duration = film.durationMinutes
        ? `${Math.floor(film.durationMinutes / 60)}ч ${film.durationMinutes % 60}мин`
        : '';

    let userStatus = '';
    let userStars  = 0;
    if (Auth.isLoggedIn()) {
        try {
            const statusUrl = tmdbId ? `${API_USER}/tmdb/${tmdbId}/status`  : `${API_USER}/films/${film.id}/status`;
            const ratingUrl = tmdbId ? `${API_USER}/tmdb/${tmdbId}/rating`  : `${API_USER}/films/${film.id}/rating`;
            const [sRes, rRes] = await Promise.all([
                Auth.authFetch(statusUrl), Auth.authFetch(ratingUrl)
            ]);
            if (sRes.ok) { const d = await sRes.json(); userStatus = d.status || ''; }
            if (rRes.ok) { const d = await rRes.json(); userStars  = d.stars  || 0; }
        } catch (_) {}
    }

    const statusOptions = [
        { value: 'WATCHING',   label: '▶ Смотрю' },
        { value: 'PLANNED',    label: '📋 Запланировано' },
        { value: 'COMPLETED',  label: '✅ Просмотрено' },
        { value: 'FAVOURITE',  label: '❤️ Любимое' },
        { value: 'REWATCHING', label: '🔁 Пересматриваю' },
        { value: 'POSTPONED',  label: '⏸ Отложено' },
        { value: 'DROPPED',    label: '❌ Брошено' },
    ];
    const filmDataAttr = tmdbId ? `data-tmdb-id="${tmdbId}"` : `data-film-id="${film.id}"`;

    const statusHtml = Auth.isLoggedIn() ? `
        <div class="user-actions mt-3">
            <div class="d-flex flex-wrap gap-3 align-items-center">
                <select class="form-select form-select-sm status-select" style="width:auto"
                        ${filmDataAttr}
                        onchange="setFilmStatusUnified(this, ${escapeHtml(JSON.stringify(film))})">
                    <option value="">— Статус —</option>
                    ${statusOptions.map(o =>
                        `<option value="${o.value}" ${userStatus === o.value ? 'selected' : ''}>${o.label}</option>`
                    ).join('')}
                </select>
                <div class="star-rating-10" ${filmDataAttr}>
                    ${[1,2,3,4,5,6,7,8,9,10].map(s => `
                        <i class="bi bi-star${s <= userStars ? '-fill' : ''} star-icon"
                           data-val="${s}"
                           onclick="setUserRatingUnified(${s}, this, ${escapeHtml(JSON.stringify(film))})"
                           title="${s} из 10"></i>
                    `).join('')}
                    ${userStars > 0 ? `<span class="star-label">${userStars}/10</span>` : ''}
                </div>
            </div>
        </div>` : `
        <div class="user-actions mt-3">
            <button class="btn btn-outline-light btn-sm" onclick="showAuthModal('login')">
                <i class="bi bi-person-circle me-1"></i>Войдите, чтобы оценить
            </button>
        </div>`;

    const detailsEl = document.getElementById('filmDetailsContent');
    if (!detailsEl) return;
    detailsEl.innerHTML = `
        <div class="mb-3">
            <button class="btn btn-outline-secondary btn-sm" onclick="showPage(previousPage)">
                <i class="bi bi-arrow-left me-1"></i>Назад
            </button>
        </div>
        <div class="film-details-hero">
            <div class="film-details-backdrop" style="${posterSrc ? `background-image:url('${escapeHtml(posterSrc)}')` : ''}"></div>
            <div class="film-details-content">
                <div class="film-details-poster">
                    ${posterSrc
                        ? `<img src="${escapeHtml(posterSrc)}" alt="${escapeHtml(film.title)}" loading="lazy"
                               onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
                        : ''}
                    <div class="film-poster-placeholder" style="${posterSrc ? 'display:none' : ''}">
                        <i class="bi bi-film" style="font-size:3rem;opacity:0.3"></i>
                    </div>
                </div>
                <div class="film-details-info">
                    <h1 class="film-details-title">${escapeHtml(film.title)}</h1>
                    ${film.originalTitle ? `<p class="film-details-original">${escapeHtml(film.originalTitle)}</p>` : ''}
                    ${film.rating ? `
                    <div class="rating-large">
                        <span class="rating-value">${film.rating.toFixed(1)}</span>
                        <div>
                            <div class="rating-stars">${generateStarsFull(film.rating)}</div>
                            <div style="font-size:.75rem;color:var(--text-muted)">из 10 · TMDb</div>
                        </div>
                    </div>` : ''}
                    <div class="film-stats">
                        ${film.year     ? `<span class="film-stat"><i class="bi bi-calendar3"></i>${film.year}</span>` : ''}
                        ${film.genre    ? `<span class="film-stat"><i class="bi bi-tag"></i>${escapeHtml(film.genre)}</span>` : ''}
                        ${film.director ? `<span class="film-stat"><i class="bi bi-camera-video"></i>${escapeHtml(film.director)}</span>` : ''}
                        ${film.country  ? `<span class="film-stat"><i class="bi bi-globe"></i>${escapeHtml(film.country)}</span>` : ''}
                        ${duration      ? `<span class="film-stat"><i class="bi bi-clock"></i>${duration}</span>` : ''}
                    </div>
                    ${film.description ? `<p class="film-description">${escapeHtml(film.description)}</p>` : ''}
                    ${statusHtml}
                </div>
            </div>
        </div>
        <div class="recommendations-section">
            <h3 class="section-title"><i class="bi bi-stars text-accent"></i> Похожие фильмы</h3>
            <div class="films-grid">${recHtml}</div>
        </div>`;
}

// ========================
// Статус и оценка (1-10)
// ========================
async function setFilmStatusUnified(selectEl, film) {
    if (!Auth.isLoggedIn()) { showAuthModal('login'); return; }
    const status = selectEl.value;
    const tmdbId  = film.tmdbId  || null;
    const localId = film.id      || null;
    try {
        if (tmdbId) {
            if (!status) await Auth.authFetch(`${API_USER}/tmdb/${tmdbId}/status`, { method: 'DELETE' });
            else await Auth.authFetch(`${API_USER}/tmdb/${tmdbId}/status`, {
                method: 'PUT', body: JSON.stringify({ status, filmData: film })
            });
        } else {
            if (!status) await Auth.authFetch(`${API_USER}/films/${localId}/status`, { method: 'DELETE' });
            else await Auth.authFetch(`${API_USER}/films/${localId}/status`, {
                method: 'PUT', body: JSON.stringify({ status })
            });
        }
        showToast(status ? 'Статус сохранён ✓' : 'Статус снят');
    } catch (_) { showToast('Ошибка сохранения статуса', 'error'); }
}

async function setUserRatingUnified(stars, el, film) {
    if (!Auth.isLoggedIn()) { showAuthModal('login'); return; }
    const tmdbId  = film.tmdbId || null;
    const localId = film.id     || null;
    try {
        if (tmdbId) {
            await Auth.authFetch(`${API_USER}/tmdb/${tmdbId}/rating`, {
                method: 'PUT', body: JSON.stringify({ stars, filmData: film })
            });
        } else {
            await Auth.authFetch(`${API_USER}/films/${localId}/rating`, {
                method: 'PUT', body: JSON.stringify({ stars })
            });
        }
        // Обновляем 10-звёздочный UI
        const container = el.closest('.star-rating-10');
        if (container) {
            container.querySelectorAll('.star-icon').forEach(icon => {
                const val = parseInt(icon.dataset.val);
                icon.className = `bi bi-star${val <= stars ? '-fill' : ''} star-icon`;
            });
            let label = container.querySelector('.star-label');
            if (!label) { label = document.createElement('span'); label.className = 'star-label'; container.appendChild(label); }
            label.textContent = `${stars}/10`;
        }
        showToast(`Оценка ${stars}/10 сохранена ✓`);
    } catch (_) { showToast('Ошибка сохранения оценки', 'error'); }
}

// ========================
// Рендеринг карточек
// ========================
function renderFilms(films, containerId) {
    const container = document.getElementById(containerId);
    const empty     = document.getElementById('emptyState');
    if (!films || films.length === 0) {
        if (container) container.innerHTML = '';
        if (empty && containerId === 'filmsContainer') empty.classList.remove('d-none');
        return;
    }
    if (empty) empty.classList.add('d-none');
    container.className = currentView === 'list' ? 'films-list' : 'films-grid';
    container.innerHTML = films.map((f, i) =>
        currentView === 'list' ? filmCardListHtml(f) : filmCardHtml(f, i)
    ).join('');
}

function filmCardHtml(film, index = 0) {
    const delay = Math.min(index * 20, 400);
    const rating = film.rating ? film.rating.toFixed(1) : null;
    const posterSrc = film.posterUrl || '';
    const clickHandler = film.tmdbId
        ? `openTmdbFilmDetails(${film.tmdbId})`
        : `openFilmDetails(${film.id})`;

    // placeholder SVG (inline, не зависит от сети)
    const placeholderSvg = `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='300'><rect width='200' height='300' fill='%23232323'/><text x='50%' y='50%' fill='%23555' font-size='14' font-family='sans-serif' text-anchor='middle' dominant-baseline='middle'>Нет постера</text></svg>`;

    return `
    <div class="film-card" onclick="${clickHandler}" style="animation-delay:${delay}ms">
        <div class="film-poster-wrap">
            <img class="film-poster"
                 src="${posterSrc ? escapeHtml(posterSrc) : placeholderSvg}"
                 alt="${escapeHtml(film.title)}"
                 loading="lazy"
                 onerror="this.src='${placeholderSvg}'">
            <div class="film-overlay"></div>
            ${rating ? `<div class="film-rating-badge"><i class="bi bi-star-fill"></i>${rating}</div>` : ''}
        </div>
        <div class="film-card-body">
            <div class="film-title">${escapeHtml(film.title)}</div>
            <div class="film-meta">${film.year || ''}${film.director ? ' · ' + escapeHtml(film.director) : ''}</div>
            ${film.genre ? `<span class="film-genre-badge">${escapeHtml(film.genre)}</span>` : ''}
        </div>
    </div>`;
}

function filmCardListHtml(film) {
    const rating = film.rating ? film.rating.toFixed(1) : '—';
    const posterSrc = film.posterUrl || '';
    const clickHandler = film.tmdbId
        ? `openTmdbFilmDetails(${film.tmdbId})`
        : `openFilmDetails(${film.id})`;
    const placeholderSvg = `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='70' height='105'><rect width='70' height='105' fill='%23232323'/><text x='50%' y='50%' fill='%23555' font-size='10' font-family='sans-serif' text-anchor='middle' dominant-baseline='middle'>?</text></svg>`;

    return `
    <div class="film-card-list" onclick="${clickHandler}">
        <div class="film-poster-wrap" style="width:70px;min-width:70px;border-radius:8px;overflow:hidden">
            <img class="film-poster" src="${posterSrc ? escapeHtml(posterSrc) : placeholderSvg}"
                 alt="${escapeHtml(film.title)}" loading="lazy"
                 onerror="this.src='${placeholderSvg}'" style="height:105px;object-fit:cover">
        </div>
        <div class="flex-grow-1">
            <div class="film-title mb-1">${escapeHtml(film.title)}</div>
            <div class="film-meta mb-1">${film.year || ''}${film.director ? ' · ' + escapeHtml(film.director) : ''}</div>
            ${film.genre ? `<span class="film-genre-badge">${escapeHtml(film.genre)}</span>` : ''}
        </div>
        <div><div class="film-rating-badge" style="position:relative"><i class="bi bi-star-fill"></i>${rating}</div></div>
    </div>`;
}

function renderFilmsWithRank(films, containerId) {
    const c = document.getElementById(containerId);
    if (!c) return;
    c.className = 'films-grid';
    c.innerHTML = films.map((film, idx) => `
        <div style="position:relative">
            <div style="position:absolute;top:-8px;left:-8px;z-index:10;width:28px;height:28px;border-radius:50%;
                        background:var(--accent);color:white;font-weight:700;font-size:.75rem;
                        display:flex;align-items:center;justify-content:center;box-shadow:0 2px 8px rgba(224,92,42,.5)">
                ${idx + 1}
            </div>
            ${filmCardHtml(film, idx)}
        </div>`).join('');
}

function setView(view) {
    currentView = view;
    document.getElementById('btnGrid')?.classList.toggle('active', view === 'grid');
    document.getElementById('btnList')?.classList.toggle('active', view === 'list');
    if (allFilms.length) renderFilms(allFilms, 'filmsContainer');
}

// ========================
// HTTP-клиент
// ========================
async function apiFetch(url, method = 'GET', body = null) {
    const opts = { method, headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const token = Auth.getAccessToken();
    if (token) opts.headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(url, opts);
    if (!res.ok) {
        const err = await res.json().catch(() => ({ error: res.statusText }));
        throw new Error(err.error || `HTTP ${res.status}`);
    }
    if (res.status === 204) return null;
    return res.json();
}

// ========================
// Утилиты UI
// ========================
function showLoading(state) {
    document.getElementById('loadingSpinner')?.classList.toggle('d-none', !state);
}

function showError(msg) {
    const c = document.getElementById('filmsContainer');
    if (c) c.innerHTML = `<div class="col-12 text-center py-5">
        <i class="bi bi-exclamation-triangle display-4 text-warning d-block mb-3"></i>
        <p class="text-muted">${escapeHtml(msg)}</p>
        <button class="btn btn-accent mt-2" onclick="loadAllFilms()">Повторить</button></div>`;
}

function updateResultsCount(count, elId) {
    const el = document.getElementById(elId);
    if (el) el.textContent = count ? `Найдено: ${count} фильм${pluralize(count)}` : '';
}

function pluralize(n) {
    if (n % 100 >= 11 && n % 100 <= 19) return 'ов';
    const r = n % 10;
    if (r === 1) return ''; if (r >= 2 && r <= 4) return 'а'; return 'ов';
}

function showToast(msg, type = 'success') {
    const el = document.getElementById('appToast');
    const tm = document.getElementById('toastMessage');
    if (!el || !tm) return;
    tm.textContent = msg;
    el.className = `toast align-items-center border-0 toast-${type}`;
    new bootstrap.Toast(el, { delay: 3000 }).show();
}

// Генерация 5 звёздочек для TMDb-рейтинга (из 10)
function generateStarsFull(rating) {
    if (!rating) return '☆☆☆☆☆';
    const full = Math.round(rating / 2);
    return '★'.repeat(Math.min(full, 5)) + '☆'.repeat(Math.max(0, 5 - full));
}

function escapeHtml(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function escapeJs(str) { return String(str || '').replace(/'/g,"\\'"); }
function errorHtml(msg) {
    return `<div class="text-center py-4 text-muted"><i class="bi bi-exclamation-circle me-2"></i>${escapeHtml(msg)}</div>`;
}
