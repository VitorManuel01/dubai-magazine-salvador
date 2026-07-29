import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { DadosCategoria } from '../interface/DadosCategoria';

const fetchCategoriasPrincipais = async (): Promise<DadosCategoria[]> => {
  const response = await axios.get<DadosCategoria[]>('/categoria', {
    params: {
      somenteVisiveis: false,
      nivel: 1,
    },
  });
  return response.data;
};

export function useCategoriasPrincipais(perfil = 'publico') {
  return useQuery({
    queryKey: ['categorias-principais', perfil],
    queryFn: fetchCategoriasPrincipais,
    staleTime: 5 * 60 * 1000,
  });
}
