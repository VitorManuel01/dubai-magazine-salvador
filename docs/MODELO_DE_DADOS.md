# Modelo de dados e regras de catálogo

## Convenções

- chaves de usuário são UUIDs persistidos como `VARCHAR(36)`;
- o código Santri identifica produtos e contas internas;
- códigos hierárquicos de categoria podem ter até quatro níveis;
- datas de auditoria da vitrine usam precisão de microssegundos;
- valores monetários usam `DECIMAL`/`BigDecimal`, nunca `double`;
- o esquema é criado e evoluído exclusivamente pelo Flyway.

## Preços personalizados por produto (V27)

- `usar_precos_personalizados`: desligado por padrão, independente para cada produto.
- `preco_a_vista` e `preco_cartao_parc`: `DECIMAL(12,2)`, positivos. O segundo é o total no cartão, não o valor da parcela.
- `max_parcelamento`: inteiro de 1 a 36; os três valores são obrigatórios ao ativar o modo personalizado.
- São valores finais: não recebem o IPI do Santri novamente. Não se aplica a promoção do Santri sobre eles.
- Desligar restaura a apresentação normal do Santri, incluindo promoção vigente quando aplicável, sem apagar os valores manuais.
- Importações continuam atualizando preços e promoções do ERP, mas não sobrescrevem estes quatro campos locais.
- Catálogo, home, detalhes e vitrine física usam o modo do produto. O filtro de faixa e a ordenação por preço usam o valor à vista quando personalizado; desligado, conservam o cálculo normal com IPI.
- A API pública omite os valores personalizados enquanto desligados; o administrador os recebe para edição.

## Relacionamentos

```mermaid
erDiagram
    USUARIOS ||--o| ADMINISTRADORES : especializa
    USUARIOS ||--o| FUNCIONARIOS : especializa
    CATEGORIAS ||--o{ CATEGORIAS : categoria_pai
    CATEGORIAS ||--o{ PRODUTOS : classifica
    CATEGORIAS ||--o| VITRINES_HOME : apresenta
    PRODUTOS ||--o{ IMAGENS_PRODUTO : possui
    VITRINES_LOJA ||--|{ PRODUTOS_VITRINE_LOJA : possui
    PRODUTOS ||--o| PRODUTOS_VITRINE_LOJA : representa
    PRODUTOS_VITRINE_LOJA ||--o{ SECOES_PRODUTO_VITRINE_LOJA : descreve
```

## Usuários

### `usuarios`

Armazena os campos comuns de autenticação:

- UUID;
- código Santri único;
- hash BCrypt da senha;
- função `ROLE_ADMIN`, `ROLE_FUNCIONARIO` ou legado `ROLE_CLIENTE`;
- quantidade de falhas consecutivas;
- instante até o qual a conta permanece bloqueada;
- `ativo`: controla se a conta pode autenticar e usar tokens;
- `versao_token`: comparada com o claim `versao` em cada requisição autenticada.

O e-mail foi removido do modelo atual de autenticação. O login é exclusivamente pelo código
Santri.

Logout e alterações do estado de funcionário incrementam a versão. Reativar uma conta não
reativa tokens antigos; é necessário novo login. A V24 inicializa contas existentes como ativas
e com versão zero. Tokens emitidos antes da adoção do claim de versão deixam de ser aceitos.

### `administradores` e `funcionarios`

São especializações um-para-um de `usuarios`. Guardam dados cadastrais como nome, CPF, sexo,
data de nascimento, CEP, bairro e telefone. Administradores também possuem endereço.

As tabelas e a entidade legada de cliente ainda existem por histórico de migrations, mas suas
rotas são negadas e o produto atual não oferece cadastro nem login de cliente.

A resposta de listagem/cadastro de funcionário expõe apenas `id`, `codigoSantri`,
`nomeFuncionario`, `funcao` e `ativo`. A minimização da resposta não remove os dados cadastrais
necessários já armazenados nas tabelas internas.

### `auditoria_administrativa`

A V25 cria a trilha com usuário, código Santri, função, método HTTP, caminho, status e horário.
IP e User-Agent são guardados como HMAC de 64 caracteres; o corpo da requisição, tokens e query
string não são armazenados. O vínculo ao usuário usa `ON DELETE SET NULL`, preservando a trilha
se o cadastro for removido. Índices por horário e usuário/horário apoiam consulta e retenção.

