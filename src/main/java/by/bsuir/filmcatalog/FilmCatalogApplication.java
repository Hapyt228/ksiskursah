package by.bsuir.filmcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в приложение «Каталог фильмов».
 * Курсовой проект БГУИР — веб-приложение с REST-интерфейсом.
 *
 * Стек: Spring Boot 3 + SQLite + JPA/Hibernate + HTML/JS/Bootstrap
 */
@SpringBootApplication
public class FilmCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilmCatalogApplication.class, args);
    }
}
