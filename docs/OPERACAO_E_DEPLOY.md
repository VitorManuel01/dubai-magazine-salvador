# Operação, ambientes e deploy

## Matriz de ambientes

| Ambiente | Frontend | Backend | HTTPS | Objetivo |
|---|---|---|:---:|---|
| `development` | Vite `0.0.0.0:5173` | `0.0.0.0:8081` | não obrigatório | desenvolvimento na rede local |
| `preproduction` | preview `127.0.0.1:4173` | `127.0.0.1:8081` | obrigatório externamente | homologação por Cloudflare Tunnel |
| `production` | arquivos estáticos no proxy | `127.0.0.1:8081` | obrigatório | domínio oficial |

Pré-produção e produção usam `VITE_API_BASE_URL=/api`, mantendo frontend e API na mesma origem.

Defina exatamente um `SPRING_PROFILES_ACTIVE`. O backend rejeita perfil ausente, desconhecido ou
combinações como `development,production`. Os perfis `test` e `migration-ci` existem somente para
testes; não são alternativas de publicação.

## Variáveis do backend

### Obrigatórias

| Variável | Exemplo seguro | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/catalogo_dubai` | banco principal |
| `DB_USERNAME` / `DB_PASSWORD` | usuário dedicado / segredo externo | desenvolvimento e pré-produção |
| `DB_APP_USERNAME` / `DB_APP_PASSWORD` | conta da aplicação / segredo externo | consultas e gravações em produção, sem DDL |
| `DB_MIGRATION_USERNAME` / `DB_MIGRATION_PASSWORD` | conta de migrations / segredo externo | Flyway em produção |
| `JWT_SECRET` | valor aleatório de pelo menos 32 bytes | assinatura JWT |
| `AUDIT_HMAC_SECRET` | outro valor aleatório de pelo menos 32 bytes | HMAC da auditoria, obrigatório em produção |
| `SPRING_PROFILES_ACTIVE` | `production` | perfil ativo |

### Operacionais

| Variável | Padrão | Uso |
|---|---|---|
| `SERVER_ADDRESS` | depende do perfil | interface de rede |
| `CORS_ALLOWED_ORIGINS` | local no desenvolvimento; vazio nos publicados | origens permitidas |
| `PRODUCT_IMAGES_DIR` | `uploads/produtos` | imagens persistentes |
| `JWT_EXPIRATION_SECONDS` | `3600` | validade entre 300 e 3.600 segundos |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `dubai-magazine-api` / `dubai-magazine-interno` | fronteira de aceitação dos JWTs |
| `LOGIN_MAX_FAILED_ATTEMPTS` | `3` | falhas por conta |
| `LOGIN_LOCK_DURATION` | `PT20M` | bloqueio da conta |
| `LOGIN_IP_MAX_FAILED_ATTEMPTS` | `20` | limite por IP |
| `LOGIN_DEVICE_MAX_FAILED_ATTEMPTS` | `5` | limite por dispositivo |
| `LOGIN_ORIGIN_WINDOW` | `PT20M` | janela do rate limit |
| `LOGIN_ORIGIN_BLOCK_DURATION` | `PT20M` | bloqueio da origem |
| `LOGIN_INITIAL_DELAY` | `PT1S` | atraso progressivo inicial |
| `LOGIN_MAX_DELAY` | `PT30S` | atraso progressivo máximo |
| `LOGIN_MAX_TRACKED_ORIGINS` | `50000` | teto de origens em memória |
| `PUBLIC_RATE_LIMIT_ENABLED` | `true` | contenção no backend para catálogo/imagens |
| `PUBLIC_CATALOG_RATE_CAPACITY` / `PUBLIC_CATALOG_RATE_REFILL` | `60` / `1` | rajada de consultas / reposição por segundo |
| `PUBLIC_CATALOG_MAX_CONCURRENT` | `4` | consultas públicas simultâneas no processo; excesso responde 503 com `Retry-After` |
| `PUBLIC_IMAGE_RATE_CAPACITY` / `PUBLIC_IMAGE_RATE_REFILL` | `240` / `4` | rajada de imagens / reposição por segundo |
| `PUBLIC_RATE_MAX_ORIGINS` / `PUBLIC_RATE_ENTRY_TTL` | `50000` / `PT20M` | teto de registros e descarte por inatividade |
| `DB_POOL_MAX_SIZE` / `DB_POOL_MIN_IDLE` | `10` / `2` | conexões máximas / ociosas mínimas |
| `DB_CONNECTION_TIMEOUT_MS` / `DB_VALIDATION_TIMEOUT_MS` | `10000` / `3000` | espera por conexão / validação |
| `DB_QUERY_TIMEOUT_MS` | `5000` | timeout JPA; verificar aplicação pelo driver/consulta |
| `PRODUCT_IMAGES_CLEANUP_ENABLED` | `true` | limpeza agendada de arquivos órfãos |
| `PRODUCT_IMAGES_CLEANUP_MINIMUM_AGE` | `PT24H` | idade mínima para remoção de órfão |
| `PRODUCT_IMAGES_CLEANUP_CRON` | `0 15 3 * * *` | execução diária às 03h15 no fuso do servidor |
| `AUDIT_RETENTION` | `P180D` | retenção da auditoria |
| `AUDIT_CLEANUP_MAX_ROWS` | `10000` | máximo removido por execução |
| `AUDIT_CLEANUP_CRON` | `0 40 3 * * *` | execução diária às 03h40 no fuso do servidor |

Durações usam o formato ISO-8601 do Java, como `PT20M` e `PT1S`.

Os limites HTTP também podem ser ajustados por `SERVER_CONNECTION_TIMEOUT` (5 s),
`SERVER_MAX_CONNECTIONS` (200), `SERVER_ACCEPT_COUNT` (100), `SERVER_MAX_THREADS` (80) e
`SERVER_MIN_SPARE_THREADS` (8). Ajuste após medir carga e recursos disponíveis; aumentar limites
sem medir pode apenas transferir a saturação para o banco.

Provisione contas de banco distintas e conceda somente o necessário a cada uma. O código separa
as credenciais, mas a configuração de permissões cabe ao DBA. `AUDIT_HMAC_SECRET` pode usar o
segredo JWT como fallback fora de produção; produção exige configuração explícita. Ao trocar
`JWT_SECRET`, todos os tokens antigos deixam de validar.

## Desenvolvimento

Backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE='development'
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm run dev
```