A retenção padrão é de 180 dias, com limpeza diária de até 10.000 registros por execução.
Esse limite pode fazer a exclusão levar mais de um ciclo em uma base com grande volume antigo.

## Categorias

`categorias` possui:

| Campo | Uso |
|---|---|
| `codigo` | identificador hierárquico vindo do relatório |
| `nome` | nome do nível atual |
| `nivel` | valor de 1 a 4 |
| `caminho` | caminho legível completo |
| `categoria_pai_codigo` | autorrelacionamento para o nível anterior |
| `exibir_no_site` | decisão de disponibilidade pública |

Exemplo de árvore:

```text
001 Utilidades
└── 001.010 Cozinha
    └── 001.010.020 Panelas
```

### Grupos com tratamento especial

| Código raiz | Tratamento atual |
|---|---|
| `008` — Uso e Consumo | excluído pelo backend das respostas públicas |
| `089` — Ativo Imobilizado | produtos e categorias removidos durante a importação |
| `123` — Ajustes de Grupos | mantido para administração, ocultado do público |
| `999` — Implantação | mantido para administração, ocultado do público |

As regras abrangem o código raiz e todos os descendentes, por exemplo `123.001`.

`008`, `123` e `999` continuam excluídas do catálogo, detalhe e amostras públicas da home mesmo
se `exibir_no_site` for alterado indevidamente para `true`. A interface não é a única barreira.

## Produtos

`produtos.codigo_santri` é a chave primária. Os campos estão divididos em dois grupos.

### Dados sincronizados do Santri

- nome original e nome de compra;
- NCM, fabricante e marca;
- estado ativo e bloqueio para compras;
- unidades de venda e compra;
- data de cadastro, código original e código de barras;
- estoque;
- preço sem IPI e percentual de IPI de entrada;
- peso, altura, largura, comprimento e volume da unidade;
- peso e dimensões da caixa;
- origem, indicadores de industrializado e insumo;
- percentual máximo de aproveitamento de IPI e número FCI;
- categoria;
- instante e disponibilidade da última importação.

### Promoções

Os campos `em_promocao`, `data_inicial_prom`, `data_final_prom`, `porc_margem`, `porc_desconto`,
`preco_promocao` e `especial` vêm do relatório de Promoções de Venda. A vigência exibida é calculada
com a data atual, evitando que um preço vencido continue público mesmo antes da próxima importação.

### Dados editoriais da aplicação

- `id_publico`, UUID usado nas URLs públicas sem revelar o código Santri;
- `nome_exibido_site`;
- `descricao_site`, com a descrição única da página detalhada;
- `exibir_no_site`;
- `destaque_na_home`.

Esses campos editoriais não são substituídos indiscriminadamente pela importação. O nome público
só volta a acompanhar o Santri quando ainda está vazio ou igual ao nome anterior.

### `imagens_produto`

É a galeria editorial única do produto, com no máximo oito registros ordenados. A primeira posição
é a imagem normal do card e a segunda é exibida no hover; a página detalhada e a vitrine física
consomem a coleção completa. Assim, uma edição feita em qualquer uma das telas é refletida nas
demais. As colunas antigas `imagem_url` e `imagem_hover_url` permanecem apenas para compatibilidade
com dados anteriores e foram migradas para essa galeria pela V23.

`descricao_site` aceita somente a marcação controlada da aplicação para negrito, lista e tamanho
de fonte. O frontend converte essa marcação em elementos React, sem executar HTML armazenado.

### Preço com IPI

O banco mantém o preço sem IPI e o percentual recebido. O preço público é calculado em memória:

```text
preço com IPI = preço sem IPI × (1 + percentual IPI / 100)
```

O resultado é arredondado para duas casas com `HALF_UP`.

### Produto público

Um produto entra na consulta pública quando:

- `exibir_no_site = true`;
- `disponivel_ultima_importacao = true`;
- atende à categoria, busca e destaque solicitados.

O DTO público não expõe campos operacionais como estoque, código Santri ou código de barras.
Administradores e funcionários usam um contrato interno mínimo que acrescenta somente o código
Santri necessário à consulta; o DTO administrativo completo continua exclusivo do administrador.

## Vitrines da home

Cada registro de `vitrines_home` referencia uma categoria única e guarda:

- título;
- descrição;
- ordem;
- estado ativo.

A resposta pública associa até quatro produtos visíveis daquela árvore de categoria. A interface
apresenta os itens de forma rotativa.

## Banners da home

