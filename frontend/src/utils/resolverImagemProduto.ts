import { API_BASE_URL } from '../config/axiosConfig';

export const IMAGEM_PRODUTO_PLACEHOLDER = '/assets/products/product-placeholder.svg';

export function resolverImagemProduto(imagemUrl: string | null): string {
  if (!imagemUrl) {
    return IMAGEM_PRODUTO_PLACEHOLDER;
  }
  if (/^https?:\/\//i.test(imagemUrl)) {
    if (window.location.protocol === 'https:' && imagemUrl.toLowerCase().startsWith('http:')) {
      return IMAGEM_PRODUTO_PLACEHOLDER;
    }
    return imagemUrl;
  }
  return `${API_BASE_URL}${imagemUrl.startsWith('/') ? '' : '/'}${imagemUrl}`;
}
