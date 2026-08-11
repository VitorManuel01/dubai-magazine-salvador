# Importação ODS do Santri

## Objetivo

A integração atual com o Santri é feita pela exportação da Relação de Produtos por Grupo em
formato ODS. A importação sincroniza os dados operacionais do catálogo sem substituir decisões
editoriais administradas no site.

Somente administradores podem abrir a tela `/admin/importacao-produtos` e chamar a rota de
importação.

## Relatório esperado

Gere uma relação analítica agrupada por categoria. A planilha precisa manter as linhas de grupo
antes dos produtos, pois elas formam a hierarquia de categorias.

Filtros recomendados na emissão:

- empresa Dubai Magazine correspondente à loja;
- produto ativo: sim;
- estoque físico positivo na loja: sim;
- ativo imobilizado: não;
- revenda: sim, quando esse filtro representar o catálogo comercial;
- sem limitação de preço que remova produtos válidos;
- somente as colunas descritas abaixo.

O backend também ignora produtos inativos ou com estoque nulo/zero, mas filtrar no Santri reduz o
arquivo, acelera a importação e deixa o resultado mais fácil de auditar.

## Colunas obrigatórias

| Cabeçalho exibido | Campo da aplicação |
|---|---|
| Código | código Santri ou código hierárquico da categoria |
| Nome | nome do produto ou da categoria |
| NCM | NCM |
| Nome de compra | descrição de compra |
| Fabricante | fabricante |
| Marca | marca |
| Ativo? | situação no Santri |
| Und. venda | unidade de venda |
| Und. compra | unidade de compra |
| Data cadastro | data no formato `dd/MM/aaaa` |
| Código original | referência original |
| Código de barras | EAN ou referência equivalente |
| Bloqueado para compras | indicador sim/não |
| Estoque | estoque físico |
| Preço | preço de venda sem IPI |
| % IPI entrada | percentual usado no cálculo público |
| Peso da unidade | dado logístico |
| Altura da unidade | dado logístico |
| Largura da unidade | dado logístico |
| Comprimento da unidade | dado logístico |
| Volume da unidade (m³) | dado logístico |
| Volume em litros | dado logístico |
| Peso da caixa | dado logístico |
| Altura da caixa | dado logístico |
| Largura da caixa | dado logístico |
| Comprimento da caixa | dado logístico |
| Origem | origem fiscal |
| Industrializado | indicador opcional |
| Insumo | indicador opcional |
| % Máx. aprov. IPI | percentual fiscal |
| Número FCI | referência FCI |

O leitor normaliza acentos, `%`, pontuação e espaços dos cabeçalhos. Ainda assim, não renomeie
manualmente as colunas: exporte-as diretamente do relatório.

## Como a planilha é interpretada

- somente a primeira planilha interna é processada;
- uma linha com código hierárquico e campos de produto vazios é tratada como categoria;
- categorias aceitam de um a quatro níveis;
- produtos devem aparecer depois de uma categoria válida;
- pontos e espaços são removidos do código do produto antes da persistência;
- valores `Sim` e `Não` são convertidos em booleanos;
- números no formato brasileiro são convertidos para `BigDecimal`;
- produto inativo ou sem estoque positivo conta como linha ignorada;
- códigos de produto repetidos no mesmo arquivo ficam representados pelo último valor lido;
- fórmulas não são aceitas: a exportação deve conter somente valores.

## Fluxo operacional

1. gere o ODS com os filtros recomendados;
2. entre como administrador;
3. abra **Minha conta → Importar relação de produtos**;
4. selecione o arquivo `.ods`;
5. confirme o nome e o tamanho;
6. clique em **Importar relação**;
7. aguarde as fases de envio e processamento;
8. confira os contadores de produtos e categorias;
9. revise o catálogo administrativo antes de tornar novos itens públicos.

Durante a importação, não feche o processo do backend. Uma segunda importação simultânea é
rejeitada.

## Efeitos no banco

Toda importação é transacional:

