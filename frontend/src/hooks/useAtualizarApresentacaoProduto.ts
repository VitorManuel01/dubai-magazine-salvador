import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { DadosProdutos } from '../interface/DadosProdutos';
import { FormPrecos } from '../utils/precosPersonalizados';

interface AtualizarApresentacao {
  codigoSantri: string;
  nomeExibidoSite: string;
  exibirNoSite: boolean;
  destaqueNaHome: boolean;
  imagem?: File;
  imagemHover?: File;
  precos: FormPrecos;
}

const atualizarApresentacao = async ({
  codigoSantri,
  nomeExibidoSite,
  exibirNoSite,
  destaqueNaHome,
  imagem,
  imagemHover,
  precos,
}: AtualizarApresentacao): Promise<DadosProdutos> => {
  const formData = new FormData();
  formData.append('nomeExibidoSite', nomeExibidoSite);
  formData.append('exibirNoSite', String(exibirNoSite));
  formData.append('destaqueNaHome', String(destaqueNaHome));
  formData.append('usarPrecosPersonalizados', String(precos.usarPrecosPersonalizados));
  if (precos.precoAVista) formData.append('precoAVista', precos.precoAVista);
  if (precos.precoCartaoParc) formData.append('precoCartaoParc', precos.precoCartaoParc);
  if (precos.maxParcelamento) formData.append('maxParcelamento', precos.maxParcelamento);
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
      await Promise.all(['dados-produto', 'produto-detalhe', 'vitrines-home',
        'admin-vitrine-loja', 'vitrine-loja', 'produtos-para-vitrine'].map((chave) =>
        queryClient.invalidateQueries({ queryKey: [chave], refetchType: 'all' })));
    },
  });
}