No desenvolvimento, ajuste `frontend/.env.development` e `CORS_ALLOWED_ORIGINS` quando o endereço
da máquina mudar.

## Pré-produção com um Quick Tunnel

É necessário somente um túnel. O tráfego segue:

```text
Internet HTTPS
  → cloudflared
  → Vite preview em 127.0.0.1:4173
  → /api encaminhado para Spring Boot em 127.0.0.1:8081
```

### Terminal 1 — backend

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE='preproduction'
.\mvnw.cmd spring-boot:run
```

### Terminal 2 — frontend

```powershell
cd frontend
npm run build:preproduction
npm run preview:preproduction
```

### Terminal 3 — túnel

```powershell
cloudflared tunnel --url http://127.0.0.1:4173
```

Abra a URL `https://...trycloudflare.com` gerada. Ela muda a cada reinício do Quick Tunnel.

O preview envia ao backend os cabeçalhos encaminhados que indicam HTTPS. O backend continua sem
exposição direta e rejeita requisições que aparentem ter chegado por HTTP.

O backend usa `RemoteIpValve` com confiança somente em conexões vindas de loopback. O preview
remove cabeçalhos encaminhados do cliente e aproveita `CF-Connecting-IP` válido apenas na conexão
local do túnel. Ao mudar a localização do proxy, revise os IPs confiáveis e o firewall; não amplie
a confiança para toda a rede.

