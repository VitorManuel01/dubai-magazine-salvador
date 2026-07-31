CREATE TABLE vitrines_loja (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL,
    atualizado_em DATETIME(6) NOT NULL
);

CREATE TABLE produtos_vitrine_loja (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vitrine_loja_id BIGINT NOT NULL,
    produto_codigo_santri VARCHAR(32) NOT NULL,
    rotulo_opcao VARCHAR(100) NOT NULL,
    ordem INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_produto_vitrine_loja_produto UNIQUE (produto_codigo_santri),
    CONSTRAINT fk_produto_vitrine_loja_vitrine
        FOREIGN KEY (vitrine_loja_id)
        REFERENCES vitrines_loja(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_produto_vitrine_loja_produto
        FOREIGN KEY (produto_codigo_santri)
        REFERENCES produtos(codigo_santri)
        ON DELETE CASCADE,
    CONSTRAINT chk_produto_vitrine_loja_ordem
        CHECK (ordem >= 0)
);

CREATE INDEX idx_produtos_vitrine_loja_vitrine
    ON produtos_vitrine_loja(vitrine_loja_id, ordem);

CREATE TABLE imagens_produto_vitrine_loja (
    produto_vitrine_loja_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    imagem_url VARCHAR(1000) NOT NULL,
    PRIMARY KEY (produto_vitrine_loja_id, ordem),
    CONSTRAINT fk_imagem_produto_vitrine_loja
        FOREIGN KEY (produto_vitrine_loja_id)
        REFERENCES produtos_vitrine_loja(id)
        ON DELETE CASCADE
);

CREATE TABLE secoes_produto_vitrine_loja (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_vitrine_loja_id BIGINT NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    conteudo TEXT NOT NULL,
    ordem INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_secao_produto_vitrine_loja
        FOREIGN KEY (produto_vitrine_loja_id)
        REFERENCES produtos_vitrine_loja(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_secao_produto_vitrine_loja_ordem
        CHECK (ordem >= 0)
);

CREATE INDEX idx_secoes_produto_vitrine_loja_produto
    ON secoes_produto_vitrine_loja(produto_vitrine_loja_id, ordem);

CREATE INDEX idx_vitrines_loja_ativas
    ON vitrines_loja(ativo, atualizado_em);
