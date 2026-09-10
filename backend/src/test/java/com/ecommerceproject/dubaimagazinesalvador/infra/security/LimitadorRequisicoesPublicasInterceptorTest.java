package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorRequisicoesPublicasService;

class LimitadorRequisicoesPublicasInterceptorTest {
    @Test
    void limitaConcorrenciaGlobalELiberaVagaMesmoAposErro() throws Exception {
        var service = mock(LimitadorRequisicoesPublicasService.class);
        when(service.consumir(any(), any()))
                .thenReturn(LimitadorRequisicoesPublicasService.Resultado.aceito());
        var interceptor = new LimitadorRequisicoesPublicasInterceptor(service, 1);
        var primeira = new MockHttpServletRequest("GET", "/produto");
        var resposta = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(primeira, resposta, this)).isTrue();

        var bloqueada = new MockHttpServletRequest("HEAD", "/produto");
        assertThat(interceptor.preHandle(bloqueada, resposta, this)).isFalse();
        assertThat(resposta.getStatus()).isEqualTo(503);
        assertThat(resposta.getHeader("Retry-After")).isEqualTo("1");

        // Imagens têm balde próprio e não ocupam vagas reservadas às consultas do banco.
        assertThat(interceptor.preHandle(new MockHttpServletRequest("GET", "/catalogo/imagens/foto.jpg"),
                new MockHttpServletResponse(), this)).isTrue();
        interceptor.afterCompletion(primeira, resposta, this, new RuntimeException("teste"));
        interceptor.afterCompletion(primeira, resposta, this, null);
        assertThat(interceptor.preHandle(bloqueada, new MockHttpServletResponse(), this)).isTrue();
        assertThat(interceptor.preHandle(primeira, new MockHttpServletResponse(), this)).isFalse();
        interceptor.afterCompletion(bloqueada, resposta, this, null);
    }
}
