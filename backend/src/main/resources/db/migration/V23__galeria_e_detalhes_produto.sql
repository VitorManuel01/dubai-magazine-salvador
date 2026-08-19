ALTER TABLE produtos
    ADD COLUMN id_publico CHAR(36) NULL AFTER codigo_santri,
    ADD COLUMN descricao_site TEXT NULL AFTER imagem_hover_url;

UPDATE produtos
SET id_publico = UUID()
WHERE id_publico IS NULL;

ALTER TABLE produtos
    MODIFY COLUMN id_publico CHAR(36) NOT NULL,
    ADD CONSTRAINT uk_produtos_id_publico UNIQUE (id_publico);

CREATE TABLE imagens_produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_codigo_santri VARCHAR(32) NOT NULL,
    imagem_url VARCHAR(1000) NOT NULL,
    ordem INT NOT NULL,
    CONSTRAINT fk_imagens_produto_produto
        FOREIGN KEY (produto_codigo_santri)
        REFERENCES produtos(codigo_santri)
        ON DELETE CASCADE,
    CONSTRAINT chk_imagens_produto_ordem
        CHECK (ordem >= 0 AND ordem < 8)
);

CREATE INDEX idx_imagens_produto_ordem
    ON imagens_produto(produto_codigo_santri, ordem);

INSERT INTO imagens_produto (produto_codigo_santri, imagem_url, ordem)
SELECT codigo_santri, imagem_url, 0
FROM produtos
WHERE imagem_url IS NOT NULL
  AND TRIM(imagem_url) <> '';

INSERT INTO imagens_produto (produto_codigo_santri, imagem_url, ordem)
SELECT produto.codigo_santri,
       produto.imagem_hover_url,
       CASE WHEN produto.imagem_url IS NULL OR TRIM(produto.imagem_url) = '' THEN 0 ELSE 1 END
FROM produtos produto
WHERE produto.imagem_hover_url IS NOT NULL
  AND TRIM(produto.imagem_hover_url) <> ''
  AND (produto.imagem_url IS NULL OR produto.imagem_hover_url <> produto.imagem_url);

INSERT INTO imagens_produto (produto_codigo_santri, imagem_url, ordem)
SELECT opcao.produto_codigo_santri,
       imagem.imagem_url,
       (SELECT COUNT(*)
        FROM imagens_produto existente
        WHERE existente.produto_codigo_santri = opcao.produto_codigo_santri)
       + imagem.ordem
FROM imagens_produto_vitrine_loja imagem
JOIN produtos_vitrine_loja opcao
  ON opcao.id = imagem.produto_vitrine_loja_id
WHERE imagem.ordem < 8
  AND (SELECT COUNT(*)
       FROM imagens_produto existente
       WHERE existente.produto_codigo_santri = opcao.produto_codigo_santri)
      + imagem.ordem < 8
  AND NOT EXISTS (
      SELECT 1
      FROM imagens_produto existente
      WHERE existente.produto_codigo_santri = opcao.produto_codigo_santri
        AND existente.imagem_url = imagem.imagem_url
  );
