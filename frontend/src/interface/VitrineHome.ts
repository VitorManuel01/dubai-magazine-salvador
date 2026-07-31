import { ProdutoCatalogoPublico } from './DadosProdutos';

export interface VitrineHome {
  id: number;
  categoriaCodigo: string;
  categoriaNome: string;
  categoriaCaminho: string;
  titulo: string;
  descricao: string;
  ordem: number;
  ativo: boolean;
  produtos: ProdutoCatalogoPublico[];
}

export interface VitrineHomeRequest {
  categoriaCodigo: string;
  titulo: string;
  descricao: string;
  ordem: number;
  ativo: boolean;
}
