package by.bsuir.filmcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа. Запускает встроенный Tomcat на порту 8080.
@SpringBootApplication
public class FilmCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilmCatalogApplication.class, args);
    }
}
