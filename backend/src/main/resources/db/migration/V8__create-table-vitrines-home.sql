CREATE TABLE vitrines_home (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_codigo VARCHAR(64) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    ordem INT NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_vitrine_home_categoria UNIQUE (categoria_codigo),
    CONSTRAINT fk_vitrine_home_categoria
        FOREIGN KEY (categoria_codigo)
        REFERENCES categorias(codigo)
        ON DELETE RESTRICT,
    CONSTRAINT chk_vitrine_home_ordem
        CHECK (ordem >= 0)
);

CREATE INDEX idx_vitrines_home_ordem
    ON vitrines_home(ativo, ordem);
