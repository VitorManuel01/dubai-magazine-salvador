import axios from "axios"
import { ProdutoCatalogo } from "../interface/DadosProdutos"
import { PaginaProdutos } from "../interface/PaginaProdutos";
import { useQuery } from "@tanstack/react-query";
import {
    ehCodigoCategoriaInterna,
    produtoPermitidoNoCatalogo,
} from "../utils/categoriasCatalogo";



const fetchData = async (
    categoriaCodigo?: string,
    pagina = 0,
    busca?: string,
    somenteDestaques = false,
    administracao = false,
): Promise<PaginaProdutos> => {
    if (!administracao && ehCodigoCategoriaInterna(categoriaCodigo)) {
        return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 24,
            number: 0,
            first: true,
            last: true,
        };
    }

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
    if (administracao) {
        return response.data;
    }

    const produtosPermitidos = response.data.content.filter(produtoPermitidoNoCatalogo);
    const removidosNestaPagina = response.data.content.length - produtosPermitidos.length;

    return {
        ...response.data,
        content: produtosPermitidos,
        totalElements: Math.max(0, response.data.totalElements - removidosNestaPagina),
    };
}


export function useDadosProdutos(
    categoriaCodigo?: string,
    pagina = 0,
    perfil = 'publico',
    busca?: string,
    somenteDestaques = false,
    habilitado = true,
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
        retry: 2,
        enabled: habilitado,
        staleTime: somenteDestaques ? 0 : undefined,
        refetchOnMount: somenteDestaques ? 'always' : true,
    })

    return {
        ...query,
        data: query.data?.content ?? ([] as ProdutoCatalogo[]),
        paginacao: query.data,
    };
}
