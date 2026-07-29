import { DadosProdutos } from './DadosProdutos';

export interface VitrineHome {
  id: number;
  categoriaCodigo: string;
  categoriaNome: string;
  categoriaCaminho: string;
  titulo: string;
  descricao: string;
  ordem: number;
  ativo: boolean;
  produtos: DadosProdutos[];
}

export interface VitrineHomeRequest {
  categoriaCodigo: string;
  titulo: string;
  descricao: string;
  ordem: number;
  ativo: boolean;
}
