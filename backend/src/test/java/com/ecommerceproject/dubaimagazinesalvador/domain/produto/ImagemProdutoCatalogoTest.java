package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ImagemProdutoCatalogoTest {

    @Test
    void deveOcultarCodigoInternoDoCaminhoDaImagem() {
        assertEquals(
                "/catalogo/imagens/7abe08ab-cebd-4dd8-a6a9-6252faa1d9f8.webp",
                ImagemProdutoCatalogo.criarUrlPublica(
                        "/uploads/produtos/855440-7abe08ab-cebd-4dd8-a6a9-6252faa1d9f8.webp"
                )
        );
    }

    @Test
    void deveRecusarCaminhoInternoLegadoSemIdentificadorSeguro() {
        assertNull(ImagemProdutoCatalogo.criarUrlPublica(
                "/uploads/produtos/855440.webp"
        ));
    }
}
