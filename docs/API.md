# Referência da API

## Endereços

| Ambiente | Base usada pelo navegador |
|---|---|
| Desenvolvimento | `http://IP_LOCAL:8081` |
| Pré-produção | `https://URL_DO_TUNNEL/api` |
| Produção | `https://DOMINIO/api` |

O prefixo `/api` só existe no proxy. O Nginx e o proxy do Vite removem esse prefixo antes de
encaminhar a requisição ao Spring Boot.

## Autenticação

Rotas protegidas esperam:

```http
Authorization: Bearer TOKEN_JWT
```

O frontend também envia `X-Device-Id` com um UUID estável do navegador no login, permitindo
limitação por dispositivo. O JWT padrão expira em 3.600 segundos.

## Papéis

- `ROLE_ADMIN`: administração completa;
- `ROLE_FUNCIONARIO`: consulta da vitrine interna e do catálogo com código Santri;
- `ROLE_CLIENTE`: legado, sem rotas disponíveis no produto atual.

## Paginação

Consultas paginadas retornam o formato `Page` do Spring:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 24,
  "number": 0,
  "first": true,
  "last": true
}
```

`pagina` começa em zero.

## Autenticação e usuários

### `POST /auth/login`

Público. Autentica administrador ou funcionário.

```json
{
  "codigoSantri": "FUNCIONARIO_01",
  "senha": "senha-forte-do-usuario"
}
```

Resposta `200`:

```json
{
  "token": "jwt"
}
```

Possíveis respostas:

- `400`: campos inválidos;
- `401`: resposta uniforme para código ou senha inválidos;
- `429`: IP ou dispositivo limitado; inclui `Retry-After` em segundos.

### `POST /auth/registerADM`

Somente administrador. Cria outro administrador.

Campos:

```json
{
  "codigoSantri": "ADMIN_02",
  "senha": "senha-com-12-ou-mais-caracteres",
  "nome": "Nome do administrador",
  "CPF": "00000000000",
  "sexo": "",
  "dataNascimento": null,
  "CEP": "",
  "endereco": "",
  "bairro": "",
  "telefone": ""
}
```

Retorna `201` sem corpo. Código Santri repetido retorna `409`.

### `GET /funcionario`

Somente administrador. Lista funcionários sem retornar hashes de senha.

### `POST /funcionario`

Somente administrador. Cria funcionário.

```json
{
  "codigoSantri": "FUNC_01",
  "senha": "SenhaForte@123",
  "nomeFuncionario": "Nome do funcionário",
  "CPF": "00000000000",
  "sexo": "",
  "dataNascimento": null,
  "CEP": "",
  "bairro": "",
  "telefone": ""
}
```

Regras principais:

- código com até 50 caracteres, formado por letras, números, ponto, `_` ou `-`;
- senha entre 12 e 128 caracteres com maiúscula, minúscula, número e símbolo;
- CPF com 11 dígitos;
- telefone opcional com 10 ou 11 dígitos.

## Produtos

### `GET /produto`

Público. Retorna somente os campos seguros do catálogo.

Parâmetros:

| Parâmetro | Padrão | Descrição |
|---|---:|---|
| `categoriaCodigo` | vazio | inclui a categoria e seus descendentes |
| `busca` | vazio | pesquisa nome público e marca |
| `precoMinimo` | vazio | preço de venda com IPI mínimo, inclusivo |
| `precoMaximo` | vazio | preço de venda com IPI máximo, inclusivo |
| `somenteDestaques` | `false` | limita à Seleção da Loja |
| `pagina` | `0` | página atual |
| `tamanho` | `24` | entre 1 e 60 |

A categoria, a busca e a faixa de preço são aplicadas no banco **antes** da paginação. Por isso,
`totalElements`, `totalPages` e o conteúdo de cada página representam somente o conjunto filtrado.
Preços negativos e faixas em que `precoMinimo` é maior que `precoMaximo` retornam `400`.

Item público:

```json
{
  "idPublico": "550e8400-e29b-41d4-a716-446655440000",
  "nomeExibidoSite": "Produto",
  "marca": "Marca",
  "precoComIpi": 100.00,
  "emPromocao": true,
  "precoPromocao": 89.90,
  "porcDesconto": 10.10,
  "dataFinalProm": "2026-08-30",
  "categoriaCodigo": "001.002",
  "categoriaNome": "Categoria",
  "categoriaCaminho": "Grupo > Categoria",
  "imagemUrl": "/catalogo/imagens/uuid.webp",
  "imagemHoverUrl": "/catalogo/imagens/outro-uuid.webp",
  "imagens": [
    "/catalogo/imagens/uuid.webp",
    "/catalogo/imagens/outro-uuid.webp"
  ]
}
```

O contrato público nunca contém código Santri, código de barras ou outros dados operacionais. O
`idPublico` é usado na navegação sem revelar o código interno do Santri. A galeria aceita até oito
fotos; as posições 1 e 2 alimentam, respectivamente, a imagem normal e o hover do card.

### `GET /produto/{idPublico}`

Público. Retorna os dados comerciais seguros, a galeria completa e a descrição editorial de um
produto visível. Produtos ocultos, indisponíveis ou pertencentes a categorias internas retornam
`404`.

### `GET /interno/produtos`

Somente administrador ou funcionário. Exibe os mesmos produtos visíveis do catálogo público,
acrescenta `codigoSantri` e permite que `busca` pesquise também esse código. Não expõe os demais
campos administrativos.

### `GET /admin/produtos`

Somente administrador. Aceita os mesmos parâmetros e inclui produtos ocultos, indisponíveis e
todos os campos operacionais. Por padrão, os produtos visíveis são ordenados antes dos ocultos.
O parâmetro `apenasVisiveis=true` restringe o resultado aos produtos disponíveis no catálogo.

### `GET /admin/produtos/{idPublico}/detalhe`

Somente administrador. Retorna o detalhe mesmo quando o produto está oculto, incluindo código
Santri, galeria com identificadores internos e os demais campos administrativos.

### `PUT /admin/produtos/{codigoSantri}/descricao`

Somente administrador. Recebe `{ "descricao": "..." }`, com até 20.000 caracteres. A formatação
controlada aceita `**negrito**`, itens iniciados por `- ` e tamanhos de 12, 14, 16, 18, 20, 24,
28 ou 32 px por meio de `[tamanho=NN]texto[/tamanho]`. O frontend transforma a marcação em
elementos React e não injeta HTML livre.

### Galeria administrativa

- `POST /admin/produtos/{codigoSantri}/imagens`: adiciona uma foto por `multipart/form-data`;
- `DELETE /admin/produtos/{codigoSantri}/imagens/{imagemId}`: remove uma foto;
- `PUT /admin/produtos/{codigoSantri}/imagens/ordem`: recebe `{ "imagens": [3, 1, 2] }` com todos
  os identificadores na ordem desejada.

As três rotas exigem administrador e respeitam o máximo de oito fotos.

### `PUT /admin/produtos/visibilidade`

Somente administrador. Exibe ou oculta até 200 produtos em uma operação. Todos os códigos precisam
existir. Para exibir, os produtos também precisam estar disponíveis na última importação; caso
contrário, nenhuma alteração é aplicada. Ocultar não exclui o produto nem seus dados internos.

```json
{
  "codigosSantri": ["100", "200"],
  "exibirNoSite": false
}
```

### `POST /produto`

Somente administrador. Cadastro manual de compatibilidade. O fluxo principal de atualização é a
importação ODS.

### `PUT /produto/{codigoSantri}/apresentacao`

Somente administrador. `multipart/form-data`.

| Campo | Tipo | Obrigatório |
|---|---|:---:|
| `nomeExibidoSite` | texto | não |
| `exibirNoSite` | boolean | sim |
| `destaqueNaHome` | boolean | sim |
| `imagem` | JPG, PNG ou WEBP | não |
| `imagemHover` | JPG, PNG ou WEBP | não |

Cada imagem tem limite de 5 MB. `imagem` é a foto principal e `imagemHover` aparece quando o
visitante posiciona o mouse sobre o produto. Deixar o nome vazio restaura o nome do Santri.
Omitir qualquer imagem preserva a respectiva foto atual.

A Seleção da Loja aceita no máximo três produtos visíveis. Ao tentar marcar um quarto produto, a
API responde `409 Conflict`. As alterações são serializadas por uma trava no banco para que duas
requisições concorrentes não ultrapassem o limite. Um produto oculto ou indisponível na última
importação não pode ser selecionado.

### `DELETE /produto/{codigoSantri}`

Somente administrador. Remove manualmente um produto, suas imagens gerenciadas e suas referências
na vitrine da loja física. Se o produto continuar no próximo ODS elegível, será cadastrado novamente.

### `DELETE /admin/produtos`

Somente administrador. Remove em uma única transação até 200 produtos selecionados no catálogo
administrativo. Todos os códigos precisam existir; se algum estiver ausente, nenhum produto é
excluído. As imagens gerenciadas e referências na vitrine da loja também são removidas depois da
confirmação da transação.

```json
{ "codigosSantri": ["100", "200"] }
```

Resposta:

```json
{ "produtosExcluidos": 2 }
```

## Categorias

### `GET /categoria`

Público. Retorna categorias disponíveis para navegação.

Parâmetros:

- `nivel`: de 1 a 4;
- `categoriaPaiCodigo`: retorna filhos diretos.

### `GET /admin/categorias`

Somente administrador. Acrescenta `somenteVisiveis`, que usa `false` por padrão.

## Importação

### `POST /admin/importacoes/produtos`

Somente administrador. Recebe `multipart/form-data` com o campo `arquivo` e um ODS de até 20 MB.
Produtos inativos, com marca/fabricante `INATIVOS` ou preço menor ou igual a zero são ignorados.
Use a Relação de Produtos com estoque físico positivo; a quantidade exata é sincronizada pela
importação separada de inventário.

Resposta:

```json
{
  "arquivo": "relacao-produtos.ods",
  "categoriasLidas": 100,
  "categoriasCriadas": 10,
  "categoriasAtualizadas": 90,
  "produtosLidos": 1500,
  "produtosCriados": 200,
  "produtosAtualizados": 1300,
  "linhasIgnoradas": 20,
  "importadoEm": "2026-08-10T10:00:00",
  "duracaoMilissegundos": 3200
}
```

Uma segunda importação concorrente é rejeitada. Consulte
[Importação ODS](IMPORTACAO_ODS.md) para regras e validações.

### `POST /admin/importacoes/estoque`

Somente administrador. Recebe o ODS **Inventário** do Santri no campo `arquivo`. Apenas as colunas
`Produto` e `Quantidade` são lidas. Produtos já cadastrados são atualizados pelo código Santri;
códigos que não existem no catálogo são contabilizados e ignorados. Nenhum produto é criado e os
demais campos, incluindo a visibilidade editorial, são preservados.

```json
{
  "arquivo": "inventario.ods",
  "registrosLidos": 86919,
  "produtosAtualizados": 2000,
  "codigosIgnorados": 84919,
  "importadoEm": "2026-08-14T12:00:00",
  "duracaoMilissegundos": 2500
}
```

O leitor trabalha em fluxo e aceita até 100.000 registros distribuídos entre a planilha principal
e as abas de continuação criadas pelo Santri.

### `POST /admin/importacoes/promocoes`

Somente administrador. Recebe o ODS **Promoções de Venda** no campo `arquivo`. A planilha é a fonte
completa das promoções: atualiza produtos encontrados pelo código Santri e remove dados promocionais
antigos que não aparecem no novo relatório. Códigos inexistentes são contabilizados e ignorados.

```json
{
  "arquivo": "promocoes-venda.ods",
  "registrosLidos": 1595,
  "produtosAtualizados": 1500,
  "promocoesAtivas": 1484,
  "promocoesDesativadas": 16,
  "codigosNaoEncontrados": 95,
  "linhasIgnoradas": 120,
  "importadoEm": "2026-08-14T11:00:00",
  "duracaoMs": 1800
}
```

## Banners da home

### `GET /banners-home`

Público. Retorna as três posições em ordem. `imagemUrl` será `null` enquanto o administrador não
substituir o banner padrão.

```json
[
  { "posicao": 1, "imagemUrl": "/catalogo/imagens/uuid.webp" },
  { "posicao": 2, "imagemUrl": null },
  { "posicao": 3, "imagemUrl": null }
]
```

### `PUT /admin/banners-home/{posicao}`

Somente administrador. `posicao` deve estar entre 1 e 3. Recebe `multipart/form-data` com o campo
`imagem`, seguindo as mesmas validações de JPG, PNG ou WEBP e limite de 5 MB usadas nas imagens de
produto.

Ao concluir, retorna a posição e a nova URL pública. A imagem anterior gerenciada pela aplicação é
removida depois que a atualização do banco é concluída.

## Depoimentos da home

### `GET /depoimentos-home`

Público. Retorna os três depoimentos persistidos em ordem.

```json
[
  { "posicao": 1, "nome": "Ana", "texto": "Excelente atendimento." },
  { "posicao": 2, "nome": "Bruno", "texto": "Muita variedade." },
  { "posicao": 3, "nome": "Carla", "texto": "Equipe prestativa." }
]
```

### `PUT /admin/depoimentos-home`

Somente administrador. Atualiza as três posições em uma única transação. `nome` é obrigatório e
aceita até 80 caracteres; `texto` é obrigatório e aceita até 300. Devem ser enviadas exatamente as
posições 1, 2 e 3, sem repetição.

```json
{
  "depoimentos": [
    { "posicao": 1, "nome": "Ana", "texto": "Excelente atendimento." },
    { "posicao": 2, "nome": "Bruno", "texto": "Muita variedade." },
    { "posicao": 3, "nome": "Carla", "texto": "Equipe prestativa." }
  ]
}
```

## Vitrines da home

### `GET /vitrines-home`

Público. Lista apenas vitrines ativas em ordem, incluindo uma amostra de produtos públicos.

### `GET /admin/vitrines-home`

Somente administrador. Lista ativas e inativas.

### `POST /admin/vitrines-home`

Somente administrador.

```json
{
  "categoriaCodigo": "001",
  "titulo": "Título editorial",
  "descricao": "Descrição editorial",
  "ordem": 0,
  "ativo": true
}
```

Uma categoria só pode possuir uma vitrine.

### `PUT /admin/vitrines-home/{id}`

Somente administrador. Usa o mesmo corpo da criação.

### `DELETE /admin/vitrines-home/{id}`

Somente administrador. Retorna `204`.

## Vitrine da loja física

### `GET /vitrine-loja`

Administrador ou funcionário. Pesquisa somente vitrines ativas.

Parâmetros: `busca`, `pagina` e `tamanho` — máximo 50.

### `GET /vitrine-loja/{id}`

Administrador ou funcionário. Retorna uma vitrine ativa.

### `GET /admin/vitrine-loja`

Somente administrador. Inclui rascunhos. Aceita `busca`, `pagina` e `tamanho`, com no máximo
50 itens por página.

### `GET /admin/vitrine-loja/produtos`

Somente administrador. Pesquisa produtos candidatos, inclusive os ocultos no catálogo público.
O tamanho máximo é 60.

### `GET /admin/vitrine-loja/{id}`

Somente administrador. Retorna a vitrine pelo identificador mesmo quando ela está inativa, para
preenchimento do formulário de edição.

### `POST /admin/vitrine-loja/imagens`

Somente administrador. Recebe `multipart/form-data` com `imagem`.

Resposta `201`:

```json
{
  "url": "/catalogo/imagens/uuid.webp"
}
```

### `POST /admin/vitrine-loja`

Somente administrador.

```json
{
  "ativo": true,
  "opcoes": [
    {
      "produtoCodigoSantri": "10001",
      "rotuloOpcao": "Azul",
      "ordem": 0,
      "imagens": ["/catalogo/imagens/uuid.webp"],
      "atualizarImagens": true,
      "imagensOriginais": [],
      "secoes": [
        {
          "titulo": "Especificações",
          "conteudo": "Descrição completa.",
          "ordem": 0
        }
      ]
    }
  ]
}
```

### `PUT /admin/vitrine-loja/{id}`

Somente administrador. Substitui a configuração de opções e seções pelo corpo recebido.
As imagens pertencem ao produto, e não à vitrine: por isso, qualquer alteração aparece também no
catálogo. `imagensOriginais` funciona como controle de concorrência e impede que um formulário
antigo sobrescreva uma galeria modificada em outra tela.

### `DELETE /admin/vitrine-loja/{id}`

Somente administrador. Retorna `204`.

## Imagens públicas

### `GET /catalogo/imagens/{nomeArquivo}`

Público. Serve somente UUIDs gerados pela aplicação com extensão JPG, PNG ou WEBP. A resposta
possui cache público de 30 dias.

`GET /uploads/produtos/**` permanece permitido por compatibilidade com referências antigas, mas
novos contratos devem usar `/catalogo/imagens/...`.

## Actuator

`/actuator/**` exige administrador. Não exponha endpoints adicionais sem revisar dados sensíveis e
as regras de autorização.

## Erros comuns

| Status | Significado típico |
|---:|---|
| `400` | parâmetros, DTO ou arquivo inválido |
| `401` | credenciais ou JWT inválidos |
| `403` | usuário autenticado sem a função exigida |
| `404` | entidade ou imagem não encontrada |
| `409` | código duplicado, produto em outra vitrine ou importação concorrente |
| `413` | imagem ou requisição acima do limite |
| `426` | HTTP usado em ambiente que exige HTTPS |
| `429` | limite de tentativas de login atingido |

Em pré-produção e produção, mensagens internas e stack traces não são retornados ao cliente.
