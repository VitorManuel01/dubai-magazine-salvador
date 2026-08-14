import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { DadosProdutos } from '../interface/DadosProdutos';

interface AtualizarApresentacao {
  codigoSantri: string;
  nomeExibidoSite: string;
  exibirNoSite: boolean;
  destaqueNaHome: boolean;
  imagem?: File;
  imagemHover?: File;
}

const atualizarApresentacao = async ({
  codigoSantri,
  nomeExibidoSite,
  exibirNoSite,
  destaqueNaHome,
  imagem,
  imagemHover,
}: AtualizarApresentacao): Promise<DadosProdutos> => {
  const formData = new FormData();
  formData.append('nomeExibidoSite', nomeExibidoSite);
  formData.append('exibirNoSite', String(exibirNoSite));
  formData.append('destaqueNaHome', String(destaqueNaHome));
  if (imagem) {
    formData.append('imagem', imagem);
  }
  if (imagemHover) {
    formData.append('imagemHover', imagemHover);
  }

  const response = await axios.put<DadosProdutos>(
    `/produto/${encodeURIComponent(codigoSantri)}/apresentacao`,
    formData
  );
  return response.data;
};

export function useAtualizarApresentacaoProduto() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: atualizarApresentacao,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['dados-produto'],
        refetchType: 'all',
      });
    },
  });
}
