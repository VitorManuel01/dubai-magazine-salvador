CREATE TABLE auditoria_administrativa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id VARCHAR(36) NULL,
    codigo_santri VARCHAR(255) NULL,
    funcao VARCHAR(50) NULL,
    metodo VARCHAR(10) NOT NULL,
    caminho VARCHAR(500) NOT NULL,
    status_http INT NOT NULL,
    ip_hmac CHAR(64) NULL,
    user_agent_hmac CHAR(64) NULL,
    ocorrido_em TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_auditoria_ocorrido_em
    ON auditoria_administrativa(ocorrido_em);

CREATE INDEX idx_auditoria_usuario_ocorrido
    ON auditoria_administrativa(usuario_id, ocorrido_em);
