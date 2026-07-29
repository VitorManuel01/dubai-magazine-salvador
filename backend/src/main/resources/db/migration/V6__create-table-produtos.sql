CREATE TABLE produtos (
    codigo_santri VARCHAR(32) PRIMARY KEY,
    descricao VARCHAR(500) NOT NULL,
    ncm VARCHAR(20),
    unidade VARCHAR(20),
    marca VARCHAR(255),
    codigo_original VARCHAR(255),
    quantidade DECIMAL(15, 3) NOT NULL DEFAULT 0.000,
    preco_venda DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    preco_venda_iva DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    categoria_codigo VARCHAR(64) NOT NULL,
    imagem_url VARCHAR(1000),
    exibir_no_site BOOLEAN NOT NULL DEFAULT FALSE,
    ultima_importacao_em DATETIME(6),
    CONSTRAINT fk_produto_categoria
        FOREIGN KEY (categoria_codigo)
        REFERENCES categorias(codigo)
        ON DELETE RESTRICT,
    CONSTRAINT chk_produto_quantidade
        CHECK (quantidade >= 0),
    CONSTRAINT chk_produto_preco_venda
        CHECK (preco_venda >= 0),
    CONSTRAINT chk_produto_preco_venda_iva
        CHECK (preco_venda_iva >= 0)
);

CREATE INDEX idx_produtos_categoria
    ON produtos(categoria_codigo);

CREATE INDEX idx_produtos_marca
    ON produtos(marca);

CREATE INDEX idx_produtos_visibilidade
    ON produtos(exibir_no_site);
