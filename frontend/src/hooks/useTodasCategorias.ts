import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { DadosCategoria } from '../interface/DadosCategoria';

const buscarCategorias = async (): Promise<DadosCategoria[]> => {
  const response = await axios.get<DadosCategoria[]>('/admin/categorias', {
    params: { somenteVisiveis: false },
  });
  return [...response.data].sort((a, b) => a.caminho.localeCompare(b.caminho, 'pt-BR'));
};

export function useTodasCategorias() {
  return useQuery({
    queryKey: ['categorias', 'todas'],
    queryFn: buscarCategorias,
    staleTime: 5 * 60 * 1000,
  });
}
