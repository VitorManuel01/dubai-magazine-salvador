import { CategoriaCatalogo } from '../interface/DadosCategoria';
import { ProdutoCatalogo } from '../interface/DadosProdutos';

const CODIGOS_CATEGORIAS_INTERNAS = ['008'];

const normalizar = (valor?: string | null) => (valor ?? '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toLocaleUpperCase('pt-BR')
  .trim();

export function ehCodigoCategoriaInterna(codigo?: string | null): boolean {
  const codigoNormalizado = (codigo ?? '').trim();
  return CODIGOS_CATEGORIAS_INTERNAS.some(
    (codigoInterno) => codigoNormalizado === codigoInterno
      || codigoNormalizado.startsWith(`${codigoInterno}.`),
  );
}

export function categoriaPermitidaNoCatalogo(categoria: CategoriaCatalogo): boolean {
  const nome = normalizar(categoria.nome);
  const caminho = normalizar(categoria.caminho);

  return !ehCodigoCategoriaInterna(categoria.codigo)
    && nome !== 'USO E CONSUMO'
    && !caminho.startsWith('USO E CONSUMO');
}

export function produtoPermitidoNoCatalogo(produto: ProdutoCatalogo): boolean {
  return !ehCodigoCategoriaInterna(produto.categoriaCodigo)
    && !normalizar(produto.categoriaCaminho).startsWith('USO E CONSUMO');
}
