import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { CategoriaCatalogo } from '../interface/DadosCategoria';
import { categoriaPermitidaNoCatalogo } from '../utils/categoriasCatalogo';

const fetchSubcategorias = async (
  categoriaPaiCodigo: string,
  administracao: boolean,
): Promise<CategoriaCatalogo[]> => {
  const response = await axios.get<CategoriaCatalogo[]>(
    administracao ? '/admin/categorias' : '/categoria',
    {
      params: {
        ...(administracao ? { somenteVisiveis: false } : {}),
        categoriaPaiCodigo,
      },
    },
  );
  return administracao
    ? response.data
    : response.data.filter(categoriaPermitidaNoCatalogo);
};

export function useSubcategorias(categoriaPaiCodigo?: string, perfil = 'publico') {
  const administracao = perfil === 'ROLE_ADMIN';
  return useQuery({
    queryKey: ['subcategorias', categoriaPaiCodigo, perfil],
    queryFn: () => fetchSubcategorias(categoriaPaiCodigo!, administracao),
    enabled: Boolean(categoriaPaiCodigo),
    staleTime: 5 * 60 * 1000,
  });
}