Quick Tunnel serve para homologação temporária. Não oferece endereço estável nem substitui uma
configuração de produção.

## Build de produção

### Frontend

```powershell
cd frontend
npm ci
npm run lint
npm run build:production
```

Artefato: conteúdo de `frontend/dist`.

### Backend

```powershell
cd backend
.\mvnw.cmd clean package
```

Artefato esperado: JAR em `backend/target`.

Teste os dois artefatos antes de copiá-los para o servidor.

## Contrato do proxy de produção

O proxy precisa:

1. redirecionar HTTP para HTTPS;
2. servir `frontend/dist` como SPA;
3. usar fallback para `index.html` nas rotas do React;
4. encaminhar `/api/*` para `http://127.0.0.1:8081/*` removendo `/api`;
5. enviar Host, IP e cabeçalhos `X-Forwarded-*`;
6. aceitar upload de até 20 MB;
7. não expor a porta 8081;
8. configurar certificado e TLS modernos;
9. adicionar cabeçalhos de segurança;
10. preservar timeouts suficientes para importação ODS.

O modelo também rejeita hosts desconhecidos, limita requisições/conexões e sobrescreve o IP
encaminhado. A regra `location ^~ /api/` tem prioridade sobre extensões estáticas, incluindo as
imagens servidas pelo backend. CSP permite apenas recursos locais e bloqueia script/estilo inline.

O arquivo [Nginx de exemplo](../deploy/nginx/dubai-magazine.conf.example) implementa esse contrato.
Se a hospedagem usar Apache, IIS ou proxy próprio, reproduza o comportamento, não necessariamente a
mesma sintaxe.

Substitua domínio/caminhos, adapte os blocos ao `nginx.conf` existente e valide a configuração no
servidor antes de recarregar. Se houver Cloudflare ou outro proxy antes do Nginx, configure a
restauração do IP real confiando somente nas redes oficiais desse proxy; caso contrário, os limites
podem tratar visitantes diferentes como uma única origem. Não aceite cabeçalhos de IP fornecidos
livremente pelo cliente.

## Inicialização do backend em produção

