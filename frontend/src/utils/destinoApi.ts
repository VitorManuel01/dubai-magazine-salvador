/** Confina a sessão à origem e ao prefixo de caminho configurados para a API. */
export function pertenceApi(destino: URL, api: URL): boolean {
  const caminho = api.pathname.replace(/\/$/, '');
  return destino.origin === api.origin
    && (caminho === '' || destino.pathname === caminho
      || destino.pathname.startsWith(`${caminho}/`));
}
