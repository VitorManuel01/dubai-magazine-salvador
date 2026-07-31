import { ProdutoCatalogo } from './DadosProdutos';

export interface PaginaProdutos {
  content: ProdutoCatalogo[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
