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
    perfil = 'publico',
    apenasVisiveis = false,
): Promise<PaginaProdutos> => {
    const administracao = perfil === 'ROLE_ADMIN';
    const interno = administracao || perfil === 'ROLE_FUNCIONARIO';
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
        administracao ? "/admin/produtos" : interno ? "/interno/produtos" : "/produto",
        {
            params: {
                ...(categoriaCodigo ? { categoriaCodigo } : {}),
                ...(busca ? { busca } : {}),
                ...(somenteDestaques ? { somenteDestaques: true } : {}),
                ...(administracao && apenasVisiveis ? { apenasVisiveis: true } : {}),
                pagina,
                tamanho: 24,
            },
        },
    );
    if (interno) {
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
    apenasVisiveis = false,
){
    const query = useQuery({
        queryFn: () => fetchData(
            categoriaCodigo,
            pagina,
            busca,
            somenteDestaques,
            perfil,
            apenasVisiveis,
        ),
        queryKey: [
            'dados-produto',
            categoriaCodigo ?? 'todos',
            busca ?? 'sem-busca',
            somenteDestaques ? 'destaques' : 'catalogo',
            pagina,
            perfil,
            apenasVisiveis ? 'apenas-visiveis' : 'todos-status',
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
