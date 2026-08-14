# Frontend — Dubai Magazine Salvador

Aplicação React responsável pelo catálogo público, páginas institucionais, administração e vitrine
interna da loja física.

Consulte também o [README principal](../README.md) e a
[documentação de arquitetura](../docs/ARQUITETURA.md).

## Stack

- React 18 e TypeScript;
- Vite 8;
- React Router;
- TanStack Query;
- Axios;
- Bootstrap e Bootstrap Icons;
- ESLint.

## Instalação

```powershell
cd frontend
npm install
npm run dev
```

O servidor usa a porta `5173`.

## Ambientes

| Arquivo | API | HTTPS obrigatório |
|---|---|:---:|
| `.env.development` | endereço local completo | não |
| `.env.preproduction` | `/api` | sim |
| `.env.production` | `/api` | sim |

Variáveis disponíveis:

```dotenv
VITE_APP_ENV=development
VITE_API_BASE_URL=http://IP_LOCAL:8081
VITE_REQUIRE_HTTPS=false
```

Toda variável `VITE_*` é pública. Não coloque senha, token, chave de API ou credencial de banco
nesses arquivos.

## Scripts

```powershell
npm run dev
npm run lint
npm run build
npm run build:preproduction
npm run build:production
npm run preview:preproduction
npm run preview
```

O build executa TypeScript antes do Vite. Erros de tipo impedem a geração do `dist`.

## Proxy

Em pré-produção e produção, a base da API é `/api`. O proxy do Vite para desenvolvimento/preview
remove o prefixo e encaminha para `http://127.0.0.1:8081`.

Quando o modo exige HTTPS, o proxy informa ao Spring Boot:

```http
X-Forwarded-Proto: https
X-Forwarded-Port: 443
```

Em produção real, Nginx ou o proxy da hospedagem assume esse papel.

## Estrutura

```text
src/
├── components/    componentes reutilizáveis
├── config/        Axios e configuração de integração
├── context/       autenticação global
├── hooks/         queries e mutations
├── interface/     contratos TypeScript
├── pages/         telas ligadas ao roteador
├── styles/        estilos compartilhados
└── utils/         arquivos, imagens, dispositivo e categorias
```

## Autenticação

`AuthProvider` lê o JWT do `localStorage`, valida sua expiração e disponibiliza:

- `isAuthenticated`;
- `funcao`;
- `login(token)`;
- `logout()`.

O interceptor do Axios adiciona o Bearer token e mantém cookies desativados. No login,
`obterIdDispositivo()` cria um UUID persistente e envia `X-Device-Id`.

`RotaAdmin` e `RotaVitrineInterna` controlam os redirecionamentos visuais. A autorização real é
repetida no backend.

## Catálogo

O catálogo usa parâmetros na URL:

```text
/produtos?categoria=001&busca=termo&pagina=0
```

Visitantes recebem somente o DTO público. Administradores usam os endpoints administrativos e
podem ver itens ocultos, editar o nome público, imagem, visibilidade e destaque.

A categoria interna Uso e Consumo é excluída da interface pública por
`src/utils/categoriasCatalogo.ts`.

### Cards de produto

No catálogo público, todos os cards têm altura fixa de **290 px**, imagem de **170 px** e nome
limitado a duas linhas. A interface do card exibe somente o nome e os preços: código Santri,
estoque, marca, categoria e data final da promoção não são renderizados.

Quando não há promoção, o preço de venda é maior e fica na parte inferior do card. Quando há
promoção, o preço anterior aparece riscado e o preço promocional, acompanhado do desconto, recebe
destaque.

No catálogo administrativo, os cards têm altura fixa de **500 px**. Código, estoque, marca e
categoria são organizados em linhas de rótulo e valor; quando necessário, apenas a área de dados
rola internamente. A edição preserva a altura do card e disponibiliza os campos em uma área rolável.

Ao final do catálogo administrativo há atalhos para as três importações: relação de produtos,
promoções de venda e atualização de estoque.

## Vitrines

### Home

Os cards de categoria vêm do backend. Título e descrição são editoriais; os produtos são
selecionados dinamicamente a partir da categoria e das regras públicas.

O carrossel sempre possui três posições. Enquanto uma posição não foi personalizada, ela usa o
placeholder padrão. Administradores veem **Trocar imagem** diretamente no banner ativo e podem
selecionar JPG, PNG ou WEBP de até 5 MB; visitantes e funcionários apenas visualizam os banners.

### Loja física

A consulta é exclusiva para administradores e funcionários. O administrador configura:

- produtos que representam opções ou cores;
- rótulo de cada opção;
- até 20 fotos por opção;
- seções de título e texto;
- estado ativo ou rascunho.

O botão **Adicionar foto** cria um card local. A imagem é validada, recebe prévia e só é enviada
quando a vitrine é salva.

## Assets

Assets públicos ficam em `public/assets`:

- `branding/`: logotipos;
- `banners/`: reservado para banners estáticos não administrados pelo sistema;
- `products/`: placeholders e imagens fixas;
- demais subpastas: ícones e elementos editoriais.

Imagens enviadas pelo administrador não pertencem ao frontend. Elas ficam no diretório
`PRODUCT_IMAGES_DIR` do backend, inclusive os banners editáveis da home.

Use caminhos absolutos públicos:

```tsx
<img src="/assets/branding/DubaiMagazine_Principal_Azul.png" alt="Dubai Magazine" />
```

## Responsividade

O layout deve funcionar a partir de 320 px. Antes de concluir mudanças, verifique:

- cabeçalho e barra de contatos;
- navegação horizontal de categorias;
- busca;
- cards do catálogo;
- paginação e filtros;
- rodapé;
- login e formulários administrativos;
- galerias e botões da vitrine física.

Não permita que um componente aumente `document.scrollWidth`. Rolagem horizontal só deve existir
dentro de elementos explicitamente roláveis, como a faixa de categorias.

## Validação de arquivos

`src/utils/validacaoArquivos.ts` faz uma validação antecipada:

- ODS: extensão, 20 MB e assinatura ZIP;
- imagens: 5 MB, extensão e magic bytes de JPG, PNG ou WEBP.

O backend repete a validação e é a autoridade final.

## Adicionando uma tela

1. crie os tipos de API em `interface`;
2. extraia chamadas reutilizáveis para `hooks`;
3. implemente a página em `pages`;
4. adicione estilos responsivos;
5. registre a rota em `App.tsx`;
6. aplique `RotaAdmin` ou `RotaVitrineInterna` quando necessário;
7. confira se o backend possui a mesma autorização;
8. execute lint, build e validação visual.

## Antes de commitar

```powershell
npm run lint
npm run build:production
```

Não versione:

- `node_modules`;
- `dist`;
- cache do Vite;
- arquivos `*.local`;
- `*.tsbuildinfo` gerado;
- credenciais;
- planilhas reais;
- imagens enviadas por usuários.
