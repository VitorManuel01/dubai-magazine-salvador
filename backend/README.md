# Backend — Dubai Magazine Salvador

API Spring Boot do catálogo, autenticação, importação ODS, produtos, categorias e vitrines.

Consulte a [documentação principal](../README.md), a [referência da API](../docs/API.md) e o
[guia de segurança](../docs/SEGURANCA.md).

## Requisitos

- Java 25;
- MySQL 8;
- variáveis privadas configuradas no `.env` da raiz;
- banco de desenvolvimento e banco de testes separados.

## Execução

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Porta padrão: `8081`.

O backend procura `.env` no diretório atual e no diretório pai. Quando executado dentro de
`backend`, o arquivo da raiz é carregado.

## Perfis

```powershell
$env:SPRING_PROFILES_ACTIVE='development'
$env:SPRING_PROFILES_ACTIVE='preproduction'
$env:SPRING_PROFILES_ACTIVE='production'
```

- `development`: HTTP e acesso pela rede local;
- `preproduction`: loopback, proxy e HTTPS obrigatório;
- `production`: loopback, proxy, HTTPS obrigatório e erros reduzidos.

Selecione exatamente um perfil; a inicialização rejeita perfil ausente, desconhecido ou combinado.
`test` e `migration-ci` são exclusivos da validação automatizada e não devem ser usados para servir
o catálogo.

## Banco

As migrations ficam em `src/main/resources/db/migration`. No início:

1. o Flyway aplica versões pendentes;
2. o Hibernate valida o esquema;
3. a aplicação só sobe se banco e entidades forem compatíveis.

Não use `ddl-auto=create` ou `update` fora do perfil de teste.

Em produção, `DB_APP_USERNAME`/`DB_APP_PASSWORD` autenticam as consultas normais e
`DB_MIGRATION_USERNAME`/`DB_MIGRATION_PASSWORD` autenticam o Flyway. Provisione contas diferentes:
a conta da aplicação não precisa de permissões de criação/alteração de tabelas. O Flyway mantém
`clean` desabilitado, valida checksums e nomes e não aplica baseline automaticamente.

As migrations V24–V26 acrescentam estado da conta e versão de sessão, auditoria administrativa e
índices para navegação do catálogo. O histórico completo está no
[modelo de dados](../docs/MODELO_DE_DADOS.md).

## Imagens

Configure:

```dotenv
PRODUCT_IMAGES_DIR=C:/dados/dubai/produtos
```

O diretório padrão relativo é `uploads/produtos`, adequado somente para desenvolvimento. Em
produção, use caminho persistente fora da pasta do artefato.

Uploads aceitam JPG/JPEG, PNG e WEBP até 5 MB, com extensão, MIME, assinatura e dimensões
compatíveis; cada eixo tem limite de 12.000 pixels e o total de 40 milhões de pixels. A leitura
usa `/catalogo/imagens/{UUID.ext}` e localização direta pelo nome; `/uploads/**` não é público.
Links simbólicos são rejeitados.

A limpeza diária consulta referências no banco e remove arquivos gerenciados órfãos com pelo
menos 24 horas. Configure `PRODUCT_IMAGES_CLEANUP_*` conforme a operação; preserve backups do
diretório junto com o banco.

## Sessões e contenção de abuso

O JWT usa emissor/audiência, versão de sessão e validade entre 300 e 3.600 segundos. Logout
revoga todos os tokens da conta quando concluído no backend; mudanças no estado de um funcionário
também invalidam tokens anteriores. O filtro consulta o estado atual do usuário antes de autorizar.

Além dos bloqueios do login, requisições de catálogo e imagens têm limites por origem com memória
limitada. As buscas normalizam texto, limitam tamanho e escapam curingas SQL. Pool de conexões,
tempo de consulta e conexões HTTP também possuem limites configuráveis.

A auditoria persiste metadados de mutações autenticadas, sem corpos ou tokens. IP e User-Agent
são pseudonimizados com HMAC; `AUDIT_HMAC_SECRET` é obrigatório e independente em produção. A
retenção padrão é de 180 dias. Consulte os limites operacionais no
[guia de deploy](../docs/OPERACAO_E_DEPLOY.md).

## Testes

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd clean package
```

Os testes de integração usam `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD`.

O CI executa separadamente `FlywayMigrationCiIT` com `RUN_FLYWAY_MIGRATION_TEST=true`, perfil
`migration-ci` e banco descartável identificado por `MIGRATION_TEST_DB_*`. Esse teste inicia o
Flyway e exige que o Hibernate valide o esquema resultante. Ele não é executado implicitamente
pelo comando comum `test`; consulte `.github/workflows/ci.yml` e o
[guia de desenvolvimento](../docs/DESENVOLVIMENTO.md).

## Pacotes

```text
controller/       contratos REST
domain/           entidades e DTOs
repositories/     persistência e consultas
services/         regras de negócio
infra/security/   JWT, autorização, CORS e HTTPS
resources/        profiles e migrations
```

## Regras para novas rotas

- declare explicitamente o acesso em `SecurityConfigurations`;
- use DTO de saída reduzido em rota pública;
- valide entrada com Bean Validation e regra de negócio no service;
- nunca confie em nome, MIME ou extensão de upload isoladamente;
- adicione teste funcional e teste de autorização;
- não devolva entidade JPA diretamente;
- não concatene valores do usuário em SQL;
- documente o contrato em `docs/API.md`.
