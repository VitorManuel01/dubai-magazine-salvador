import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { DepoimentoHome } from '../interface/DepoimentoHome';

const CHAVE_DEPOIMENTOS_HOME = ['depoimentos-home'] as const;

export function useDepoimentosHome() {
  return useQuery({
    queryKey: CHAVE_DEPOIMENTOS_HOME,
    queryFn: async () => (
      await axios.get<DepoimentoHome[]>('/depoimentos-home')
    ).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAtualizarDepoimentosHome() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (depoimentos: DepoimentoHome[]) => (
      await axios.put<DepoimentoHome[]>('/admin/depoimentos-home', {
        depoimentos,
      })
    ).data,
    onSuccess: (depoimentos) => {
      queryClient.setQueryData(CHAVE_DEPOIMENTOS_HOME, depoimentos);
    },
  });
}
