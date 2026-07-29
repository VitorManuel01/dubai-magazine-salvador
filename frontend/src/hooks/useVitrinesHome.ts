import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { VitrineHome } from '../interface/VitrineHome';

const buscarVitrines = async (administracao: boolean): Promise<VitrineHome[]> => {
  const rota = administracao ? '/admin/vitrines-home' : '/vitrines-home';
  const response = await axios.get<VitrineHome[]>(rota);
  return response.data;
};

export function useVitrinesHome(administracao = false) {
  return useQuery({
    queryKey: ['vitrines-home', administracao ? 'administracao' : 'publicas'],
    queryFn: () => buscarVitrines(administracao),
    staleTime: administracao ? 0 : 5 * 60 * 1000,
  });
}
