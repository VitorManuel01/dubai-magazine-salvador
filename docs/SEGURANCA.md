# Autenticação e segurança

## Modelo de confiança

O navegador e qualquer dado recebido por HTTP são considerados não confiáveis. As verificações do
frontend existem para experiência e prevenção de erro; autorização, validação de arquivos e regras
de dados são repetidas no backend.

Fronteiras principais:

1. internet ou rede local → proxy HTTPS;
2. proxy → Spring Boot em interface interna;
3. Spring Boot → MySQL;
4. Spring Boot → diretório persistente de imagens;
5. arquivo ODS do ERP → parser e importador.

## Autenticação

- identificador: código Santri normalizado em maiúsculas;
- senha: hash BCrypt com custo 12;
- sessão: JWT stateless no cabeçalho Authorization;
- algoritmo: HMAC256;
- emissor verificado: `auth-api`;
- expiração padrão: 3.600 segundos;
- token inválido ou expirado é rejeitado e removido pelo frontend.

Senhas não são criptografadas de forma reversível. Somente o hash BCrypt é persistido.

### Política de senha

Novos administradores e funcionários exigem:

- entre 12 e 128 caracteres;
- ao menos uma letra maiúscula;
- ao menos uma letra minúscula;
- ao menos um número;
- ao menos um caractere especial.

## Proteção do login

Há três controles complementares.

### Conta

Após três falhas consecutivas, a conta é bloqueada por 20 minutos. Tentativas e instante de
bloqueio ficam no banco. Um login bem-sucedido limpa os contadores.

### IP

O backend rastreia tentativas por IP em uma janela de 20 minutos. O padrão permite 20 tentativas
antes do bloqueio da origem.

### Dispositivo

O frontend gera um UUID e o envia em `X-Device-Id`. O padrão permite cinco tentativas na janela.
IDs ausentes ou inválidos não são confiados.

Entre tentativas, existe atraso exponencial de 1 a 30 segundos. Ao limitar uma origem, a API retorna
`429` e `Retry-After`.

A mensagem de credenciais inválidas é uniforme, evitando revelar se o código Santri existe.

### Limitação operacional

O limite por IP/dispositivo está em memória:

- reiniciar a aplicação limpa esses contadores;
- múltiplas instâncias não compartilham o estado;
- a proteção persistente por conta continua funcionando;
- uma implantação distribuída deve mover o rate limit para Redis, gateway ou Cloudflare.

## Autorização

O Spring Security aplica:

- catálogo, categorias, vitrines da home e imagens públicas: leitura pública;
- vitrine da loja física: administrador ou funcionário;
- importação, produtos administrativos, vitrines administrativas, funcionários e Actuator:
  administrador;
- clientes: acesso negado;
- qualquer rota não declarada: exige autenticação.

As proteções de rota do React não substituem essas regras.

## CSRF, cookies e CORS

O backend é stateless e usa Bearer token, não cookie de autenticação. Por isso:

- CSRF está desativado;
- Axios mantém `withCredentials = false`;
- CORS não permite credenciais;
- somente origens configuradas são aceitas no desenvolvimento;
- pré-produção e produção usam mesma origem por `/api`, dispensando CORS normalmente.

Se a autenticação for migrada para cookie, a decisão sobre CSRF precisa ser revista antes da
publicação.

## HTTPS

No perfil `development`, HTTP é permitido para a rede local.

Nos perfis `preproduction` e `production`:

- o backend escuta `127.0.0.1` por padrão;
- o proxy termina TLS;
- `Forwarded`/`X-Forwarded-*` é interpretado pelo Spring;
- requisição cujo protocolo original não era HTTPS recebe `426 Upgrade Required`;
- HSTS é enviado com validade de um ano, subdomínios e preload;
- a porta 8081 não deve ser publicada.

Só use `server.forward-headers-strategy=framework` quando o backend estiver atrás de um proxy
controlado. Expor o backend diretamente e confiar em cabeçalhos fornecidos pelo cliente permite
falsificação de protocolo e IP.

## Segredos

Variáveis privadas esperadas:

- `DB_URL`;
- `DB_USERNAME`;
- `DB_PASSWORD`;
- `JWT_SECRET`;
- credenciais equivalentes do banco de testes.

Regras:

- mantenha o `.env` real fora do Git;
- use segredo JWT aleatório, longo e diferente entre ambientes;
- não compartilhe o banco de testes com produção;
- não coloque segredo em variável `VITE_*`;
- não grave senhas em documentação, logs, screenshots ou mensagens de commit;
- rotacione imediatamente qualquer segredo que tenha sido versionado.

O `.env.example` contém somente placeholders e pode ser versionado.

