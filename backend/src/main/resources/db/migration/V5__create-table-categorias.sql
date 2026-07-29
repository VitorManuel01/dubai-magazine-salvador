CREATE TABLE categorias (
    codigo VARCHAR(64) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    nivel INT NOT NULL,
    caminho VARCHAR(1000) NOT NULL,
    categoria_pai_codigo VARCHAR(64),
    exibir_no_site BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_categoria_pai
        FOREIGN KEY (categoria_pai_codigo)
        REFERENCES categorias(codigo)
        ON DELETE RESTRICT,
    CONSTRAINT chk_categoria_nivel
        CHECK (nivel BETWEEN 1 AND 4)
);

CREATE INDEX idx_categorias_pai
    ON categorias(categoria_pai_codigo);

CREATE INDEX idx_categorias_visibilidade
    ON categorias(exibir_no_site);
