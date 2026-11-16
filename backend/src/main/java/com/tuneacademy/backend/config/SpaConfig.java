package com.tuneacademy.backend.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the React app, which the Docker build copies into resources/static.
 * A path that isn't a file (like /app/practice/3) gets index.html, so a reload or a shared link
 * still opens the right page: React's router reads the URL and takes it from there.
 * /api paths never fall back, so an unknown API path stays a 404 instead of returning a web page.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String path, Resource location) throws IOException {
                    Resource file = super.getResource(path, location);
                    if (file != null) {
                        return file;
                    }
                    boolean isApi = path.equals("api") || path.startsWith("api/");
                    boolean looksLikeFile = path.substring(path.lastIndexOf('/') + 1).contains(".");
                    return !isApi && !looksLikeFile && INDEX.exists() ? INDEX : null;
                }
            });
    }
}
