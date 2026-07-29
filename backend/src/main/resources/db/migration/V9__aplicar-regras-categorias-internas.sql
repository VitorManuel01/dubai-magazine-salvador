DELETE FROM vitrines_home
WHERE categoria_codigo = '089'
   OR categoria_codigo LIKE '089.%';

DELETE FROM produtos
WHERE categoria_codigo = '089'
   OR categoria_codigo LIKE '089.%';

DELETE FROM categorias
WHERE nivel = 4
  AND (codigo = '089' OR codigo LIKE '089.%');

DELETE FROM categorias
WHERE nivel = 3
  AND (codigo = '089' OR codigo LIKE '089.%');

DELETE FROM categorias
WHERE nivel = 2
  AND (codigo = '089' OR codigo LIKE '089.%');

DELETE FROM categorias
WHERE nivel = 1
  AND (codigo = '089' OR codigo LIKE '089.%');

UPDATE categorias
SET exibir_no_site = FALSE
WHERE codigo = '123'
   OR codigo LIKE '123.%';

UPDATE produtos
SET exibir_no_site = FALSE,
    destaque_na_home = FALSE
WHERE categoria_codigo = '123'
   OR categoria_codigo LIKE '123.%';
