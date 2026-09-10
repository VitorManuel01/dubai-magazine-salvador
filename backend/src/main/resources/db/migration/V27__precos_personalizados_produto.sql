ALTER TABLE produtos
    ADD COLUMN usar_precos_personalizados BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN preco_a_vista DECIMAL(12,2) NULL,
    ADD COLUMN preco_cartao_parc DECIMAL(12,2) NULL,
    ADD COLUMN max_parcelamento INT NULL,
    ADD CONSTRAINT ck_produtos_preco_a_vista CHECK (preco_a_vista IS NULL OR preco_a_vista > 0),
    ADD CONSTRAINT ck_produtos_preco_cartao CHECK (preco_cartao_parc IS NULL OR preco_cartao_parc > 0),
    ADD CONSTRAINT ck_produtos_max_parcelamento CHECK (max_parcelamento IS NULL OR max_parcelamento BETWEEN 1 AND 36),
    ADD CONSTRAINT ck_produtos_precos_personalizados CHECK (
        usar_precos_personalizados = FALSE OR (
            preco_a_vista IS NOT NULL AND preco_cartao_parc IS NOT NULL AND max_parcelamento IS NOT NULL
        )
    );
