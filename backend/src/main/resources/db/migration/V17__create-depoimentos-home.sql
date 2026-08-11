CREATE TABLE depoimentos_home (
    posicao INT PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    texto VARCHAR(300) NOT NULL,
    CONSTRAINT chk_depoimento_home_posicao
        CHECK (posicao BETWEEN 1 AND 3)
);

INSERT INTO depoimentos_home (posicao, nome, texto)
VALUES
    (1, 'Cliente Dubai Magazine', 'Ótimo atendimento e variedade de produtos.'),
    (2, 'Cliente Dubai Magazine', 'Equipe atenciosa e pronta para ajudar.'),
    (3, 'Cliente Dubai Magazine', 'Uma experiência de compra prática e agradável.');
