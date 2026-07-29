ALTER TABLE produtos
    ADD COLUMN nome_exibido_site VARCHAR(500) NULL
    AFTER descricao;

UPDATE produtos
SET nome_exibido_site = descricao
WHERE nome_exibido_site IS NULL
   OR TRIM(nome_exibido_site) = '';

ALTER TABLE produtos
    MODIFY COLUMN nome_exibido_site VARCHAR(500) NOT NULL;
