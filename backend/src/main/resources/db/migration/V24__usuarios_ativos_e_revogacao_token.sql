ALTER TABLE usuarios
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN versao_token INT NOT NULL DEFAULT 0;

CREATE INDEX idx_usuarios_ativo
    ON usuarios(ativo);
