import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

async function excluirProduto(codigoSantri: string): Promise<void> {
  await axios.delete(`/produto/${encodeURIComponent(codigoSantri)}`);
}

export function useExcluirProduto() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: excluirProduto,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dados-produto'] }),
        queryClient.invalidateQueries({ queryKey: ['admin-vitrine-loja'] }),
        queryClient.invalidateQueries({ queryKey: ['vitrine-loja'] }),
        queryClient.invalidateQueries({ queryKey: ['produtos-para-vitrine'] }),
      ]);
    },
  });
}
