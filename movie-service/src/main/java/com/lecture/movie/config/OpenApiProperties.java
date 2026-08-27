package com.lecture.movie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    private Kobis kobis = new Kobis();
    private Tmdb tmdb = new Tmdb();

    @Getter
    @Setter
    public static class Kobis {
        private String baseUrl;
        private String key;
    }

    @Getter
    @Setter
    public static class Tmdb {
        private String baseUrl;
        private String imageBaseUrl;
        private String key;
        private String language = "ko-KR";
    }
}
