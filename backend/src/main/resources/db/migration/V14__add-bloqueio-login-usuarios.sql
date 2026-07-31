ALTER TABLE usuarios
    ADD COLUMN tentativas_login_falhas INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_ate TIMESTAMP(6) NULL;
