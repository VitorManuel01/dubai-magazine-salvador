const MAX_ODS_BYTES = 20 * 1024 * 1024;
const MAX_IMAGEM_BYTES = 5 * 1024 * 1024;

const extensao = (nome: string) => {
  const indice = nome.lastIndexOf('.');
  return indice < 0 ? '' : nome.slice(indice).toLowerCase();
};

const corresponde = (bytes: Uint8Array, inicio: number, assinatura: number[]) =>
  assinatura.every((valor, indice) => bytes[inicio + indice] === valor);

export async function validarArquivoOds(arquivo: File): Promise<string | null> {
  if (extensao(arquivo.name) !== '.ods') {
    return 'O arquivo selecionado precisa ter a extensão .ods.';
  }
  if (arquivo.size > MAX_ODS_BYTES) {
    return 'O arquivo ODS deve possuir no máximo 20 MB.';
  }

  const bytes = new Uint8Array(await arquivo.slice(0, 4).arrayBuffer());
  if (!corresponde(bytes, 0, [0x50, 0x4b, 0x03, 0x04])) {
    return 'O conteúdo do arquivo selecionado não corresponde a um ODS válido.';
  }
  return null;
}

export async function validarImagemProduto(imagem: File): Promise<string | null> {
  if (imagem.size > MAX_IMAGEM_BYTES) {
    return 'A imagem deve possuir no máximo 5 MB.';
  }

  const bytes = new Uint8Array(await imagem.slice(0, 12).arrayBuffer());
  const ehJpeg = corresponde(bytes, 0, [0xff, 0xd8, 0xff]);
  const ehPng = corresponde(bytes, 0, [
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
  ]);
  const ehWebp = corresponde(bytes, 0, [0x52, 0x49, 0x46, 0x46])
    && corresponde(bytes, 8, [0x57, 0x45, 0x42, 0x50]);

  const extensaoInformada = extensao(imagem.name);
  if (ehJpeg && ['.jpg', '.jpeg'].includes(extensaoInformada)) return null;
  if (ehPng && extensaoInformada === '.png') return null;
  if (ehWebp && extensaoInformada === '.webp') return null;

  return 'A extensão e o conteúdo da imagem não correspondem. Use JPG, PNG ou WEBP.';
}
