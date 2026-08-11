# Desenvolvimento, testes e manutenção

## Preparação do ambiente

### Backend

1. instale Java 25;
2. crie os bancos de desenvolvimento e teste;
3. copie `.env.example` para `.env` na raiz;
4. preencha `DB_*`, `TEST_DB_*` e `JWT_SECRET`;
5. execute o Maven Wrapper dentro de `backend`.

Exemplo de bancos separados:

```sql
CREATE DATABASE catalogo_dubai
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE dubai_testes
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Nunca aponte `TEST_DB_URL` para o banco de produção.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Se o IP local mudar, ajuste `frontend/.env.development` e a origem permitida do backend. Arquivos
`VITE_*` são públicos e não podem conter credenciais.

## Comandos

### Backend

| Comando | Uso |
|---|---|
| `.\mvnw.cmd spring-boot:run` | inicia com o perfil selecionado |
| `.\mvnw.cmd test` | executa a suíte completa |
| `.\mvnw.cmd -DskipTests compile` | valida compilação sem testes |
| `.\mvnw.cmd clean package` | limpa, testa e gera o JAR |
| `.\mvnw.cmd -Dtest=NomeDoTeste test` | executa teste específico |

### Frontend

| Comando | Uso |
|---|---|
| `npm run dev` | servidor de desenvolvimento |
| `npm run lint` | ESLint |
| `npm run build` | build no modo padrão |
| `npm run build:preproduction` | build com `/api` e HTTPS obrigatório |
| `npm run build:production` | build de produção |
| `npm run preview:preproduction` | preview local na porta 4173 |
| `npm run preview` | preview do build de produção |

## Banco e migrations

O backend usa:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Portanto, o Hibernate não é o responsável pela evolução do banco normal. O fluxo correto é:

1. criar uma migration em `backend/src/main/resources/db/migration`;
2. usar o próximo número disponível;
3. iniciar a aplicação em banco descartável;
4. testar migração desde um banco vazio;
5. testar atualização de uma cópia compatível com o estado anterior;
6. executar a suíte.

Não renomeie nem edite migrations que já tenham sido aplicadas em outro ambiente.

Os testes de integração usam o perfil `test`, banco separado, Flyway desativado e
`ddl-auto=update`. Isso acelera a suíte, mas não substitui a validação manual das migrations.

## Organização de uma mudança

### Backend

Uma funcionalidade típica deve seguir:

1. DTO de entrada e saída em `domain`;
2. entidade ou migration quando houver persistência;
3. consulta em `repositories`;
4. regra transacional em `services`;
5. contrato HTTP em `controller`;
6. regra de acesso em `SecurityConfigurations`;
7. testes unitários e de autorização.

Controllers devem permanecer finos. Regras de negócio e validações que dependem de estado devem
ficar nos services.

### Frontend

Uma tela típica deve separar:

1. tipos de contrato em `interface`;
2. integração reutilizável em `hooks`;
3. utilitários puros em `utils`;
4. tela em `pages`;
5. componentes reaproveitáveis em `components`;
6. estilos responsivos no CSS do módulo.

Chamadas HTTP usam a instância global do Axios configurada em `axiosConfig.ts`. Não monte URLs do
backend diretamente dentro de componentes.

## Responsividade

O catálogo precisa funcionar a partir de 320 px. Ao alterar uma página, valide pelo menos:

- 320 × 720;
- 390 × 844;
- tablet próximo de 768 px;
- desktop a partir de 1280 px.

Regras práticas:

- use `minmax(0, 1fr)` em grids que contenham texto;
- imagens devem ter largura máxima e `object-fit` definido;
- ações precisam quebrar linha no mobile;
- tabelas e listas extensas devem possuir rolagem local, não aumentar a largura do documento;
- hover não pode ser a única forma de interação;
- mantenha labels e nomes acessíveis em botões de ícone.

## Testes atuais

A suíte do backend cobre:

- login e validação de credenciais;
- autorização do catálogo, funcionários e vitrine interna;
- geração e validação JWT;
- bloqueios de conta, IP e dispositivo;
- repositório de administrador;
- regras de produtos e imagens públicas;
- armazenamento e validação de imagens;
- parser seguro e importação ODS;
- exclusão mútua da importação;
- vitrines da home e da loja física;
- segurança do upload de imagens da vitrine.

O frontend possui lint e verificação TypeScript no build, mas ainda não possui uma suíte própria de
testes de componentes. Para mudanças visuais, combine build, inspeção responsiva e teste manual do
fluxo autenticado.

## Checklist antes de abrir PR ou fazer merge

- [ ] mudança respeita o escopo do catálogo sem reintroduzir compra ou cliente;
- [ ] nenhuma credencial ou planilha real entrou no diff;
- [ ] migrations novas foram testadas;
- [ ] DTO público não ganhou campo interno;
- [ ] permissões do backend foram revisadas;
- [ ] uploads continuam validados no backend;
- [ ] `npm run lint` passou;
- [ ] `npm run build:production` passou;
- [ ] `.\mvnw.cmd test` passou;
- [ ] mobile e desktop foram verificados;
- [ ] documentação foi atualizada;
- [ ] `git diff --check` não encontrou espaços ou conflitos;
- [ ] artefatos como `dist`, `target`, uploads e `*.tsbuildinfo` não estão no commit.

## Convenção de commits

Use Conventional Commits em português ou inglês de forma consistente:

```text
feat: adiciona gerenciamento de imagens da vitrine
fix: corrige quebra do catálogo em telas pequenas
docs: documenta importação ODS e ambientes
test: cobre autorização do upload de imagens
chore: atualiza configuração de build
```

O corpo pode listar as alterações relevantes, evitando credenciais, dados pessoais e detalhes de
infraestrutura que não precisam ser públicos.

## Diagnóstico rápido

### Porta 8081 em uso

Identifique o processo ou altere `server.port`. Não inicie duas instâncias sobre a mesma porta.

### Frontend recebe erro de CORS

No desenvolvimento, confira:

- endereço realmente usado pelo navegador;
- `CORS_ALLOWED_ORIGINS` com protocolo e porta corretos;
- ausência de `withCredentials=true`;
- backend reiniciado depois da mudança.

Em pré-produção, use `/api` na mesma origem em vez de liberar CORS para a URL temporária.

### Requisição recebe 426

O perfil exige HTTPS, mas o proxy não informou `X-Forwarded-Proto: https` ou a aplicação foi
acessada diretamente.

### Migration falha

Confira a mensagem do Flyway e a tabela `flyway_schema_history`. Não apague registros para forçar
uma migration em ambiente importante. Corrija com uma nova migration ou restaure o ambiente.

### Testes acessam o banco errado

Revise `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` e o perfil ativo. Interrompa a suíte se
o endereço apontar para dados reais.

### Imagem não aparece

Confira:

- existência do arquivo em `PRODUCT_IMAGES_DIR`;
- permissão de leitura e gravação;
- persistência do diretório após reinício ou deploy;
- URL `/catalogo/imagens/UUID.ext`;
- proxy encaminhando a rota `/api` corretamente quando a imagem usa a base da API.

## Manutenção de dependências

Antes de atualizar Java, Spring Boot, React Router, Vite ou bibliotecas de segurança:

1. leia as notas de migração da versão;
2. atualize em branch separada;
3. execute a suíte completa;
4. valide login, upload, ODS e proxy;
5. rode auditoria de dependências;
6. confirme o comportamento no Java e Node escolhidos para produção.
