import { DadosProdutos } from './DadosProdutos';

export interface PaginaProdutos {
  content: DadosProdutos[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
