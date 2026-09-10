package com.ecommerceproject.dubaimagazinesalvador.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ecommerceproject.dubaimagazinesalvador.infra.security.LimitadorRequisicoesPublicasInterceptor;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.AuditoriaAdministrativaInterceptor;

@Configuration
public class ProtecaoRequisicoesWebConfig implements WebMvcConfigurer {

    private final LimitadorRequisicoesPublicasInterceptor limitador;
    private final AuditoriaAdministrativaInterceptor auditoria;

    public ProtecaoRequisicoesWebConfig(
            LimitadorRequisicoesPublicasInterceptor limitador,
            AuditoriaAdministrativaInterceptor auditoria
    ) {
        this.limitador = limitador;
        this.auditoria = auditoria;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(limitador)
                    .addPathPatterns(
                            "/produto",
                            "/produto/**",
                            "/categoria",
                            "/vitrines-home",
                            "/banners-home",
                            "/depoimentos-home",
                            "/catalogo/imagens/**"
                    );
        registry.addInterceptor(auditoria).addPathPatterns("/**");
    }
}
