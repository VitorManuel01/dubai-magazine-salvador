export interface ProdutoCatalogoPublico {
    idPublico: string;
    nomeExibidoSite: string;
    marca: string | null;
    precoComIpi: number;
    emPromocao: boolean;
    precoPromocao: number | null;
    porcDesconto: number | null;
    dataFinalProm: string | null;
    esgotado: boolean;
    categoriaCodigo: string;
    categoriaNome: string;
    categoriaCaminho: string;
    imagemUrl: string | null;
    imagemHoverUrl: string | null;
    imagens: string[];
}

export interface ProdutoCatalogoInterno extends ProdutoCatalogoPublico {
    codigoSantri: string;
}

export interface DadosProdutos extends ProdutoCatalogoInterno {
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
    dataInicialProm: string | null;
    porcMargem: number | null;
    especial: boolean;
    descricao: string | null;
    galeria: ImagemProduto[];
}

export interface ImagemProduto {
    id: number;
    url: string;
    ordem: number;
}

export interface ProdutoDetalhePublico {
    idPublico: string;
    nomeExibidoSite: string;
    precoComIpi: number;
    emPromocao: boolean;
    precoPromocao: number | null;
    porcDesconto: number | null;
    esgotado: boolean;
    categoriaNome: string;
    categoriaCaminho: string;
    imagens: string[];
    descricao: string | null;
}

export type ProdutoCatalogo = ProdutoCatalogoPublico | ProdutoCatalogoInterno | DadosProdutos;

export function ehProdutoInterno(
    produto: ProdutoCatalogo,
): produto is ProdutoCatalogoInterno | DadosProdutos {
    return 'codigoSantri' in produto;
}

export function ehProdutoAdministrativo(
    produto: ProdutoCatalogo,
): produto is DadosProdutos {
    return 'exibirNoSite' in produto;
}
