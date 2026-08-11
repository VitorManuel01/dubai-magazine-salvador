import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { BannerHome } from '../interface/BannerHome';

const CHAVE_BANNERS_HOME = ['banners-home'] as const;

export function useBannersHome() {
  return useQuery({
    queryKey: CHAVE_BANNERS_HOME,
    queryFn: async () => (
      await axios.get<BannerHome[]>('/banners-home')
    ).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useAtualizarBannerHome() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      posicao,
      imagem,
    }: {
      posicao: number;
      imagem: File;
    }) => {
      const formData = new FormData();
      formData.append('imagem', imagem);
      return (
        await axios.put<BannerHome>(
          `/admin/banners-home/${posicao}`,
          formData
        )
      ).data;
    },
    onSuccess: (bannerAtualizado) => {
      queryClient.setQueryData<BannerHome[]>(CHAVE_BANNERS_HOME, (atuais = []) => {
        const demais = atuais.filter(
          (banner) => banner.posicao !== bannerAtualizado.posicao
        );
        return [...demais, bannerAtualizado].sort(
          (primeiro, segundo) => primeiro.posicao - segundo.posicao
        );
      });
    },
  });
}
