UPDATE produtos
SET destaque_na_home = FALSE
WHERE destaque_na_home = TRUE
  AND (
      exibir_no_site = FALSE
      OR disponivel_ultima_importacao = FALSE
  );
