package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.EstoqueProdutoImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoEstoqueResponseDTO;

@ExtendWith(MockitoExtension.class)
class ImportacaoEstoqueServiceTest {

    @Mock
    private LeitorRelacaoProdutosOds leitorOds;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ImportacaoEstoqueService service;

    @BeforeEach
    void preparar() {
        service = new ImportacaoEstoqueService(leitorOds, jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveAtualizarSomenteProdutosQueJaExistem() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("100", "200"));

        doAnswer(invocacao -> {
            Consumer<EstoqueProdutoImportacaoDTO> consumidor = invocacao.getArgument(1);
            consumidor.accept(new EstoqueProdutoImportacaoDTO("100", BigDecimal.ZERO));
            consumidor.accept(new EstoqueProdutoImportacaoDTO("999", BigDecimal.TEN));
            consumidor.accept(new EstoqueProdutoImportacaoDTO("200", new BigDecimal("3.500")));
            return 3;
        }).when(leitorOds).lerInventario(any(), any());

        List<EstoqueProdutoImportacaoDTO> registrosAtualizados = new ArrayList<>();
        doAnswer(invocacao -> {
            Collection<EstoqueProdutoImportacaoDTO> lote = invocacao.getArgument(1);
            registrosAtualizados.addAll(lote);
            return new int[][]{new int[lote.size()]};
        }).when(jdbcTemplate).batchUpdate(
                anyString(),
                anyCollection(),
                eq(1_000),
                any()
        );

        ImportacaoEstoqueResponseDTO resposta = service.importar(arquivoOdsValido());

        assertEquals(3, resposta.registrosLidos());
        assertEquals(2, resposta.produtosAtualizados());
        assertEquals(1, resposta.codigosIgnorados());
        assertEquals(List.of("100", "200"), registrosAtualizados.stream()
                .map(EstoqueProdutoImportacaoDTO::codigoSantri)
                .toList());
        assertEquals(BigDecimal.ZERO, registrosAtualizados.getFirst().quantidade());
        assertEquals(new BigDecimal("3.500"), registrosAtualizados.getLast().quantidade());
    }

    private MockMultipartFile arquivoOdsValido() {
        return new MockMultipartFile(
                "arquivo",
                "inventario.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );
    }
}
