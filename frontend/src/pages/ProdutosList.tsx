import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Produtos } from '../components/produtos/produtos';
import { useDadosProdutos } from '../hooks/useDadosProdutos';
import { useAuth } from '../context/AuthContext';
import { useCategoriasPrincipais } from '../hooks/useCategoriasPrincipais';
import { useSubcategorias } from '../hooks/useSubcategorias';
import { useAlterarVisibilidadeProdutos } from '../hooks/useAlterarVisibilidadeProdutos';
import { useExcluirProdutos } from '../hooks/useExcluirProdutos';
import '../styles/ProdutoList.css';

function ProdutoList() {
  const { funcao } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoriaCodigo = searchParams.get('categoria')?.trim() || undefined;
  const subcategoriaCodigo = searchParams.get('subcategoria')?.trim() || undefined;
  const busca = searchParams.get('busca')?.trim() || undefined;
  const categoriaFiltro = subcategoriaCodigo ?? categoriaCodigo;
  const [pagina, setPagina] = useState(0);
  const [paginaDigitada, setPaginaDigitada] = useState('1');
  const [priceDraft, setPriceDraft] = useState({ min: '', max: '' });
  const [appliedPriceRange, setAppliedPriceRange] = useState<{
    min: number | null;
    max: number | null;
  }>({ min: null, max: null });
  const [sortBy, setSortBy] = useState('featured');
  const [apenasVisiveis, setApenasVisiveis] = useState(false);
  const [produtosMarcados, setProdutosMarcados] = useState<Set<string>>(new Set());
  const alterarVisibilidade = useAlterarVisibilidadeProdutos();
  const excluirProdutos = useExcluirProdutos();

  const {
    data = [],
    paginacao,
    isLoading,
    error,
  } = useDadosProdutos(
    categoriaFiltro,
    pagina,
    funcao || 'publico',
    busca,
    false,
    true,
    apenasVisiveis,
  );
  const paginasRetornadas = paginacao?.totalPages ?? 0;
  const totalPaginas = Math.max(paginasRetornadas, 1);
  const { data: categoriasPrincipais = [] } =
    useCategoriasPrincipais(funcao || 'publico');
  const administrador = funcao === 'ROLE_ADMIN';
  const {
    data: produtosSelecionados = [],
    isLoading: carregandoProdutosSelecionados,
  } = useDadosProdutos(
    undefined,
    0,
    'ROLE_ADMIN',
    undefined,
    true,
    administrador
  );
  const {
    data: subcategorias = [],
    isLoading: carregandoSubcategorias,
  } = useSubcategorias(categoriaCodigo, funcao || 'publico');

  const categoriaPrincipal = categoriasPrincipais.find(
    (categoria) => categoria.codigo === categoriaCodigo
  );
  const subcategoriaSelecionada = subcategorias.find(
    (categoria) => categoria.codigo === subcategoriaCodigo
  );
  const tituloCategoria = subcategoriaSelecionada?.nome
    ?? categoriaPrincipal?.nome
    ?? (categoriaCodigo ? `Categoria ${categoriaCodigo}` : 'Todos os produtos');
  const tituloPagina = busca
    ? `Resultados para “${busca}”`
    : tituloCategoria;

  useEffect(() => {
    setPagina(0);
    setPriceDraft({ min: '', max: '' });
    setAppliedPriceRange({ min: null, max: null });
    setSortBy('featured');
    setApenasVisiveis(false);
    setProdutosMarcados(new Set());
  }, [busca, categoriaCodigo, subcategoriaCodigo]);

  useEffect(() => {
    setPaginaDigitada(String(pagina + 1));
  }, [pagina]);

  useEffect(() => {
    if (paginasRetornadas > 0 && pagina >= paginasRetornadas) {
      setPagina(paginasRetornadas - 1);
    }
  }, [pagina, paginasRetornadas]);

  const produtosSelecionaveisNaPagina = useMemo(
    () => data.filter(
      (produto) => 'codigoSantri' in produto
        && 'disponivelUltimaImportacao' in produto
        && produto.disponivelUltimaImportacao,
    ),
    [data],
  );
  const paginaTodaMarcada = produtosSelecionaveisNaPagina.length > 0
    && produtosSelecionaveisNaPagina.every(
      (produto) => 'codigoSantri' in produto && produtosMarcados.has(produto.codigoSantri),
    );

  const alterarSelecaoProduto = (codigoSantri: string, selecionado: boolean) => {
    setProdutosMarcados((atuais) => {
      const novos = new Set(atuais);
      if (selecionado) novos.add(codigoSantri);
      else novos.delete(codigoSantri);
      return novos;
    });
    alterarVisibilidade.reset();
  };

  const alterarSelecaoPagina = (selecionar: boolean) => {
    setProdutosMarcados((atuais) => {
      const novos = new Set(atuais);
      produtosSelecionaveisNaPagina.forEach((produto) => {
        if (!('codigoSantri' in produto)) return;
        if (selecionar) novos.add(produto.codigoSantri);
        else novos.delete(produto.codigoSantri);
      });
      return novos;
    });
    alterarVisibilidade.reset();
  };

  const alterarVisibilidadeDosMarcados = (exibirNoSite: boolean) => {
    const codigos = Array.from(produtosMarcados);
    if (codigos.length === 0) return;
    alterarVisibilidade.mutate({ codigosSantri: codigos, exibirNoSite }, {
      onSuccess: () => setProdutosMarcados(new Set()),
    });
  };

  const excluirMarcados = () => {
    const codigos = Array.from(produtosMarcados);
    if (codigos.length === 0) return;
    const confirmou = window.confirm(
      `Excluir definitivamente ${codigos.length} produto(s) selecionado(s)?\n\n`
      + 'As imagens gerenciadas e referências na vitrine da loja também serão removidas. '
      + 'Produtos que continuarem no próximo relatório Santri poderão ser importados novamente.',
    );
    if (!confirmou) return;

    excluirProdutos.mutate(codigos, {
      onSuccess: () => setProdutosMarcados(new Set()),
    });
  };

  const getPrice = (price: unknown) => {
    const value = Number(String(price));
    return Number.isFinite(value) ? value : 0;
  };

  const filteredProducts = useMemo(() => {
    const result = data.filter((produto) => {
      const price = getPrice(produto.precoComIpi);
      const matchesMin = appliedPriceRange.min === null || price >= appliedPriceRange.min;
      const matchesMax = appliedPriceRange.max === null || price <= appliedPriceRange.max;
      return matchesMin && matchesMax;
    });

    return [...result].sort((left, right) => {
      if (administrador && 'exibirNoSite' in left && 'exibirNoSite' in right) {
        const leftVisivel = left.exibirNoSite && left.disponivelUltimaImportacao;
        const rightVisivel = right.exibirNoSite && right.disponivelUltimaImportacao;
        if (leftVisivel !== rightVisivel) {
          return leftVisivel ? -1 : 1;
        }
      }

      const leftPrice = getPrice(left.precoComIpi);
      const rightPrice = getPrice(right.precoComIpi);

      switch (sortBy) {
        case 'price-asc':
          return leftPrice - rightPrice;
        case 'price-desc':
          return rightPrice - leftPrice;
        case 'stock-desc':
          return ('estoque' in right ? right.estoque : 0)
            - ('estoque' in left ? left.estoque : 0);
        case 'name-asc':
          return left.nomeExibidoSite.localeCompare(right.nomeExibidoSite, 'pt-BR');
        default:
          return 0;
      }
    });
  }, [administrador, appliedPriceRange.max, appliedPriceRange.min, data, sortBy]);

  const selecionarSubcategoria = (codigo?: string) => {
    const novosParametros = new URLSearchParams(searchParams);
    if (codigo) {
      novosParametros.set('subcategoria', codigo);
    } else {
      novosParametros.delete('subcategoria');
    }
    setSearchParams(novosParametros);
  };

  const handleApplyPrice = () => {
    const min = priceDraft.min === '' ? null : Number(priceDraft.min);
    const max = priceDraft.max === '' ? null : Number(priceDraft.max);

    setAppliedPriceRange({
      min: Number.isFinite(min ?? NaN) ? min : null,
      max: Number.isFinite(max ?? NaN) ? max : null,
    });
  };

  const handleClearFilters = () => {
    selecionarSubcategoria();
    setPriceDraft({ min: '', max: '' });
    setAppliedPriceRange({ min: null, max: null });
    setSortBy('featured');
    setApenasVisiveis(false);
  };

  const irParaPagina = () => {
    const paginaInformada = Number(paginaDigitada);
    if (!Number.isInteger(paginaInformada)) {
      setPaginaDigitada(String(pagina + 1));
      return;
    }
    const destino = Math.min(Math.max(paginaInformada, 1), totalPaginas) - 1;
    setPagina(destino);
    setPaginaDigitada(String(destino + 1));
  };

  if (isLoading) {
    return <div className="catalog-page">Carregando catálogo...</div>;
  }

  if (error) {
    return <div className="catalog-page">Erro ao carregar produtos: {(error as Error).message}</div>;
  }

  return (
    <div className="catalog-page">
      <aside className="catalog-store-notice" aria-label="Informações para compra">
        <i className="bi bi-info-circle" aria-hidden="true" />
        <div>
          <strong>Catálogo da loja física</strong>
          <p>
            Este portal refere-se somente ao catálogo de itens disponíveis na loja. Para clientes
            empresa, entre em contato para realizar um pedido.
          </p>
          <div className="catalog-store-notice__actions">
            <a href="https://wa.me/message/7LVGHYAFP55NL1" target="_blank" rel="noreferrer">
              <i className="bi bi-whatsapp" aria-hidden="true" />
              Falar com a Dubai Magazine
              <i className="bi bi-box-arrow-up-right" aria-hidden="true" />
            </a>
            <span><i className="bi bi-shop" /> Compras somente na loja.</span>
          </div>
        </div>
      </aside>
      <section className="catalog-header">
        <div>
          <p className="catalog-breadcrumb">
            <Link to="/">Início</Link> / {tituloPagina}
          </p>
          <h1>{tituloPagina}</h1>
          <p className="catalog-description">
            {busca
              ? `Pesquisa em ${tituloCategoria.toLocaleLowerCase('pt-BR')}.`
              : subcategoriaSelecionada
              ? `Produtos de ${subcategoriaSelecionada.nome} e de seus grupos internos.`
              : categoriaPrincipal
                ? `Produtos de ${categoriaPrincipal.nome} e de todas as suas subcategorias.`
                : 'Explore os produtos disponíveis no catálogo da Dubai Magazine.'}
          </p>
        </div>

        <div className="catalog-toolbar">
          <span className="catalog-results-count">
            Exibindo {filteredProducts.length} de{' '}
            {(paginacao?.totalElements ?? 0).toLocaleString('pt-BR')} produtos
          </span>
          <label className="catalog-sort">
            <span>Ordenar esta página por</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="featured">Ordem do catálogo</option>
              <option value="price-asc">Menor preço</option>
              <option value="price-desc">Maior preço</option>
              <option value="name-asc">Nome A-Z</option>
              {funcao === 'ROLE_ADMIN' && (
                <option value="stock-desc">Maior estoque</option>
              )}
            </select>
          </label>
        </div>
      </section>

      <section className="catalog-layout">
        <aside className="filter-card">
          <div className="filter-card__header">
            <span className="filter-card__eyebrow">Catálogo</span>
            <h2>Filtros</h2>
          </div>

          <div className="filter-group">
            <span className="filter-group__title">Subcategorias</span>

            {!categoriaCodigo && (
              <p className="filter-empty">
                Selecione uma categoria no menu superior para ver suas subcategorias.
              </p>
            )}

            {categoriaCodigo && (
              <>
                <label className="filter-checkbox filter-checkbox--all">
                  <input
                    type="radio"
                    name="subcategoria"
                    checked={!subcategoriaCodigo}
                    onChange={() => selecionarSubcategoria()}
                  />
                  <span>Todas de {categoriaPrincipal?.nome ?? categoriaCodigo}</span>
                </label>

                <div className="filter-list">
                  {subcategorias.map((categoria) => (
                    <label className="filter-checkbox" key={categoria.codigo}>
                      <input
                        type="radio"
                        name="subcategoria"
                        checked={subcategoriaCodigo === categoria.codigo}
                        onChange={() => selecionarSubcategoria(categoria.codigo)}
                      />
                      <span>{categoria.nome}</span>
                    </label>
                  ))}
                  {carregandoSubcategorias && <span>Carregando...</span>}
                  {!carregandoSubcategorias && subcategorias.length === 0 && (
                    <p className="filter-empty">Nenhuma subcategoria cadastrada.</p>
                  )}
                </div>
              </>
            )}
          </div>

          {administrador && (
            <div className="filter-group">
                <span className="filter-group__title">Visibilidade</span>
                <label className="filter-checkbox">
                  <input
                    type="checkbox"
                    checked={apenasVisiveis}
                    onChange={(event) => {
                      setApenasVisiveis(event.target.checked);
                      setPagina(0);
                    }}
                  />
                  <span>Apenas visíveis</span>
                </label>
            </div>
          )}

          <div className="filter-group">
            <span className="filter-group__title">Preço nesta página</span>
            <div className="price-grid">
              <input
                type="number"
                placeholder="De"
                value={priceDraft.min}
                onChange={(event) => setPriceDraft((prev) => ({ ...prev, min: event.target.value }))}
              />
              <input
                type="number"
                placeholder="Até"
                value={priceDraft.max}
                onChange={(event) => setPriceDraft((prev) => ({ ...prev, max: event.target.value }))}
              />
            </div>

            <div className="filter-actions">
              <button className="filter-button filter-button--primary" type="button" onClick={handleApplyPrice}>
                Aplicar
              </button>
              <button className="filter-button" type="button" onClick={handleClearFilters}>
                Limpar
              </button>
            </div>
          </div>
        </aside>

        <section className="catalog-results">
          {administrador && (
            <div className="catalog-bulk-actions" aria-label="Ações em lote">
              <label className="filter-checkbox">
                <input
                  type="checkbox"
                  checked={paginaTodaMarcada}
                  onChange={(event) => alterarSelecaoPagina(event.target.checked)}
                  disabled={produtosSelecionaveisNaPagina.length === 0
                    || alterarVisibilidade.isPending
                    || excluirProdutos.isPending}
                />
                <span>Selecionar página</span>
              </label>
              <strong>{produtosMarcados.size} selecionado(s)</strong>
              <button
                className="btn btn-success"
                type="button"
                onClick={() => alterarVisibilidadeDosMarcados(true)}
                disabled={produtosMarcados.size === 0
                  || alterarVisibilidade.isPending
                  || excluirProdutos.isPending}
              >
                {alterarVisibilidade.isPending && alterarVisibilidade.variables?.exibirNoSite
                  ? 'Exibindo...'
                  : 'Exibir todos selecionados'}
              </button>
              <button
                className="btn btn-danger"
                type="button"
                onClick={() => alterarVisibilidadeDosMarcados(false)}
                disabled={produtosMarcados.size === 0
                  || alterarVisibilidade.isPending
                  || excluirProdutos.isPending}
              >
                {alterarVisibilidade.isPending && alterarVisibilidade.variables?.exibirNoSite === false
                  ? 'Ocultando...'
                  : 'Ocultar todos selecionados'}
              </button>
              <button
                className="btn btn-outline-danger"
                type="button"
                onClick={excluirMarcados}
                disabled={produtosMarcados.size === 0
                  || alterarVisibilidade.isPending
                  || excluirProdutos.isPending}
              >
                {excluirProdutos.isPending ? 'Excluindo...' : 'Excluir todos selecionados'}
              </button>
              {produtosMarcados.size > 0 && (
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={() => setProdutosMarcados(new Set())}
                  disabled={alterarVisibilidade.isPending || excluirProdutos.isPending}
                >
                  Limpar seleção
                </button>
              )}
              {alterarVisibilidade.isSuccess && (
                <span className="catalog-bulk-actions__success">
                  {alterarVisibilidade.data.produtosAlterados} produto(s) passaram a ser{' '}
                  {alterarVisibilidade.data.exibirNoSite ? 'exibidos' : 'ocultados'}.
                </span>
              )}
              {alterarVisibilidade.isError && (
                <span className="catalog-bulk-actions__error">
                  Não foi possível {alterarVisibilidade.variables?.exibirNoSite
                    ? 'exibir'
                    : 'ocultar'} os produtos selecionados.
                </span>
              )}
              {excluirProdutos.isSuccess && (
                <span className="catalog-bulk-actions__success">
                  {excluirProdutos.data.produtosExcluidos} produto(s) foram excluídos.
                </span>
              )}
              {excluirProdutos.isError && (
                <span className="catalog-bulk-actions__error">
                  Não foi possível excluir os produtos selecionados.
                </span>
              )}
            </div>
          )}
          <div className="catalog-results__bar">
            <span>
              {administrador
                ? apenasVisiveis
                  ? 'Visão administrativa: apenas produtos visíveis'
                  : 'Visão administrativa: produtos visíveis primeiro'
                : 'Produtos disponíveis no site'}
            </span>
            <span className="catalog-results__hint">
              Página {(paginacao?.number ?? 0) + 1} de {Math.max(paginacao?.totalPages ?? 1, 1)}
            </span>
          </div>

          {filteredProducts.length === 0 ? (
            <div className="catalog-empty">
              {busca
                ? `Nenhum produto encontrado para “${busca}”.`
                : 'Nenhum produto encontrado com os filtros atuais.'}
            </div>
          ) : (
            <div className="catalog-grid">
              {filteredProducts.map((dadosProdutos, index) => (
                <div
                  className="catalog-grid__item"
                  key={'codigoSantri' in dadosProdutos
                    ? dadosProdutos.codigoSantri
                    : `${dadosProdutos.categoriaCodigo}-${dadosProdutos.nomeExibidoSite}-${index}`}
                >
                  <Produtos
                    {...dadosProdutos}
                    limiteDestaquesAtingido={produtosSelecionados.length >= 3}
                    carregandoLimiteDestaques={carregandoProdutosSelecionados}
                    selecionado={'codigoSantri' in dadosProdutos
                      && produtosMarcados.has(dadosProdutos.codigoSantri)}
                    onSelecionadoChange={administrador ? alterarSelecaoProduto : undefined}
                  />
                </div>
              ))}
            </div>
          )}

          {(paginacao?.totalPages ?? 0) > 1 && (
            <nav className="catalog-pagination" aria-label="Paginação de produtos">
              <button
                type="button"
                onClick={() => setPagina(0)}
                disabled={paginacao?.first}
              >
                <i className="bi bi-chevron-bar-left" />
                Primeira
              </button>
              <button
                type="button"
                onClick={() => setPagina((atual) => Math.max(0, atual - 1))}
                disabled={paginacao?.first}
              >
                <i className="bi bi-chevron-left" />
                Anterior
              </button>
              <form
                className="catalog-pagination__current"
                onSubmit={(event) => {
                  event.preventDefault();
                  irParaPagina();
                }}
              >
                <label htmlFor="pagina-catalogo">Página</label>
                <input
                  id="pagina-catalogo"
                  type="number"
                  min="1"
                  max={totalPaginas}
                  inputMode="numeric"
                  value={paginaDigitada}
                  onChange={(event) => setPaginaDigitada(event.target.value)}
                  onBlur={irParaPagina}
                  aria-label={`Ir para uma página entre 1 e ${totalPaginas}`}
                />
                <span>de {totalPaginas}</span>
              </form>
              <button
                type="button"
                onClick={() => setPagina((atual) => atual + 1)}
                disabled={paginacao?.last}
              >
                Próxima
                <i className="bi bi-chevron-right" />
              </button>
              <button
                type="button"
                onClick={() => setPagina(totalPaginas - 1)}
                disabled={paginacao?.last}
              >
                Última
                <i className="bi bi-chevron-bar-right" />
              </button>
            </nav>
          )}
        </section>
      </section>

      {administrador && (
        <div className="admin-actions">
          <Link className="btn btn-outline-primary" to="/admin/vitrine-loja">
            <i className="bi bi-display me-2" />
            Gerenciar vitrine da loja
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/vitrines-home">
            <i className="bi bi-layout-text-window-reverse me-2" />
            Gerenciar vitrines
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-produtos">
            <i className="bi bi-file-earmark-arrow-up me-2" />
            Importar relação de produtos
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-promocoes">
            <i className="bi bi-tags me-2" />
            Importar promoções
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-estoque">
            <i className="bi bi-boxes me-2" />
            Atualizar estoque
          </Link>
        </div>
      )}
    </div>
  );
}

export default ProdutoList;