- categorias são inseridas ou atualizadas;
- produtos são inseridos ou atualizados por código Santri;
- lotes de 1.000 reduzem o custo de persistência;
- novos produtos começam ocultos e sem destaque;
- nome público personalizado e imagem existente são preservados;
- produtos ausentes são marcados como indisponíveis, ocultos e sem destaque;
- produtos ausentes não são apagados, permitindo que reapareçam em uma importação futura;
- produtos de ativo imobilizado (`089` e descendentes) são removidos;
- ajustes de grupos (`123`) e implantação (`999`) permanecem ocultos;
- produto com preço sem IPI menor ou igual a zero fica oculto.

O cálculo de `precoComIpi` não é armazenado. Ele é recalculado quando o DTO é montado.

## Proteções do arquivo

### Antes do envio

O frontend verifica:

- extensão `.ods`;
- limite de 20 MB;
- assinatura inicial ZIP `PK`.

Essa etapa melhora a experiência, mas não é considerada uma barreira de segurança.

### No backend

O backend repete e amplia a validação:

- rota exclusiva de administrador;
- nome, extensão, MIME e tamanho do upload;
- cópia temporária limitada a 20 MB;
- até 100 entradas no ZIP;
- rejeição de nomes duplicados ou caminhos internos suspeitos;
- presença e conteúdo correto do arquivo `mimetype`;
- presença de `content.xml`;
- até 250 MB para `content.xml` descompactado;
- até 300 MB para o conjunto descompactado;
- razão mínima de compactação de 1% para entradas com pelo menos 1 MB;
- até 25.000 linhas e 25.000 produtos;
- até 80 colunas por linha;
- até 75 caracteres Unicode por célula;
- parser XML configurado em modo fail-closed;
- DTD, entidades externas, referências de entidade e resolução externa desativadas;
- fórmulas rejeitadas;
- arquivo temporário removido mesmo quando ocorre erro;
- parâmetros preparados no `JdbcTemplate`, sem concatenação de valores da planilha no SQL.

## Limites

| Limite | Valor |
|---|---:|
| Upload ODS | 20 MB |
| Entradas internas do ZIP | 100 |
| `content.xml` descompactado | 250 MB |
| Total descompactado | 300 MB |
| Linhas | 25.000 |
| Produtos únicos | 25.000 |
| Colunas lidas | 80 |
| Caracteres por célula | 75 |

Alterar esses limites exige revisar consumo de memória, tempo de transação e risco de negação de
serviço.

## Resultado

A resposta informa:

- arquivo processado;
- categorias lidas, criadas e atualizadas;
- produtos lidos, criados e atualizados;
- linhas ignoradas;
- instante da importação;
- duração em milissegundos.

Guarde o arquivo importado e o resultado da tela durante a homologação. O sistema ainda não possui
uma tabela de histórico completo de importações.

## Erros frequentes

### “O arquivo selecionado precisa ter a extensão .ods”

O relatório foi salvo em XLSX, CSV ou teve a extensão alterada manualmente. Exporte novamente como
OpenDocument Spreadsheet.

### “Tipo ODS não confirmado”

O ZIP interno não possui o `mimetype` correto. Não renomeie outro formato para `.ods`.

### “A relação analítica não possui as colunas obrigatórias”

Uma ou mais colunas foram omitidas na configuração do resultado do Santri.

### “Produto encontrado antes de qualquer categoria”

O relatório perdeu as linhas de agrupamento. Gere novamente com agrupamento por grupo de produto.

### “Nenhuma categoria ou produto ativo com estoque positivo foi encontrado”

Confira empresa, ativo, estoque e filtros da emissão.

### “O ODS contém fórmulas”

Salve uma exportação com valores finais. Não edite o arquivo inserindo fórmulas.

### Produto desapareceu do catálogo depois da importação

Verifique se ele estava presente, ativo, com estoque positivo e preço válido. Ausentes ficam
indisponíveis e ocultos até reaparecerem.

## Recuperação

Antes da primeira importação em um ambiente importante:

1. faça backup do banco;
2. confirme que `PRODUCT_IMAGES_DIR` também possui backup;
3. teste o mesmo ODS em pré-produção;
4. confira contagens e amostras por categoria;
5. só então execute em produção.

Como a operação é transacional, uma exceção durante o processamento deve reverter as alterações de
banco daquela importação. Arquivos e backups continuam sendo necessários para erros operacionais ou
decisões incorretas de filtro.
