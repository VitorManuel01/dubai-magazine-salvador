import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

interface RespostaExclusaoProdutos {
  produtosExcluidos: number;
}

async function excluirProdutos(codigosSantri: string[]): Promise<RespostaExclusaoProdutos> {
  const resposta = await axios.delete<RespostaExclusaoProdutos>('/admin/produtos', {
    data: { codigosSantri },
  });
  return resposta.data;
}

export function useExcluirProdutos() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: excluirProdutos,
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
