package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@ExtendWith(OutputCaptureExtension.class)
class ValidacaoExceptionHandlerTest {

    @Test
    void naoExpoeSenhaRejeitadaNaRespostaOuLog(CapturedOutput output) throws Exception {
        verificar("{\"senha\":\"segredo-invalido-nao-registrar\"}", output);
    }

    @Test
    void naoExpoeJsonMalformadoNaRespostaOuLog(CapturedOutput output) throws Exception {
        verificar("{\"senha\":\"segredo-invalido-nao-registrar\"", output);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 413, 429, 500, 503})
    void preservaStatusSemExporMotivoInterno(int status, CapturedOutput output) {
        var resposta = new ValidacaoExceptionHandler().erroHttp(new ResponseStatusException(
                HttpStatusCode.valueOf(status), "segredo-invalido-nao-registrar"));
        assertThat(resposta.getStatusCode().value()).isEqualTo(status);
        assertThat(resposta.getBody().erro()).doesNotContain("segredo-invalido");
        assertThat(output.getAll()).doesNotContain("segredo-invalido");
    }

    @Test
    void preservaRetryAfterDaResposta() {
        var exception = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE) {
            @Override
            public HttpHeaders getHeaders() {
                var headers = new HttpHeaders();
                headers.set(HttpHeaders.RETRY_AFTER, "10");
                return headers;
            }
        };
        var resposta = new ValidacaoExceptionHandler().erroHttp(exception);
        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("10");
    }

    private void verificar(String corpo, CapturedOutput output) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new EndpointTeste())
                .setControllerAdvice(new ValidacaoExceptionHandler()).build();
        var resposta = mvc.perform(post("/teste-validacao")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(resposta).contains("Dados inv").doesNotContain("segredo-invalido");
        assertThat(output.getAll()).doesNotContain("segredo-invalido");
    }

    @RestController
    static class EndpointTeste {
        @PostMapping("/teste-validacao")
        void receber(@RequestBody @Valid CredencialTeste credencial) {
        }
    }

    record CredencialTeste(@Size(min = 50) String senha) {
    }
}
