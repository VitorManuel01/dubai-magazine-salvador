ALTER TABLE produtos
    ADD COLUMN destaque_na_home BOOLEAN NOT NULL DEFAULT FALSE
    AFTER exibir_no_site;

CREATE INDEX idx_produtos_destaque_home
    ON produtos(destaque_na_home);