`banners_home` possui exatamente três posições, criadas pela migration. Cada posição guarda a URL
da imagem escolhida pelo administrador. Enquanto a URL estiver vazia, o frontend utiliza o banner
padrão correspondente.

As imagens ficam em `PRODUCT_IMAGES_DIR`, e o banco armazena somente a URL pública com UUID. Ao
substituir um banner, a aplicação remove o arquivo anterior quando ele foi gerado pelo próprio
sistema.

## Depoimentos da home

`depoimentos_home` possui exatamente três posições. Cada registro armazena o nome exibido do
cliente, limitado a 80 caracteres, e o texto do depoimento, limitado a 300 caracteres. A leitura é
pública, mas a atualização das três posições exige autenticação de administrador e ocorre em uma
única transação.

## Vitrine da loja física

### `vitrines_loja`

Agrupa uma apresentação interna e controla se ela está ativa para funcionários.

### `produtos_vitrine_loja`

Cada opção referencia um produto real e possui rótulo e ordem. Uma vitrine pode representar
variações como cores ou capacidades usando produtos diferentes. Um produto não pode estar em
duas vitrines.

Limites da regra de negócio:

- até 20 opções por vitrine;
- até 8 imagens compartilhadas por produto;
- até 30 seções por opção;
- rótulo com até 100 caracteres;
- título de seção com até 180 caracteres;
- conteúdo de seção com até 10.000 caracteres;
- vitrine ativa exige ao menos uma seção em cada opção.

### Imagens e seções

A vitrine não mantém uma galeria independente. Cada opção utiliza `imagens_produto`, pertencente
ao produto referenciado. Ao salvar uma edição, a versão original da lista é enviada para impedir
que um formulário antigo sobrescreva imagens alteradas simultaneamente em outra tela.

`secoes_produto_vitrine_loja` guarda blocos de título e texto, como resumo, motor, consumo,
dimensões e características. O modelo não fixa esses títulos, permitindo representar produtos
além de veículos.

## Comportamento da sincronização ODS

- categorias e produtos existentes são atualizados por chave;
- novos registros são inseridos;
- a gravação é feita em lotes de 1.000;
- produtos ausentes na importação não são apagados;
- produtos ausentes recebem `disponivel_ultima_importacao = false`, são ocultados e deixam de ser
  destaque;
- produtos que reaparecem voltam a ficar disponíveis, mas a decisão editorial de exibição é
  preservada quando aplicável;
- preço sem valor positivo força ocultação;
- ativo imobilizado é removido;
- ajustes de grupos e implantação permanecem ocultos.

## Migrations

| Versão | Objetivo |
|---|---|
| V1 | criação de usuários |
| V2 | estrutura legada de clientes |
| V3 | funcionários |
| V4 | administradores |
| V5 | categorias hierárquicas |
| V6 | estrutura inicial de produtos |
| V7 | destaque de produto na home |
| V8 | vitrines da home |
| V9 | regras iniciais de categorias internas |
| V10 | nome exibido no site |
| V11 | vitrine da loja física, imagens e seções |
| V12 | adaptação de produtos ao relatório analítico |
| V13 | correções de visibilidade pública |
| V14 | controle de falhas e bloqueio de login |
| V15 | autenticação por código Santri e remoção de e-mail |
| V16 | criação e preenchimento das três posições de banners da home |
| V17 | criação e preenchimento dos três depoimentos persistentes da home |
| V18 | controle transacional do limite de três produtos na Seleção da Loja |
| V19 | compatibilidade do identificador do controle da Seleção da Loja com JPA |
| V20 | remoção de destaques ocultos ou indisponíveis da Seleção da Loja |
| V21 | dados de preço e período das promoções de venda |
| V22 | segunda imagem do produto para hover no catálogo |
| V23 | identificador público, descrição e galeria compartilhada de até oito fotos |
| V24 | estado ativo do usuário e versão de sessão para revogação de JWT |
| V25 | trilha de auditoria administrativa e índices de consulta/retenção |
| V26 | índices de navegação do catálogo por categoria, visibilidade, disponibilidade e ordem |

Nunca altere uma migration que já foi aplicada em um ambiente compartilhado. Toda mudança futura
de esquema deve usar a próxima versão disponível após conferir o diretório de migrations.

## Recriação do banco

Em desenvolvimento, um banco descartável pode ser recriado do zero e o Flyway reaplicará todas
as migrations. Em produção, apagar o banco não é uma estratégia de atualização. Faça backup e
deixe o Flyway aplicar somente as versões pendentes.
