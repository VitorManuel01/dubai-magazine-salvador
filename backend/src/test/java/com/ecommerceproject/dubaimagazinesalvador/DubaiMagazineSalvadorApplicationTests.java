package com.ecommerceproject.dubaimagazinesalvador;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.LimitadorRequisicoesPublicasInterceptor;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.AuditoriaAdministrativaInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DubaiMagazineSalvadorApplication.class)
@ActiveProfiles("test")
class DubaiMagazineSalvadorApplicationTests {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rotas;

    @Test
    void contextoCarrega() {
    }

    @Test
    void protecoesObrigatoriasEstaoRegistradasNasRotasReais() throws Exception {
        var catalogo = rotas.getHandler(new MockHttpServletRequest("GET", "/produto"));
        assertThat(catalogo).isNotNull();
        assertThat(catalogo.getInterceptorList())
                .anyMatch(LimitadorRequisicoesPublicasInterceptor.class::isInstance);

        var administracao = rotas.getHandler(
                new MockHttpServletRequest("PUT", "/funcionario/550e8400-e29b-41d4-a716-446655440000/status")
        );
        assertThat(administracao).isNotNull();
        assertThat(administracao.getInterceptorList())
                .anyMatch(AuditoriaAdministrativaInterceptor.class::isInstance);
    }
}
