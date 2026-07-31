import axios from "axios"
import { useMutation, useQueryClient } from "@tanstack/react-query";

export interface ProdutoCadastro {
    codigoSantri: string;
    descricao: string;
    nomeExibidoSite: string;
    ncm: string;
    unidade: string;
    marca: string;
    codigoOriginal: string;
    quantidade: number;
    precoVenda: number;
    precoVendaIva: number;
    categoriaCodigo: string;
    imagemUrl: string;
    exibirNoSite: boolean;
}

const postData = async (data: ProdutoCadastro): Promise<void> => {
    await axios.post("/produto", data);
}


export function useDadosProdutosMutate(){
    const queryClient = useQueryClient();
    const mutate = useMutation({
        mutationFn: postData,
        retry: 2,
        onSuccess: () =>{
            queryClient.invalidateQueries({ queryKey: ['dados-produto'] })
        }
    })

    return mutate;
}
