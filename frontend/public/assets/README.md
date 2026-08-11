# Assets públicos

- `branding/`: logotipo e elementos da marca;
- `banners/`: imagens estáticas não administradas pelo sistema;
- `products/`: placeholders e imagens estáticas de produto;
- demais subpastas: ícones e elementos editoriais versionados com o frontend.

Arquivos deste diretório são copiados para o build e ficam públicos.

As imagens enviadas pelo administrador não devem ser adicionadas aqui. Elas são armazenadas pelo
backend em `PRODUCT_IMAGES_DIR` e servidas por `/catalogo/imagens/{nomeArquivo}`. Isso inclui as
imagens de produtos, a galeria da vitrine física e os três banners editáveis da home.
