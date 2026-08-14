# Importação ODS do Santri

## Objetivo

A integração atual com o Santri é feita pela exportação da Relação de Produtos por Grupo em
formato ODS. A importação sincroniza os dados operacionais do catálogo sem substituir decisões
editoriais administradas no site.

Somente administradores podem abrir a tela `/admin/importacao-produtos` e chamar a rota de
importação.

## Promoções de venda

A tela `/admin/importacao-promocoes` recebe separadamente o relatório **Promoções de Venda**. Ela
sincroniza, por código Santri, período inicial/final, margem, desconto, preço promocional e o
indicador especial. Linhas de grupo e subtotal são ignoradas.

Essa importação funciona como uma fotografia completa: antes de aplicar o novo relatório, os dados
promocionais anteriores são limpos dentro da mesma transação. Se a leitura ou atualização falhar,
o banco faz rollback. Produtos ausentes do catálogo não são criados e, se nenhum código do relatório
existir no catálogo, a operação é rejeitada antes de qualquer limpeza.

O preço promocional só é devolvido ao catálogo enquanto `em_promocao` estiver ativo e a data atual
estiver entre o início e o fim da promoção.

## Inventário e estoque

A tela `/admin/importacao-estoque` recebe o relatório **Inventário** do Santri. Esse fluxo foi
separado da Relação de Produtos porque o inventário completo pode ultrapassar 80 mil itens, enquanto
somente uma pequena parte deles pertence ao catálogo do site.

O leitor processa o XML em fluxo e materializa apenas `Produto` e `Quantidade`. Se o código Santri
já existir no banco, seu estoque é atualizado; caso contrário, a linha é ignorada. A operação não
cria produtos e não altera nome, preço, categoria, imagens, destaque ou visibilidade no site.

O Santri pode dividir relatórios grandes em abas como `Inventário` e `Inventário_1`. As abas de
continuação são processadas automaticamente. No arquivo validado em 14/08/2026 foram lidos 86.919
registros, dos quais 32.869 possuíam estoque zero.

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

O backend ignora produtos inativos, com marca ou fabricante `INATIVOS`, ou com preço de venda menor
ou igual a zero. Depois da relação de produtos, execute a importação de inventário para atualizar
inclusive os itens zerados que devem aparecer como `ESGOTADO` quando já estiverem visíveis.

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
- produto inativo, com marca/fabricante `INATIVOS` ou preço de venda menor ou igual a zero conta como linha ignorada;
- se uma linha de produto trouxer estoque vazio, ele é interpretado como zero;
- códigos de produto repetidos no mesmo arquivo ficam representados pelo último valor lido;
- fórmulas não são aceitas: a exportação deve conter somente valores.

## Fluxo operacional

1. gere o ODS com os filtros recomendados;
2. entre como administrador;
3. abra o **catálogo administrativo** e use o botão **Importar relação de produtos** no final da
   página;
4. selecione o arquivo `.ods`;
5. confirme o nome e o tamanho;
6. clique em **Importar relação**;
7. aguarde as fases de envio e processamento;
8. confira os contadores de produtos e categorias;
9. se houver promoções, use o botão **Importar promoções** no mesmo local e envie o ODS de
   Promoções de Venda;
10. use o botão **Atualizar estoque** e envie o ODS de Inventário;
11. revise o catálogo administrativo antes de tornar novos itens públicos.

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

Na importação de inventário, somente `produtos.estoque` é atualizado, em lotes de 1.000. Códigos
desconhecidos são ignorados e contabilizados na resposta. A leitura e os lotes participam de uma
única transação: uma falha reverte todas as quantidades daquela execução.

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
- no inventário, até 120.000 linhas e 100.000 registros;
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
| Linhas da relação de produtos | 25.000 |
| Produtos únicos da relação de produtos | 25.000 |
| Linhas do inventário | 120.000 |
| Registros do inventário | 100.000 |
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

### “Nenhuma categoria ou produto ativo foi encontrado”

Confira empresa, indicador de produto ativo e os filtros da emissão.

### “O ODS contém fórmulas”

Salve uma exportação com valores finais. Não edite o arquivo inserindo fórmulas.

### Produto desapareceu do catálogo depois da importação

Verifique se ele estava presente, ativo, com marca/fabricante diferente de `INATIVOS` e preço maior
que zero na Relação de Produtos. Em seguida, importe o Inventário para sincronizar sua quantidade.
Produtos já visíveis cujo estoque seja atualizado para zero aparecem com a indicação `ESGOTADO`.

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