## Exposição mínima de dados

O catálogo público recebe `ProdutoCatalogoPublicoDTO`. Ele não inclui:

- código Santri;
- código de barras;
- NCM;
- estoque;
- fornecedor, origem e dados de compra;
- dimensões logísticas;
- flags administrativas.

Filtros visuais de categoria não são barreira de segurança. Toda categoria realmente interna deve
também permanecer com `exibir_no_site = false` ou ser excluída na consulta pública do backend.

## Upload de imagens

As imagens passam pelas seguintes verificações:

- limite de 5 MB;
- extensão JPG/JPEG, PNG ou WEBP;
- MIME declarado correspondente;
- magic bytes correspondentes ao conteúdo;
- nome original limitado e sem byte nulo;
- nome armazenado substituído por UUID;
- caminho normalizado e restrito a `PRODUCT_IMAGES_DIR`;
- rota administrativa protegida;
- leitura pública limitada ao padrão de nome gerado.

As mesmas regras são usadas para produtos, galeria da vitrine física e banners da home. Somente
administradores podem enviar ou substituir essas imagens.

O servidor não executa nem interpreta o conteúdo como HTML. O diretório de uploads não deve ter
permissão de execução.

## Importação ODS

A importação trata o ODS como ZIP e XML não confiáveis. As proteções contra ZIP bomb, path
traversal, XXE, fórmulas, documentos excessivos e SQL injection estão detalhadas em
[IMPORTACAO_ODS.md](IMPORTACAO_ODS.md).

## Cabeçalhos HTTP

O exemplo Nginx adiciona:

- `Strict-Transport-Security`;
- `X-Content-Type-Options: nosniff`;
- `X-Frame-Options: DENY`;
- `Referrer-Policy: strict-origin-when-cross-origin`;
- ocultação da versão do Nginx.

Antes da produção, avalie uma Content Security Policy compatível com os assets e destinos externos
utilizados pelo site.

## Logs e erros

Em pré-produção e produção:

- SQL detalhado fica desativado;
- stack trace não é enviado ao cliente;
- mensagens internas e erros de binding não são incluídos na resposta.

Não registre JWTs, senhas, conteúdo completo de planilhas nem dados pessoais desnecessários. Logs
de produção devem possuir retenção, controle de acesso e rotação.

## Dados pessoais

Administradores e funcionários possuem CPF e outros dados cadastrais. O acesso deve seguir o
princípio de menor privilégio. Backups, banco de testes e exportações devem receber o mesmo cuidado
do banco principal.

## Riscos e limitações conhecidos

- JWT em `localStorage` é acessível a JavaScript; uma falha XSS poderia capturá-lo;
- não existe lista de revogação de tokens antes da expiração;
- rate limit por origem não é distribuído;
- não existe trilha completa de auditoria administrativa;
- não existe antivírus ou decodificação completa de imagem no upload;
- `/uploads/produtos/**` continua público para compatibilidade com caminhos antigos;
- o Quick Tunnel é apropriado para homologação, não para disponibilidade de produção;
- o bootstrap inicial do administrador depende de procedimento operacional externo.

Esses itens não anulam as proteções atuais, mas devem orientar a próxima rodada de endurecimento.

## Checklist antes de publicar

- [ ] banco de produção vazio ou migrado e com backup testado;
- [ ] primeiro administrador provisionado de forma controlada;
- [ ] `JWT_SECRET` exclusivo, aleatório e fora do repositório;
- [ ] usuário do banco com permissões mínimas necessárias;
- [ ] `PRODUCT_IMAGES_DIR` persistente, gravável só pela aplicação e incluído no backup;
- [ ] frontend e API acessíveis apenas por HTTPS;
- [ ] porta 8081 bloqueada externamente;
- [ ] proxy enviando os cabeçalhos `X-Forwarded-*` corretos;
- [ ] domínio e certificado válidos;
- [ ] CORS vazio ou restrito às origens oficiais;
- [ ] SQL e stack traces desativados;
- [ ] restore do banco e das imagens testado;
- [ ] `npm audit`, lint, build e testes Maven executados;
- [ ] revisão de dependências e headers concluída;
- [ ] conta administrativa de emergência armazenada em cofre apropriado.

## Resposta a incidente

Em caso de suspeita de vazamento:

1. interrompa o acesso externo se houver risco ativo;
2. rotacione `JWT_SECRET`, invalidando todos os tokens;
3. rotacione banco e demais credenciais afetadas;
4. preserve logs e evidências;
5. revise usuários, alterações editoriais e arquivos enviados;
6. restaure de backup somente após identificar a causa;
7. comunique os responsáveis pela infraestrutura e proteção de dados.
