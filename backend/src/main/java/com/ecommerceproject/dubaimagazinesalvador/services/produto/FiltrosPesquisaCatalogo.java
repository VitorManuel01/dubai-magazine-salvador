package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Normaliza os filtros recebidos pelas consultas do catálogo antes que eles
 * cheguem ao banco. Os curingas do {@code LIKE} são tratados como texto do
 * usuário, e não como parte da linguagem da consulta.
 */
public final class FiltrosPesquisaCatalogo {

    public static final int TAMANHO_MAXIMO_BUSCA = 100;
    public static final int TAMANHO_MAXIMO_CATEGORIA = 64;

    private static final Pattern CODIGO_CATEGORIA = Pattern.compile(
            "[0-9]+(?:\\.[0-9]+)*"
    );

    private FiltrosPesquisaCatalogo() {
    }

    public static String normalizarBuscaParaLike(String busca) {
        return normalizarBuscaParaLike(busca, 1);
    }

    public static String normalizarBuscaParaLike(String busca, int minimoCaracteres) {
        if (busca == null || busca.isBlank()) {
            return null;
        }

        String normalizada = normalizarTexto(busca);
        if (normalizada.isEmpty()) {
            return null;
        }
        int quantidadeCaracteres = normalizada.codePointCount(0, normalizada.length());
        if (quantidadeCaracteres > TAMANHO_MAXIMO_BUSCA) {
            throw erro("A busca deve possuir no máximo 100 caracteres.");
        }
        // Verifica o texto original normalizado antes de duplicar os curingas.
        if (quantidadeCaracteres < minimoCaracteres) {
            throw erro("A busca pública deve possuir pelo menos 2 caracteres.");
        }

        return normalizada
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static String normalizarCodigoCategoria(String categoriaCodigo) {
        if (categoriaCodigo == null || categoriaCodigo.isBlank()) {
            return null;
        }

        String normalizado = Normalizer.normalize(
                categoriaCodigo,
                Normalizer.Form.NFC
        ).strip();
        if (normalizado.length() > TAMANHO_MAXIMO_CATEGORIA
                || !CODIGO_CATEGORIA.matcher(normalizado).matches()) {
            throw erro("O código da categoria informado é inválido.");
        }
        return normalizado;
    }

    private static String normalizarTexto(String texto) {
        String normalizadoUnicode = Normalizer.normalize(texto, Normalizer.Form.NFC);
        StringBuilder resultado = new StringBuilder(normalizadoUnicode.length());
        boolean espacoPendente = false;

        for (int indice = 0; indice < normalizadoUnicode.length();) {
            int caractere = normalizadoUnicode.codePointAt(indice);
            indice += Character.charCount(caractere);

            int tipo = Character.getType(caractere);
            if (Character.isISOControl(caractere) || tipo == Character.FORMAT) {
                throw erro("A busca contém caracteres inválidos.");
            }

            if (Character.isWhitespace(caractere) || Character.isSpaceChar(caractere)) {
                espacoPendente = resultado.length() > 0;
                continue;
            }

            if (espacoPendente) {
                resultado.append(' ');
                espacoPendente = false;
            }
            resultado.appendCodePoint(caractere);
        }

        return resultado.toString();
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
