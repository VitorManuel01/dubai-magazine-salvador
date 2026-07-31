export interface ProdutoVitrineLoja {
  codigoSantri: string;
  nomeExibidoSite: string;
  marca: string | null;
  codigoOriginal: string | null;
  unidade: string | null;
  quantidade: number;
  precoVenda: number;
  imagemUrl: string | null;
}

export interface ProdutoCandidatoVitrineLoja {
  codigoSantri: string;
  nomeExibidoSite: string;
  marca: string | null;
  codigoOriginal: string | null;
  categoriaCodigo: string;
  categoriaCaminho: string;
  exibirNoSite: boolean;
}

export interface SecaoVitrineLoja {
  id: number;
  titulo: string;
  conteudo: string;
  ordem: number;
}

export interface OpcaoVitrineLoja {
  id: number;
  produto: ProdutoVitrineLoja;
  rotuloOpcao: string;
  ordem: number;
  imagens: string[];
  secoes: SecaoVitrineLoja[];
}

export interface VitrineLoja {
  id: number;
  ativo: boolean;
  opcoes: OpcaoVitrineLoja[];
  criadoEm: string;
  atualizadoEm: string;
}

export interface PaginaVitrinesLoja {
  content: VitrineLoja[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface PaginaProdutosCandidatosVitrineLoja {
  content: ProdutoCandidatoVitrineLoja[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface SecaoVitrineLojaRequest {
  titulo: string;
  conteudo: string;
  ordem: number;
}

export interface OpcaoVitrineLojaRequest {
  produtoCodigoSantri: string;
  rotuloOpcao: string;
  ordem: number;
  imagens: string[];
  secoes: SecaoVitrineLojaRequest[];
}

export interface VitrineLojaRequest {
  ativo: boolean;
  opcoes: OpcaoVitrineLojaRequest[];
}
