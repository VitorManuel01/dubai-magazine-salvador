import axios from "axios"
import { ProdutoCatalogo } from "../interface/DadosProdutos"
import { PaginaProdutos } from "../interface/PaginaProdutos";
import { useQuery } from "@tanstack/react-query";



const fetchData = async (
    categoriaCodigo?: string,
    pagina = 0,
    busca?: string,
    somenteDestaques = false,
    administracao = false,
): Promise<PaginaProdutos> => {
    const response = await axios.get<PaginaProdutos>(
        administracao ? "/admin/produtos" : "/produto",
        {
            params: {
                ...(categoriaCodigo ? { categoriaCodigo } : {}),
                ...(busca ? { busca } : {}),
                ...(somenteDestaques ? { somenteDestaques: true } : {}),
                pagina,
                tamanho: 24,
            },
        },
    );
    return response.data;
}


export function useDadosProdutos(
    categoriaCodigo?: string,
    pagina = 0,
    perfil = 'publico',
    busca?: string,
    somenteDestaques = false,
){
    const administracao = perfil === 'ROLE_ADMIN';
    const query = useQuery({
        queryFn: () => fetchData(
            categoriaCodigo,
            pagina,
            busca,
            somenteDestaques,
            administracao,
        ),
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
        data: query.data?.content ?? ([] as ProdutoCatalogo[]),
        paginacao: query.data,
    };
}
