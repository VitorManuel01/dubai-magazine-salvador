CREATE TABLE banners_home (
    posicao INT PRIMARY KEY,
    imagem_url VARCHAR(500) NULL,
    CONSTRAINT chk_banner_home_posicao
        CHECK (posicao BETWEEN 1 AND 3)
);

INSERT INTO banners_home (posicao, imagem_url)
VALUES
    (1, NULL),
    (2, NULL),
    (3, NULL);
