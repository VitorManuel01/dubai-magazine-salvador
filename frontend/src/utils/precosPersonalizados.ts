import type { PrecosPersonalizados } from '../interface/PrecosPersonalizados';

export interface FormPrecos {
  usarPrecosPersonalizados: boolean;
  precoAVista: string;
  precoCartaoParc: string;
  maxParcelamento: string;
}

export const formularioPrecos = (produto: PrecosPersonalizados): FormPrecos => ({
  usarPrecosPersonalizados: produto.usarPrecosPersonalizados ?? false,
  precoAVista: produto.precoAVista == null ? '' : String(produto.precoAVista),
  precoCartaoParc: produto.precoCartaoParc == null ? '' : String(produto.precoCartaoParc),
  maxParcelamento: produto.maxParcelamento == null ? '' : String(produto.maxParcelamento),
});

export function validarPrecos(form: FormPrecos): string | null {
  for (const valor of [form.precoAVista, form.precoCartaoParc]) {
    if (!valor && !form.usarPrecosPersonalizados) continue;
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(valor) || Number(valor) <= 0) {
      return 'Informe preços positivos, com no máximo duas casas decimais.';
    }
  }
  if (form.maxParcelamento || form.usarPrecosPersonalizados) {
    if (!/^\d+$/.test(form.maxParcelamento) || Number(form.maxParcelamento) < 1 || Number(form.maxParcelamento) > 36) {
      return 'Informe um número inteiro de parcelas, entre 1 e 36.';
    }
  }
  return null;
}
