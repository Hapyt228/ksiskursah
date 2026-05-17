# CineBase 🎬

Веб-приложение — каталог фильмов на базе Spring Boot с интеграцией TMDb API.  
Курсовой проект БГУИР, 2026.

## Стек технологий

| Слой | Технология |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Security, Spring WebFlux (WebClient) |
| База данных | SQLite + Hibernate / Spring Data JPA |
| Авторизация | JWT (access token 15 мин + refresh token 7 дней, BCrypt) |
| Внешний API | TMDb API v3 (фильмы, жанры, постеры, поиск) |
| Frontend | HTML5, Bootstrap 5.3, Vanilla JS |

## Функциональность

- **Каталог фильмов** — 60+ популярных фильмов из TMDb, постеры, рейтинг
- **Поиск** — поиск по названию через TMDb (несколько страниц результатов)
- **Фильтры** — по жанру (через TMDb Discover API), году, рейтингу
- **Топ фильмов** — топ-100 по рейтингу TMDb
- **JWT авторизация** — регистрация, вход, refresh, logout
- **История просмотров** — автосохранение при открытии фильма
- **Статусы** — Смотрю / Запланировано / Просмотрено / Любимое / Пересматриваю / Отложено / Брошено
- **Оценки** — система оценок 1-10 звёзд
- **Рекомендации** — на основе жанров просмотренных фильмов

## Запуск

### Требования
- Java 21+
- Maven 3.9+
- Аккаунт TMDb (бесплатно): https://www.themoviedb.org/settings/api

### Конфигурация

В файле `src/main/resources/application.properties` укажите ваш TMDb API Read Access Token:

```properties
tmdb.api.token=ВАШ_ТОКЕН_ЗДЕСЬ
```

Токен получить: https://www.themoviedb.org/settings/api → Developer → API Read Access Token

### Сборка и запуск

```bash
mvn clean package -DskipTests
java -jar target/film-catalog-1.0.0.jar
```

Приложение доступно по адресу: http://localhost:8080

## Структура проекта

```
src/main/java/by/bsuir/filmcatalog/
├── config/          # SecurityConfig, DataInitializer
├── controller/      # REST контроллеры
├── dto/             # DTO объекты (включая tmdb/)
├── model/           # JPA сущности
├── repository/      # Spring Data JPA репозитории
├── security/        # JWT фильтр и утилиты
├── service/         # Бизнес-логика
└── tmdb/            # TmdbClient + TmdbService

src/main/resources/static/
├── index.html       # Единственная HTML-страница (SPA)
├── css/style.css    # Стили
└── js/
    ├── app.js       # Основная логика фронтенда
    └── auth.js      # Авторизация на фронтенде
```

## API

| Метод | URL | Описание |
|---|---|---|
| GET | /api/tmdb/popular?page=1 | Популярные фильмы |
| GET | /api/tmdb/search?q=... | Поиск по названию |
| GET | /api/tmdb/movie/{id} | Детали фильма |
| GET | /api/tmdb/genres | Список жанров |
| GET | /api/tmdb/discover?genreId=28 | Фильмы по жанру |
| POST | /api/auth/register | Регистрация |
| POST | /api/auth/login | Вход |
| POST | /api/auth/refresh | Обновление токена |
| PUT | /api/user/tmdb/{id}/status | Установить статус |
| PUT | /api/user/tmdb/{id}/rating | Поставить оценку |
| GET | /api/user/history | История просмотров |
| GET | /api/user/recommendations | Рекомендации |

---
Built with [Perplexity Computer](https://www.perplexity.ai/computer)
