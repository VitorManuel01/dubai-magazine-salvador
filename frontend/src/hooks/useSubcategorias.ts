import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { CategoriaCatalogo } from '../interface/DadosCategoria';

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
  return response.data;
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
