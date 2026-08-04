const CHAVE_DISPOSITIVO = 'dubai-magazine-device-id';

let identificadorTemporario: string | null = null;

export function obterIdDispositivo(): string {
  try {
    const existente = localStorage.getItem(CHAVE_DISPOSITIVO);
    if (existente) {
      return existente;
    }

    const novo = crypto.randomUUID();
    localStorage.setItem(CHAVE_DISPOSITIVO, novo);
    return novo;
  } catch {
    identificadorTemporario ??= crypto.randomUUID();
    return identificadorTemporario;
  }
}
