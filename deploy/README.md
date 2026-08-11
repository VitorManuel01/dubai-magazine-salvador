# Deploy

Este diretório contém arquivos auxiliares de publicação.

- `nginx/dubai-magazine.conf.example`: modelo de HTTPS, SPA e reverse proxy;
- [documentação completa de operação](../docs/OPERACAO_E_DEPLOY.md);
- [checklist de segurança](../docs/SEGURANCA.md#checklist-antes-de-publicar).

## Pré-produção rápida

Backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE='preproduction'
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm run build:preproduction
npm run preview:preproduction
```

Túnel único:

```powershell
cloudflared tunnel --url http://127.0.0.1:4173
```

O preview encaminha `/api` ao backend. Não é necessário expor a porta 8081 ou abrir um segundo
túnel.

## Produção

Gere os artefatos:

```powershell
cd frontend
npm ci
npm run build:production

cd ..\backend
.\mvnw.cmd clean package
```

Antes de usar o modelo Nginx:

1. substitua `DOMINIO_EXEMPLO`;
2. ajuste os caminhos do frontend e certificado;
3. configure as variáveis privadas do backend;
4. defina um `PRODUCT_IMAGES_DIR` persistente;
5. mantenha o Spring Boot em `127.0.0.1:8081`;
6. teste backup e restauração;
7. execute o checklist pós-deploy.

O exemplo Nginx é uma referência de contrato. A hospedagem pode usar outro proxy desde que sirva a
SPA, encaminhe `/api`, preserve os cabeçalhos `X-Forwarded-*` e mantenha HTTPS obrigatório.
