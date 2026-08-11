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

## Banco

As migrations ficam em `src/main/resources/db/migration`. No início:

1. o Flyway aplica versões pendentes;
2. o Hibernate valida o esquema;
3. a aplicação só sobe se banco e entidades forem compatíveis.

Não use `ddl-auto=create` ou `update` fora do perfil de teste.

## Imagens

Configure:

```dotenv
PRODUCT_IMAGES_DIR=C:/dados/dubai/produtos
```

O diretório padrão relativo é `uploads/produtos`, adequado somente para desenvolvimento. Em
produção, use caminho persistente fora da pasta do artefato.

## Testes

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd clean package
```

Os testes de integração usam `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD`.

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
