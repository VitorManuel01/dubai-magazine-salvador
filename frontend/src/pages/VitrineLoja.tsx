import {
  type FormEvent,
  type MouseEvent,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Link } from 'react-router-dom';
import {
  type OpcaoVitrineLoja,
  type PaginaVitrinesLoja,
  type VitrineLoja as VitrineLojaDados,
} from '../interface/VitrineLoja';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../utils/resolverImagemProduto';
import './VitrineLoja.css';

function formatarMoeda(valor: number) {
  return Number(valor || 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}

function imagensDaOpcao(opcao: OpcaoVitrineLoja) {
  const imagens = opcao.imagens.length > 0
    ? opcao.imagens
    : opcao.produto.imagemUrl
      ? [opcao.produto.imagemUrl]
      : [IMAGEM_PRODUTO_PLACEHOLDER];
  return imagens.map(resolverImagemProduto);
}

function VitrineLoja() {
  const [termo, setTermo] = useState('');
  const [busca, setBusca] = useState('');
  const [vitrineSelecionadaId, setVitrineSelecionadaId] = useState<number | null>(null);
  const [opcaoSelecionadaId, setOpcaoSelecionadaId] = useState<number | null>(null);
  const [imagemSelecionada, setImagemSelecionada] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['vitrine-loja', busca],
    queryFn: async () => (
      await axios.get<PaginaVitrinesLoja>('/vitrine-loja', {
        params: {
          ...(busca ? { busca } : {}),
          pagina: 0,
          tamanho: 50,
        },
      })
    ).data,
  });

  const vitrines = useMemo(() => data?.content ?? [], [data?.content]);
  const vitrineSelecionada = vitrines.find(
    (vitrine) => vitrine.id === vitrineSelecionadaId
  ) ?? vitrines[0];
  const opcaoSelecionada = vitrineSelecionada?.opcoes.find(
    (opcao) => opcao.id === opcaoSelecionadaId
  ) ?? vitrineSelecionada?.opcoes[0];
  const imagens = useMemo(
    () => opcaoSelecionada ? imagensDaOpcao(opcaoSelecionada) : [],
    [opcaoSelecionada]
  );

  useEffect(() => {
    if (vitrines.length === 0) {
      setVitrineSelecionadaId(null);
      return;
    }
    if (!vitrines.some((vitrine) => vitrine.id === vitrineSelecionadaId)) {
      setVitrineSelecionadaId(vitrines[0].id);
    }
  }, [vitrineSelecionadaId, vitrines]);

  useEffect(() => {
    if (!vitrineSelecionada || vitrineSelecionada.opcoes.length === 0) {
      setOpcaoSelecionadaId(null);
      return;
    }
    if (!vitrineSelecionada.opcoes.some((opcao) => opcao.id === opcaoSelecionadaId)) {
      setOpcaoSelecionadaId(vitrineSelecionada.opcoes[0].id);
    }
  }, [opcaoSelecionadaId, vitrineSelecionada]);

  useEffect(() => {
    setImagemSelecionada(imagens[0] ?? '');
  }, [imagens]);

  const pesquisar = (event: FormEvent) => {
    event.preventDefault();
    setBusca(termo.trim());
  };

  const selecionarVitrine = (vitrine: VitrineLojaDados) => {
    setVitrineSelecionadaId(vitrine.id);
    setOpcaoSelecionadaId(vitrine.opcoes[0]?.id ?? null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const acompanharPonteiro = (event: MouseEvent<HTMLDivElement>) => {
    const area = event.currentTarget;
    const limites = area.getBoundingClientRect();
    const posicaoX = ((event.clientX - limites.left) / limites.width) * 100;
    const posicaoY = ((event.clientY - limites.top) / limites.height) * 100;
    area.style.setProperty('--zoom-x', `${posicaoX}%`);
    area.style.setProperty('--zoom-y', `${posicaoY}%`);
  };

  const centralizarZoom = (event: MouseEvent<HTMLDivElement>) => {
    event.currentTarget.style.setProperty('--zoom-x', '50%');
    event.currentTarget.style.setProperty('--zoom-y', '50%');
  };

  return (
    <div className="loja-showcase-page">
      <section className="loja-showcase-intro">
        <div>
          <span className="loja-showcase-eyebrow">Consulta interna</span>
          <h1>Vitrine da loja física</h1>
          <p>Pesquise os produtos selecionados para apresentar aos clientes na loja.</p>
          <Link className="loja-showcase-catalog-link" to="/produtos">
            <i className="bi bi-arrow-left" />
            Voltar ao catálogo
          </Link>
        </div>
        <form className="loja-showcase-search" role="search" onSubmit={pesquisar}>
          <i className="bi bi-search" />
          <input
            type="search"
            value={termo}
            onChange={(event) => setTermo(event.target.value)}
            placeholder="Nome, código, marca ou especificação"
            aria-label="Pesquisar na vitrine"
          />
          {termo && (
            <button
              className="loja-showcase-search__clear"
              type="button"
              aria-label="Limpar pesquisa"
              onClick={() => {
                setTermo('');
                setBusca('');
              }}
            >
              <i className="bi bi-x-lg" />
            </button>
          )}
          <button className="loja-showcase-search__submit" type="submit">
            Pesquisar
          </button>
        </form>
      </section>

      {isLoading && <div className="loja-showcase-state">Carregando a vitrine...</div>}
      {error && (
        <div className="loja-showcase-state loja-showcase-state--error">
          Não foi possível carregar a vitrine. Confirme se você está conectado como
          funcionário ou administrador.
        </div>
      )}
      {!isLoading && !error && vitrines.length === 0 && (
        <div className="loja-showcase-state">
          {busca
            ? `Nenhum produto selecionado foi encontrado para “${busca}”.`
            : 'Ainda não existem produtos ativos na vitrine da loja.'}
        </div>
      )}

      {opcaoSelecionada && vitrineSelecionada && (
        <>
          <article className="loja-product-detail">
            <section className="loja-product-gallery" aria-label="Galeria do produto">
              <div className="loja-product-thumbnails">
                {imagens.map((imagem, indice) => (
                  <button
                    type="button"
                    key={`${imagem}-${indice}`}
                    className={imagemSelecionada === imagem ? 'is-active' : ''}
                    onClick={() => setImagemSelecionada(imagem)}
                    aria-label={`Ver imagem ${indice + 1}`}
                  >
                    <img src={imagem} alt="" />
                  </button>
                ))}
              </div>
              <div
                className="loja-product-image"
                onMouseMove={acompanharPonteiro}
                onMouseLeave={centralizarZoom}
              >
                <img
                  src={imagemSelecionada || imagens[0]}
                  alt={opcaoSelecionada.produto.nomeExibidoSite}
                  onError={(event) => {
                    event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
                  }}
                />
              </div>
            </section>

            <section className="loja-product-summary">
              <h2>{opcaoSelecionada.produto.nomeExibidoSite}</h2>
              {opcaoSelecionada.produto.marca && (
                <p className="loja-product-summary__brand">
                  Marca: <strong>{opcaoSelecionada.produto.marca}</strong>
                </p>
              )}

              {vitrineSelecionada.opcoes.length > 1 && (
                <div className="loja-product-options">
                  <span>Opções disponíveis</span>
                  <div>
                    {vitrineSelecionada.opcoes.map((opcao) => (
                      <button
                        type="button"
                        key={opcao.id}
                        className={opcao.id === opcaoSelecionada.id ? 'is-active' : ''}
                        onClick={() => setOpcaoSelecionadaId(opcao.id)}
                      >
                        {opcao.rotuloOpcao}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="loja-product-price">
                <small>Preço de venda</small>
                <strong>{formatarMoeda(opcaoSelecionada.produto.precoVenda)}</strong>
              </div>

              <div className="loja-product-stock">
                <i className="bi bi-check-circle-fill" />
                <span>
                  {Number(opcaoSelecionada.produto.quantidade) > 0
                    ? `${Number(opcaoSelecionada.produto.quantidade).toLocaleString('pt-BR')} ${opcaoSelecionada.produto.unidade ?? 'un.'} em estoque`
                    : 'Consulte a disponibilidade'}
                </span>
              </div>

              {opcaoSelecionada.produto.codigoOriginal && (
                <p className="loja-product-summary__original">
                  Referência: {opcaoSelecionada.produto.codigoOriginal}
                </p>
              )}
            </section>
          </article>

          <section className="loja-product-sections">
            {opcaoSelecionada.secoes.map((secao) => (
              <article key={secao.id}>
                <h3>{secao.titulo}</h3>
                <p>{secao.conteudo}</p>
              </article>
            ))}
          </section>
        </>
      )}

      {vitrines.length > 1 && (
        <section className="loja-showcase-results">
          <div className="loja-showcase-results__heading">
            <div>
              <span>Produtos selecionados</span>
              <h2>{busca ? 'Resultados da pesquisa' : 'Outras vitrines'}</h2>
            </div>
            <strong>{data?.totalElements ?? vitrines.length} encontrados</strong>
          </div>
          <div className="loja-showcase-results__grid">
            {vitrines.map((vitrine) => {
              const primeiraOpcao = vitrine.opcoes[0];
              if (!primeiraOpcao) return null;
              const imagem = imagensDaOpcao(primeiraOpcao)[0];
              return (
                <button
                  type="button"
                  key={vitrine.id}
                  className={vitrine.id === vitrineSelecionada.id ? 'is-active' : ''}
                  onClick={() => selecionarVitrine(vitrine)}
                >
                  <img src={imagem} alt="" />
                  <span>
                    <strong>{primeiraOpcao.produto.nomeExibidoSite}</strong>
                    <small>{vitrine.opcoes.length} opção(ões)</small>
                  </span>
                  <i className="bi bi-chevron-right" />
                </button>
              );
            })}
          </div>
        </section>
      )}
    </div>
  );
}

export default VitrineLoja;
