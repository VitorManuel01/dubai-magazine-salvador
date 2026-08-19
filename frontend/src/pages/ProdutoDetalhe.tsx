import {
  type ChangeEvent,
  type MouseEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios, { AxiosError } from 'axios';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  type DadosProdutos,
  type ProdutoDetalhePublico,
  type ImagemProduto,
} from '../interface/DadosProdutos';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../utils/resolverImagemProduto';
import { validarImagemProduto } from '../utils/validacaoArquivos';
import './ProdutoDetalhe.css';

type DetalheProduto = ProdutoDetalhePublico | DadosProdutos;

const TAMANHOS_FONTE = [12, 14, 16, 18, 20, 24, 28, 32];
const PADRAO_FORMATACAO = /(\*\*(.+?)\*\*|\[tamanho=(12|14|16|18|20|24|28|32)](.+?)\[\/tamanho])/g;

function formatarMoeda(valor: number) {
  return Number(valor || 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}

function formatarDesconto(valor: number) {
  return Number(valor).toLocaleString('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}

function renderizarTrechos(texto: string, chave: string): ReactNode[] {
  const elementos: ReactNode[] = [];
  let cursor = 0;
  let indice = 0;
  PADRAO_FORMATACAO.lastIndex = 0;

  for (const correspondencia of texto.matchAll(PADRAO_FORMATACAO)) {
    const inicio = correspondencia.index ?? 0;
    if (inicio > cursor) {
      elementos.push(texto.slice(cursor, inicio));
    }
    if (correspondencia[2] != null) {
      elementos.push(
        <strong key={`${chave}-negrito-${indice}`}>
          {renderizarTrechos(correspondencia[2], `${chave}-negrito-${indice}`)}
        </strong>,
      );
    } else {
      elementos.push(
        <span
          key={`${chave}-tamanho-${indice}`}
          style={{ fontSize: `${Number(correspondencia[3])}px` }}
        >
          {renderizarTrechos(correspondencia[4], `${chave}-tamanho-${indice}`)}
        </span>,
      );
    }
    cursor = inicio + correspondencia[0].length;
    indice += 1;
  }
  if (cursor < texto.length) elementos.push(texto.slice(cursor));
  return elementos;
}

function DescricaoFormatada({ descricao }: { descricao: string | null }) {
  if (!descricao?.trim()) {
    return <p className="product-detail-description__empty">Descrição ainda não cadastrada.</p>;
  }

  const linhas = descricao.split('\n');
  const blocos: ReactNode[] = [];
  let indice = 0;
  while (indice < linhas.length) {
    const linha = linhas[indice];
    if (linha.startsWith('- ')) {
      const itens: string[] = [];
      while (indice < linhas.length && linhas[indice].startsWith('- ')) {
        itens.push(linhas[indice].slice(2));
        indice += 1;
      }
      blocos.push(
        <ul key={`lista-${indice}`}>
          {itens.map((item, posicao) => (
            <li key={`item-${indice}-${posicao}`}>
              {renderizarTrechos(item, `item-${indice}-${posicao}`)}
            </li>
          ))}
        </ul>,
      );
      continue;
    }
    if (!linha.trim()) {
      blocos.push(<div className="product-detail-description__space" key={`vazio-${indice}`} />);
    } else {
      blocos.push(
        <p key={`paragrafo-${indice}`}>
          {renderizarTrechos(linha, `paragrafo-${indice}`)}
        </p>,
      );
    }
    indice += 1;
  }
  return <>{blocos}</>;
}

function mensagemErro(erro: unknown, padrao: string) {
  const resposta = (erro as AxiosError<{ detail?: string; message?: string }>).response?.data;
  return resposta?.detail || resposta?.message || padrao;
}

function ProdutoDetalhe() {
  const { idPublico = '' } = useParams();
  const { funcao } = useAuth();
  const administrador = funcao === 'ROLE_ADMIN';
  const queryClient = useQueryClient();
  const [imagemSelecionada, setImagemSelecionada] = useState('');
  const [editandoDescricao, setEditandoDescricao] = useState(false);
  const [descricao, setDescricao] = useState('');
  const [tamanhoFonte, setTamanhoFonte] = useState(16);
  const [erroImagem, setErroImagem] = useState('');
  const descricaoRef = useRef<HTMLTextAreaElement>(null);

  const chaveDetalhe = ['produto-detalhe', idPublico, administrador];
  const detalheQuery = useQuery({
    queryKey: chaveDetalhe,
    enabled: Boolean(idPublico),
    queryFn: async () => (
      await axios.get<DetalheProduto>(administrador
        ? `/admin/produtos/${encodeURIComponent(idPublico)}/detalhe`
        : `/produto/${encodeURIComponent(idPublico)}`)
    ).data,
  });

  const produto = detalheQuery.data;
  const produtoAdmin = produto && 'codigoSantri' in produto ? produto : null;
  const galeriaDetalhada: ImagemProduto[] = useMemo(
    () => produtoAdmin?.galeria ?? [],
    [produtoAdmin],
  );
  const imagens = useMemo(() => {
    const urls = produtoAdmin
      ? galeriaDetalhada.map((imagem) => imagem.url)
      : produto?.imagens ?? [];
    return urls.length > 0
      ? urls.map(resolverImagemProduto)
      : [IMAGEM_PRODUTO_PLACEHOLDER];
  }, [galeriaDetalhada, produto, produtoAdmin]);

  useEffect(() => {
    setImagemSelecionada((atual) => imagens.includes(atual) ? atual : imagens[0]);
  }, [imagens]);

  useEffect(() => {
    if (produto) setDescricao(produto.descricao ?? '');
  }, [produto]);

  const atualizarConsultas = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['produto-detalhe'] }),
      queryClient.invalidateQueries({ queryKey: ['dados-produto'] }),
      queryClient.invalidateQueries({ queryKey: ['vitrine-loja'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-vitrine-loja'] }),
    ]);
  };

  const salvarDescricao = useMutation({
    mutationFn: async () => {
      if (!produtoAdmin) throw new Error('Produto administrativo indisponível.');
      return (
        await axios.put<DadosProdutos>(
          `/admin/produtos/${encodeURIComponent(produtoAdmin.codigoSantri)}/descricao`,
          { descricao },
        )
      ).data;
    },
    onSuccess: async () => {
      setEditandoDescricao(false);
      await atualizarConsultas();
    },
  });

  const enviarImagem = useMutation({
    mutationFn: async (arquivo: File) => {
      if (!produtoAdmin) throw new Error('Produto administrativo indisponível.');
      const dados = new FormData();
      dados.append('imagem', arquivo);
      return (
        await axios.post<DadosProdutos>(
          `/admin/produtos/${encodeURIComponent(produtoAdmin.codigoSantri)}/imagens`,
          dados,
        )
      ).data;
    },
    onSuccess: atualizarConsultas,
  });

  const removerImagem = useMutation({
    mutationFn: async (imagemId: number) => {
      if (!produtoAdmin) throw new Error('Produto administrativo indisponível.');
      return (
        await axios.delete<DadosProdutos>(
          `/admin/produtos/${encodeURIComponent(produtoAdmin.codigoSantri)}/imagens/${imagemId}`,
        )
      ).data;
    },
    onSuccess: atualizarConsultas,
  });

  const reordenarImagens = useMutation({
    mutationFn: async (ids: number[]) => {
      if (!produtoAdmin) throw new Error('Produto administrativo indisponível.');
      return (
        await axios.put<DadosProdutos>(
          `/admin/produtos/${encodeURIComponent(produtoAdmin.codigoSantri)}/imagens/ordem`,
          { imagens: ids },
        )
      ).data;
    },
    onSuccess: atualizarConsultas,
  });

  const selecionarArquivo = async (event: ChangeEvent<HTMLInputElement>) => {
    const arquivo = event.target.files?.[0];
    event.target.value = '';
    setErroImagem('');
    if (!arquivo) return;
    const erro = await validarImagemProduto(arquivo);
    if (erro) {
      setErroImagem(erro);
      return;
    }
    enviarImagem.mutate(arquivo);
  };

  const moverImagem = (indice: number, deslocamento: -1 | 1) => {
    const destino = indice + deslocamento;
    if (destino < 0 || destino >= galeriaDetalhada.length) return;
    const ids = galeriaDetalhada.map((imagem) => imagem.id);
    [ids[indice], ids[destino]] = [ids[destino], ids[indice]];
    reordenarImagens.mutate(ids);
  };

  const aplicarMarcacao = (inicio: string, fim: string) => {
    const campo = descricaoRef.current;
    if (!campo) return;
    const selecaoInicio = campo.selectionStart;
    const selecaoFim = campo.selectionEnd;
    const selecionado = descricao.slice(selecaoInicio, selecaoFim) || 'texto';
    const proximo = descricao.slice(0, selecaoInicio)
      + inicio
      + selecionado
      + fim
      + descricao.slice(selecaoFim);
    setDescricao(proximo);
    requestAnimationFrame(() => {
      campo.focus();
      campo.setSelectionRange(
        selecaoInicio + inicio.length,
        selecaoInicio + inicio.length + selecionado.length,
      );
    });
  };

  const acompanharPonteiro = (event: MouseEvent<HTMLDivElement>) => {
    const limites = event.currentTarget.getBoundingClientRect();
    event.currentTarget.style.setProperty(
      '--detail-zoom-x',
      `${((event.clientX - limites.left) / limites.width) * 100}%`,
    );
    event.currentTarget.style.setProperty(
      '--detail-zoom-y',
      `${((event.clientY - limites.top) / limites.height) * 100}%`,
    );
  };

  if (detalheQuery.isLoading) {
    return <div className="product-detail-state">Carregando produto...</div>;
  }
  if (detalheQuery.isError || !produto) {
    return (
      <div className="product-detail-state product-detail-state--error">
        <p>Este produto não foi encontrado ou não está disponível no catálogo.</p>
        <Link to="/produtos">Voltar ao catálogo</Link>
      </div>
    );
  }

  const promocao = produto.emPromocao && produto.precoPromocao != null;

  return (
    <div className="product-detail-page">
      <nav className="product-detail-breadcrumb" aria-label="Navegação estrutural">
        <Link to="/">Início</Link>
        <i className="bi bi-chevron-right" />
        <Link to="/produtos">Produtos</Link>
        <i className="bi bi-chevron-right" />
        <span>{produto.categoriaCaminho}</span>
      </nav>

      <article className="product-detail-main">
        <section className="product-detail-gallery" aria-label="Fotos do produto">
          <div className="product-detail-thumbnails">
            {imagens.map((imagem, indice) => (
              <button
                type="button"
                key={`${imagem}-${indice}`}
                className={imagemSelecionada === imagem ? 'is-active' : ''}
                onClick={() => setImagemSelecionada(imagem)}
                aria-label={`Exibir foto ${indice + 1}`}
              >
                <img src={imagem} alt="" />
              </button>
            ))}
          </div>
          <div
            className="product-detail-image"
            onMouseMove={acompanharPonteiro}
            onMouseLeave={(event) => {
              event.currentTarget.style.setProperty('--detail-zoom-x', '50%');
              event.currentTarget.style.setProperty('--detail-zoom-y', '50%');
            }}
          >
            <img
              src={imagemSelecionada || imagens[0]}
              alt={produto.nomeExibidoSite}
              onError={(event) => {
                event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
              }}
            />
          </div>
        </section>

        <section className="product-detail-summary">
          <span className="product-detail-summary__category">{produto.categoriaNome}</span>
          <h1>{produto.nomeExibidoSite}</h1>
          {produtoAdmin && (
            <p className="product-detail-summary__code">
              Código Santri: <strong>{produtoAdmin.codigoSantri}</strong>
            </p>
          )}
          <div className="product-detail-prices">
            <span className={promocao ? 'is-old' : 'is-current'}>
              {formatarMoeda(produto.precoComIpi)}
            </span>
            {promocao && (
              <strong>
                {formatarMoeda(produto.precoPromocao!)}
                {produto.porcDesconto != null
                  && ` (-${formatarDesconto(produto.porcDesconto)}%)`}
              </strong>
            )}
          </div>
          <div className={`product-detail-availability${produto.esgotado ? ' is-sold-out' : ''}`}>
            <i className={`bi ${produto.esgotado ? 'bi-x-circle-fill' : 'bi-check-circle-fill'}`} />
            {produto.esgotado ? 'ESGOTADO' : 'Disponível na loja'}
          </div>
          <a
            className="product-detail-contact"
            href="https://wa.me/message/7LVGHYAFP55NL1"
            target="_blank"
            rel="noreferrer"
          >
            <i className="bi bi-whatsapp" />
            Ficou com dúvida? Fale conosco
          </a>
          <p className="product-detail-purchase-note">
            Este portal é somente um catálogo. Compras são realizadas na loja física.
          </p>
        </section>
      </article>

      {administrador && produtoAdmin && (
        <section className="product-detail-admin-gallery">
          <div>
            <span>Administração</span>
            <h2>Fotos</h2>
            <p>
              A primeira foto é a principal; a segunda aparece ao passar o mouse no card.
              A mesma ordem é usada na vitrine da loja física.
            </p>
          </div>
          <div className="product-detail-admin-gallery__grid">
            {galeriaDetalhada.map((imagem, indice) => (
              <article key={imagem.id}>
                <img src={resolverImagemProduto(imagem.url)} alt={`Foto ${indice + 1}`} />
                <strong>Foto {indice + 1}</strong>
                <div>
                  <button
                    type="button"
                    onClick={() => moverImagem(indice, -1)}
                    disabled={indice === 0 || reordenarImagens.isPending}
                    aria-label="Mover foto para a esquerda"
                  >
                    <i className="bi bi-arrow-left" />
                  </button>
                  <button
                    type="button"
                    onClick={() => moverImagem(indice, 1)}
                    disabled={indice === galeriaDetalhada.length - 1 || reordenarImagens.isPending}
                    aria-label="Mover foto para a direita"
                  >
                    <i className="bi bi-arrow-right" />
                  </button>
                  <button
                    type="button"
                    className="is-danger"
                    onClick={() => {
                      if (window.confirm(`Remover a foto ${indice + 1}?`)) {
                        removerImagem.mutate(imagem.id);
                      }
                    }}
                    disabled={removerImagem.isPending}
                    aria-label="Remover foto"
                  >
                    <i className="bi bi-trash3" />
                  </button>
                </div>
              </article>
            ))}
            {galeriaDetalhada.length < 8 && (
              <label className="product-detail-add-image">
                <i className="bi bi-image" />
                <strong>{enviarImagem.isPending ? 'Enviando...' : 'Adicionar foto'}</strong>
                <small>JPG, PNG ou WEBP, até 5 MB</small>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={selecionarArquivo}
                  disabled={enviarImagem.isPending}
                />
              </label>
            )}
          </div>
          {(erroImagem || enviarImagem.isError || removerImagem.isError || reordenarImagens.isError) && (
            <p className="product-detail-admin-error">
              {erroImagem || mensagemErro(
                enviarImagem.error || removerImagem.error || reordenarImagens.error,
                'Não foi possível atualizar a galeria.',
              )}
            </p>
          )}
        </section>
      )}

      <section className="product-detail-description">
        <header>
          <h2>Descrição</h2>
          {administrador && produtoAdmin && !editandoDescricao && (
            <button type="button" onClick={() => setEditandoDescricao(true)}>
              <i className="bi bi-pencil-square" />
              Editar descrição
            </button>
          )}
        </header>

        {editandoDescricao && produtoAdmin ? (
          <div className="product-description-editor">
            <div className="product-description-toolbar" aria-label="Formatação da descrição">
              <button
                type="button"
                onClick={() => aplicarMarcacao('**', '**')}
                title="Aplicar negrito ao texto selecionado"
              >
                <i className="bi bi-type-bold" /> Negrito
              </button>
              <select
                value={tamanhoFonte}
                onChange={(event) => setTamanhoFonte(Number(event.target.value))}
                aria-label="Tamanho da fonte"
              >
                {TAMANHOS_FONTE.map((tamanho) => (
                  <option key={tamanho} value={tamanho}>{tamanho}px</option>
                ))}
              </select>
              <button
                type="button"
                onClick={() => aplicarMarcacao(`[tamanho=${tamanhoFonte}]`, '[/tamanho]')}
              >
                Aplicar tamanho
              </button>
              <button
                type="button"
                onClick={() => aplicarMarcacao('- ', '')}
                title="Transformar a linha em item de lista"
              >
                <i className="bi bi-list-ul" /> Lista
              </button>
            </div>
            <textarea
              ref={descricaoRef}
              value={descricao}
              onChange={(event) => setDescricao(event.target.value)}
              rows={14}
              maxLength={20000}
              placeholder="Escreva a descrição do produto. Selecione um trecho e use os controles acima."
            />
            <small>{descricao.length.toLocaleString('pt-BR')} de 20.000 caracteres</small>
            <div className="product-description-preview">
              <strong>Pré-visualização</strong>
              <DescricaoFormatada descricao={descricao} />
            </div>
            {salvarDescricao.isError && (
              <p className="product-detail-admin-error">
                {mensagemErro(salvarDescricao.error, 'Não foi possível salvar a descrição.')}
              </p>
            )}
            <div className="product-description-actions">
              <button
                type="button"
                className="is-primary"
                onClick={() => salvarDescricao.mutate()}
                disabled={salvarDescricao.isPending}
              >
                {salvarDescricao.isPending ? 'Salvando...' : 'Salvar descrição'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setDescricao(produto.descricao ?? '');
                  setEditandoDescricao(false);
                  salvarDescricao.reset();
                }}
                disabled={salvarDescricao.isPending}
              >
                Cancelar
              </button>
            </div>
          </div>
        ) : (
          <div className="product-detail-description__content">
            <DescricaoFormatada descricao={produto.descricao} />
          </div>
        )}
      </section>
    </div>
  );
}

export default ProdutoDetalhe;
