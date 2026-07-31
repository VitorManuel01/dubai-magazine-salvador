import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { CategoriaCatalogo } from '../interface/DadosCategoria';

const fetchCategoriasPrincipais = async (
  administracao: boolean,
): Promise<CategoriaCatalogo[]> => {
  const response = await axios.get<CategoriaCatalogo[]>(
    administracao ? '/admin/categorias' : '/categoria',
    {
      params: {
        ...(administracao ? { somenteVisiveis: false } : {}),
        nivel: 1,
      },
    },
  );
  return response.data;
};

export function useCategoriasPrincipais(perfil = 'publico') {
  const administracao = perfil === 'ROLE_ADMIN';
  return useQuery({
    queryKey: ['categorias-principais', perfil],
    queryFn: () => fetchCategoriasPrincipais(administracao),
    staleTime: 5 * 60 * 1000,
  });
}
