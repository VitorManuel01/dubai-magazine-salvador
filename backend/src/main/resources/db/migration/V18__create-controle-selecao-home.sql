CREATE TABLE controle_selecao_home (
    id TINYINT PRIMARY KEY,
    CONSTRAINT chk_controle_selecao_home_unico CHECK (id = 1)
);

INSERT INTO controle_selecao_home (id) VALUES (1);
