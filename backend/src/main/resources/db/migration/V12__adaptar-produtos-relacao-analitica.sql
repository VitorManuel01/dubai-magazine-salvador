ALTER TABLE produtos
    DROP CHECK chk_produto_quantidade,
    DROP CHECK chk_produto_preco_venda,
    DROP CHECK chk_produto_preco_venda_iva;

ALTER TABLE produtos
    CHANGE COLUMN descricao nome VARCHAR(500) NOT NULL,
    CHANGE COLUMN unidade unidade_venda VARCHAR(20) NULL,
    CHANGE COLUMN quantidade estoque DECIMAL(15, 3) NOT NULL DEFAULT 0.000,
    CHANGE COLUMN preco_venda preco_sem_ipi DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    DROP COLUMN preco_venda_iva,
    ADD COLUMN nome_compra VARCHAR(500) NULL AFTER ncm,
    ADD COLUMN fabricante VARCHAR(255) NULL AFTER nome_compra,
    ADD COLUMN ativo_santri BOOLEAN NOT NULL DEFAULT TRUE AFTER marca,
    ADD COLUMN unidade_compra VARCHAR(20) NULL AFTER unidade_venda,
    ADD COLUMN data_cadastro DATE NULL AFTER unidade_compra,
    ADD COLUMN codigo_barras VARCHAR(32) NULL AFTER codigo_original,
    ADD COLUMN bloqueado_para_compras BOOLEAN NOT NULL DEFAULT FALSE AFTER codigo_barras,
    ADD COLUMN percentual_ipi_entrada DECIMAL(7, 4) NULL AFTER preco_sem_ipi,
    ADD COLUMN peso_unidade DECIMAL(18, 6) NULL AFTER percentual_ipi_entrada,
    ADD COLUMN altura_unidade DECIMAL(18, 6) NULL AFTER peso_unidade,
    ADD COLUMN largura_unidade DECIMAL(18, 6) NULL AFTER altura_unidade,
    ADD COLUMN comprimento_unidade DECIMAL(18, 6) NULL AFTER largura_unidade,
    ADD COLUMN volume_unidade_m3 DECIMAL(18, 9) NULL AFTER comprimento_unidade,
    ADD COLUMN volume_litros DECIMAL(18, 6) NULL AFTER volume_unidade_m3,
    ADD COLUMN peso_caixa DECIMAL(18, 6) NULL AFTER volume_litros,
    ADD COLUMN altura_caixa DECIMAL(18, 6) NULL AFTER peso_caixa,
    ADD COLUMN largura_caixa DECIMAL(18, 6) NULL AFTER altura_caixa,
    ADD COLUMN comprimento_caixa DECIMAL(18, 6) NULL AFTER largura_caixa,
    ADD COLUMN origem VARCHAR(255) NULL AFTER comprimento_caixa,
    ADD COLUMN industrializado BOOLEAN NULL AFTER origem,
    ADD COLUMN insumo BOOLEAN NULL AFTER industrializado,
    ADD COLUMN percentual_maximo_aproveitamento_ipi DECIMAL(7, 4) NULL AFTER insumo,
    ADD COLUMN numero_fci VARCHAR(64) NULL AFTER percentual_maximo_aproveitamento_ipi,
    ADD COLUMN disponivel_ultima_importacao BOOLEAN NOT NULL DEFAULT TRUE
        AFTER destaque_na_home;

UPDATE produtos
SET nome_compra = nome
WHERE nome_compra IS NULL;

ALTER TABLE produtos
    ADD CONSTRAINT chk_produto_estoque CHECK (estoque >= 0),
    ADD CONSTRAINT chk_produto_preco_sem_ipi CHECK (preco_sem_ipi >= 0),
    ADD CONSTRAINT chk_produto_percentual_ipi_entrada
        CHECK (percentual_ipi_entrada IS NULL OR percentual_ipi_entrada >= 0),
    ADD CONSTRAINT chk_produto_percentual_maximo_ipi
        CHECK (
            percentual_maximo_aproveitamento_ipi IS NULL
            OR percentual_maximo_aproveitamento_ipi >= 0
        );

CREATE INDEX idx_produtos_disponibilidade
    ON produtos(disponivel_ultima_importacao);

CREATE INDEX idx_produtos_codigo_barras
    ON produtos(codigo_barras);
