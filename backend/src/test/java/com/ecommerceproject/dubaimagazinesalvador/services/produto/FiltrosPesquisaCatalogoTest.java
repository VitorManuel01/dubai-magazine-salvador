package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class FiltrosPesquisaCatalogoTest {

    @Test
    void deveTratarBuscaAusenteOuEmBrancoComoFiltroAusente() {
        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike(null)).isNull();
        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike("   ")).isNull();
        assertThat(FiltrosPesquisaCatalogo.normalizarCodigoCategoria(null)).isNull();
        assertThat(FiltrosPesquisaCatalogo.normalizarCodigoCategoria("   ")).isNull();
    }

    @Test
    void deveNormalizarUnicodeEEspacosDaBusca() {
        String busca = "  Cafe\u0301\u00a0   Dubai  ";

        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike(busca))
                .isEqualTo("Café Dubai");
    }

    @Test
    void deveEscaparMetacaracteresDoLikeComoTextoLiteral() {
        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike("%_!"))
                .isEqualTo("!%!_!!");
    }

    @Test
    void deveContarCaracteresUnicodeEmVezDeUnidadesUtf16() {
        String cemCaracteres = "🛒".repeat(100);
        String centoEUmCaracteres = "🛒".repeat(101);

        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike(cemCaracteres))
                .isEqualTo(cemCaracteres);
        assertThatThrownBy(() -> FiltrosPesquisaCatalogo
                .normalizarBuscaParaLike(centoEUmCaracteres))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro ->
                        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deveRejeitarCaracteresDeControleEFormatacao() {
        assertThatThrownBy(() -> FiltrosPesquisaCatalogo
                .normalizarBuscaParaLike("cadeira\nvermelha"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> FiltrosPesquisaCatalogo
                .normalizarBuscaParaLike("cadeira\u202evermelha"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deveAceitarSomenteCaminhoNumericoDeCategoria() {
        assertThat(FiltrosPesquisaCatalogo.normalizarCodigoCategoria(" 001.003.0006.0002 "))
                .isEqualTo("001.003.0006.0002");

        for (String invalido : new String[]{"001%", "001_", "001..002", "categoria"}) {
            assertThatThrownBy(() -> FiltrosPesquisaCatalogo
                    .normalizarCodigoCategoria(invalido))
                    .isInstanceOfSatisfying(ResponseStatusException.class, erro ->
                            assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    void buscaPublicaContaCaracteresAntesDeEscaparCuringas() {
        for (String busca : new String[]{"%", "_", "!", "a", "🛒"}) {
            assertThatThrownBy(() -> FiltrosPesquisaCatalogo.normalizarBuscaParaLike(busca, 2))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike("%_", 2)).isEqualTo("!%!_");
        assertThat(FiltrosPesquisaCatalogo.normalizarBuscaParaLike("\u00a0", 2)).isNull();
    }

    @Test
    void deveLimitarComprimentoDoCodigoDaCategoria() {
        String categoriaLonga = "1".repeat(65);

        assertThatThrownBy(() -> FiltrosPesquisaCatalogo
                .normalizarCodigoCategoria(categoriaLonga))
                .isInstanceOf(ResponseStatusException.class);
    }
}
