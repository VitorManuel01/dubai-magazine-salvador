import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

interface AlteracaoVisibilidade {
  codigosSantri: string[];
  exibirNoSite: boolean;
}

interface RespostaVisibilidade {
  produtosSelecionados: number;
  produtosAlterados: number;
  exibirNoSite: boolean;
}

async function alterarVisibilidadeProdutos(
  dados: AlteracaoVisibilidade,
): Promise<RespostaVisibilidade> {
  const resposta = await axios.put<RespostaVisibilidade>(
    '/admin/produtos/visibilidade',
    dados,
  );
  return resposta.data;
}

export function useAlterarVisibilidadeProdutos() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: alterarVisibilidadeProdutos,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['dados-produto'],
        refetchType: 'all',
      });
    },
  });
}
