import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { DadosCategoria } from '../interface/DadosCategoria';

const fetchSubcategorias = async (categoriaPaiCodigo: string): Promise<DadosCategoria[]> => {
  const response = await axios.get<DadosCategoria[]>('/categoria', {
    params: {
      somenteVisiveis: false,
      categoriaPaiCodigo,
    },
  });
  return response.data;
};

export function useSubcategorias(categoriaPaiCodigo?: string, perfil = 'publico') {
  return useQuery({
    queryKey: ['subcategorias', categoriaPaiCodigo, perfil],
    queryFn: () => fetchSubcategorias(categoriaPaiCodigo!),
    enabled: Boolean(categoriaPaiCodigo),
    staleTime: 5 * 60 * 1000,
  });
}
