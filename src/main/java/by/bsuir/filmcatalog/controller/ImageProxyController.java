package by.bsuir.filmcatalog.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

// Прокси для постеров TMDb.
// Нужен потому что у некоторых пользователей TMDb заблокирован — браузер не может
// загрузить изображение напрямую. Сервер скачивает постер сам и отдаёт клиенту.
@RestController
@RequestMapping("/api/image-proxy")
public class ImageProxyController {

    private final WebClient webClient = WebClient.builder().build();

    @GetMapping
    public ResponseEntity<byte[]> proxy(@RequestParam String url) {
        // Разрешаем только TMDb изображения
        if (!url.startsWith("https://image.tmdb.org/")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            byte[] bytes = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            HttpHeaders headers = new HttpHeaders();
            String contentType = url.endsWith(".png") ? "image/png" : "image/jpeg";
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            // Кэшируем на 7 дней — постеры не меняются
            headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=604800");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
