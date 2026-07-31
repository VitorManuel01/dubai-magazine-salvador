UPDATE categorias
SET exibir_no_site = TRUE
WHERE codigo <> '123'
  AND codigo NOT LIKE '123.%'
  AND codigo <> '999'
  AND codigo NOT LIKE '999.%';

UPDATE categorias
SET exibir_no_site = FALSE
WHERE codigo = '123'
   OR codigo LIKE '123.%'
   OR codigo = '999'
   OR codigo LIKE '999.%';
