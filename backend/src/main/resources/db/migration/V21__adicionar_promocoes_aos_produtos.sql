ALTER TABLE produtos
    ADD COLUMN em_promocao BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN data_inicial_prom DATE NULL,
    ADD COLUMN data_final_prom DATE NULL,
    ADD COLUMN porc_margem DECIMAL(9,4) NULL,
    ADD COLUMN porc_desconto DECIMAL(9,4) NULL,
    ADD COLUMN preco_promocao DECIMAL(15,2) NULL,
    ADD COLUMN especial BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_produtos_promocao
    ON produtos (em_promocao, data_inicial_prom, data_final_prom);
