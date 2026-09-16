package com.udea.ScoreCrediticio.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Patrones base: dev local (CRA/Vite) + cualquier deploy de Vercel.
        // FRONTEND_URL (Render) permite fijar además la URL final, ej:
        // https://score-front.vercel.app
        String[] patterns;
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            patterns = new String[]{
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "https://*.vercel.app",
                    frontendUrl.trim()
            };
        } else {
            patterns = new String[]{
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "https://*.vercel.app"
            };
        }

        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
