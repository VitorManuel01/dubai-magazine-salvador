# Ambientes e HTTPS

O navegador usa três configurações de ambiente. Pré-produção e produção acessam a API por
`/api`, na mesma origem HTTPS do site. Somente o proxy conversa com o Spring Boot pela porta
interna `8081`.

| Ambiente | Frontend | Backend | Transporte externo |
|---|---|---|---|
| Desenvolvimento | `http://172.16.0.11:5173` | `http://172.16.0.11:8081` | HTTP restrito à rede local |
| Pré-produção | URL aleatória `*.trycloudflare.com` | `127.0.0.1:8081` | HTTPS do Cloudflare |
| Produção | novo domínio | endereço interno da hospedagem | HTTPS do domínio |

## 1. Desenvolvimento

No backend, use o perfil padrão `development`:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

No frontend:

```powershell
cd frontend
npm run dev
```

Acesse `http://172.16.0.11:5173`. Se o IP da máquina mudar, atualize
`frontend/.env.development` e `CORS_ALLOWED_ORIGINS` no `.env` do backend.

## 2. Pré-produção com Quick Tunnel

O Quick Tunnel aponta somente para o preview do frontend. O preview encaminha `/api` para
`127.0.0.1:8081`; portanto, não é necessário abrir um segundo túnel nem copiar a URL aleatória
para os arquivos `.env`.

Terminal 1 — backend protegido por HTTPS externo:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE='preproduction'
.\mvnw.cmd spring-boot:run
```

Terminal 2 — build e preview:

```powershell
cd frontend
npm run build:preproduction
npm run preview:preproduction
```

Terminal 3 — túnel temporário:

```powershell
cloudflared tunnel --url http://127.0.0.1:4173
```

Abra a URL HTTPS gerada pelo `cloudflared`. O endereço muda quando o túnel é reiniciado.
No modo de pré-produção, frontend e backend escutam apenas em `127.0.0.1`; o acesso externo
acontece exclusivamente pelo túnel HTTPS.

## 3. Produção na Locaweb

Gere o frontend com:

```powershell
cd frontend
npm run build:production
```

Entregue o conteúdo de `frontend/dist` e o backend empacotado à equipe de TI. O ambiente da
Locaweb deverá:

1. servir o frontend por HTTPS;
2. encaminhar `https://DOMINIO/api/*` para o Spring Boot, removendo o prefixo `/api`;
3. enviar `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-Port` e `X-Forwarded-For`;
4. executar o backend com `SPRING_PROFILES_ACTIVE=production`;
5. fornecer banco, diretório persistente de imagens e segredos por variáveis de ambiente;
6. impedir acesso público direto à porta `8081`.

O modelo em `nginx/dubai-magazine.conf.example` demonstra o contrato esperado pelo proxy.
Caso a Locaweb use outro proxy, a equipe pode reproduzir as mesmas rotas e cabeçalhos.

## Observações de segurança

- TLS termina no Cloudflare ou no proxy da hospedagem; o salto para `127.0.0.1:8081` pode usar
  HTTP porque não sai da máquina.
- O Spring confia em cabeçalhos encaminhados somente nos perfis que ficam atrás do proxy.
- Imagens externas em `http://` são rejeitadas quando o site está em HTTPS, evitando conteúdo misto.
- Variáveis `VITE_*` são públicas e nunca devem conter senhas, tokens ou chaves privadas.
