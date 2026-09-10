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
- autenticação sem sessão HTTP: JWT no cabeçalho Authorization, com versão e estado da conta no banco;
- algoritmo: HMAC256;
- emissor verificado: `dubai-magazine-api`;
- audiência verificada: `dubai-magazine-interno`;
- expiração padrão: 3.600 segundos, configurável entre 300 e 3.600;
- identificador único (`jti`), instante de emissão e versão de sessão;
- token inválido, expirado ou revogado é rejeitado e removido pelo frontend.

O frontend envia Bearer apenas à origem/prefixo da API. Login, logout, expiração e encerramento
por `401` limpam o cache de consultas, separando os dados de contas utilizadas na mesma aba.

O navegador mantém o token em `sessionStorage`, com duração vinculada à sessão da aba (a
restauração da aba depende do navegador). Logout concluído no backend e mudanças no estado de
um funcionário incrementam `versao_token` no banco e invalidam todos os JWTs anteriores da conta.
Se a requisição de logout falhar, o frontend encerra a sessão local, mas não pode garantir a
revogação no servidor.

Senhas não são criptografadas de forma reversível. Somente o hash BCrypt é persistido.

### Erros HTTP e preservação da sessão

Erros de negócio lançados como `ResponseStatusException` são respondidos diretamente em JSON,
preservando status e cabeçalhos HTTP, sem incluir a mensagem interna da exceção. Assim, uma
consulta inválida retorna `400`, e um recurso ausente retorna `404`, sem provocar logout.
Somente o despacho interno `ERROR` para `/error` dispensa nova autenticação; chamadas HTTP
diretas a essa rota e aos endpoints administrativos continuam protegidas. O frontend mantém
a remoção do token para respostas `401` genuínas, incluindo tokens expirados ou revogados.

`ErrosHttpServidorTest` usa Tomcat real em porta aleatória e interface loopback, sem banco,
para verificar erros `400`/`404`/`500`, continuidade da autenticação e bloqueios `401`/`403`.
O Tomcat está fixado em `11.0.25` no `pom.xml` para incluir as correções de segurança dessa
versão; todos os seus módulos herdam a mesma versão. Reavaliar essa fixação ao atualizar o
Spring Boot ou quando forem publicados novos avisos de segurança.

### Política de senha

Novos administradores e funcionários exigem:

- entre 12 e 72 caracteres e no máximo 72 bytes UTF-8, evitando truncamento do BCrypt;
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

Os endpoints públicos também usam baldes separados para consultas do catálogo e arquivos de
imagem. O número de origens mantidas em memória é limitado e o sistema falha fechado quando esse
teto é alcançado. Há também um semáforo global de consultas públicas (quatro por padrão), que
responde 503 com `Retry-After` quando o banco já está ocupado. No proxy Nginx há uma segunda camada
por IP e por conexões simultâneas.

As buscas têm no máximo 100 caracteres, caracteres invisíveis/de controle são rejeitados e `%`,
`_` e `!` são escapados antes do `LIKE`. A busca pública exige dois caracteres. Página, tamanho,
formato de preço e tempo de consulta ao banco também possuem limites.

Os índices da V26 apoiam filtros e ordenação do catálogo; busca por trecho ainda usa `LIKE` e
pode examinar muitos registros. A contenção de requisições e o timeout configurado reduzem a
exposição, mas é necessário medir planos e latência com o volume real antes de aumentar limites.

## Autorização

O Spring Security aplica:

- catálogo, categorias, vitrines da home e imagens públicas: leitura pública;
- vitrine da loja física e catálogo interno reduzido com código Santri: administrador ou funcionário;
- importação, produtos administrativos, vitrines administrativas, funcionários e Actuator:
  administrador;
- rotas legadas de cliente: acesso negado; login atual aceita somente contas internas;
- qualquer rota não declarada: exige autenticação.

As proteções de rota do React não substituem essas regras.

O gerenciamento de funcionários retorna somente identificador, código Santri, nome, função e
estado. Campos cadastrais como CPF e telefone continuam internos no banco.

## CSRF, cookies e CORS

O backend não usa sessão HTTP nem cookie de autenticação; recebe o Bearer token explicitamente.
Por isso:

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
- `X-Forwarded-*` é interpretado pelo `RemoteIpValve` nativo do Tomcat, confiando somente em loopback;
- requisição cujo protocolo original não era HTTPS recebe `426 Upgrade Required`;
- HSTS é enviado com validade de um ano, subdomínios e preload;
- a porta 8081 não deve ser publicada.

`server.forward-headers-strategy=native` restringe o proxy confiável a endereços de loopback. O
Nginx e o preview sobrescrevem os cabeçalhos de encaminhamento. A porta 8081 permanece interna;
se o proxy for movido para outra máquina ou rede de containers, revise a configuração com uma
lista explícita de origens confiáveis, sem aceitar qualquer IP.

## Segredos

Variáveis privadas esperadas:

- `DB_URL`;
- `DB_USERNAME` e `DB_PASSWORD` em desenvolvimento/pré-produção;
- `DB_APP_USERNAME` e `DB_APP_PASSWORD` para a aplicação em produção;
- `DB_MIGRATION_USERNAME` e `DB_MIGRATION_PASSWORD` para o Flyway;
- `JWT_SECRET`;
- `AUDIT_HMAC_SECRET`, diferente do segredo JWT;
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