Exemplo conceitual no PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE='production'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/catalogo_dubai'
$env:DB_APP_USERNAME='usuario_da_aplicacao'
$env:DB_APP_PASSWORD='fornecida_por_cofre'
$env:DB_MIGRATION_USERNAME='usuario_das_migrations'
$env:DB_MIGRATION_PASSWORD='outra_senha_fornecida_por_cofre'
$env:JWT_SECRET='fornecido_por_cofre'
$env:AUDIT_HMAC_SECRET='outro_segredo_fornecido_por_cofre'
$env:PRODUCT_IMAGES_DIR='D:\dados\dubai\produtos'
java -jar .\dubaiMagazineSalvador-0.0.1-SNAPSHOT.jar
```

No Linux, prefira uma unidade `systemd` ou o gerenciador oferecido pela hospedagem. Não grave os
segredos diretamente no arquivo de serviço quando houver cofre ou mecanismo próprio de secrets.

Os valores acima são ilustrativos: os dois segredos precisam cumprir o mínimo de 32 bytes
aleatórios. A inicialização executa as migrations com a conta própria e valida o esquema; `clean`
do Flyway e baseline automático estão desativados. O backend encerra de forma graciosa, com janela
de 30 segundos, mas uma importação deve terminar antes de desligamentos planejados.

## Persistência de imagens

`PRODUCT_IMAGES_DIR` precisa:

- estar fora da pasta substituída em cada deploy;
- sobreviver a reinícios e novas versões;
- permitir escrita somente ao usuário da aplicação;
- impedir execução de arquivos;
- ter espaço monitorado;
- entrar no plano de backup junto com o banco.

Banco e diretório de imagens formam um único conjunto lógico. Restaurar apenas um deles pode deixar
URLs sem arquivo ou arquivos sem referência.

Arquivos órfãos gerenciados recebem limpeza diária após a janela mínima de 24 horas. Isso permite
recolher uploads de formulários abandonados. Referências antigas no banco participam da verificação.
Monitore falhas dessa rotina; a limpeza não substitui limite de disco ou backup.

## Backups

### Antes de deploy ou importação importante

1. gere dump consistente do MySQL;
2. copie ou fotografe o diretório de imagens;
3. registre versão do JAR e hash do build do frontend;
4. verifique se o backup pode ser lido;
5. mantenha retenção adequada fora do mesmo disco.

### Restauração

1. interrompa escrita da aplicação;
2. restaure o banco em uma instância isolada;
3. restaure as imagens correspondentes;
4. execute a mesma versão do backend usada no backup;
5. valide migrations e amostras de imagens;
6. só então troque o tráfego.

Teste o processo periodicamente. Backup não testado não é garantia de recuperação.

## Rollout recomendado

1. execute lint, builds e testes;
2. faça backup;
3. publique o frontend versionado;
4. inicie o novo backend sem expor externamente;
5. aguarde Flyway e inicialização concluírem;
6. valide login, catálogo, imagem e consulta interna;
7. altere o proxy para a nova versão;
8. monitore erros, latência e espaço;
9. mantenha o artefato anterior disponível para rollback de aplicação.

Se uma migration modificar dados de forma incompatível, voltar apenas o JAR pode não ser seguro.
Planeje migrations reversíveis ou restauração de backup.

## Verificações pós-deploy

- home e catálogo carregam sem conteúdo misto;
- busca e categorias funcionam;
- `/admin` autentica;
- administrador acessa importação e vitrines;
- funcionário consulta `/vitrine-loja` e catálogo interno reduzido, sem acesso às funções administrativas;
- logout concluído invalida o token anterior; funcionário desativado não consegue autenticar;
- uma imagem existente e uma nova carregam;
- upload inválido é rejeitado;
- acesso HTTP redireciona no proxy;
- acesso direto ao backend não está disponível externamente;
- respostas de erro não exibem stack trace;
- logs não contêm segredos.

## Monitoramento mínimo

Monitore:

- disponibilidade HTTPS;
- uso de CPU, memória e disco;
- conexões e espaço do MySQL;
- tamanho do diretório de imagens;
- quantidade de `401`, `403`, `426`, `429` e `5xx`;
- falhas de login e importação;
- tempo de resposta do catálogo;
- validade do certificado;
- sucesso dos backups.

Inclua falhas de gravação/limpeza de auditoria, crescimento da tabela e imagens órfãs. A trilha de
auditoria é gravada após a requisição em transação própria; uma falha é registrada no log e não
desfaz a operação já concluída. Centralize esses alertas para não perder visibilidade operacional.

Os limites por IP/dispositivo e de catálogo são locais a uma instância. Antes de executar várias
instâncias, coordene esses contadores no proxy/gateway ou em armazenamento compartilhado.

O Actuator é restrito a administradores. Antes de integrar monitoramento externo, crie um contrato
específico e mínimo em vez de liberar todos os endpoints.

## Locaweb ou outra hospedagem

Confirme com a equipe responsável se o plano contratado permite:

- processo Java 25 de longa duração;
- configuração de variáveis privadas;
- MySQL compatível;
- diretório persistente gravável;
- reverse proxy de `/api`;
- certificado HTTPS;
- reinício supervisionado do processo;
- backup do banco e dos arquivos.

Hospedagem estática simples não executa o backend. Nesse caso, frontend, API e banco precisarão de
serviços separados ou de uma VPS.

O repositório prepara esse contrato, mas não provisiona domínio, TLS, contas de banco, firewall,
volume persistente ou backups na hospedagem. A liberação de produção depende de verificar esses
itens no plano contratado e concluir uma homologação com os artefatos e migrations da versão.
