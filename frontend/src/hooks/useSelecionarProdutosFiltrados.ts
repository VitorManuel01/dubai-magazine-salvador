import { useMutation } from '@tanstack/react-query';
import axios from 'axios';
import { PaginaProdutos } from '../interface/PaginaProdutos';

const LIMITE_PRODUTOS_EM_LOTE = 200;
const TAMANHO_PAGINA_SELECAO = 60;

interface FiltrosSelecaoProdutos {
  categoriaCodigo?: string;
  busca?: string;
  precoMinimo: number | null;
  precoMaximo: number | null;
  apenasVisiveis: boolean;
  totalEncontrado: number;
}

interface ResultadoSelecaoProdutos {
  codigosSantri: string[];
  totalEncontrado: number;
  limitado: boolean;
}

async function selecionarProdutosFiltrados({
  categoriaCodigo,
  busca,
  precoMinimo,
  precoMaximo,
  apenasVisiveis,
  totalEncontrado,
}: FiltrosSelecaoProdutos): Promise<ResultadoSelecaoProdutos> {
  const quantidadeDesejada = Math.min(totalEncontrado, LIMITE_PRODUTOS_EM_LOTE);
  if (quantidadeDesejada <= 0) {
    return { codigosSantri: [], totalEncontrado: 0, limitado: false };
  }

  const paginasNecessarias = Math.ceil(quantidadeDesejada / TAMANHO_PAGINA_SELECAO);
  const respostas = await Promise.all(
    Array.from({ length: paginasNecessarias }, (_, pagina) => axios.get<PaginaProdutos>(
      '/admin/produtos',
      {
        params: {
          ...(categoriaCodigo ? { categoriaCodigo } : {}),
          ...(busca ? { busca } : {}),
          ...(precoMinimo !== null ? { precoMinimo } : {}),
          ...(precoMaximo !== null ? { precoMaximo } : {}),
          ...(apenasVisiveis ? { apenasVisiveis: true } : {}),
          pagina,
          tamanho: TAMANHO_PAGINA_SELECAO,
        },
      },
    )),
  );

  const codigos = new Set<string>();
  respostas.forEach(({ data }) => {
    data.content.forEach((produto) => {
      if (codigos.size >= LIMITE_PRODUTOS_EM_LOTE) return;
      if ('codigoSantri' in produto) codigos.add(produto.codigoSantri);
    });
  });

  return {
    codigosSantri: Array.from(codigos),
    totalEncontrado,
    limitado: totalEncontrado > codigos.size,
  };
}

export function useSelecionarProdutosFiltrados() {
  return useMutation({ mutationFn: selecionarProdutosFiltrados });
}

export { LIMITE_PRODUTOS_EM_LOTE };
