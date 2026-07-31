export interface CategoriaCatalogoPublico {
  codigo: string;
  nome: string;
  caminho: string;
}

export interface DadosCategoria extends CategoriaCatalogoPublico {
  nivel: number;
  categoriaPaiCodigo: string | null;
  exibirNoSite: boolean;
}

export type CategoriaCatalogo = CategoriaCatalogoPublico | DadosCategoria;
