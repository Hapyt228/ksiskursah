package by.bsuir.filmcatalog.service;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.model.Film;
import by.bsuir.filmcatalog.repository.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Сервисный слой приложения «Каталог фильмов».
 * Содержит бизнес-логику: CRUD, поиск, фильтрацию, рекомендации.
 */
@Service
@Transactional
public class FilmService {

    private final FilmRepository filmRepository;

    @Autowired
    public FilmService(FilmRepository filmRepository) {
        this.filmRepository = filmRepository;
    }

    // ========================
    // CRUD операции
    // ========================

    /**
     * Получить все фильмы.
     */
    @Transactional(readOnly = true)
    public List<FilmDto> getAllFilms() {
        return filmRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить фильм по ID.
     */
    @Transactional(readOnly = true)
    public FilmDto getFilmById(Long id) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Фильм с ID=" + id + " не найден"));
        return toDto(film);
    }

    /**
     * Создать новый фильм.
     */
    public FilmDto createFilm(FilmDto dto) {
        Film film = toEntity(dto);
        Film saved = filmRepository.save(film);
        return toDto(saved);
    }

    /**
     * Обновить существующий фильм.
     */
    public FilmDto updateFilm(Long id, FilmDto dto) {
        Film existing = filmRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Фильм с ID=" + id + " не найден"));
        updateEntityFromDto(existing, dto);
        Film updated = filmRepository.save(existing);
        return toDto(updated);
    }

    /**
     * Удалить фильм по ID.
     */
    public void deleteFilm(Long id) {
        if (!filmRepository.existsById(id)) {
            throw new NoSuchElementException("Фильм с ID=" + id + " не найден");
        }
        filmRepository.deleteById(id);
    }

    // ========================
    // Поиск и фильтрация
    // ========================

    /**
     * Полнотекстовый поиск по названию, режиссёру, описанию.
     */
    @Transactional(readOnly = true)
    public List<FilmDto> searchFilms(String query) {
        return filmRepository.searchByQuery(query)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Комбинированная фильтрация по нескольким критериям.
     */
    @Transactional(readOnly = true)
    public List<FilmDto> filterFilms(String genre, Integer yearFrom, Integer yearTo,
                                      Double minRating, String country) {
        return filmRepository.findByFilters(genre, yearFrom, yearTo, minRating, country)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Топ фильмов по рейтингу.
     */
    @Transactional(readOnly = true)
    public List<FilmDto> getTopRated(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return filmRepository.findTopRated(safeLimit)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ========================
    // Веб-служба рекомендаций
    // ========================

    /**
     * Получить рекомендации для конкретного фильма.
     * Алгоритм: сначала ищет фильмы того же жанра,
     * затем добавляет фильмы того же режиссёра (если нужно).
     */
    @Transactional(readOnly = true)
    public List<FilmDto> getRecommendations(Long filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new NoSuchElementException("Фильм с ID=" + filmId + " не найден"));

        List<Film> byGenre = filmRepository.findSimilarByGenre(film.getGenre(), filmId);
        List<Film> byDirector = filmRepository.findByDirectorExcluding(film.getDirector(), filmId);

        // Объединяем, убираем дубликаты, берём не более 8 рекомендаций
        return byGenre.stream()
                .collect(Collectors.toList())
                .stream()
                .map(this::toDto)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            byDirector.stream()
                                    .filter(d -> byGenre.stream().noneMatch(g -> g.getId().equals(d.getId())))
                                    .map(this::toDto)
                                    .forEach(list::add);
                            return list.subList(0, Math.min(list.size(), 8));
                        }
                ));
    }

    // ========================
    // Метаданные (для фильтров)
    // ========================

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return filmRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public List<String> getAllCountries() {
        return filmRepository.findAllDistinctCountries();
    }

    // ========================
    // Маппинг DTO <-> Entity
    // ========================

    private FilmDto toDto(Film film) {
        FilmDto dto = new FilmDto();
        dto.setId(film.getId());
        dto.setTitle(film.getTitle());
        dto.setOriginalTitle(film.getOriginalTitle());
        dto.setYear(film.getYear());
        dto.setGenre(film.getGenre());
        dto.setDirector(film.getDirector());
        dto.setCountry(film.getCountry());
        dto.setDescription(film.getDescription());
        dto.setRating(film.getRating());
        dto.setDurationMinutes(film.getDurationMinutes());
        dto.setPosterUrl(film.getPosterUrl());
        dto.setLanguage(film.getLanguage());
        dto.setTags(film.getTags());
        return dto;
    }

    private Film toEntity(FilmDto dto) {
        Film film = new Film();
        updateEntityFromDto(film, dto);
        return film;
    }

    private void updateEntityFromDto(Film film, FilmDto dto) {
        film.setTitle(dto.getTitle());
        film.setOriginalTitle(dto.getOriginalTitle());
        film.setYear(dto.getYear());
        film.setGenre(dto.getGenre());
        film.setDirector(dto.getDirector());
        film.setCountry(dto.getCountry());
        film.setDescription(dto.getDescription());
        film.setRating(dto.getRating());
        film.setDurationMinutes(dto.getDurationMinutes());
        film.setPosterUrl(dto.getPosterUrl());
        film.setLanguage(dto.getLanguage());
        film.setTags(dto.getTags());
    }
}