As árvores `008` (Uso e Consumo), `123` (Ajustes de Grupos) e `999` (Implantação) são excluídas
pelo backend no catálogo, detalhe e cards públicos da home, inclusive se a flag estiver `true`.

## Upload de imagens

As imagens passam pelas seguintes verificações:

- limite de 5 MB;
- extensão JPG/JPEG, PNG ou WEBP;
- MIME declarado correspondente;
- magic bytes correspondentes ao conteúdo;
- nome original limitado e sem byte nulo;
- nome armazenado substituído por UUID;
- caminho normalizado e restrito a `PRODUCT_IMAGES_DIR`;
- largura/altura de no máximo 12.000 pixels e total de no máximo 40 milhões de pixels;
- links simbólicos recusados;
- rota administrativa protegida;
- leitura pública limitada ao padrão de nome gerado.

As mesmas regras são usadas para produtos, galeria da vitrine física e banners da home. Somente
administradores podem enviar ou substituir essas imagens.

O catálogo localiza o nome UUID diretamente, sem percorrer todo o diretório a cada requisição. Uma
rotina diária remove arquivos órfãos antigos; arquivos recém-enviados recebem uma janela de
segurança antes da remoção. O servidor não executa nem interpreta o conteúdo como HTML. O
diretório de uploads não deve ter permissão de execução.

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
- Content Security Policy sem script ou estilo inline;
- `Permissions-Policy`, `Cross-Origin-Opener-Policy` e
  `Cross-Origin-Resource-Policy`;
- ocultação da versão do Nginx.

## Logs e erros

Em pré-produção e produção:

- SQL detalhado fica desativado;
- stack trace não é enviado ao cliente;
- mensagens internas e erros de binding não são incluídos na resposta.

O tratamento de falhas de validação também evita registrar valores rejeitados de DTOs nos logs,
o que impediria a proteção da resposta de ser contornada por vazamento de senhas ou dados pessoais
no registro da exceção.

Não registre JWTs, senhas, conteúdo completo de planilhas nem dados pessoais desnecessários. Logs
de produção devem possuir retenção, controle de acesso e rotação.

Mutações feitas por usuários autenticados geram uma trilha no banco contendo usuário, papel,
método, caminho, status e horário. IP e User-Agent são armazenados apenas como HMAC. Corpos,
senhas, tokens e parâmetros de consulta não são gravados. A retenção padrão é de 180 dias, com
limpeza diária limitada por lote.

A auditoria é executada após a requisição em transação própria. Uma falha ao persistir a trilha
gera erro no log e não reverte a ação concluída; monitore esse evento. Requisições rejeitadas antes
de alcançar o interceptor não fazem parte dessa trilha, que não substitui logs do proxy e alertas
de autenticação.

## Dados pessoais

Administradores e funcionários possuem CPF e outros dados cadastrais. O acesso deve seguir o
princípio de menor privilégio. Backups, banco de testes e exportações devem receber o mesmo cuidado
do banco principal.

## Riscos e limitações conhecidos

- um JWT em `sessionStorage` ainda é acessível ao JavaScript da própria origem; a CSP reduz o risco,
  mas não substitui a prevenção contínua de XSS;
- o rate limit é local ao processo; múltiplas instâncias exigem Redis, gateway ou proteção
  equivalente compartilhada;
- não existe MFA para administradores; adote MFA no provedor de acesso/rede ou implemente um fluxo
  de segundo fator antes de ampliar o impacto da área administrativa;
- a validação de imagem confirma formato, cabeçalhos e dimensões, mas não usa antivírus nem
  reencodificação completa;
- as imagens residem em disco local e dependem de backup, monitoramento de espaço e persistência da
  hospedagem;
- o Quick Tunnel é apropriado para homologação, não para disponibilidade de produção;
- o bootstrap inicial do administrador depende de procedimento operacional externo controlado.

Esses itens não anulam as proteções atuais, mas devem orientar a próxima rodada de endurecimento.

## Checklist antes de publicar

- [ ] banco de produção vazio ou migrado e com backup testado;
- [ ] primeiro administrador provisionado de forma controlada;
- [ ] `JWT_SECRET` exclusivo, aleatório e fora do repositório;
- [ ] `AUDIT_HMAC_SECRET` aleatório, independente e fora do repositório;
- [ ] usuário do banco com permissões mínimas necessárias;
- [ ] usuário da aplicação separado do usuário que executa migrations;
- [ ] `PRODUCT_IMAGES_DIR` persistente, gravável só pela aplicação e incluído no backup;
- [ ] frontend e API acessíveis apenas por HTTPS;
- [ ] porta 8081 bloqueada externamente;
- [ ] proxy enviando os cabeçalhos `X-Forwarded-*` corretos;
- [ ] domínio e certificado válidos;
- [ ] CORS vazio ou restrito às origens oficiais;
- [ ] SQL e stack traces desativados;
- [ ] restore do banco e das imagens testado;
- [ ] `npm audit`, lint, build e testes Maven executados;
- [ ] CI, CodeQL, Dependency Review e atualização automática de dependências habilitados no GitHub;
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
