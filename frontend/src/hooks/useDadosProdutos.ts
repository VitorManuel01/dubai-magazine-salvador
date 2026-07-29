import axios from "axios"
import { DadosProdutos } from "../interface/DadosProdutos"
import { PaginaProdutos } from "../interface/PaginaProdutos";
import { useQuery } from "@tanstack/react-query";



const fetchData = async (
    categoriaCodigo?: string,
    pagina = 0,
    busca?: string,
    somenteDestaques = false,
): Promise<PaginaProdutos> => {
    const response = await axios.get<PaginaProdutos>("/produto", {
        params: {
            ...(categoriaCodigo ? { categoriaCodigo } : {}),
            ...(busca ? { busca } : {}),
            ...(somenteDestaques ? { somenteDestaques: true } : {}),
            pagina,
            tamanho: 24,
        },
    });
    return response.data;
}


export function useDadosProdutos(
    categoriaCodigo?: string,
    pagina = 0,
    perfil = 'publico',
    busca?: string,
    somenteDestaques = false,
){
    const query = useQuery({
        queryFn: () => fetchData(categoriaCodigo, pagina, busca, somenteDestaques),
        queryKey: [
            'dados-produto',
            categoriaCodigo ?? 'todos',
            busca ?? 'sem-busca',
            somenteDestaques ? 'destaques' : 'catalogo',
            pagina,
            perfil,
        ],
        retry: 2
    })

    return {
        ...query,
        data: query.data?.content ?? ([] as DadosProdutos[]),
        paginacao: query.data,
    };
}
