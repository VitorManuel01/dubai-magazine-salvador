package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImagemProdutoCatalogo {

    private static final String PREFIXO_INTERNO = "/uploads/produtos/";
    private static final Pattern IDENTIFICADOR_PUBLICO = Pattern.compile(
            "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(?:jpg|png|webp))$",
            Pattern.CASE_INSENSITIVE
    );

    private ImagemProdutoCatalogo() {
    }

    public static String criarUrlPublica(String imagemUrl) {
        if (imagemUrl == null || imagemUrl.isBlank()) {
            return null;
        }
        if (!imagemUrl.startsWith(PREFIXO_INTERNO)) {
            return imagemUrl;
        }

        Matcher matcher = IDENTIFICADOR_PUBLICO.matcher(imagemUrl);
        return matcher.find() ? "/catalogo/imagens/" + matcher.group(1) : null;
    }
}
