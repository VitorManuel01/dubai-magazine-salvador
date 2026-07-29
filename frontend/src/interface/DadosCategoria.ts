export interface DadosCategoria {
  codigo: string;
  nome: string;
  nivel: number;
  caminho: string;
  categoriaPaiCodigo: string | null;
  exibirNoSite: boolean;
}
