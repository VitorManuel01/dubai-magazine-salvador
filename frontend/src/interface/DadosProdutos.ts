export interface DadosProdutos {
    codigoSantri: string;
    descricao: string;
    nomeExibidoSite: string;
    ncm: string;
    unidade: string;
    marca: string;
    codigoOriginal: string;
    quantidade: number;
    precoVenda: number;
    precoVendaIva: number;
    categoriaCodigo: string;
    categoriaNome: string;
    categoriaCaminho: string;
    imagemUrl: string | null;
    exibirNoSite: boolean;
    destaqueNaHome: boolean;
    ultimaImportacaoEm?: string | null;
}
