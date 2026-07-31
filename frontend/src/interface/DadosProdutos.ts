export interface ProdutoCatalogoPublico {
    nomeExibidoSite: string;
    marca: string | null;
    precoComIpi: number;
    categoriaCodigo: string;
    categoriaNome: string;
    categoriaCaminho: string;
    imagemUrl: string | null;
}

export interface DadosProdutos extends ProdutoCatalogoPublico {
    codigoSantri: string;
    nome: string;
    ncm: string | null;
    nomeCompra: string | null;
    fabricante: string | null;
    ativoSantri: boolean;
    unidadeVenda: string | null;
    unidadeCompra: string | null;
    dataCadastro: string | null;
    codigoOriginal: string | null;
    codigoBarras: string | null;
    bloqueadoParaCompras: boolean;
    estoque: number;
    precoSemIpi: number;
    percentualIpiEntrada: number | null;
    pesoUnidade: number | null;
    alturaUnidade: number | null;
    larguraUnidade: number | null;
    comprimentoUnidade: number | null;
    volumeUnidadeM3: number | null;
    volumeLitros: number | null;
    pesoCaixa: number | null;
    alturaCaixa: number | null;
    larguraCaixa: number | null;
    comprimentoCaixa: number | null;
    origem: string | null;
    industrializado: boolean | null;
    insumo: boolean | null;
    percentualMaximoAproveitamentoIpi: number | null;
    numeroFci: string | null;
    exibirNoSite: boolean;
    destaqueNaHome: boolean;
    disponivelUltimaImportacao: boolean;
    ultimaImportacaoEm: string | null;
}

export type ProdutoCatalogo = ProdutoCatalogoPublico | DadosProdutos;

export function ehProdutoAdministrativo(
    produto: ProdutoCatalogo,
): produto is DadosProdutos {
    return 'codigoSantri' in produto;
}
