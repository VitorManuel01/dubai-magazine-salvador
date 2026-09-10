-- Navegação por categoria: usa a categoria para localizar os produtos e
-- aplica os estados públicos sem voltar à tabela para descartar a maioria deles.
CREATE INDEX idx_produtos_catalogo_categoria
    ON produtos (
        categoria_codigo,
        exibir_no_site,
        disponivel_ultima_importacao,
        destaque_na_home
    );

-- Listagem pública sem categoria: filtra os dois estados obrigatórios e
-- entrega a ordenação estável usada pela paginação do catálogo.
CREATE INDEX idx_produtos_catalogo_publico_ordem
    ON produtos (
        exibir_no_site,
        disponivel_ultima_importacao,
        nome_exibido_site,
        codigo_santri
    );
