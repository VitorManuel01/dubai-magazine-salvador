# Operação, ambientes e deploy

## Matriz de ambientes

| Ambiente | Frontend | Backend | HTTPS | Objetivo |
|---|---|---|:---:|---|
| `development` | Vite `0.0.0.0:5173` | `0.0.0.0:8081` | não obrigatório | desenvolvimento na rede local |
| `preproduction` | preview `127.0.0.1:4173` | `127.0.0.1:8081` | obrigatório externamente | homologação por Cloudflare Tunnel |
| `production` | arquivos estáticos no proxy | `127.0.0.1:8081` | obrigatório | domínio oficial |

Pré-produção e produção usam `VITE_API_BASE_URL=/api`, mantendo frontend e API na mesma origem.

## Variáveis do backend

### Obrigatórias

| Variável | Exemplo seguro | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/catalogo_dubai` | banco principal |
| `DB_USERNAME` | usuário dedicado | acesso ao banco |
| `DB_PASSWORD` | segredo externo | acesso ao banco |
| `JWT_SECRET` | valor aleatório longo | assinatura JWT |
| `SPRING_PROFILES_ACTIVE` | `production` | perfil ativo |

### Operacionais

| Variável | Padrão | Uso |
|---|---|---|
| `SERVER_ADDRESS` | depende do perfil | interface de rede |
| `CORS_ALLOWED_ORIGINS` | local no desenvolvimento; vazio nos publicados | origens permitidas |
| `PRODUCT_IMAGES_DIR` | `uploads/produtos` | imagens persistentes |
| `JWT_EXPIRATION_SECONDS` | `3600` | validade do token |
| `LOGIN_MAX_FAILED_ATTEMPTS` | `3` | falhas por conta |
| `LOGIN_LOCK_DURATION` | `PT20M` | bloqueio da conta |
| `LOGIN_IP_MAX_FAILED_ATTEMPTS` | `20` | limite por IP |
| `LOGIN_DEVICE_MAX_FAILED_ATTEMPTS` | `5` | limite por dispositivo |
| `LOGIN_ORIGIN_WINDOW` | `PT20M` | janela do rate limit |
| `LOGIN_ORIGIN_BLOCK_DURATION` | `PT20M` | bloqueio da origem |
| `LOGIN_INITIAL_DELAY` | `PT1S` | atraso progressivo inicial |
| `LOGIN_MAX_DELAY` | `PT30S` | atraso progressivo máximo |
| `LOGIN_MAX_TRACKED_ORIGINS` | `50000` | teto de origens em memória |

Durações usam o formato ISO-8601 do Java, como `PT20M` e `PT1S`.

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

O arquivo [Nginx de exemplo](../deploy/nginx/dubai-magazine.conf.example) implementa esse contrato.
Se a hospedagem usar Apache, IIS ou proxy próprio, reproduza o comportamento, não necessariamente a
mesma sintaxe.

## Inicialização do backend em produção

Exemplo conceitual no PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE='production'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/catalogo_dubai'
$env:DB_USERNAME='usuario_da_aplicacao'
$env:DB_PASSWORD='fornecida_por_cofre'
$env:JWT_SECRET='fornecido_por_cofre'
$env:PRODUCT_IMAGES_DIR='D:\dados\dubai\produtos'
java -jar .\dubaiMagazineSalvador-0.0.1-SNAPSHOT.jar
```

No Linux, prefira uma unidade `systemd` ou o gerenciador oferecido pela hospedagem. Não grave os
segredos diretamente no arquivo de serviço quando houver cofre ou mecanismo próprio de secrets.

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
- funcionário acessa somente `/vitrine-loja`;
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
