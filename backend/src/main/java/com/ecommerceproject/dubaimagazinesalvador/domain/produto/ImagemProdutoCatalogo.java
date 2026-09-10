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
        if (imagemUrl.startsWith("/catalogo/imagens/")) {
            Matcher publica = IDENTIFICADOR_PUBLICO.matcher(imagemUrl);
            return publica.find() ? "/catalogo/imagens/" + publica.group(1) : null;
        }
        if (!imagemUrl.startsWith(PREFIXO_INTERNO)) {
            return null;
        }

        Matcher matcher = IDENTIFICADOR_PUBLICO.matcher(imagemUrl);
        return matcher.find() ? "/catalogo/imagens/" + matcher.group(1) : null;
    }

    public static String normalizarParaPersistencia(String imagemUrl) {
        String publica = criarUrlPublica(imagemUrl == null ? null : imagemUrl.trim());
        if (publica == null) {
            throw new IllegalArgumentException("A foto deve ter sido enviada por este sistema.");
        }
        Matcher matcher = IDENTIFICADOR_PUBLICO.matcher(publica);
        if (!matcher.find()) {
            throw new IllegalArgumentException("A URL da foto é inválida.");
        }
        return PREFIXO_INTERNO + matcher.group(1).toLowerCase(java.util.Locale.ROOT);
    }
}
