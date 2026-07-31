# Preparação para hospedagem segura

Esta pasta contém somente modelos. Nada aqui altera o desenvolvimento local, que continua usando:

```properties
VITE_API_BASE_URL=http://localhost:8081
CORS_ALLOWED_ORIGINS=http://localhost:5173
REQUIRE_HTTPS=false
```

## Produção com Nginx

O modelo `nginx/dubai-magazine.conf.example` foi preparado para:

- servir o build do frontend como SPA;
- redirecionar HTTP para HTTPS;
- encaminhar `/api/` ao Spring Boot na porta interna `8081`;
- informar corretamente ao Spring que a conexão externa usa HTTPS;
- limitar o upload a 20 MB e adicionar cabeçalhos básicos de segurança.

Quando houver domínio e servidor:

1. Aponte o DNS do domínio para o servidor.
2. Instale Nginx e Certbot conforme a documentação da distribuição do servidor.
3. Gere um certificado confiável para o domínio com Let's Encrypt/Certbot.
4. Substitua `DOMINIO_EXEMPLO` e confirme os caminhos do certificado no modelo.
5. Gere o frontend com `VITE_API_BASE_URL=https://DOMINIO_EXEMPLO/api`.
6. Configure o backend:

```properties
CORS_ALLOWED_ORIGINS=https://DOMINIO_EXEMPLO
REQUIRE_HTTPS=true
```

7. Valide a configuração do Nginx antes de recarregá-lo.

Não habilite o bloco HTTPS antes de o certificado existir, pois o Nginx não iniciará se os arquivos indicados não estiverem disponíveis.

## Certificado autoassinado

Um certificado autoassinado é gratuito, mas os navegadores não confiam nele por padrão. Ele exige instalar manualmente a autoridade/certificado em cada computador e celular que acessar o sistema. É aceitável para testes ou uma rede interna totalmente controlada, mas não é indicado para um site público.

Para produção pública, prefira um certificado confiável e renovado automaticamente pelo Let's Encrypt.
