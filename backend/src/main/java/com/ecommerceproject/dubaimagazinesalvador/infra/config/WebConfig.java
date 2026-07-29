package com.ecommerceproject.dubaimagazinesalvador.infra.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    private final Path diretorioImagensProdutos;

    public WebConfig(
            @Value("${app.upload.produtos-dir:uploads/produtos}") String diretorioImagensProdutos
    ) {
        this.diretorioImagensProdutos = Path.of(diretorioImagensProdutos)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // URL do seu frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localizacao = diretorioImagensProdutos.toUri().toString();
        if (!localizacao.endsWith("/")) {
            localizacao += "/";
        }
        registry.addResourceHandler("/uploads/produtos/**")
                .addResourceLocations(localizacao);
    }
}
